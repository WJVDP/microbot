package net.runelite.client.plugins.microbot.questhelper.automation;

import org.junit.After;
import org.junit.Test;

import static net.runelite.client.plugins.microbot.questhelper.automation.QuestAutomationLease.Owner.LEGACY_QUEST_SCRIPT;
import static net.runelite.client.plugins.microbot.questhelper.automation.QuestAutomationLease.Owner.PLAN_QUESTING;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class QuestAutomationLeaseTest
{
    @After
    public void tearDown()
    {
        QuestAutomationLease.clearForTests();
    }

    @Test
    public void onlyOneControllerOwnsQuestInteraction()
    {
        assertTrue(QuestAutomationLease.acquire(PLAN_QUESTING));
        assertTrue(QuestAutomationLease.acquire(PLAN_QUESTING));
        assertFalse(QuestAutomationLease.acquire(LEGACY_QUEST_SCRIPT));
        assertSame(PLAN_QUESTING, QuestAutomationLease.currentOwner());
    }

    @Test
    public void nonOwnerCannotReleaseLease()
    {
        assertTrue(QuestAutomationLease.acquire(PLAN_QUESTING));
        QuestAutomationLease.release(LEGACY_QUEST_SCRIPT);
        assertSame(PLAN_QUESTING, QuestAutomationLease.currentOwner());

        QuestAutomationLease.release(PLAN_QUESTING);
        assertNull(QuestAutomationLease.currentOwner());
        assertTrue(QuestAutomationLease.acquire(LEGACY_QUEST_SCRIPT));
    }
}
