package net.runelite.client.plugins.microbot.planquesting;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questhelper.QuestHelperPlugin;
import net.runelite.client.plugins.microbot.questhelper.automation.QuestAutomationLease;
import net.runelite.client.plugins.microbot.questhelper.automation.QuestRisk;
import net.runelite.client.plugins.microbot.questhelper.automation.QuestRiskPolicy;
import net.runelite.client.plugins.microbot.questhelper.automation.QuestStepSnapshot;
import net.runelite.client.plugins.microbot.questhelper.automation.QuestStepSnapshotFactory;
import net.runelite.client.plugins.microbot.statemachine.StateMachineScript;
import net.runelite.client.plugins.microbot.statemachine.Transition;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class PlanQuestingScript extends StateMachineScript<PlanQuestingScript.State>
{
    private static final long PROGRESS_TIMEOUT_MS = 10_000L;

    enum State
    {
        PRECHECK,
        CLASSIFY_STEP,
        AUTOMATION_READY,
        MANUAL_REQUIRED,
        COMPLETE,
        ERROR
    }

    private final QuestRiskPolicy riskPolicy = new QuestRiskPolicy();
    private final QuestStepSnapshotFactory snapshotFactory = new QuestStepSnapshotFactory();

    private PlanQuestingConfig config;
    private QuestHelperPlugin questHelperPlugin;
    private QuestStepSnapshot snapshot;
    private QuestRisk risk = QuestRisk.MANUAL_REQUIRED;
    private boolean questSelected;
    private boolean completed;
    private boolean resumeRequested;
    private boolean contextChanged;
    private boolean noProgressExhausted;
    private long verificationDeadline;
    private int attempts;
    private boolean leaseHeld;
    private String error;

    @Getter
    private volatile PlanQuestingStatus status = PlanQuestingStatus.waiting();

    @Override
    protected State initialState()
    {
        return State.PRECHECK;
    }

    @Override
    protected List<Transition<State>> defineTransitions()
    {
        return List.of(
                Transition.<State>from(State.PRECHECK)
                        .when(() -> error != null, "error != null")
                        .because("Questing prerequisites could not be established")
                        .goTo(State.ERROR),
                Transition.<State>from(State.PRECHECK)
                        .when(() -> completed, "completed")
                        .because("Quest Helper reports the selected quest complete")
                        .goTo(State.COMPLETE),
                Transition.<State>from(State.PRECHECK)
                        .when(() -> questSelected && snapshot != null,
                                "questSelected && snapshot != null")
                        .because("A fresh active Quest Helper step is available")
                        .goTo(State.CLASSIFY_STEP),

                Transition.<State>from(State.CLASSIFY_STEP)
                        .when(() -> risk == QuestRisk.MANUAL_REQUIRED,
                                "risk == MANUAL_REQUIRED")
                        .because("The reviewed safety catalog requires player control")
                        .goTo(State.MANUAL_REQUIRED),
                Transition.<State>from(State.CLASSIFY_STEP)
                        .when(() -> risk != QuestRisk.MANUAL_REQUIRED,
                                "risk != MANUAL_REQUIRED")
                        .because("The active step is eligible for deterministic execution")
                        .goTo(State.AUTOMATION_READY),

                Transition.<State>from(State.AUTOMATION_READY)
                        .when(() -> completed, "completed")
                        .because("Quest Helper reports the selected quest complete")
                        .goTo(State.COMPLETE),
                Transition.<State>from(State.AUTOMATION_READY)
                        .when(() -> noProgressExhausted, "noProgressExhausted")
                        .because("Bounded deterministic attempts made no semantic progress")
                        .goTo(State.MANUAL_REQUIRED),
                Transition.<State>from(State.AUTOMATION_READY)
                        .when(() -> contextChanged, "contextChanged")
                        .because("Quest Helper advanced to a different context")
                        .goTo(State.CLASSIFY_STEP),

                Transition.<State>from(State.MANUAL_REQUIRED)
                        .when(() -> resumeRequested, "resumeRequested")
                        .because("The player requested a fresh safety evaluation")
                        .goTo(State.PRECHECK)
        );
    }

    @Override
    protected void onState(State state)
    {
        switch (state)
        {
            case PRECHECK:
                precheck();
                break;
            case CLASSIFY_STEP:
                classifyStep();
                break;
            case AUTOMATION_READY:
                observeSafeStep();
                break;
            case MANUAL_REQUIRED:
                waitForPlayer();
                break;
            case COMPLETE:
                publish("Quest complete", "NONE", "The selected quest is complete");
                break;
            case ERROR:
                publish("Stopped", "NONE", error == null ? "Unknown error" : error);
                break;
            default:
                throw new IllegalStateException("Unhandled state: " + state);
        }
    }

    @Override
    protected void onTransition(State from, State to, String reason)
    {
        super.onTransition(from, to, reason);
        if (to == State.MANUAL_REQUIRED)
        {
            Rs2Walker.clearWalkingRoute("plan-questing:manual-handoff");
            if (Microbot.getNotifier() != null)
            {
                Microbot.getNotifier().notify("Plan Questing needs manual intervention");
            }
        }
    }

    private void precheck()
    {
        resumeRequested = false;
        if (!leaseHeld)
        {
            leaseHeld = QuestAutomationLease.acquire(QuestAutomationLease.Owner.PLAN_QUESTING);
            if (!leaseHeld)
            {
                publish("Waiting for quest control", "NONE",
                        "Another quest controller is finishing its current action");
                return;
            }
        }

        questHelperPlugin = Microbot.getPlugin(QuestHelperPlugin.class);
        if (questHelperPlugin == null)
        {
            error = "Quest Helper plugin is unavailable";
            return;
        }

        String selectedName = Microbot.getClientThread().runOnClientThreadOptional(() ->
                questHelperPlugin.getSelectedQuest() == null ? null :
                        questHelperPlugin.getSelectedQuest().getQuest().getName()).orElse(null);
        String requestedName = config.pilotQuest().getDisplayName();
        if (!requestedName.equals(selectedName))
        {
            questSelected = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    questHelperPlugin.startQuestHelper(requestedName)).orElse(false);
        }
        else
        {
            questSelected = true;
        }

        completed = isQuestCompleted();
        snapshot = snapshotFactory.capture(questHelperPlugin);
        if (!completed && questSelected && snapshot == null)
        {
            publish("Waiting for Quest Helper", "AUTOMATION",
                    "No active step is available yet");
        }
    }

    private void classifyStep()
    {
        risk = riskPolicy.classify(snapshot);
        contextChanged = false;
        noProgressExhausted = false;
        verificationDeadline = 0;
        attempts = 0;
        publish("Classifying step", "AUTOMATION", risk.name());
    }

    private void observeSafeStep()
    {
        QuestStepSnapshot before = snapshot;
        refreshSnapshot();
        contextChanged = before != null && snapshot != null &&
                before.getContextRevision() != snapshot.getContextRevision();
        if (contextChanged || completed)
        {
            publish("Progress verified", "AUTOMATION", "Quest Helper advanced");
            return;
        }

        long now = System.currentTimeMillis();
        if (verificationDeadline > 0 && now < verificationDeadline)
        {
            publish("Verifying progress", "AUTOMATION",
                    "Attempt " + attempts + " of " + config.maxAttempts());
            return;
        }
        if (verificationDeadline > 0)
        {
            verificationDeadline = 0;
            if (attempts >= config.maxAttempts())
            {
                noProgressExhausted = true;
                publish("No progress", "AUTOMATION",
                        "Deterministic recovery exhausted");
                return;
            }
            Rs2Walker.clearWalkingRoute("plan-questing:no-progress-retry");
        }

        boolean issued = questHelperPlugin.getQuestScript().executeActiveStepOnce();
        attempts++;
        verificationDeadline = now + PROGRESS_TIMEOUT_MS;
        publish(issued ? "Executing Quest Helper step" : "Step was not actionable",
                "AUTOMATION", "Attempt " + attempts + " of " + config.maxAttempts());
    }

    private void waitForPlayer()
    {
        refreshSnapshot();
        publish("Manual intervention required", "PLAYER",
                manualReason(snapshot));
    }

    private void refreshSnapshot()
    {
        completed = isQuestCompleted();
        QuestStepSnapshot latest = snapshotFactory.capture(questHelperPlugin);
        if (latest != null)
        {
            snapshot = latest;
        }
    }

    private boolean isQuestCompleted()
    {
        return questHelperPlugin != null && Microbot.getClientThread()
                .runOnClientThreadOptional(() -> questHelperPlugin.getSelectedQuest() != null &&
                        questHelperPlugin.getSelectedQuest().isCompleted())
                .orElse(false);
    }

    private String manualReason(QuestStepSnapshot current)
    {
        if (current == null)
        {
            return "The active step cannot be classified safely";
        }
        if (QuestRiskPolicy.VAMPYRE_SLAYER.equals(current.getStepKey().getQuestName()))
        {
            return "Complete or exit the Count Draynor encounter, then press Resume";
        }
        if (noProgressExhausted)
        {
            return "Deterministic recovery made no progress; complete the step and press Resume";
        }
        return "Complete the unsupported mirror sequence, then press Resume";
    }

    private void publish(String action, String authority, String reason)
    {
        String quest = config == null ? "None" : config.pilotQuest().getDisplayName();
        String step = snapshot == null ? "No active step" : snapshot.getStepKey().toString();
        long revision = snapshot == null ? 0 : snapshot.getContextRevision();
        status = new PlanQuestingStatus(action, quest, authority, risk.name(), step,
                revision, reason == null ? "" : reason);
        Microbot.status = action;
    }

    public void requestResume()
    {
        resumeRequested = true;
    }

    public boolean run(PlanQuestingConfig config)
    {
        this.config = config;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try
            {
                if (Microbot.isLoggedIn())
                {
                    step();
                }
            }
            catch (Exception exception)
            {
                error = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                log.error("[PlanQuesting] Loop failed: {}", exception.getMessage());
            }
        }, 0, config.loopDelay(), TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown()
    {
        super.shutdown();
        Rs2Walker.clearWalkingRoute("plan-questing:shutdown");
        QuestAutomationLease.release(QuestAutomationLease.Owner.PLAN_QUESTING);
        leaseHeld = false;
        questSelected = false;
        completed = false;
        resumeRequested = false;
        contextChanged = false;
        noProgressExhausted = false;
        verificationDeadline = 0;
        attempts = 0;
        snapshot = null;
        snapshotFactory.reset();
        status = PlanQuestingStatus.waiting();
    }
}
