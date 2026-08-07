package net.runelite.client.plugins.microbot.planfiremaker;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.agentserver.controlcenter.ControlCenterPlugin;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = PluginDescriptor.Plan + "Firemaker",
        description = "Burns banked logs with a level-aware Firemaking forecast",
        tags = {"plan", "microbot", "automation", "firemaking", "wintertodt"},
        enabledByDefault = false
)
@ControlCenterPlugin(id = "plan-firemaker")
public class PlanFiremakerPlugin extends Plugin
{
    @Inject
    private PlanFiremakerScript script;

    @Inject
    private PlanFiremakerConfig config;

    @Inject
    private PlanFiremakerOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Provides
    PlanFiremakerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PlanFiremakerConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        script.run(config);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        script.shutdown();
    }
}
