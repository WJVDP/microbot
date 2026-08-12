package net.runelite.client.plugins.microbot.planquesting;

import net.runelite.client.plugins.microbot.questhelper.automation.QuestAutomationLease;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;

import static net.runelite.client.plugins.microbot.questhelper.automation.QuestAutomationLease.Owner.PLAN_QUESTING;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PlanQuestingScriptTest
{
	@After
	public void tearDown()
	{
		QuestAutomationLease.release(PLAN_QUESTING);
	}

	@Test
	public void completedSessionReleasesAutomationLease() throws Exception
	{
		PlanQuestingScript script = new PlanQuestingScript();
		assertTrue(QuestAutomationLease.acquire(PLAN_QUESTING));
		setLeaseHeld(script);

		script.onState(PlanQuestingScript.State.COMPLETE);

		assertNull(QuestAutomationLease.currentOwner());
	}

	@Test
	public void erroredSessionReleasesAutomationLease() throws Exception
	{
		PlanQuestingScript script = new PlanQuestingScript();
		assertTrue(QuestAutomationLease.acquire(PLAN_QUESTING));
		setLeaseHeld(script);

		script.onState(PlanQuestingScript.State.ERROR);

		assertNull(QuestAutomationLease.currentOwner());
	}

	private void setLeaseHeld(PlanQuestingScript script) throws Exception
	{
		Field field = PlanQuestingScript.class.getDeclaredField("leaseHeld");
		field.setAccessible(true);
		field.set(script, true);
	}
}
