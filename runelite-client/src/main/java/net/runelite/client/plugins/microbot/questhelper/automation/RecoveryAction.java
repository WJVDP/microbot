package net.runelite.client.plugins.microbot.questhelper.automation;

/** Strictly allowlisted action categories an external recovery agent may propose. */
public enum RecoveryAction
{
	OPEN_QUEST_JOURNAL,
	WALK_TO_OBSERVED_LOCATION,
	INTERACT_WITH_OBSERVED_NPC,
	INTERACT_WITH_OBSERVED_OBJECT,
	CONTINUE_KNOWN_DIALOGUE,
	USE_REQUIRED_ITEM_ON_OBSERVED_TARGET,
	REQUEST_MANUAL_HELP
}
