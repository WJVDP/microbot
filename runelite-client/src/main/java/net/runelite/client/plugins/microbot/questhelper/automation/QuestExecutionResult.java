package net.runelite.client.plugins.microbot.questhelper.automation;

/** Result returned by a bounded deterministic or approved recovery action. */
public enum QuestExecutionResult
{
	PROGRESS,
	NO_PROGRESS,
	RETRYABLE_FAILURE,
	MANUAL_REQUIRED,
	BLOCKED
}
