/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.runelite.client.externalplugins.PluginHubManifest;

public class RuneLiteHubPluginRepository implements PluginRepository
{
	private final Supplier<PluginHubManifest.ManifestFull> manifestSupplier;
	private final File artifactDirectory;

	public RuneLiteHubPluginRepository(Supplier<PluginHubManifest.ManifestFull> manifestSupplier)
	{
		this(manifestSupplier, null);
	}

	public RuneLiteHubPluginRepository(Supplier<PluginHubManifest.ManifestFull> manifestSupplier, File artifactDirectory)
	{
		this.manifestSupplier = manifestSupplier;
		this.artifactDirectory = artifactDirectory;
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

	private PluginArtifact toArtifact(PluginHubManifest.JarData jar, PluginHubManifest.DisplayData display)
	{
		String displayName = display == null ? jar.getDisplayName() : display.getDisplayName();
		String version = display == null ? "" : display.getVersion();
		File artifactFile = getArtifactFile(jar);
		List<String> entryClasses = Collections.emptyList();
		boolean malformedManifest = false;
		PluginArtifactMetadataSource metadataSource = PluginArtifactMetadataSource.HUB_MANIFEST;
		if (artifactFile != null)
		{
			entryClasses = PluginJarStubReader.readEntryClassesOrEmpty(artifactFile);
			malformedManifest = PluginJarStubReader.hasMalformedManifest(artifactFile);
			if (!entryClasses.isEmpty())
			{
				metadataSource = PluginArtifactMetadataSource.JAR_STUB;
			}
			else if (!malformedManifest)
			{
				entryClasses = LegacyPluginDescriptorJarScanner.scanEntryClassesOrEmpty(artifactFile);
				metadataSource = entryClasses.isEmpty()
					? PluginArtifactMetadataSource.HUB_MANIFEST
					: PluginArtifactMetadataSource.LEGACY_PLUGIN_DESCRIPTOR_SCAN;
			}
		}

		PluginArtifact.Builder builder = PluginArtifact.builder(PluginArtifactSource.RUNELITE_HUB, jar.getInternalName())
			.displayName(displayName)
			.version(version)
			.checksumSha256(toHexSha256(jar.getJarHash()))
			.entryClasses(entryClasses)
			.malformedManifest(malformedManifest)
			.metadataSource(metadataSource);

		if (artifactFile != null)
		{
			builder.artifactFile(artifactFile);
		}

		return builder.build();
	}

	private File getArtifactFile(PluginHubManifest.JarData jar)
	{
		if (artifactDirectory == null || jar.getJarHash() == null)
		{
			return null;
		}

		File artifactFile = new File(artifactDirectory, jar.getInternalName() + "_" + jar.getJarHash() + ".jar");
		return artifactFile.isFile() ? artifactFile : null;
	}

	private static String toHexSha256(String jarHash)
	{
		if (jarHash == null)
		{
			return null;
		}

		try
		{
			byte[] bytes = Base64.getUrlDecoder().decode(jarHash);
			if (bytes.length != 32)
			{
				return jarHash;
			}

			StringBuilder builder = new StringBuilder(bytes.length * 2);
			for (byte value : bytes)
			{
				builder.append(String.format("%02x", value));
			}
			return builder.toString();
		}
		catch (IllegalArgumentException ex)
		{
			return jarHash;
		}
	}
}
