package net.runelite.client.plugins.microbot.planfiremaker;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;

public class PlanFiremakerOverlay extends OverlayPanel
{
    private static final Color SUCCESS = new Color(92, 184, 92);
    private static final Color WARNING = new Color(240, 173, 78);

    private final PlanFiremakerConfig config;
    private final PlanFiremakerScript script;

    @Inject
    PlanFiremakerOverlay(
            PlanFiremakerPlugin plugin,
            PlanFiremakerConfig config,
            PlanFiremakerScript script)
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

        PlanFiremakerSnapshot snapshot = script.getStatusSnapshot();
        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(225, 0));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Plan Firemaker")
                .color(Color.ORANGE)
                .build());
        addLine("State", snapshot.getState());
        addLine("Current", snapshot.getCurrentLevel() + " (" + format(snapshot.getCurrentXp()) + " XP)");

        if (!snapshot.isBankSnapshotKnown() || snapshot.getPlan() == null)
        {
            addLine("Forecast", "Open bank", WARNING);
            addLine("Status", snapshot.getStatus());
            return super.render(graphics);
        }

        PlanFiremakerPlan plan = snapshot.getPlan();
        addLine("Target " + plan.getTargetLevel(),
                plan.isTargetReachable() ? "Reachable" : "Not reachable",
                plan.isTargetReachable() ? SUCCESS : WARNING);

        PlanFiremakerLogType next = plan.nextLogType();
        addLine("Next log", next == null ? "-" : next.toString());
        addLine(plan.isTargetReachable() ? "Logs to target" : "Burnable logs",
                format(plan.logsNeededForTarget()));
        addLine(plan.isTargetReachable() ? "Trips to target" : "Planned trips",
                Integer.toString(plan.tripsNeededForTarget()));
        int pathIndex = 1;
        for (PlanFiremakerPlanStep step : plan.getTargetSteps())
        {
            addLine("Path " + pathIndex++, step.getQuantity() + " " + step.getLogType()
                    + " → " + step.getEndingLevel());
        }
        addLine("All-log result", plan.getProjectedLevel()
                + " (" + format(plan.getProjectedXp()) + " XP)");

        for (Map.Entry<PlanFiremakerLogType, Integer> entry : snapshot.getAvailableLogs().entrySet())
        {
            if (entry.getValue() > 0 && entry.getKey().ordinal() <= config.maximumLogType().ordinal())
            {
                addLine(entry.getKey().toString(), format(entry.getValue()));
            }
        }

        int locked = plan.getLockedAtEnd().values().stream().mapToInt(Integer::intValue).sum();
        if (locked > 0)
        {
            addLine("Locked at end", format(locked), WARNING);
        }
        addLine("Status", snapshot.getStatus());
        return super.render(graphics);
    }

    private void addLine(String left, String right)
    {
        addLine(left, right, Color.WHITE);
    }

    private void addLine(String left, String right, Color rightColor)
    {
        panelComponent.getChildren().add(LineComponent.builder()
                .left(left)
                .right(right)
                .rightColor(rightColor)
                .build());
    }

    private static String format(int value)
    {
        return String.format("%,d", value);
    }
}
