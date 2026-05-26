/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

public enum PluginArtifactSignatureClassification
{
	TRUSTED_MICROBOT,
	TRUSTED_RUNELITE_HUB,
	UNSIGNED_LOCAL,
	UNKNOWN_SIGNER,
	INVALID_SIGNATURE,
	MALFORMED_SIGNATURE,
	UNSIGNED_BLOCKED
}
