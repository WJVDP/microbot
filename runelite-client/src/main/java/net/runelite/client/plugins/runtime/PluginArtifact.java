/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public final class PluginArtifact
{
	private final PluginArtifactSource source;
	private final PluginArtifactMetadataSource metadataSource;
	private final String id;
	private final String displayName;
	private final String version;
	@Nullable
	private final String checksumSha256;
	@Nullable
	private final String signature;
	@Nullable
	private final File artifactFile;
	private final List<String> entryClasses;
	@Nullable
	private final String minClientVersion;
	@Nullable
	private final String pluginApiVersion;
	private final boolean disabled;
	private final boolean malformedManifest;
	@Nullable
	private final PluginCapabilityManifest capabilityManifest;

	private PluginArtifact(
		PluginArtifactSource source,
		PluginArtifactMetadataSource metadataSource,
		String id,
		String displayName,
		String version,
		@Nullable String checksumSha256,
		@Nullable String signature,
		@Nullable File artifactFile,
		List<String> entryClasses,
		@Nullable String minClientVersion,
		@Nullable String pluginApiVersion,
		boolean disabled,
		boolean malformedManifest,
		@Nullable PluginCapabilityManifest capabilityManifest)
	{
		this.source = Objects.requireNonNull(source, "source");
		this.metadataSource = Objects.requireNonNull(metadataSource, "metadataSource");
		this.id = requireText(id, "id");
		this.displayName = displayName == null || displayName.trim().isEmpty() ? id : displayName;
		this.version = version == null ? "" : version;
		this.checksumSha256 = emptyToNull(checksumSha256);
		this.signature = emptyToNull(signature);
		this.artifactFile = artifactFile;
		this.entryClasses = Collections.unmodifiableList(new ArrayList<>(entryClasses));
		this.minClientVersion = emptyToNull(minClientVersion);
		this.pluginApiVersion = emptyToNull(pluginApiVersion);
		this.disabled = disabled;
		this.malformedManifest = malformedManifest;
		this.capabilityManifest = capabilityManifest;
	}

	public static Builder builder(PluginArtifactSource source, String id)
	{
		return new Builder(source, id);
	}

	public PluginArtifactSource getSource()
	{
		return source;
	}

	public PluginArtifactMetadataSource getMetadataSource()
	{
		return metadataSource;
	}

	public String getId()
	{
		return id;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getVersion()
	{
		return version;
	}

	@Nullable
	public String getChecksumSha256()
	{
		return checksumSha256;
	}

	@Nullable
	public String getSignature()
	{
		return signature;
	}

	@Nullable
	public File getArtifactFile()
	{
		return artifactFile;
	}

	public List<String> getEntryClasses()
	{
		return entryClasses;
	}

	@Nullable
	public String getMinClientVersion()
	{
		return minClientVersion;
	}

	@Nullable
	public String getPluginApiVersion()
	{
		return pluginApiVersion;
	}

	public boolean isDisabled()
	{
		return disabled;
	}

	public boolean hasMalformedManifest()
	{
		return malformedManifest;
	}

	@Nullable
	public PluginCapabilityManifest getCapabilityManifest()
	{
		return capabilityManifest;
	}

	private static String requireText(String value, String field)
	{
		if (value == null || value.trim().isEmpty())
		{
			throw new IllegalArgumentException(field + " is required");
		}
		return value;
	}

	@Nullable
	private static String emptyToNull(@Nullable String value)
	{
		return value == null || value.trim().isEmpty() ? null : value;
	}

	public static final class Builder
	{
		private final PluginArtifactSource source;
		private final String id;
		private PluginArtifactMetadataSource metadataSource;
		private String displayName;
		private String version;
		private String checksumSha256;
		private String signature;
		private File artifactFile;
		private List<String> entryClasses = Collections.emptyList();
		private String minClientVersion;
		private String pluginApiVersion;
		private boolean disabled;
		private boolean malformedManifest;
		private PluginCapabilityManifest capabilityManifest;

		private Builder(PluginArtifactSource source, String id)
		{
			this.source = source;
			this.id = id;
		}

		public Builder displayName(String displayName)
		{
			this.displayName = displayName;
			return this;
		}

		public Builder metadataSource(PluginArtifactMetadataSource metadataSource)
		{
			this.metadataSource = metadataSource;
			return this;
		}

		public Builder version(String version)
		{
			this.version = version;
			return this;
		}

		public Builder checksumSha256(String checksumSha256)
		{
			this.checksumSha256 = checksumSha256;
			return this;
		}

		public Builder signature(String signature)
		{
			this.signature = signature;
			return this;
		}

		public Builder artifactFile(File artifactFile)
		{
			this.artifactFile = artifactFile;
			return this;
		}

		public Builder entryClasses(String... entryClasses)
		{
			this.entryClasses = entryClasses == null ? Collections.emptyList() : Arrays.asList(entryClasses);
			return this;
		}

		public Builder entryClasses(List<String> entryClasses)
		{
			this.entryClasses = entryClasses == null ? Collections.emptyList() : entryClasses;
			return this;
		}

		public Builder minClientVersion(String minClientVersion)
		{
			this.minClientVersion = minClientVersion;
			return this;
		}

		public Builder pluginApiVersion(int pluginApiVersion)
		{
			this.pluginApiVersion = Integer.toString(pluginApiVersion);
			return this;
		}

		public Builder pluginApiVersion(String pluginApiVersion)
		{
			this.pluginApiVersion = pluginApiVersion;
			return this;
		}

		public Builder disabled(boolean disabled)
		{
			this.disabled = disabled;
			return this;
		}

		public Builder malformedManifest(boolean malformedManifest)
		{
			this.malformedManifest = malformedManifest;
			return this;
		}

		public Builder capabilityManifest(PluginCapabilityManifest capabilityManifest)
		{
			this.capabilityManifest = capabilityManifest;
			return this;
		}

		public PluginArtifact build()
		{
			return new PluginArtifact(
				source,
				metadataSource == null ? defaultMetadataSource(source, artifactFile, entryClasses) : metadataSource,
				id,
				displayName,
				version,
				checksumSha256,
				signature,
				artifactFile,
				entryClasses,
				minClientVersion,
				pluginApiVersion,
				disabled,
				malformedManifest,
				capabilityManifest);
		}

		private static PluginArtifactMetadataSource defaultMetadataSource(
			PluginArtifactSource source,
			@Nullable File artifactFile,
			List<String> entryClasses)
		{
			if (source == PluginArtifactSource.CORE)
			{
				return PluginArtifactMetadataSource.CORE_ANNOTATION;
			}
			if (artifactFile != null && entryClasses != null && !entryClasses.isEmpty())
			{
				return PluginArtifactMetadataSource.JAR_STUB;
			}
			if (artifactFile != null)
			{
				return PluginArtifactMetadataSource.FILE_NAME;
			}
			return PluginArtifactMetadataSource.HUB_MANIFEST;
		}
	}
}
