package net.runelite.client.plugins.microbot.questhelper.automation;

/** Outcome of validating an agent proposal against current observed state. */
public enum RecoveryProposalValidation
{
	ACCEPTED,
	STALE_CONTEXT,
	STEP_MISMATCH,
	MANUAL_STEP
}
