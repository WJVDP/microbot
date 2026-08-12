package net.runelite.client.plugins.microbot.questhelper.automation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.NpcID;
import net.runelite.api.gameval.ObjectID;

/**
 * Reviewed pilot safety catalog. Unknown steps are executable only by the
 * deterministic executor; an agent can advise on them after it is stuck.
 */
public final class QuestRiskPolicy
{
	public static final String VAMPYRE_SLAYER = "Vampyre Slayer";
	public static final String MISTHALIN_MYSTERY = "Misthalin Mystery";
	public static final String MISTHALIN_MIRROR_REFLECTION = "MIRROR_REFLECTION";

	private static final Map<StepKey, QuestRisk> REVIEWED_RISKS;

	static
	{
		Map<StepKey, QuestRisk> risks = new HashMap<>();
		risks.put(StepKey.object(VAMPYRE_SLAYER, ObjectID.CRYPTSTAIRSDOWN), QuestRisk.MANUAL_REQUIRED);
		risks.put(StepKey.object(VAMPYRE_SLAYER, ObjectID.VAMPCOFFIN), QuestRisk.MANUAL_REQUIRED);
		risks.put(StepKey.npc(VAMPYRE_SLAYER, NpcID.COUNT_DRAYNOR), QuestRisk.MANUAL_REQUIRED);
		risks.put(StepKey.questStage(MISTHALIN_MYSTERY, 110, MISTHALIN_MIRROR_REFLECTION), QuestRisk.MANUAL_REQUIRED);
		risks.put(StepKey.questStage(MISTHALIN_MYSTERY, 111, MISTHALIN_MIRROR_REFLECTION), QuestRisk.MANUAL_REQUIRED);
		REVIEWED_RISKS = Collections.unmodifiableMap(risks);
	}

	public QuestRisk classify(QuestStepSnapshot snapshot)
	{
		return REVIEWED_RISKS.getOrDefault(snapshot.getStepKey(), QuestRisk.SAFE_AUTOMATION);
	}

	/**
	 * Manual gates take precedence over a matching context or otherwise valid
	 * agent advice. A proposal never survives a changed snapshot revision.
	 */
	public RecoveryProposalValidation validate(QuestStepSnapshot current, RecoveryProposal proposal)
	{
		if (current.getContextRevision() != proposal.getContextRevision())
		{
			return RecoveryProposalValidation.STALE_CONTEXT;
		}
		if (!current.getStepKey().equals(proposal.getStepKey()))
		{
			return RecoveryProposalValidation.STEP_MISMATCH;
		}
		if (classify(current) == QuestRisk.MANUAL_REQUIRED)
		{
			return RecoveryProposalValidation.MANUAL_STEP;
		}
		return RecoveryProposalValidation.ACCEPTED;
	}
}
