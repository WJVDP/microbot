package net.runelite.client.plugins.microbot.planfiremaker;

import net.runelite.api.Experience;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PlanFiremakerPlanner
{
    private static final long MAX_XP_TENTHS = (long) Experience.MAX_SKILL_XP * 10L;

    PlanFiremakerPlan plan(
            int currentXp,
            int targetLevel,
            Map<PlanFiremakerLogType, Integer> availableLogs,
            PlanFiremakerLogType maximumLogType)
    {
        int safeXp = Math.max(0, Math.min(currentXp, Experience.MAX_SKILL_XP));
        int safeTarget = Math.max(1, Math.min(targetLevel, Experience.MAX_REAL_LEVEL));
        EnumMap<PlanFiremakerLogType, Integer> permitted = permittedLogs(
                availableLogs, maximumLogType);

        Simulation target = simulate(safeXp, safeTarget, permitted, true);
        Simulation exhaustion = simulate(safeXp, safeTarget, permitted, false);

        return new PlanFiremakerPlan(
                safeXp,
                safeTarget,
                target.reachedTarget,
                target.steps,
                target.remaining,
                exhaustion.endingXp,
                Experience.getLevelForXp(exhaustion.endingXp),
                exhaustion.steps,
                exhaustion.remaining);
    }

    static EnumMap<PlanFiremakerLogType, Integer> mergeQuantities(
            Map<PlanFiremakerLogType, Integer> bank,
            Map<PlanFiremakerLogType, Integer> inventory)
    {
        EnumMap<PlanFiremakerLogType, Integer> merged = new EnumMap<>(PlanFiremakerLogType.class);
        for (PlanFiremakerLogType type : PlanFiremakerLogType.values())
        {
            long amount = (long) positiveQuantity(bank, type) + positiveQuantity(inventory, type);
            merged.put(type, (int) Math.min(amount, Integer.MAX_VALUE));
        }
        return merged;
    }

    private Simulation simulate(
            int startingXp,
            int targetLevel,
            EnumMap<PlanFiremakerLogType, Integer> available,
            boolean stopAtTarget)
    {
        EnumMap<PlanFiremakerLogType, Integer> remaining = new EnumMap<>(available);
        List<PlanFiremakerPlanStep> steps = new ArrayList<>();
        long xpTenths = (long) startingXp * 10L;
        long targetXpTenths = (long) Experience.getXpForLevel(targetLevel) * 10L;

        while (xpTenths < MAX_XP_TENTHS)
        {
            if (stopAtTarget && xpTenths >= targetXpTenths)
            {
                break;
            }

            int currentLevel = Experience.getLevelForXp((int) (xpTenths / 10L));
            PlanFiremakerLogType selected = PlanFiremakerLogType
                    .bestUnlocked(currentLevel, lastPermitted(available), remaining)
                    .orElse(null);
            if (selected == null)
            {
                break;
            }

            long milestone = stopAtTarget ? targetXpTenths : MAX_XP_TENTHS;
            for (PlanFiremakerLogType locked : remaining.keySet())
            {
                if (locked.getRequiredLevel() <= currentLevel)
                {
                    continue;
                }
                if (stopAtTarget && locked.getRequiredLevel() > targetLevel)
                {
                    continue;
                }
                milestone = Math.min(milestone,
                        (long) Experience.getXpForLevel(locked.getRequiredLevel()) * 10L);
            }

            int availableCount = remaining.getOrDefault(selected, 0);
            int quantity;
            if (!stopAtTarget && milestone == MAX_XP_TENTHS)
            {
                quantity = availableCount;
            }
            else
            {
                long xpNeeded = Math.max(1L, milestone - xpTenths);
                long neededCount = ceilingDivide(xpNeeded, selected.getXpTenths());
                quantity = (int) Math.min(availableCount, neededCount);
            }

            long startingStepXp = xpTenths;
            long awarded = Math.min(MAX_XP_TENTHS - xpTenths,
                    (long) quantity * selected.getXpTenths());
            xpTenths += awarded;
            remaining.put(selected, availableCount - quantity);
            addStep(steps, selected, quantity, startingStepXp, xpTenths);
        }

        int endingXp = (int) Math.min(Experience.MAX_SKILL_XP, xpTenths / 10L);
        boolean reachedTarget = endingXp >= Experience.getXpForLevel(targetLevel);
        return new Simulation(endingXp, reachedTarget, steps, remaining);
    }

    private static void addStep(
            List<PlanFiremakerPlanStep> steps,
            PlanFiremakerLogType type,
            int quantity,
            long startingXpTenths,
            long endingXpTenths)
    {
        int startingXp = (int) (startingXpTenths / 10L);
        int endingXp = (int) (endingXpTenths / 10L);
        int endingLevel = Experience.getLevelForXp(endingXp);
        steps.add(new PlanFiremakerPlanStep(type, quantity, startingXp, endingXp, endingLevel));
    }

    private static EnumMap<PlanFiremakerLogType, Integer> permittedLogs(
            Map<PlanFiremakerLogType, Integer> available,
            PlanFiremakerLogType maximum)
    {
        EnumMap<PlanFiremakerLogType, Integer> result = new EnumMap<>(PlanFiremakerLogType.class);
        for (PlanFiremakerLogType type : PlanFiremakerLogType.values())
        {
            if (type.ordinal() <= maximum.ordinal())
            {
                result.put(type, positiveQuantity(available, type));
            }
        }
        return result;
    }

    private static int positiveQuantity(
            Map<PlanFiremakerLogType, Integer> quantities,
            PlanFiremakerLogType type)
    {
        if (quantities == null)
        {
            return 0;
        }
        return Math.max(0, quantities.getOrDefault(type, 0));
    }

    private static PlanFiremakerLogType lastPermitted(
            EnumMap<PlanFiremakerLogType, Integer> available)
    {
        PlanFiremakerLogType last = PlanFiremakerLogType.NORMAL;
        for (PlanFiremakerLogType type : available.keySet())
        {
            last = type;
        }
        return last;
    }

    private static long ceilingDivide(long numerator, long denominator)
    {
        return (numerator + denominator - 1L) / denominator;
    }

    private static final class Simulation
    {
        private final int endingXp;
        private final boolean reachedTarget;
        private final List<PlanFiremakerPlanStep> steps;
        private final EnumMap<PlanFiremakerLogType, Integer> remaining;

        private Simulation(
                int endingXp,
                boolean reachedTarget,
                List<PlanFiremakerPlanStep> steps,
                EnumMap<PlanFiremakerLogType, Integer> remaining)
        {
            this.endingXp = endingXp;
            this.reachedTarget = reachedTarget;
            this.steps = steps;
            this.remaining = remaining;
        }
    }
}
