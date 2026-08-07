package net.runelite.client.plugins.microbot.planfiremaker;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Getter
final class PlanFiremakerPlan
{
    private final int startingXp;
    private final int targetLevel;
    private final boolean targetReachable;
    private final List<PlanFiremakerPlanStep> targetSteps;
    private final Map<PlanFiremakerLogType, Integer> remainingAtTarget;
    private final int projectedXp;
    private final int projectedLevel;
    private final List<PlanFiremakerPlanStep> exhaustionSteps;
    private final Map<PlanFiremakerLogType, Integer> lockedAtEnd;

    PlanFiremakerPlan(
            int startingXp,
            int targetLevel,
            boolean targetReachable,
            List<PlanFiremakerPlanStep> targetSteps,
            Map<PlanFiremakerLogType, Integer> remainingAtTarget,
            int projectedXp,
            int projectedLevel,
            List<PlanFiremakerPlanStep> exhaustionSteps,
            Map<PlanFiremakerLogType, Integer> lockedAtEnd)
    {
        this.startingXp = startingXp;
        this.targetLevel = targetLevel;
        this.targetReachable = targetReachable;
        this.targetSteps = List.copyOf(targetSteps);
        this.remainingAtTarget = immutableCopy(remainingAtTarget);
        this.projectedXp = projectedXp;
        this.projectedLevel = projectedLevel;
        this.exhaustionSteps = List.copyOf(exhaustionSteps);
        this.lockedAtEnd = immutableCopy(lockedAtEnd);
    }

    int logsNeededForTarget()
    {
        return targetSteps.stream().mapToInt(PlanFiremakerPlanStep::getQuantity).sum();
    }

    int tripsNeededForTarget()
    {
        int logs = logsNeededForTarget();
        return logs == 0 ? 0 : (logs + 26) / 27;
    }

    PlanFiremakerLogType nextLogType()
    {
        return targetSteps.isEmpty() ? null : targetSteps.get(0).getLogType();
    }

    private static Map<PlanFiremakerLogType, Integer> immutableCopy(
            Map<PlanFiremakerLogType, Integer> source)
    {
        return Collections.unmodifiableMap(new EnumMap<>(source));
    }
}
