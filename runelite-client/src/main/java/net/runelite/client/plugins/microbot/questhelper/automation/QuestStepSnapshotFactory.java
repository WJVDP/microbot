package net.runelite.client.plugins.microbot.questhelper.automation;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questhelper.QuestHelperPlugin;
import net.runelite.client.plugins.microbot.questhelper.questhelpers.QuestHelper;
import net.runelite.client.plugins.microbot.questhelper.steps.NpcStep;
import net.runelite.client.plugins.microbot.questhelper.steps.ObjectStep;
import net.runelite.client.plugins.microbot.questhelper.steps.QuestStep;
import net.runelite.client.plugins.microbot.questhelper.steps.WidgetStep;
import net.runelite.client.plugins.microbot.questhelper.steps.widget.WidgetDetails;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Captures a minimal immutable identity for the active Quest Helper step. */
public final class QuestStepSnapshotFactory
{
	private StepKey previousKey;
	private int previousProgressFingerprint;
	private long revision;

	public QuestStepSnapshot capture(QuestHelperPlugin plugin)
	{
		if (plugin == null)
		{
			return null;
		}

		Observation observation = Microbot.getClientThread().runOnClientThreadOptional(() -> createObservation(plugin))
				.orElse(null);
		if (observation == null)
		{
			return null;
		}

		if (!Objects.equals(previousKey, observation.key) ||
			previousProgressFingerprint != observation.progressFingerprint)
		{
			previousKey = observation.key;
			previousProgressFingerprint = observation.progressFingerprint;
			revision++;
		}
		return new QuestStepSnapshot(observation.key, revision);
	}

	public void reset()
	{
		previousKey = null;
		previousProgressFingerprint = 0;
		revision = 0;
	}

	private static Observation createObservation(QuestHelperPlugin plugin)
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
		StepKey key = createKey(questName, stage, active);
		return key == null ? null : new Observation(key, dialogueProgressFingerprint());
	}

	static StepKey createKey(String questName, int stage, QuestStep active)
	{
		if (active == null)
		{
			return null;
		}
		if (QuestRiskPolicy.MISTHALIN_MYSTERY.equals(questName) && (stage == 110 || stage == 111))
		{
			return StepKey.questStage(questName, stage,
					QuestRiskPolicy.MISTHALIN_MIRROR_REFLECTION);
		}
		if (active instanceof ObjectStep)
		{
			return StepKey.object(questName, stage, ((ObjectStep) active).allIds().get(0));
		}
		if (active instanceof NpcStep)
		{
			return StepKey.npc(questName, stage, ((NpcStep) active).allIds().get(0));
		}
		if (active instanceof WidgetStep)
		{
			List<WidgetDetails> widgets = ((WidgetStep) active).getWidgetDetails();
			if (widgets.isEmpty())
			{
				return StepKey.other(questName, stage, active.getClass().getSimpleName());
			}
			WidgetDetails widget = widgets.get(0);
			return StepKey.widget(questName, stage, widget.groupID, widget.childID,
				widget.childChildID);
		}
		return StepKey.other(questName, stage, active.getClass().getSimpleName());
	}

	private static int dialogueProgressFingerprint()
	{
		if (!Rs2Dialogue.isInDialogue())
		{
			return 0;
		}

		String question = Rs2Dialogue.getQuestion();
		String dialogue = Rs2Dialogue.getDialogueText();
		List<String> options = Rs2Dialogue.getDialogueOptions().stream()
			.map(widget -> widget.getText() == null ? "" : widget.getText())
			.collect(Collectors.toList());
		return dialogueProgressFingerprint(question, dialogue, options);
	}

	static int dialogueProgressFingerprint(String question, String dialogue, List<String> options)
	{
		return Objects.hash(true, question, dialogue, options);
	}

	private static final class Observation
	{
		private final StepKey key;
		private final int progressFingerprint;

		private Observation(StepKey key, int progressFingerprint)
		{
			this.key = key;
			this.progressFingerprint = progressFingerprint;
		}
	}
}
