package net.runelite.client.plugins.microbot.planfiremaker;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Getter
final class PlanFiremakerSnapshot
{
    private final String state;
    private final String status;
    private final boolean bankSnapshotKnown;
    private final int currentXp;
    private final int currentLevel;
    private final Map<PlanFiremakerLogType, Integer> availableLogs;
    private final PlanFiremakerPlan plan;

    PlanFiremakerSnapshot(
            String state,
            String status,
            boolean bankSnapshotKnown,
            int currentXp,
            int currentLevel,
            Map<PlanFiremakerLogType, Integer> availableLogs,
            PlanFiremakerPlan plan)
    {
        this.state = state;
        this.status = status;
        this.bankSnapshotKnown = bankSnapshotKnown;
        this.currentXp = currentXp;
        this.currentLevel = currentLevel;
        this.availableLogs = Collections.unmodifiableMap(new EnumMap<>(availableLogs));
        this.plan = plan;
    }

    static PlanFiremakerSnapshot waiting()
    {
        return new PlanFiremakerSnapshot(
                "STARTING", "Waiting for login", false, 0, 1,
                new EnumMap<>(PlanFiremakerLogType.class), null);
    }
}
