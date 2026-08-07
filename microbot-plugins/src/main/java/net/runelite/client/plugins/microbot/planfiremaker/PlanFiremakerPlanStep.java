package net.runelite.client.plugins.microbot.planfiremaker;

import lombok.Value;

@Value
class PlanFiremakerPlanStep
{
    PlanFiremakerLogType logType;
    int quantity;
    int startingXp;
    int endingXp;
    int endingLevel;
}
