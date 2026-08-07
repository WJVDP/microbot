package net.runelite.client.plugins.microbot.planwoodcutter;

public enum PlanWoodcutterFullInventoryAction
{
    DROP_LOGS("Drop all logs"),
    ARROW_SHAFTS("Cut into arrow shafts");

    private final String displayName;

    PlanWoodcutterFullInventoryAction(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
