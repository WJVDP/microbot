package net.runelite.client.plugins.microbot.questhelper.automation;

import java.util.Objects;

/** Immutable, redacted input to the hybrid-quest safety policy. */
public final class QuestStepSnapshot
{
	private final StepKey stepKey;
	private final long contextRevision;

	public QuestStepSnapshot(StepKey stepKey, long contextRevision)
	{
		this.stepKey = Objects.requireNonNull(stepKey, "stepKey");
		if (contextRevision < 0)
		{
			throw new IllegalArgumentException("contextRevision must not be negative");
		}
		this.contextRevision = contextRevision;
	}

	public StepKey getStepKey()
	{
		return stepKey;
	}

	public long getContextRevision()
	{
		return contextRevision;
	}
}
