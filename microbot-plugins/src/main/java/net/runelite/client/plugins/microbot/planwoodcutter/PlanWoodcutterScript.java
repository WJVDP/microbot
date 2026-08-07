package net.runelite.client.plugins.microbot.planwoodcutter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.statemachine.StateMachineScript;
import net.runelite.client.plugins.microbot.statemachine.Transition;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PlanWoodcutterScript
        extends StateMachineScript<PlanWoodcutterScript.State>
{
    // Replace before running. Keeping an invalid id makes missing setup fail closed.
    private static final int REQUIRED_ITEM_ID = -1;

    enum State
    {
        CHECK_REQUIREMENTS,
        EQUIP,
        WORK,
        BANK,
        ERROR
    }

    private PlanWoodcutterConfig config;

    @Override
    protected State initialState()
    {
        return State.CHECK_REQUIREMENTS;
    }

    @Override
    protected List<Transition<State>> defineTransitions()
    {
        return List.of(
                // Priority matters: first matching transition wins.
                Transition.<State>from(State.WORK)
                        .when(Rs2Inventory::isFull, "Rs2Inventory.isFull()")
                        .because("Inventory full")
                        .goTo(State.BANK),
                Transition.<State>from(State.WORK)
                        .when(() -> !hasRequiredEquipment(), "!hasRequiredEquipment()")
                        .because("Required equipment missing")
                        .goTo(State.EQUIP),
                Transition.<State>from(State.CHECK_REQUIREMENTS)
                        .when(() -> !hasRequiredEquipment(), "!hasRequiredEquipment()")
                        .because("Equipment must be prepared")
                        .goTo(State.EQUIP),
                Transition.<State>from(State.CHECK_REQUIREMENTS)
                        .when(this::requirementsMet, "requirementsMet()")
                        .because("Requirements satisfied")
                        .goTo(State.WORK),
                Transition.<State>from(State.EQUIP)
                        .when(this::hasRequiredEquipment, "hasRequiredEquipment()")
                        .because("Equipment ready")
                        .goTo(State.WORK),
                Transition.<State>from(State.BANK)
                        .when(() -> !Rs2Inventory.isFull(), "!Rs2Inventory.isFull()")
                        .because("Inventory has space")
                        .goTo(State.CHECK_REQUIREMENTS),
                Transition.<State>from(State.ERROR)
                        .when(Microbot::isLoggedIn, "Microbot.isLoggedIn()")
                        .because("Retry after recoverable error")
                        .goTo(State.CHECK_REQUIREMENTS)
        );
    }

    @Override
    protected void onState(State state)
    {
        switch (state)
        {
            case CHECK_REQUIREMENTS:
                Microbot.status = "Checking requirements";
                break;
            case EQUIP:
                equipRequiredItems();
                break;
            case WORK:
                performWork();
                break;
            case BANK:
                bankItems();
                break;
            case ERROR:
                Microbot.status = "Recovering from error";
                break;
            default:
                throw new IllegalStateException("Unhandled state: " + state);
        }
    }

    private boolean requirementsMet()
    {
        return hasRequiredEquipment();
    }

    private boolean hasRequiredEquipment()
    {
        return Rs2Equipment.isWearing(REQUIRED_ITEM_ID);
    }

    private void equipRequiredItems()
    {
        Microbot.status = "Equipping required items";
        // Interact, then sleepUntil(this::hasRequiredEquipment, timeoutMs).
    }

    private void performWork()
    {
        Microbot.status = "Working";
        // Re-query target through Microbot.getRs2XxxCache().query(), then interact.
    }

    private void bankItems()
    {
        Microbot.status = "Banking";
        // Walk/open/deposit using Rs2Walker and Rs2Bank utilities.
    }

    @Override
    protected State onError(State state, Exception error)
    {
        log.error("[PlanWoodcutter] State {} failed: {}", state, error.getMessage());
        return State.ERROR;
    }

    public boolean run(PlanWoodcutterConfig config)
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
            catch (Exception error)
            {
                log.error("[PlanWoodcutter] Loop failed: {}", error.getMessage());
            }
        }, 0, config.loopDelay(), TimeUnit.MILLISECONDS);
        return true;
    }
}
