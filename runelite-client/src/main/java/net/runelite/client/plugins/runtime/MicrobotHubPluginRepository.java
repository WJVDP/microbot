/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;

public class MicrobotHubPluginRepository implements PluginRepository
{
	private final Supplier<List<MicrobotPluginManifest>> manifestSupplier;

	public MicrobotHubPluginRepository(Supplier<List<MicrobotPluginManifest>> manifestSupplier)
	{
		this.manifestSupplier = manifestSupplier;
	}

	@Override
	public PluginArtifactSource getSource()
	{
		return PluginArtifactSource.MICROBOT_HUB;
	}

	@Override
	public List<PluginArtifact> discover() throws IOException
	{
		List<MicrobotPluginManifest> manifests = manifestSupplier.get();
		if (manifests == null)
		{
			return Collections.emptyList();
		}

		return manifests.stream()
			.map(MicrobotHubPluginRepository::toArtifact)
			.collect(Collectors.toList());
	}

	private static PluginArtifact toArtifact(MicrobotPluginManifest manifest)
	{
		return PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, manifest.getInternalName())
			.displayName(manifest.getDisplayName())
			.version(manifest.getVersion())
			.checksumSha256(manifest.getSha256())
			.minClientVersion(manifest.getMinClientVersion())
			.disabled(manifest.isDisable())
			.build();
	}
}
