package net.runelite.client.plugins.microbot.planwoodcutter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("planwoodcutter")
public interface PlanWoodcutterConfig extends Config
{
    @ConfigItem(
            keyName = "loopDelay",
            name = "Loop delay (ms)",
            description = "Delay between automation decisions",
            position = 0
    )
    default int loopDelay()
    {
        return 600;
    }
}
