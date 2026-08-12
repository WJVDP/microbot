package net.runelite.client.plugins.microbot.planquesting;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class PlanQuestingOverlay extends OverlayPanel
{
    private static final Color SAFE = new Color(92, 184, 92);
    private static final Color MANUAL = new Color(217, 83, 79);

    private final PlanQuestingConfig config;
    private final PlanQuestingScript script;

    @Inject
    PlanQuestingOverlay(PlanQuestingPlugin plugin, PlanQuestingConfig config,
                        PlanQuestingScript script)
    {
        super(plugin);
        this.config = config;
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay())
        {
            return null;
        }

        PlanQuestingStatus status = script.getStatus();
        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(285, 0));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Plan Questing")
                .color(Color.ORANGE)
                .build());
        addLine("Quest", status.getQuest(), Color.WHITE);
        addLine("Authority", status.getAuthority(),
                "PLAYER".equals(status.getAuthority()) ? MANUAL : SAFE);
        addLine("Risk", status.getRisk(),
                "MANUAL_REQUIRED".equals(status.getRisk()) ? MANUAL : SAFE);
        addLine("Action", status.getAction(), Color.WHITE);
        addLine("Reason", status.getReason(), Color.LIGHT_GRAY);
        return super.render(graphics);
    }

    private void addLine(String left, String right, Color color)
    {
        panelComponent.getChildren().add(LineComponent.builder()
                .left(left)
                .right(right == null ? "" : right)
                .rightColor(color)
                .build());
    }
}
