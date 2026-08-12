package net.runelite.client.plugins.microbot.questhelper.automation;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questhelper.QuestHelperPlugin;
import net.runelite.client.plugins.microbot.questhelper.questhelpers.QuestHelper;
import net.runelite.client.plugins.microbot.questhelper.steps.NpcStep;
import net.runelite.client.plugins.microbot.questhelper.steps.ObjectStep;
import net.runelite.client.plugins.microbot.questhelper.steps.QuestStep;
import net.runelite.client.plugins.microbot.questhelper.steps.WidgetStep;

import java.util.Objects;

/** Captures a minimal immutable identity for the active Quest Helper step. */
public final class QuestStepSnapshotFactory
{
    private StepKey previousKey;
    private long revision;

    public QuestStepSnapshot capture(QuestHelperPlugin plugin)
    {
        if (plugin == null)
        {
            return null;
        }

        StepKey key = Microbot.getClientThread().runOnClientThreadOptional(() -> createKey(plugin))
                .orElse(null);
        if (key == null)
        {
            return null;
        }

        if (!Objects.equals(previousKey, key))
        {
            previousKey = key;
            revision++;
        }
        return new QuestStepSnapshot(key, revision);
    }

    public void reset()
    {
        previousKey = null;
        revision = 0;
    }

    private static StepKey createKey(QuestHelperPlugin plugin)
    {
        QuestHelper helper = plugin.getSelectedQuest();
        if (helper == null || helper.getCurrentStep() == null)
        {
            return null;
        }

        QuestStep active = helper.getCurrentStep().getActiveStep();
        if (active == null)
        {
            active = helper.getCurrentStep();
        }

        String questName = helper.getQuest().getName();
        int stage = helper.getVar();
        if (QuestRiskPolicy.MISTHALIN_MYSTERY.equals(questName) && (stage == 110 || stage == 111))
        {
            return StepKey.questStage(questName, stage,
                    QuestRiskPolicy.MISTHALIN_MIRROR_REFLECTION);
        }
        if (active instanceof ObjectStep)
        {
            return StepKey.object(questName, ((ObjectStep) active).allIds().get(0));
        }
        if (active instanceof NpcStep)
        {
            return StepKey.npc(questName, ((NpcStep) active).allIds().get(0));
        }
        if (active instanceof WidgetStep)
        {
            return StepKey.questStage(questName, stage,
                    QuestStepType.WIDGET.name() + '-' + active.getClass().getSimpleName());
        }
        return StepKey.questStage(questName, stage,
                QuestStepType.OTHER.name() + '-' + active.getClass().getSimpleName());
    }
}
