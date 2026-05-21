/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.runelite.client.externalplugins.PluginHubManifest;

public class RuneLiteHubPluginRepository implements PluginRepository
{
	private final Supplier<PluginHubManifest.ManifestFull> manifestSupplier;

	public RuneLiteHubPluginRepository(Supplier<PluginHubManifest.ManifestFull> manifestSupplier)
	{
		this.manifestSupplier = manifestSupplier;
	}

	@Override
	public PluginArtifactSource getSource()
	{
		return PluginArtifactSource.RUNELITE_HUB;
	}

	@Override
	public List<PluginArtifact> discover() throws IOException
	{
		PluginHubManifest.ManifestFull manifest = manifestSupplier.get();
		if (manifest == null || manifest.getJars() == null)
		{
			return Collections.emptyList();
		}

		Map<String, PluginHubManifest.DisplayData> displayByName = manifest.getDisplay() == null
			? Collections.emptyMap()
			: manifest.getDisplay().stream()
				.collect(Collectors.toMap(PluginHubManifest.DisplayData::getInternalName, d -> d, (a, b) -> a));

		return manifest.getJars().stream()
			.map(jar -> toArtifact(jar, displayByName.get(jar.getInternalName())))
			.collect(Collectors.toList());
	}

	private static PluginArtifact toArtifact(PluginHubManifest.JarData jar, PluginHubManifest.DisplayData display)
	{
		String displayName = display == null ? jar.getDisplayName() : display.getDisplayName();
		String version = display == null ? "" : display.getVersion();

		return PluginArtifact.builder(PluginArtifactSource.RUNELITE_HUB, jar.getInternalName())
			.displayName(displayName)
			.version(version)
			.checksumSha256(jar.getJarHash())
			.build();
	}
}
