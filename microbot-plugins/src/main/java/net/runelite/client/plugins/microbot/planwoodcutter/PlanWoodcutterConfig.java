package net.runelite.client.plugins.microbot.planwoodcutter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("planwoodcutter")
public interface PlanWoodcutterConfig extends Config
{
    @ConfigItem(
            keyName = "treeType",
            name = "Tree type",
            description = "The type of tree to cut",
            position = 0
    )
    default PlanWoodcutterTreeType treeType()
    {
        return PlanWoodcutterTreeType.NORMAL;
    }

    @ConfigItem(
            keyName = "customTreeName",
            name = "Custom tree name",
            description = "Exact in-game object name used when Tree type is Custom",
            position = 1
    )
    default String customTreeName()
    {
        return "";
    }

    @ConfigItem(
            keyName = "fullInventoryAction",
            name = "Full inventory",
            description = "What to do with logs when the inventory is full",
            position = 2
    )
    default PlanWoodcutterFullInventoryAction fullInventoryAction()
    {
        return PlanWoodcutterFullInventoryAction.DROP_LOGS;
    }
}
