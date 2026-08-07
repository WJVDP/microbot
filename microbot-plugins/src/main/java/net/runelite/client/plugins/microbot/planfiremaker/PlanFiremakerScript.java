package net.runelite.client.plugins.microbot.planfiremaker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.statemachine.StateMachineScript;
import net.runelite.client.plugins.microbot.statemachine.Transition;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Singleton;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@Singleton
@Slf4j
public class PlanFiremakerScript
        extends StateMachineScript<PlanFiremakerScript.State>
{
    private static final int ACTION_TIMEOUT_MS = 5_000;
    private static final int WALK_TIMEOUT_MS = 12_000;
    private static final int LOOP_DELAY_MS = 600;
    private static final int MAX_FAILURES = 3;
    private static final long ERROR_RETRY_DELAY_MS = 5_000L;
    private static final int[] LANE_OFFSETS = {0, 1, -1, 2, -2, 3, -3};

    enum State
    {
        CHECK_REQUIREMENTS,
        BANK,
        WALK_TO_LINE,
        LIGHT_LOGS,
        REPOSITION,
        COMPLETE,
        EXHAUSTED,
        ERROR
    }

    private final PlanFiremakerPlanner planner = new PlanFiremakerPlanner();

    private PlanFiremakerConfig config;
    private int currentXp;
    private int currentLevel = 1;
    private int bankEpoch;
    private boolean bankSnapshotKnown;
    private boolean bankPrepared;
    private boolean resourcesExhausted;
    private int bankFailures;
    private int movementFailures;
    private int lightingFailures;
    private int laneIndex = -1;
    private int exhaustedSignature;
    private int exhaustedBankEpoch;
    private long retryAt;
    private String errorMessage;
    private WorldPoint playerLocation;
    private WorldPoint bankTile;
    private WorldPoint lineStart;
    private EnumMap<PlanFiremakerLogType, Integer> bankQuantities = emptyQuantities();
    private EnumMap<PlanFiremakerLogType, Integer> inventoryQuantities = emptyQuantities();
    private EnumMap<PlanFiremakerLogType, Integer> availableQuantities = emptyQuantities();
    private PlanFiremakerPlan plan;
    private volatile PlanFiremakerSnapshot statusSnapshot = PlanFiremakerSnapshot.waiting();

    @Override
    protected State initialState()
    {
        return State.CHECK_REQUIREMENTS;
    }

    @Override
    protected List<Transition<State>> defineTransitions()
    {
        return List.of(
                Transition.<State>from(State.CHECK_REQUIREMENTS)
                        .when(this::targetReached, "targetReached()")
                        .because("The configured Firemaking target has been reached")
                        .goTo(State.COMPLETE),
                Transition.<State>from(State.CHECK_REQUIREMENTS)
                        .when(() -> !targetReached(), "!targetReached()")
                        .because("A verified bank snapshot and burn inventory are required")
                        .goTo(State.BANK),

                Transition.<State>from(State.BANK)
                        .when(this::targetReached, "targetReached()")
                        .because("The configured Firemaking target has been reached")
                        .goTo(State.COMPLETE),
                Transition.<State>from(State.BANK)
                        .when(() -> errorMessage != null, "errorMessage != null")
                        .because("Bank preparation failed repeatedly")
                        .goTo(State.ERROR),
                Transition.<State>from(State.BANK)
                        .when(() -> resourcesExhausted, "resourcesExhausted")
                        .because("No permitted logs can be burned with the available levels and supplies")
                        .goTo(State.EXHAUSTED),
                Transition.<State>from(State.BANK)
                        .when(() -> bankPrepared && inventoryLogType() != null,
                                "bankPrepared && inventoryLogType() != null")
                        .because("A tinderbox and burnable logs are ready")
                        .goTo(State.WALK_TO_LINE),

                Transition.<State>from(State.WALK_TO_LINE)
                        .when(this::targetReached, "targetReached()")
                        .because("The configured Firemaking target has been reached")
                        .goTo(State.COMPLETE),
                Transition.<State>from(State.WALK_TO_LINE)
                        .when(() -> inventoryLogType() == null, "inventoryLogType() == null")
                        .because("No burnable logs remain in the inventory")
                        .goTo(State.BANK),
                Transition.<State>from(State.WALK_TO_LINE)
                        .when(() -> errorMessage != null, "errorMessage != null")
                        .because("The fire line could not be reached")
                        .goTo(State.ERROR),
                Transition.<State>from(State.WALK_TO_LINE)
                        .when(this::atLineStart, "atLineStart()")
                        .because("The player reached the start of the fire line")
                        .goTo(State.LIGHT_LOGS),

                Transition.<State>from(State.LIGHT_LOGS)
                        .when(this::targetReached, "targetReached()")
                        .because("The configured Firemaking target has been reached")
                        .goTo(State.COMPLETE),
                Transition.<State>from(State.LIGHT_LOGS)
                        .when(this::shouldSwitchLogTier, "shouldSwitchLogTier()")
                        .because("A more efficient planned log tier was unlocked")
                        .goTo(State.BANK),
                Transition.<State>from(State.LIGHT_LOGS)
                        .when(() -> inventoryLogType() == null, "inventoryLogType() == null")
                        .because("The current inventory of logs has been burned")
                        .goTo(State.BANK),
                Transition.<State>from(State.LIGHT_LOGS)
                        .when(() -> lightingFailures >= MAX_FAILURES,
                                "lightingFailures >= MAX_FAILURES")
                        .because("The current fire line is obstructed")
                        .goTo(State.REPOSITION),

                Transition.<State>from(State.REPOSITION)
                        .when(() -> inventoryLogType() == null, "inventoryLogType() == null")
                        .because("No burnable logs remain in the inventory")
                        .goTo(State.BANK),
                Transition.<State>from(State.REPOSITION)
                        .when(() -> errorMessage != null, "errorMessage != null")
                        .because("No usable nearby fire line could be reached")
                        .goTo(State.ERROR),
                Transition.<State>from(State.REPOSITION)
                        .when(this::atLineStart, "atLineStart()")
                        .because("An alternate fire line was reached")
                        .goTo(State.LIGHT_LOGS),

                Transition.<State>from(State.COMPLETE)
                        .when(() -> !targetReached(), "!targetReached()")
                        .because("The configured target was raised")
                        .goTo(State.CHECK_REQUIREMENTS),
                Transition.<State>from(State.EXHAUSTED)
                        .when(this::targetReached, "targetReached()")
                        .because("The configured Firemaking target has been reached outside the script")
                        .goTo(State.COMPLETE),
                Transition.<State>from(State.EXHAUSTED)
                        .when(this::hasBurnablePlan, "hasBurnablePlan()")
                        .because("A Firemaking level change unlocked an available log tier")
                        .goTo(State.CHECK_REQUIREMENTS),
                Transition.<State>from(State.EXHAUSTED)
                        .when(this::resourcesChangedSinceExhaustion,
                                "resourcesChangedSinceExhaustion()")
                        .because("The available logs or bank snapshot changed")
                        .goTo(State.CHECK_REQUIREMENTS),
                Transition.<State>from(State.ERROR)
                        .when(() -> System.currentTimeMillis() >= retryAt,
                                "System.currentTimeMillis() >= retryAt")
                        .because("The bounded recovery delay elapsed")
                        .goTo(State.CHECK_REQUIREMENTS)
        );
    }

    @Override
    protected void onState(State state)
    {
        switch (state)
        {
            case CHECK_REQUIREMENTS:
                clearTransientFailures();
                Microbot.status = "Checking Firemaker requirements";
                break;
            case BANK:
                prepareBankInventory();
                break;
            case WALK_TO_LINE:
                walkToLine(false);
                break;
            case LIGHT_LOGS:
                lightNextLog();
                break;
            case REPOSITION:
                walkToLine(true);
                break;
            case COMPLETE:
                Microbot.status = "Firemaking target reached";
                break;
            case EXHAUSTED:
                Microbot.status = exhaustionMessage();
                break;
            case ERROR:
                Microbot.status = errorMessage == null
                        ? "Firemaker recovering"
                        : errorMessage;
                break;
            default:
                throw new IllegalStateException("Unhandled state: " + state);
        }
    }

    private void prepareBankInventory()
    {
        bankPrepared = false;
        resourcesExhausted = false;

        if (bankTile != null && playerLocation != null && playerLocation.distanceTo2D(bankTile) > 5)
        {
            Microbot.status = "Returning to the bank";
            if (!Rs2Walker.walkTo(bankTile, 2))
            {
                recordBankFailure("Could not return to the saved banking tile");
            }
            return;
        }

        boolean wasOpen = Rs2Bank.isOpen();
        int epochBeforeOpen = Rs2Bank.getBankLiveEpoch();
        Microbot.status = "Opening bank for Firemaking supplies";
        if (!Rs2Bank.openBank()
                || !Rs2Bank.verifyBankMirrorAfterOpen(wasOpen, epochBeforeOpen))
        {
            Rs2Bank.closeBank();
            recordBankFailure("Start beside an outdoor bank with a clear line to the east");
            return;
        }

        bankFailures = 0;
        bankSnapshotKnown = true;
        bankEpoch = Rs2Bank.getBankLiveEpoch();
        if (bankTile == null)
        {
            bankTile = Rs2Player.getWorldLocation();
        }

        if (!Rs2Bank.setWithdrawAsItem())
        {
            recordBankFailure("Could not set the bank to withdraw items");
            return;
        }

        if (!Rs2Inventory.isEmpty())
        {
            int epochBeforeDeposit = Rs2Bank.getBankLiveEpoch();
            if (!Rs2Bank.depositAll()
                    || !Rs2Bank.syncBankInventoryAfterChange(epochBeforeDeposit))
            {
                recordBankFailure("Could not synchronize deposited inventory items");
                return;
            }
        }

        if (Rs2Bank.count(ItemID.TINDERBOX) <= 0)
        {
            setError("A tinderbox is required in the bank or inventory");
            return;
        }

        int epochBeforeTinderbox = Rs2Bank.getBankLiveEpoch();
        if (!Rs2Bank.withdrawX(ItemID.TINDERBOX, 1)
                || !sleepUntil(() -> Rs2Inventory.hasItem(ItemID.TINDERBOX), ACTION_TIMEOUT_MS)
                || !Rs2Bank.syncBankInventoryAfterChange(epochBeforeTinderbox))
        {
            recordBankFailure("Could not withdraw a tinderbox");
            return;
        }

        refreshRuntimeState();
        if (targetReached())
        {
            Rs2Bank.closeBank();
            return;
        }

        PlanFiremakerLogType next = plan == null ? null : plan.nextLogType();
        if (next == null)
        {
            resourcesExhausted = true;
            exhaustedSignature = availableSignature();
            exhaustedBankEpoch = Rs2Bank.getBankLiveEpoch();
            Rs2Bank.closeBank();
            return;
        }

        int amount = Math.min(
                configuredLineLength(),
                Math.min(Rs2Inventory.capacity() - Rs2Inventory.count(),
                        Rs2Bank.count(next.getItemId())));
        if (amount <= 0)
        {
            resourcesExhausted = true;
            exhaustedSignature = availableSignature();
            exhaustedBankEpoch = Rs2Bank.getBankLiveEpoch();
            Rs2Bank.closeBank();
            return;
        }

        int epochBeforeLogs = Rs2Bank.getBankLiveEpoch();
        if (!Rs2Bank.withdrawX(next.getItemId(), amount)
                || !sleepUntil(() -> Rs2Inventory.itemQuantity(next.getItemId()) > 0,
                        ACTION_TIMEOUT_MS)
                || !Rs2Bank.syncBankInventoryAfterChange(epochBeforeLogs))
        {
            recordBankFailure("Could not withdraw " + next);
            return;
        }

        chooseNextLane();
        if (!Rs2Bank.closeBank())
        {
            recordBankFailure("Could not close the bank");
            return;
        }

        bankPrepared = true;
        movementFailures = 0;
        lightingFailures = 0;
        Microbot.status = "Withdrew " + amount + " " + next;
        refreshRuntimeState();
    }

    private void walkToLine(boolean chooseAlternateLane)
    {
        if (bankTile == null)
        {
            setError("No banking tile has been captured");
            return;
        }
        if (chooseAlternateLane)
        {
            chooseNextLane();
            lightingFailures = 0;
        }
        if (lineStart == null)
        {
            chooseNextLane();
        }
        if (atLineStart())
        {
            movementFailures = 0;
            return;
        }

        Microbot.status = chooseAlternateLane
                ? "Moving to an alternate fire line"
                : "Walking to the fire line";
        boolean walked = Rs2Walker.walkTo(lineStart, 0);
        boolean arrived = walked && sleepUntil(() -> {
            WorldPoint location = Rs2Player.getWorldLocation();
            return location != null && location.distanceTo2D(lineStart) <= 1;
        }, WALK_TIMEOUT_MS);
        if (arrived)
        {
            movementFailures = 0;
            playerLocation = Rs2Player.getWorldLocation();
            return;
        }

        movementFailures++;
        if (movementFailures >= MAX_FAILURES)
        {
            setError("No clear east-to-west fire line was reachable from this bank");
        }
    }

    private void lightNextLog()
    {
        PlanFiremakerLogType logType = inventoryLogType();
        if (logType == null)
        {
            return;
        }

        WorldPoint location = Rs2Player.getWorldLocation();
        if (location == null)
        {
            lightingFailures++;
            return;
        }

        boolean standingOnFire = Microbot.getRs2TileObjectCache().query()
                .fromWorldView()
                .withName("Fire")
                .where(object -> location.equals(object.getWorldLocation()))
                .firstOnClientThread() != null;
        if (standingOnFire)
        {
            lightingFailures = MAX_FAILURES;
            Microbot.status = "Fire line obstructed";
            return;
        }

        int beforeXp = currentFiremakingXp();
        Microbot.status = "Burning " + logType;
        boolean interacted = Rs2Inventory.hasItem(ItemID.TINDERBOX)
                && Rs2Inventory.hasItem(logType.getItemId())
                && Rs2Inventory.combine(ItemID.TINDERBOX, logType.getItemId());
        boolean lit = interacted && sleepUntil(
                () -> currentFiremakingXp() > beforeXp,
                ACTION_TIMEOUT_MS);

        if (lit)
        {
            lightingFailures = 0;
            refreshRuntimeState();
        }
        else
        {
            lightingFailures++;
        }
    }

    private void refreshRuntimeState()
    {
        currentXp = currentFiremakingXp();
        currentLevel = Experience.getLevelForXp(Math.max(0, currentXp));
        playerLocation = Rs2Player.getWorldLocation();
        bankEpoch = Rs2Bank.getBankLiveEpoch();
        bankSnapshotKnown = hasVerifiedBankSnapshot(bankEpoch);

        bankQuantities = emptyQuantities();
        inventoryQuantities = emptyQuantities();
        for (PlanFiremakerLogType type : PlanFiremakerLogType.values())
        {
            bankQuantities.put(type,
                    bankSnapshotKnown ? Rs2Bank.count(type.getItemId()) : 0);
            inventoryQuantities.put(type, Rs2Inventory.itemQuantity(type.getItemId()));
        }
        availableQuantities = PlanFiremakerPlanner.mergeQuantities(
                bankQuantities, inventoryQuantities);
        plan = planner.plan(
                currentXp,
                configuredTargetLevel(),
                availableQuantities,
                configuredMaximumLogType());
        publishStatusSnapshot();
    }

    private void publishStatusSnapshot()
    {
        State state = getCurrentState();
        statusSnapshot = new PlanFiremakerSnapshot(
                state == null ? "STARTING" : state.name(),
                Microbot.status,
                bankSnapshotKnown,
                currentXp,
                currentLevel,
                availableQuantities,
                plan);
    }

    private boolean targetReached()
    {
        return currentXp >= Experience.getXpForLevel(configuredTargetLevel());
    }

    private PlanFiremakerLogType inventoryLogType()
    {
        return PlanFiremakerLogType.bestUnlocked(
                currentLevel,
                configuredMaximumLogType(),
                inventoryQuantities).orElse(null);
    }

    private boolean atLineStart()
    {
        return playerLocation != null
                && lineStart != null
                && playerLocation.distanceTo2D(lineStart) <= 1;
    }

    private boolean shouldSwitchLogTier()
    {
        PlanFiremakerLogType inventoryType = inventoryLogType();
        PlanFiremakerLogType plannedType = plan == null ? null : plan.nextLogType();
        return inventoryType != null && plannedType != null && inventoryType != plannedType;
    }

    private boolean hasBurnablePlan()
    {
        return plan != null && plan.nextLogType() != null;
    }

    private int currentFiremakingXp()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(
                () -> Microbot.getClient().getSkillExperience(Skill.FIREMAKING))
                .orElse(currentXp);
    }

    private boolean resourcesChangedSinceExhaustion()
    {
        return availableSignature() != exhaustedSignature
                || bankEpoch != exhaustedBankEpoch;
    }

    private int availableSignature()
    {
        int result = availableQuantities.hashCode();
        result = 31 * result + configuredMaximumLogType().hashCode();
        result = 31 * result + configuredTargetLevel();
        return result;
    }

    private void chooseNextLane()
    {
        laneIndex = (laneIndex + 1) % LANE_OFFSETS.length;
        int offset = LANE_OFFSETS[laneIndex];
        lineStart = new WorldPoint(
                bankTile.getX() + configuredLineLength(),
                bankTile.getY() + offset,
                bankTile.getPlane());
    }

    private void recordBankFailure(String message)
    {
        bankFailures++;
        Microbot.status = message;
        if (bankFailures >= MAX_FAILURES)
        {
            setError(message);
        }
    }

    private void setError(String message)
    {
        errorMessage = message;
        retryAt = System.currentTimeMillis() + ERROR_RETRY_DELAY_MS;
        Microbot.status = message;
    }

    private void clearTransientFailures()
    {
        errorMessage = null;
        retryAt = 0L;
        bankFailures = 0;
        movementFailures = 0;
        lightingFailures = 0;
        bankPrepared = false;
        resourcesExhausted = false;
    }

    private String exhaustionMessage()
    {
        if (plan == null)
        {
            return "Open the bank to calculate available Firemaking levels";
        }
        int locked = plan.getLockedAtEnd().values().stream().mapToInt(Integer::intValue).sum();
        if (locked > 0)
        {
            return "Logs exhausted at level " + plan.getProjectedLevel()
                    + "; " + locked + " higher-tier logs remain locked";
        }
        return "Permitted logs exhausted; projected level " + plan.getProjectedLevel();
    }

    private int configuredTargetLevel()
    {
        return config == null
                ? 50
                : Math.max(1, Math.min(config.targetLevel(), Experience.MAX_REAL_LEVEL));
    }

    private int configuredLineLength()
    {
        return config == null ? 27 : Math.max(5, Math.min(config.lineLength(), 27));
    }

    private PlanFiremakerLogType configuredMaximumLogType()
    {
        return config == null || config.maximumLogType() == null
                ? PlanFiremakerLogType.WILLOW
                : config.maximumLogType();
    }

    private static EnumMap<PlanFiremakerLogType, Integer> emptyQuantities()
    {
        EnumMap<PlanFiremakerLogType, Integer> result =
                new EnumMap<>(PlanFiremakerLogType.class);
        for (PlanFiremakerLogType type : PlanFiremakerLogType.values())
        {
            result.put(type, 0);
        }
        return result;
    }

    static boolean hasVerifiedBankSnapshot(int liveEpoch)
    {
        return liveEpoch > 0;
    }

    PlanFiremakerSnapshot getStatusSnapshot()
    {
        return statusSnapshot;
    }

    @Override
    protected State onError(State state, Exception error)
    {
        if (Thread.currentThread().isInterrupted())
        {
            Thread.currentThread().interrupt();
            return state;
        }
        log.error("[PlanFiremaker] State {} failed: {}", state, error.getMessage());
        setError("Firemaker failed in state " + state);
        return State.ERROR;
    }

    public synchronized boolean run(PlanFiremakerConfig config)
    {
        if (isRunning())
        {
            return true;
        }
        this.config = config;
        resetRunState();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try
            {
                if (Microbot.isLoggedIn())
                {
                    refreshRuntimeState();
                    step();
                    publishStatusSnapshot();
                }
            }
            catch (Exception error)
            {
                if (Thread.currentThread().isInterrupted())
                {
                    Thread.currentThread().interrupt();
                    return;
                }
                log.error("[PlanFiremaker] Loop failed: {}", error.getMessage());
            }
        }, 0, LOOP_DELAY_MS, TimeUnit.MILLISECONDS);
        return true;
    }

    private void resetRunState()
    {
        clearTransientFailures();
        bankTile = null;
        lineStart = null;
        laneIndex = -1;
        bankEpoch = 0;
        bankSnapshotKnown = false;
        bankQuantities = emptyQuantities();
        inventoryQuantities = emptyQuantities();
        availableQuantities = emptyQuantities();
        plan = null;
        exhaustedSignature = 0;
        exhaustedBankEpoch = 0;
        statusSnapshot = PlanFiremakerSnapshot.waiting();
    }
}
