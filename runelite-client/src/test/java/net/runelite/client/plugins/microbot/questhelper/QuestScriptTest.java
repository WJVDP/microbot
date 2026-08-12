package net.runelite.client.plugins.microbot.questhelper;

import net.runelite.client.plugins.microbot.questhelper.steps.DetailedQuestStep;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class QuestScriptTest
{
	@Test
	public void targetlessPassiveDetailedStepIsNotActionable()
	{
		QuestScript script = new QuestScript();

		assertFalse(script.applyStep(new DetailedQuestStep(null, "Wait for the cutscene.")));
	}
}
