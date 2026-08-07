package net.runelite.client.plugins.microbot.planwoodcutter;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.agentserver.controlcenter.ControlCenterPlugin;

import javax.inject.Inject;

@PluginDescriptor(
        name = PluginDescriptor.Plan + "Woodcutter",
        description = "Cuts the closest selected tree and drops logs or fletches arrow shafts",
        tags = {"plan", "microbot", "automation", "woodcutting", "fletching"},
        enabledByDefault = false
)
@ControlCenterPlugin(id = "plan-woodcutter")
public class PlanWoodcutterPlugin extends Plugin
{
    @Inject
    private PlanWoodcutterScript script;

    @Inject
    private PlanWoodcutterConfig config;

    @Provides
    PlanWoodcutterConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PlanWoodcutterConfig.class);
    }

    @Override
    protected void startUp()
    {
        script.run(config);
    }

    @Override
    protected void shutDown()
    {
        script.shutdown();
    }
}
