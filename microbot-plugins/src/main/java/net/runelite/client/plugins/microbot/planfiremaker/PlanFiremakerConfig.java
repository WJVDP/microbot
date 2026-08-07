package net.runelite.client.plugins.microbot.planfiremaker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("planfiremaker")
public interface PlanFiremakerConfig extends Config
{
    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop burning logs when this Firemaking level is reached",
            position = 0
    )
    default int targetLevel()
    {
        return 50;
    }

    @ConfigItem(
            keyName = "maximumLogType",
            name = "Maximum log type",
            description = "Allow automatic progression through logs up to this tier",
            position = 1
    )
    default PlanFiremakerLogType maximumLogType()
    {
        return PlanFiremakerLogType.WILLOW;
    }

    @Range(min = 5, max = 27)
    @ConfigItem(
            keyName = "lineLength",
            name = "Fire line length",
            description = "Start beside an outdoor bank; the script walks this many tiles east for each fire line",
            position = 2
    )
    default int lineLength()
    {
        return 27;
    }

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show overlay",
            description = "Show the banked-log forecast and current script state",
            position = 3
    )
    default boolean showOverlay()
    {
        return true;
    }
}
