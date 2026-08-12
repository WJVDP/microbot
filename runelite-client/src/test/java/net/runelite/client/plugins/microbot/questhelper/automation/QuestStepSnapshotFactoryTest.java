package net.runelite.client.plugins.microbot.questhelper.automation;

import net.runelite.client.plugins.microbot.questhelper.steps.ObjectStep;
import net.runelite.client.plugins.microbot.questhelper.steps.WidgetStep;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class QuestStepSnapshotFactoryTest
{
	@Test
	public void objectKeyIncludesQuestStage()
	{
		ObjectStep step = new ObjectStep(null, 123, "Use the object.");

		assertEquals(StepKey.object("Quest", 20, 123),
			QuestStepSnapshotFactory.createKey("Quest", 20, step));
	}

	@Test
	public void widgetKeyIncludesStructuredWidgetIdentity()
	{
		WidgetStep step = new WidgetStep(null, "Click the control.", 554, 10);

		assertEquals(StepKey.widget("Quest", 75, 554, 10, -1),
			QuestStepSnapshotFactory.createKey("Quest", 75, step));
	}

	@Test
	public void dialoguePageChangesProgressFingerprint()
	{
		int first = QuestStepSnapshotFactory.dialogueProgressFingerprint(
			"Question", "First page", List.of("Continue"));
		int second = QuestStepSnapshotFactory.dialogueProgressFingerprint(
			"Question", "Second page", List.of("Continue"));

		assertNotEquals(first, second);
	}
}
