package net.runelite.client.plugins.microbot.planquesting;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigButton;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("planquesting")
public interface PlanQuestingConfig extends Config
{
    @ConfigItem(
            keyName = "pilotQuest",
            name = "Pilot quest",
            description = "Quest Helper quest controlled by this attended pilot",
            position = 0
    )
    default PlanQuestingPilotQuest pilotQuest()
    {
        return PlanQuestingPilotQuest.MISTHALIN_MYSTERY;
    }

    @Range(min = 400, max = 2_000)
    @ConfigItem(
            keyName = "loopDelay",
            name = "Loop delay (ms)",
            description = "Delay between automation decisions",
            position = 1
    )
    default int loopDelay()
    {
        return 600;
    }

    @Range(min = 1, max = 5)
    @ConfigItem(
            keyName = "maxAttempts",
            name = "Attempts before handoff",
            description = "Verified no-progress attempts before requesting manual intervention",
            position = 2
    )
    default int maxAttempts()
    {
        return 3;
    }

    @ConfigItem(
            keyName = "resume",
            name = "Resume after manual step",
            description = "Re-read Quest Helper state after you complete or exit the manual section",
            position = 3
    )
    default ConfigButton resume()
    {
        return new ConfigButton();
    }

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show overlay",
            description = "Show quest authority, risk, and manual intervention state",
            position = 4
    )
    default boolean showOverlay()
    {
        return true;
    }
}
