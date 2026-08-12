package net.runelite.client.plugins.microbot.questhelper.automation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/**
 * Reviewed pilot safety catalog. Only exact, structured entries may reach the
 * deterministic executor; every unknown step fails closed to player control.
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
		registerMisthalinMystery(risks);
		registerVampyreSlayer(risks);
		risks.put(StepKey.object(VAMPYRE_SLAYER, 2, ObjectID.CRYPTSTAIRSDOWN), QuestRisk.MANUAL_REQUIRED);
		risks.put(StepKey.object(VAMPYRE_SLAYER, 2, ObjectID.VAMPCOFFIN), QuestRisk.MANUAL_REQUIRED);
		risks.put(StepKey.npc(VAMPYRE_SLAYER, 2, NpcID.COUNT_DRAYNOR), QuestRisk.MANUAL_REQUIRED);
		risks.put(StepKey.questStage(MISTHALIN_MYSTERY, 110, MISTHALIN_MIRROR_REFLECTION), QuestRisk.MANUAL_REQUIRED);
		risks.put(StepKey.questStage(MISTHALIN_MYSTERY, 111, MISTHALIN_MIRROR_REFLECTION), QuestRisk.MANUAL_REQUIRED);
		REVIEWED_RISKS = Collections.unmodifiableMap(risks);
	}

	public QuestRisk classify(QuestStepSnapshot snapshot)
	{
		return REVIEWED_RISKS.getOrDefault(snapshot.getStepKey(), QuestRisk.MANUAL_REQUIRED);
	}

	private static void registerMisthalinMystery(Map<StepKey, QuestRisk> risks)
	{
		safe(risks,
			StepKey.npc(MISTHALIN_MYSTERY, 0, NpcID.MISTMYST_ABIGALE_LUM_VIS),
			StepKey.npc(MISTHALIN_MYSTERY, 5, NpcID.MISTMYST_ABIGALE_LUM_VIS));

		for (int stage : new int[] {10, 15})
		{
			safe(risks,
				StepKey.object(MISTHALIN_MYSTERY, stage, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
				StepKey.object(MISTHALIN_MYSTERY, stage, ObjectID.MISTMYST_EMPTY_BUCKET),
				StepKey.object(MISTHALIN_MYSTERY, stage, ObjectID.MISTMYST_BARREL));
		}
		safe(risks,
			StepKey.object(MISTHALIN_MYSTERY, 20, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 20, ObjectID.MISTMYST_EMPTY_BUCKET),
			StepKey.object(MISTHALIN_MYSTERY, 20, ObjectID.MISTMYST_BARREL),
			StepKey.object(MISTHALIN_MYSTERY, 25, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 25, ObjectID.MISTMYST_BARREL),
			StepKey.object(MISTHALIN_MYSTERY, 25, ObjectID.MISTMYST_FRONT_DOORL),
			StepKey.object(MISTHALIN_MYSTERY, 30, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 30, ObjectID.MISTMYST_TABLE_KNIFE),
			StepKey.object(MISTHALIN_MYSTERY, 30, ObjectID.MISTMYST_DOOR_REDTOPAZ),
			StepKey.object(MISTHALIN_MYSTERY, 35, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 35, ObjectID.MISTMYST_CLUE_LIBRARY),
			StepKey.other(MISTHALIN_MYSTERY, 35, "DetailedQuestStep"),
			StepKey.object(MISTHALIN_MYSTERY, 40, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 40, ObjectID.MISTMYST_TABLE_KNIFE),
			StepKey.object(MISTHALIN_MYSTERY, 40, ObjectID.MISTMYST_PAINTING),
			StepKey.object(MISTHALIN_MYSTERY, 45, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 45, ObjectID.MISTMYST_PAINTING),
			StepKey.object(MISTHALIN_MYSTERY, 45, ObjectID.MISTMYST_DOOR_RUBY));

		safe(risks,
			StepKey.object(MISTHALIN_MYSTERY, 50, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 50, ObjectID.MISTMYST_SHELVES_TINDERBOX),
			StepKey.object(MISTHALIN_MYSTERY, 50, ObjectID.MISTMYST_CANDLE4),
			StepKey.object(MISTHALIN_MYSTERY, 50, ObjectID.MISTMYST_CANDLE3),
			StepKey.object(MISTHALIN_MYSTERY, 50, ObjectID.MISTMYST_CANDLE1),
			StepKey.object(MISTHALIN_MYSTERY, 50, ObjectID.MISTMYST_CANDLE2),
			StepKey.object(MISTHALIN_MYSTERY, 55, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 55, ObjectID.MISTMYST_SHELVES_TINDERBOX),
			StepKey.object(MISTHALIN_MYSTERY, 55, ObjectID.MISTMYST_EXPLOSIVE_BARREL),
			StepKey.object(MISTHALIN_MYSTERY, 60, ObjectID.MISTMYST_DOOR_RUBY),
			StepKey.object(MISTHALIN_MYSTERY, 65, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 65, ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE),
			StepKey.object(MISTHALIN_MYSTERY, 65, ObjectID.MISTMYST_TREE),
			StepKey.object(MISTHALIN_MYSTERY, 70, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 70, ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE),
			StepKey.object(MISTHALIN_MYSTERY, 70, ObjectID.MISTMYST_CLUE_OUTSIDE),
			StepKey.other(MISTHALIN_MYSTERY, 70, "DetailedQuestStep"));

		safe(risks,
			StepKey.object(MISTHALIN_MYSTERY, 75, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 75, ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE),
			StepKey.object(MISTHALIN_MYSTERY, 75, ObjectID.MISTMYST_PIANO),
			StepKey.widgetComponent(MISTHALIN_MYSTERY, 75, InterfaceID.MistmystPiano.LABEL_D1),
			StepKey.widgetComponent(MISTHALIN_MYSTERY, 75, InterfaceID.MistmystPiano.LABEL_E1),
			StepKey.widgetComponent(MISTHALIN_MYSTERY, 75, InterfaceID.MistmystPiano.LABEL_A2),
			StepKey.object(MISTHALIN_MYSTERY, 80, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 80, ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE),
			StepKey.object(MISTHALIN_MYSTERY, 80, ObjectID.MISTMYST_PIANO),
			StepKey.object(MISTHALIN_MYSTERY, 80, ObjectID.MISTMYST_DOOR_EMERALD),
			StepKey.object(MISTHALIN_MYSTERY, 85, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 85, ObjectID.MISTMYST_DOOR_DIAMOND),
			StepKey.object(MISTHALIN_MYSTERY, 90, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 90, ObjectID.MISTMYST_CLUE_KITCHEN),
			StepKey.other(MISTHALIN_MYSTERY, 90, "DetailedQuestStep"),
			StepKey.object(MISTHALIN_MYSTERY, 95, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 95, ObjectID.MISTMYST_TABLE_KNIFE),
			StepKey.object(MISTHALIN_MYSTERY, 95, ObjectID.MISTMYST_FIREPLACE));

		safe(risks,
			StepKey.object(MISTHALIN_MYSTERY, 100, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 100, ObjectID.MISTMYST_FIREPLACE),
			StepKey.widget(MISTHALIN_MYSTERY, 100, 555, 19, -1),
			StepKey.widget(MISTHALIN_MYSTERY, 100, 555, 4, -1),
			StepKey.widget(MISTHALIN_MYSTERY, 100, 555, 11, -1),
			StepKey.widget(MISTHALIN_MYSTERY, 100, 555, 23, -1),
			StepKey.widget(MISTHALIN_MYSTERY, 100, 555, 7, -1),
			StepKey.widget(MISTHALIN_MYSTERY, 100, 555, 15, -1),
			StepKey.object(MISTHALIN_MYSTERY, 105, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 105, ObjectID.MISTMYST_FIREPLACE),
			StepKey.object(MISTHALIN_MYSTERY, 105, ObjectID.MISTMYST_DOOR_SAPPHIRE),
			StepKey.object(MISTHALIN_MYSTERY, 115, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 115, ObjectID.MISTMYST_DOOR_SAPPHIRE),
			StepKey.other(MISTHALIN_MYSTERY, 115, "DetailedQuestStep"),
			StepKey.object(MISTHALIN_MYSTERY, 120, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 120, ObjectID.MISTMYST_DOOR_SAPPHIRE),
			StepKey.other(MISTHALIN_MYSTERY, 120, "DetailedQuestStep"),
			StepKey.npc(MISTHALIN_MYSTERY, 120, NpcID.MISTMYST_ABIGALE_KILLER_ATTACKABLE),
			StepKey.object(MISTHALIN_MYSTERY, 125, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.object(MISTHALIN_MYSTERY, 125, ObjectID.MISTMYST_DOOR_SAPPHIRE),
			StepKey.object(MISTHALIN_MYSTERY, 130, ObjectID.MISTMYST_BOAT_LUMBRIDGE),
			StepKey.npc(MISTHALIN_MYSTERY, 130, NpcID.MISTMYST_MANDY_POST_VIS));
	}

	private static void registerVampyreSlayer(Map<StepKey, QuestRisk> risks)
	{
		safe(risks,
			StepKey.npc(VAMPYRE_SLAYER, 0, NpcID.MORGAN),
			StepKey.object(VAMPYRE_SLAYER, 1, ObjectID.STAIRS),
			StepKey.object(VAMPYRE_SLAYER, 1, ObjectID.GARLICCUPBOARDOPEN),
			StepKey.npc(VAMPYRE_SLAYER, 1, NpcID.DR_HARLOW),
			StepKey.object(VAMPYRE_SLAYER, 2, ObjectID.STAIRS),
			StepKey.object(VAMPYRE_SLAYER, 2, ObjectID.GARLICCUPBOARDOPEN),
			StepKey.npc(VAMPYRE_SLAYER, 2, NpcID.DR_HARLOW),
			StepKey.object(VAMPYRE_SLAYER, 2, ObjectID.HAUNTEDDOORL));
	}

	private static void safe(Map<StepKey, QuestRisk> risks, StepKey... keys)
	{
		for (StepKey key : keys)
		{
			risks.put(key, QuestRisk.SAFE_AUTOMATION);
		}
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
