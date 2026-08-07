package net.runelite.client.plugins.microbot.planwoodcutter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.statemachine.StateMachineScript;
import net.runelite.client.plugins.microbot.statemachine.Transition;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.skills.fletching.Rs2Fletching;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@Slf4j
public class PlanWoodcutterScript
        extends StateMachineScript<PlanWoodcutterScript.State>
{
    private static final int ACTION_TIMEOUT_MS = 5_000;
    private static final int INVENTORY_TIMEOUT_MS = 10_000;
    private static final int LOOP_DELAY_MS = 600;
    private static final Set<String> WOODCUTTING_ACTIONS = Set.of(
            "chop down", "chop", "cut down", "cut", "chop-down");

    enum State
    {
        CHECK_REQUIREMENTS,
        CHOP,
        CLEAR_INVENTORY,
        ERROR
    }

    private PlanWoodcutterConfig config;
    private int woodcuttingLevel = -1;
    private int fletchingLevel = -1;
    private boolean inMembersWorld;
    private String inventoryClearFailure;

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
                        .when(this::requirementsKnownAndMissing, "requirementsKnownAndMissing()")
                        .because("Woodcutting requirements are missing")
                        .goTo(State.ERROR),
                Transition.<State>from(State.CHECK_REQUIREMENTS)
                        .when(this::requirementsMet, "requirementsMet()")
                        .because("Woodcutting requirements are satisfied")
                        .goTo(State.CHOP),
                Transition.<State>from(State.CHOP)
                        .when(Rs2Inventory::isFull, "Rs2Inventory.isFull()")
                        .because("Inventory is full")
                        .goTo(State.CLEAR_INVENTORY),
                Transition.<State>from(State.CHOP)
                        .when(() -> !requirementsMet(), "!requirementsMet()")
                        .because("A required item or level is missing")
                        .goTo(State.ERROR),
                Transition.<State>from(State.CLEAR_INVENTORY)
                        .when(() -> Rs2Inventory.isFull() && inventoryClearFailure != null,
                                "Rs2Inventory.isFull() && inventoryClearFailure != null")
                        .because("The inventory clearing attempt failed")
                        .goTo(State.ERROR),
                Transition.<State>from(State.CLEAR_INVENTORY)
                        .when(() -> Rs2Inventory.isFull() && !canClearInventory(),
                                "Rs2Inventory.isFull() && !canClearInventory()")
                        .because("The full inventory has no processable logs")
                        .goTo(State.ERROR),
                Transition.<State>from(State.CLEAR_INVENTORY)
                        .when(() -> !Rs2Inventory.isFull(), "!Rs2Inventory.isFull()")
                        .because("Inventory has space")
                        .goTo(State.CHECK_REQUIREMENTS),
                Transition.<State>from(State.ERROR)
                        .when(() -> Rs2Inventory.isFull() && canRetryClearInventory(),
                                "Rs2Inventory.isFull() && canRetryClearInventory()")
                        .because("Full inventory can now be cleared")
                        .goTo(State.CLEAR_INVENTORY),
                Transition.<State>from(State.ERROR)
                        .when(() -> !Rs2Inventory.isFull() && requirementsMet(),
                                "!Rs2Inventory.isFull() && requirementsMet()")
                        .because("Requirements have been restored")
                        .goTo(State.CHECK_REQUIREMENTS)
        );
    }

    @Override
    protected void onState(State state)
    {
        switch (state)
        {
            case CHECK_REQUIREMENTS:
                refreshRequirements();
                inventoryClearFailure = null;
                Microbot.status = "Checking Woodcutter requirements";
                break;
            case CHOP:
                chopClosestTree();
                break;
            case CLEAR_INVENTORY:
                clearInventory();
                break;
            case ERROR:
                refreshRequirements();
                Microbot.status = requirementError();
                break;
            default:
                throw new IllegalStateException("Unhandled state: " + state);
        }
    }

    private void refreshRequirements()
    {
        woodcuttingLevel = Rs2Player.getBoostedSkillLevel(Skill.WOODCUTTING);
        fletchingLevel = Rs2Player.getBoostedSkillLevel(Skill.FLETCHING);
        inMembersWorld = Microbot.getClientThread().runOnClientThreadOptional(
                () -> Microbot.getClient().getWorldType().contains(WorldType.MEMBERS))
                .orElse(false);
    }

    private boolean requirementsKnownAndMissing()
    {
        return woodcuttingLevel >= 0 && fletchingLevel >= 0 && !requirementsMet();
    }

    private boolean requirementsMet()
    {
        if (config == null || woodcuttingLevel < 0 || fletchingLevel < 0)
        {
            return false;
        }

        PlanWoodcutterTreeType treeType = config.treeType();
        if (!treeType.hasValidTargetName(config.customTreeName())
                || woodcuttingLevel < treeType.getWoodcuttingLevel()
                || !PlanWoodcutterData.hasUsableAxe(woodcuttingLevel))
        {
            return false;
        }

        if (config.fullInventoryAction() == PlanWoodcutterFullInventoryAction.ARROW_SHAFTS)
        {
            return treeType.supportsArrowShafts()
                    && inMembersWorld
                    && fletchingLevel >= treeType.getArrowShaftLevel()
                    && Rs2Fletching.hasKnife();
        }

        return true;
    }

    private String requirementError()
    {
        PlanWoodcutterTreeType treeType = config.treeType();
        if (!treeType.hasValidTargetName(config.customTreeName()))
        {
            return "Enter an exact custom tree name";
        }
        if (woodcuttingLevel < treeType.getWoodcuttingLevel())
        {
            return "Woodcutting level " + treeType.getWoodcuttingLevel() + " required";
        }
        if (!PlanWoodcutterData.hasUsableAxe(woodcuttingLevel))
        {
            return "A usable axe is required";
        }
        if (config.fullInventoryAction() == PlanWoodcutterFullInventoryAction.ARROW_SHAFTS)
        {
            if (!treeType.supportsArrowShafts())
            {
                return treeType + " logs cannot make arrow shafts";
            }
            if (!inMembersWorld)
            {
                return "A members world is required for arrow shafts";
            }
            if (fletchingLevel < treeType.getArrowShaftLevel())
            {
                return "Fletching level " + treeType.getArrowShaftLevel() + " required";
            }
            if (!Rs2Fletching.hasKnife())
            {
                return "A knife is required for arrow shafts";
            }
            if (inventoryClearFailure != null)
            {
                return inventoryClearFailure;
            }
        }
        if (Rs2Inventory.isFull() && !canClearInventory())
        {
            return "Inventory is full with no processable logs";
        }
        return "Woodcutter paused: check requirements";
    }

    private void chopClosestTree()
    {
        refreshRequirements();
        if (!requirementsMet())
        {
            Microbot.status = requirementError();
            return;
        }

        if (Rs2Player.isMoving() || Rs2Antiban.isWoodcutting())
        {
            Microbot.status = "Cutting " + config.treeType();
            return;
        }

        Rs2TileObjectModel tree = Microbot.getRs2TileObjectCache().query()
                .fromWorldView()
                .where(object -> config.treeType().matches(object.getName(), config.customTreeName()))
                .where(object -> woodcuttingAction(object) != null)
                .nearestOnClientThread();

        if (tree == null)
        {
            Microbot.status = "No " + config.treeType() + " tree found nearby";
            return;
        }

        String action = woodcuttingAction(tree);
        if (action == null)
        {
            Microbot.status = "No " + config.treeType() + " tree found nearby";
            return;
        }

        Microbot.status = "Cutting " + config.treeType();
        tree.click(action);
        sleepUntil(() -> Rs2Player.isMoving()
                || Rs2Antiban.isWoodcutting()
                || Rs2Inventory.isFull(), ACTION_TIMEOUT_MS);
    }

    private static String woodcuttingAction(Rs2TileObjectModel object)
    {
        ObjectComposition composition = object.getObjectComposition();
        if (composition == null)
        {
            return null;
        }

        return findWoodcuttingAction(composition.getActions());
    }

    static String findWoodcuttingAction(String[] actions)
    {
        if (actions == null)
        {
            return null;
        }

        for (String action : actions)
        {
            if (action != null
                    && WOODCUTTING_ACTIONS.contains(action.trim().toLowerCase(Locale.ROOT)))
            {
                return action;
            }
        }
        return null;
    }

    private boolean canClearInventory()
    {
        if (config.fullInventoryAction() == PlanWoodcutterFullInventoryAction.DROP_LOGS)
        {
            return Rs2Inventory.contains(PlanWoodcutterData::isLog);
        }

        PlanWoodcutterTreeType treeType = config.treeType();
        return treeType.supportsArrowShafts()
                && inMembersWorld
                && fletchingLevel >= treeType.getArrowShaftLevel()
                && Rs2Fletching.hasKnife()
                && Rs2Inventory.hasItem(treeType.getLogItemId());
    }

    private boolean canRetryClearInventory()
    {
        return canClearInventory()
                && (inventoryClearFailure == null
                || config.fullInventoryAction() != PlanWoodcutterFullInventoryAction.ARROW_SHAFTS);
    }

    private void clearInventory()
    {
        if (config.fullInventoryAction() == PlanWoodcutterFullInventoryAction.DROP_LOGS)
        {
            inventoryClearFailure = null;
            Microbot.status = "Dropping logs";
            Rs2Inventory.dropAll(PlanWoodcutterData::isLog, InteractOrder.ZIGZAG);
            sleepUntil(() -> !Rs2Inventory.isFull(), INVENTORY_TIMEOUT_MS);
            return;
        }

        refreshRequirements();
        if (!canClearInventory())
        {
            Microbot.status = requirementError();
            return;
        }

        PlanWoodcutterTreeType treeType = config.treeType();
        Microbot.status = "Cutting logs into arrow shafts";
        if (!Rs2Fletching.fletchItems(treeType.getLogItemId(), "arrow shaft", "All"))
        {
            inventoryClearFailure = "Failed to cut logs into arrow shafts";
            Microbot.status = inventoryClearFailure;
            return;
        }

        inventoryClearFailure = null;
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
        }, 0, LOOP_DELAY_MS, TimeUnit.MILLISECONDS);
        return true;
    }
}
