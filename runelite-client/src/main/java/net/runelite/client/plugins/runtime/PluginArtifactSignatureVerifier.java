/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface PluginArtifactSignatureVerifier
{
	PluginArtifactSignatureVerifier ACCEPT_UNSIGNED = (artifact, artifactFile) -> PluginArtifactSignatureVerification.TRUSTED;

	PluginArtifactSignatureVerification verify(PluginArtifact artifact, File artifactFile) throws IOException;
}
