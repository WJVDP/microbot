package net.runelite.client.plugins.microbot.planfiremaker;

import net.runelite.api.Experience;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlanFiremakerPlannerTest
{
    private final PlanFiremakerPlanner planner = new PlanFiremakerPlanner();

    @Test
    public void buildsNormalOakWillowPathToFifty()
    {
        PlanFiremakerPlan plan = planner.plan(0, 50, quantities(
                61, 183, 977, 0, 0, 0, 0, 0, 0), PlanFiremakerLogType.WILLOW);

        assertTrue(plan.isTargetReachable());
        assertEquals(3, plan.getTargetSteps().size());
        assertStep(plan, 0, PlanFiremakerLogType.NORMAL, 61, 15);
        assertStep(plan, 1, PlanFiremakerLogType.OAK, 183, 30);
        assertStep(plan, 2, PlanFiremakerLogType.WILLOW, 977, 50);
        assertEquals(1_221, plan.logsNeededForTarget());
        assertEquals(46, plan.tripsNeededForTarget());
        assertEquals(101_350, plan.getProjectedXp());
        assertEquals(50, plan.getProjectedLevel());
    }

    @Test
    public void leavesHigherTiersLockedWhenLowerLogsCannotReachUnlock()
    {
        PlanFiremakerPlan plan = planner.plan(0, 50, quantities(
                60, 10_000, 10_000, 0, 0, 0, 0, 0, 0), PlanFiremakerLogType.WILLOW);

        assertFalse(plan.isTargetReachable());
        assertEquals(2_400, plan.getProjectedXp());
        assertEquals(14, plan.getProjectedLevel());
        assertEquals(Integer.valueOf(10_000), plan.getLockedAtEnd().get(PlanFiremakerLogType.OAK));
        assertEquals(Integer.valueOf(10_000), plan.getLockedAtEnd().get(PlanFiremakerLogType.WILLOW));
    }

    @Test
    public void keepsEachUnlockVisibleWhenTheSameLogSpansSeveralTiers()
    {
        PlanFiremakerPlan plan = planner.plan(0, 50, quantities(
                400, 0, 1_000, 0, 0, 0, 0, 0, 0), PlanFiremakerLogType.WILLOW);

        assertTrue(plan.isTargetReachable());
        assertEquals(3, plan.getTargetSteps().size());
        assertStep(plan, 0, PlanFiremakerLogType.NORMAL, 61, 15);
        assertStep(plan, 1, PlanFiremakerLogType.NORMAL, 274, 30);
        assertStep(plan, 2, PlanFiremakerLogType.WILLOW, 978, 50);
    }

    @Test
    public void usesOnlyPartOfTierWhenTargetIsReached()
    {
        int levelThirtyXp = Experience.getXpForLevel(30);
        PlanFiremakerPlan plan = planner.plan(levelThirtyXp, 50, quantities(
                0, 0, 1_000, 0, 0, 0, 0, 0, 0), PlanFiremakerLogType.WILLOW);

        assertTrue(plan.isTargetReachable());
        assertEquals(978, plan.logsNeededForTarget());
        assertEquals(Integer.valueOf(22),
                plan.getRemainingAtTarget().get(PlanFiremakerLogType.WILLOW));
        assertEquals(levelThirtyXp + 90_000, plan.getProjectedXp());
    }

    @Test
    public void preservesAllLogsWhenTargetIsAlreadyReached()
    {
        int levelFiftyXp = Experience.getXpForLevel(50);
        PlanFiremakerPlan plan = planner.plan(levelFiftyXp, 50, quantities(
                5, 4, 3, 0, 0, 0, 0, 0, 0), PlanFiremakerLogType.WILLOW);

        assertTrue(plan.isTargetReachable());
        assertTrue(plan.getTargetSteps().isEmpty());
        assertEquals(Integer.valueOf(5),
                plan.getRemainingAtTarget().get(PlanFiremakerLogType.NORMAL));
        assertEquals(levelFiftyXp + 5 * 40 + 4 * 60 + 3 * 90, plan.getProjectedXp());
    }

    @Test
    public void combinesBankAndInventoryWithoutLosingWithdrawnLogs()
    {
        Map<PlanFiremakerLogType, Integer> bank = new EnumMap<>(PlanFiremakerLogType.class);
        bank.put(PlanFiremakerLogType.WILLOW, 973);
        Map<PlanFiremakerLogType, Integer> inventory = new EnumMap<>(PlanFiremakerLogType.class);
        inventory.put(PlanFiremakerLogType.WILLOW, 27);

        Map<PlanFiremakerLogType, Integer> merged =
                PlanFiremakerPlanner.mergeQuantities(bank, inventory);

        assertEquals(Integer.valueOf(1_000), merged.get(PlanFiremakerLogType.WILLOW));
        assertEquals(Integer.valueOf(0), merged.get(PlanFiremakerLogType.OAK));
    }

    @Test
    public void tracksFractionalExperienceInTenths()
    {
        int startingXp = Experience.getXpForLevel(75);
        PlanFiremakerPlan plan = planner.plan(startingXp, 99, quantities(
                0, 0, 0, 0, 0, 0, 2, 2, 0), PlanFiremakerLogType.MAGIC);

        assertEquals(startingXp + 405 + 607, plan.getProjectedXp());
        assertEquals(4, plan.getExhaustionSteps().stream()
                .mapToInt(PlanFiremakerPlanStep::getQuantity).sum());
    }

    @Test
    public void excludesLogsAboveConfiguredMaximum()
    {
        int levelThirtyXp = Experience.getXpForLevel(30);
        PlanFiremakerPlan plan = planner.plan(levelThirtyXp, 50, quantities(
                0, 0, 10, 0, 10_000, 0, 0, 0, 0), PlanFiremakerLogType.WILLOW);

        assertFalse(plan.isTargetReachable());
        assertEquals(levelThirtyXp + 900, plan.getProjectedXp());
        assertFalse(plan.getLockedAtEnd().containsKey(PlanFiremakerLogType.MAPLE));
    }

    @Test
    public void capsProjectionAtMaximumSkillExperience()
    {
        int startingXp = Experience.MAX_SKILL_XP - 10;
        PlanFiremakerPlan plan = planner.plan(startingXp, 99, quantities(
                Integer.MAX_VALUE, 0, 0, 0, 0, 0, 0, 0, 0), PlanFiremakerLogType.NORMAL);

        assertEquals(Experience.MAX_SKILL_XP, plan.getProjectedXp());
        assertTrue(plan.getLockedAtEnd().isEmpty());
    }

    @Test
    public void doesNotClassifyUnlockedXpCapLeftoversAsLocked()
    {
        PlanFiremakerPlan plan = planner.plan(Experience.MAX_SKILL_XP - 10, 99, quantities(
                10, 10, 0, 0, 0, 0, 0, 0, 0), PlanFiremakerLogType.OAK);

        assertEquals(Experience.MAX_SKILL_XP, plan.getProjectedXp());
        assertTrue(plan.getLockedAtEnd().isEmpty());
    }

    private static void assertStep(
            PlanFiremakerPlan plan,
            int index,
            PlanFiremakerLogType type,
            int quantity,
            int endingLevel)
    {
        PlanFiremakerPlanStep step = plan.getTargetSteps().get(index);
        assertEquals(type, step.getLogType());
        assertEquals(quantity, step.getQuantity());
        assertEquals(endingLevel, step.getEndingLevel());
    }

    private static Map<PlanFiremakerLogType, Integer> quantities(
            int normal,
            int oak,
            int willow,
            int teak,
            int maple,
            int mahogany,
            int yew,
            int magic,
            int redwood)
    {
        EnumMap<PlanFiremakerLogType, Integer> values =
                new EnumMap<>(PlanFiremakerLogType.class);
        values.put(PlanFiremakerLogType.NORMAL, normal);
        values.put(PlanFiremakerLogType.OAK, oak);
        values.put(PlanFiremakerLogType.WILLOW, willow);
        values.put(PlanFiremakerLogType.TEAK, teak);
        values.put(PlanFiremakerLogType.MAPLE, maple);
        values.put(PlanFiremakerLogType.MAHOGANY, mahogany);
        values.put(PlanFiremakerLogType.YEW, yew);
        values.put(PlanFiremakerLogType.MAGIC, magic);
        values.put(PlanFiremakerLogType.REDWOOD, redwood);
        return values;
    }
}
