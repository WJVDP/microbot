package net.runelite.client.plugins.microbot.planquesting;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.agentserver.controlcenter.ControlCenterPlugin;
import net.runelite.client.plugins.microbot.agentserver.controlcenter.ControlCenterStatusRegistry;
import net.runelite.client.plugins.microbot.agentserver.controlcenter.ControlCenterStatusSnapshot;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

@PluginDescriptor(
        name = PluginDescriptor.Plan + "Questing",
        description = "Planned Questing automation",
        tags = {"plan", "microbot", "automation"},
        enabledByDefault = false
)
@ControlCenterPlugin(id = "plan-questing")
public class PlanQuestingPlugin extends Plugin
{
    @Inject
    private PlanQuestingScript script;

    @Inject
    private PlanQuestingConfig config;

    @Inject
    private PlanQuestingOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Provides
    PlanQuestingConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PlanQuestingConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        ControlCenterStatusRegistry.register("plan-questing", this::statusSnapshot);
        script.run(config);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        script.shutdown();
        ControlCenterStatusRegistry.unregister("plan-questing");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if ("planquesting".equals(event.getGroup()) && "resume".equals(event.getKey()))
        {
            script.requestResume();
        }
    }

    private ControlCenterStatusSnapshot statusSnapshot()
    {
        PlanQuestingStatus status = script.getStatus();
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Quest", status.getQuest());
        details.put("Authority", status.getAuthority());
        details.put("Risk", status.getRisk());
        details.put("Step", status.getStep());
        details.put("Revision", Long.toString(status.getRevision()));
        details.put("Reason", status.getReason());
        return new ControlCenterStatusSnapshot(status.getAction(), details);
    }
}
