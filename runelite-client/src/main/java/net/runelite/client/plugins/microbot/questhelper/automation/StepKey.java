package net.runelite.client.plugins.microbot.questhelper.automation;

import java.util.Objects;

/**
 * Stable identifier for a Quest Helper step. It deliberately does not include
 * display text, which is authored content and may change independently of the
 * game target.
 */
public final class StepKey
{
	private final String questName;
	private final QuestStepType type;
	private final int targetId;
	private final String semanticId;

	private StepKey(String questName, QuestStepType type, int targetId, String semanticId)
	{
		this.questName = requireText(questName, "questName");
		this.type = Objects.requireNonNull(type, "type");
		this.targetId = targetId;
		this.semanticId = Objects.requireNonNull(semanticId, "semanticId");
	}

	public static StepKey object(String questName, int objectId)
	{
		return new StepKey(questName, QuestStepType.OBJECT, objectId, "");
	}

	public static StepKey npc(String questName, int npcId)
	{
		return new StepKey(questName, QuestStepType.NPC, npcId, "");
	}

	/**
	 * Identifies a non-target step by a Quest Helper stage and a reviewed,
	 * code-owned semantic identifier.
	 */
	public static StepKey questStage(String questName, int stage, String semanticId)
	{
		requireText(semanticId, "semanticId");
		return new StepKey(questName, QuestStepType.QUEST_STAGE, stage, semanticId);
	}

	public String getQuestName()
	{
		return questName;
	}

	public QuestStepType getType()
	{
		return type;
	}

	public int getTargetId()
	{
		return targetId;
	}

	public String getSemanticId()
	{
		return semanticId;
	}

	private static String requireText(String value, String name)
	{
		if (value == null || value.trim().isEmpty())
		{
			throw new IllegalArgumentException(name + " must not be blank");
		}
		return value;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof StepKey))
		{
			return false;
		}
		StepKey that = (StepKey) other;
		return targetId == that.targetId && questName.equals(that.questName) && type == that.type && semanticId.equals(that.semanticId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(questName, type, targetId, semanticId);
	}

	@Override
	public String toString()
	{
		return questName + ':' + type + ':' + targetId + ':' + semanticId;
	}
}
