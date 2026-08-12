package net.runelite.client.plugins.microbot.questhelper.automation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StepKeyTest
{
	@Test
	public void objectStepsAtDifferentQuestStagesHaveDistinctKeys()
	{
		StepKey first = StepKey.object("Quest", 10, 123);
		StepKey second = StepKey.object("Quest", 20, 123);

		assertNotEquals(first, second);
		assertEquals(10, first.getQuestStage());
		assertEquals(20, second.getQuestStage());
	}

	@Test
	public void widgetSubstepsAtTheSameStageHaveDistinctKeys()
	{
		StepKey first = StepKey.widget("Quest", 75, 554, 10, -1);
		StepKey second = StepKey.widget("Quest", 75, 554, 11, -1);

		assertNotEquals(first, second);
	}
}
