package net.runelite.client.plugins.microbot.questhelper.automation;

import java.util.Objects;

/** Agent advice tied to the exact snapshot from which it was generated. */
public final class RecoveryProposal
{
	private final StepKey stepKey;
	private final long contextRevision;
	private final RecoveryAction action;

	public RecoveryProposal(StepKey stepKey, long contextRevision, RecoveryAction action)
	{
		this.stepKey = Objects.requireNonNull(stepKey, "stepKey");
		if (contextRevision < 0)
		{
			throw new IllegalArgumentException("contextRevision must not be negative");
		}
		this.contextRevision = contextRevision;
		this.action = Objects.requireNonNull(action, "action");
	}

	public StepKey getStepKey()
	{
		return stepKey;
	}

	public long getContextRevision()
	{
		return contextRevision;
	}

	public RecoveryAction getAction()
	{
		return action;
	}
}
