package net.runelite.client.plugins.microbot.questhelper.automation;

import net.runelite.api.NpcID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QuestRiskPolicyTest
{
	private final QuestRiskPolicy policy = new QuestRiskPolicy();

	@Test
	public void vampyreSlayerBossGatesAreAlwaysManual()
	{
		assertManual(StepKey.object(QuestRiskPolicy.VAMPYRE_SLAYER, ObjectID.CRYPTSTAIRSDOWN));
		assertManual(StepKey.object(QuestRiskPolicy.VAMPYRE_SLAYER, ObjectID.VAMPCOFFIN));
		assertManual(StepKey.npc(QuestRiskPolicy.VAMPYRE_SLAYER, NpcID.COUNT_DRAYNOR));
	}

	@Test
	public void misthalinMirrorReflectionUsesStructuredQuestStageKey()
	{
		assertManual(StepKey.questStage(QuestRiskPolicy.MISTHALIN_MYSTERY, 110,
			QuestRiskPolicy.MISTHALIN_MIRROR_REFLECTION));
		assertManual(StepKey.questStage(QuestRiskPolicy.MISTHALIN_MYSTERY, 111,
			QuestRiskPolicy.MISTHALIN_MIRROR_REFLECTION));
		assertEquals(QuestRisk.SAFE_AUTOMATION, policy.classify(snapshot(StepKey.questStage(
			QuestRiskPolicy.MISTHALIN_MYSTERY, 110, "UNRELATED_STEP"))));
	}

	@Test
	public void manualRiskPrecedesOtherwiseValidAgentProposal()
	{
		QuestStepSnapshot snapshot = snapshot(StepKey.object(QuestRiskPolicy.VAMPYRE_SLAYER, ObjectID.VAMPCOFFIN));
		RecoveryProposal proposal = new RecoveryProposal(snapshot.getStepKey(), snapshot.getContextRevision(),
			RecoveryAction.INTERACT_WITH_OBSERVED_OBJECT);

		assertEquals(RecoveryProposalValidation.MANUAL_STEP, policy.validate(snapshot, proposal));
	}

	@Test
	public void rejectsProposalFromOlderContextRevision()
	{
		StepKey step = StepKey.object("A Quest", 123);
		RecoveryProposal proposal = new RecoveryProposal(step, 7, RecoveryAction.OPEN_QUEST_JOURNAL);

		assertEquals(RecoveryProposalValidation.STALE_CONTEXT, policy.validate(new QuestStepSnapshot(step, 8), proposal));
	}

	@Test
	public void rejectsProposalForDifferentStepEvenAtSameRevision()
	{
		QuestStepSnapshot current = new QuestStepSnapshot(StepKey.object("A Quest", 123), 8);
		RecoveryProposal proposal = new RecoveryProposal(StepKey.object("A Quest", 456), 8,
			RecoveryAction.OPEN_QUEST_JOURNAL);

		assertEquals(RecoveryProposalValidation.STEP_MISMATCH, policy.validate(current, proposal));
	}

	@Test
	public void acceptsSingleSafeProposalForCurrentSnapshot()
	{
		QuestStepSnapshot current = snapshot(StepKey.object("A Quest", 123));
		RecoveryProposal proposal = new RecoveryProposal(current.getStepKey(), current.getContextRevision(),
			RecoveryAction.OPEN_QUEST_JOURNAL);

		assertEquals(RecoveryProposalValidation.ACCEPTED, policy.validate(current, proposal));
	}

	private void assertManual(StepKey key)
	{
		assertEquals(QuestRisk.MANUAL_REQUIRED, policy.classify(snapshot(key)));
	}

	private QuestStepSnapshot snapshot(StepKey key)
	{
		return new QuestStepSnapshot(key, 4);
	}
}
