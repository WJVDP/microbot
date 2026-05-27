/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;

public class MicrobotHubPluginRepository implements PluginRepository
{
	private final Supplier<List<MicrobotPluginManifest>> manifestSupplier;
	private final File artifactDirectory;

	public MicrobotHubPluginRepository(Supplier<List<MicrobotPluginManifest>> manifestSupplier)
	{
		this(manifestSupplier, null);
	}

	public MicrobotHubPluginRepository(Supplier<List<MicrobotPluginManifest>> manifestSupplier, File artifactDirectory)
	{
		this.manifestSupplier = manifestSupplier;
		this.artifactDirectory = artifactDirectory;
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

		List<PluginArtifact> artifacts = new ArrayList<>(manifests.size());
		for (MicrobotPluginManifest manifest : manifests)
		{
			artifacts.add(toArtifact(manifest));
		}
		return artifacts;
	}

	private PluginArtifact toArtifact(MicrobotPluginManifest manifest)
	{
		File artifactFile = getArtifactFile(manifest);

		PluginArtifact.Builder builder = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, manifest.getInternalName())
			.displayName(manifest.getDisplayName())
			.version(manifest.getVersion())
			.checksumSha256(manifest.getSha256())
			.minClientVersion(manifest.getMinClientVersion())
			.pluginApiVersion(manifest.getPluginApiVersion())
			.disabled(manifest.isDisable())
			.metadataSource(PluginArtifactMetadataSource.HUB_MANIFEST);

		if (artifactFile != null)
		{
			List<String> entryClasses = PluginJarStubReader.readEntryClassesOrEmpty(artifactFile);
			boolean malformedManifest = PluginJarStubReader.hasMalformedManifest(artifactFile);
			PluginArtifactMetadataSource metadataSource = PluginArtifactMetadataSource.JAR_STUB;
			if (entryClasses.isEmpty() && !malformedManifest)
			{
				entryClasses = LegacyPluginDescriptorJarScanner.scanEntryClassesOrEmpty(artifactFile);
				metadataSource = entryClasses.isEmpty()
					? PluginArtifactMetadataSource.HUB_MANIFEST
					: PluginArtifactMetadataSource.LEGACY_PLUGIN_DESCRIPTOR_SCAN;
			}

			builder.artifactFile(artifactFile)
				.entryClasses(entryClasses)
				.malformedManifest(malformedManifest)
				.metadataSource(metadataSource);
		}

		return builder.build();
	}

	private File getArtifactFile(MicrobotPluginManifest manifest)
	{
		if (artifactDirectory == null)
		{
			return null;
		}

		File artifactFile = new File(artifactDirectory, manifest.getInternalName() + ".jar");
		return artifactFile.isFile() ? artifactFile : null;
	}
}
