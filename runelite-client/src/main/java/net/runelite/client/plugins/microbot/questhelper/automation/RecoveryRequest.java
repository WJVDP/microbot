package net.runelite.client.plugins.microbot.questhelper.automation;

import java.util.Objects;

/** A bounded request for advice after deterministic recovery has failed. */
public final class RecoveryRequest
{
	private final QuestStepSnapshot snapshot;
	private final int failedAttempts;

	public RecoveryRequest(QuestStepSnapshot snapshot, int failedAttempts)
	{
		this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
		if (failedAttempts < 0)
		{
			throw new IllegalArgumentException("failedAttempts must not be negative");
		}
		this.failedAttempts = failedAttempts;
	}

	public QuestStepSnapshot getSnapshot()
	{
		return snapshot;
	}

	public int getFailedAttempts()
	{
		return failedAttempts;
	}
}
