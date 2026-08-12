package net.runelite.client.plugins.microbot.planquesting;

enum PlanQuestingPilotQuest
{
    MISTHALIN_MYSTERY("Misthalin Mystery"),
    VAMPYRE_SLAYER("Vampyre Slayer");

    private final String displayName;

    PlanQuestingPilotQuest(String displayName)
    {
        this.displayName = displayName;
    }

    String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
