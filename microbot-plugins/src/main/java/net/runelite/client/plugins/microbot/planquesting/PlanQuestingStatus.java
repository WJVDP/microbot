package net.runelite.client.plugins.microbot.planquesting;

import lombok.Value;

@Value
class PlanQuestingStatus
{
    String action;
    String quest;
    String authority;
    String risk;
    String step;
    long revision;
    String reason;

    static PlanQuestingStatus waiting()
    {
        return new PlanQuestingStatus("Waiting", "None", "NONE", "UNKNOWN",
                "No active step", 0, "");
    }
}
