/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PluginCapabilityManifest
{
	private final String id;
	private final String name;
	private final String version;
	private final PluginArtifactSource source;
	private final List<String> capabilities;
	private final Map<String, String> capabilityRationale;
	private final int permissionSchemaVersion;
	private final boolean declaredAtBuildTime;

	private PluginCapabilityManifest(
		String id,
		String name,
		String version,
		PluginArtifactSource source,
		List<String> capabilities,
		Map<String, String> capabilityRationale,
		int permissionSchemaVersion,
		boolean declaredAtBuildTime)
	{
		this.id = requireText(id, "id");
		this.name = requireText(name, "name");
		this.version = requireText(version, "version");
		this.source = Objects.requireNonNull(source, "source");
		this.capabilities = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(capabilities, "capabilities")));
		this.capabilityRationale = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(capabilityRationale, "capabilityRationale")));
		this.permissionSchemaVersion = permissionSchemaVersion;
		this.declaredAtBuildTime = declaredAtBuildTime;
	}

	public static Builder builder(String id, String name, String version, PluginArtifactSource source)
	{
		return new Builder(id, name, version, source);
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getVersion()
	{
		return version;
	}

	public PluginArtifactSource getSource()
	{
		return source;
	}

	public List<String> getCapabilities()
	{
		return capabilities;
	}

	public Map<String, String> getCapabilityRationale()
	{
		return capabilityRationale;
	}

	public int getPermissionSchemaVersion()
	{
		return permissionSchemaVersion;
	}

	public boolean isDeclaredAtBuildTime()
	{
		return declaredAtBuildTime;
	}

	private static String requireText(String value, String field)
	{
		if (value == null || value.trim().isEmpty())
		{
			throw new IllegalArgumentException(field + " is required");
		}
		return value;
	}

	public static final class Builder
	{
		private final String id;
		private final String name;
		private final String version;
		private final PluginArtifactSource source;
		private List<String> capabilities = Collections.emptyList();
		private Map<String, String> capabilityRationale = Collections.emptyMap();
		private int permissionSchemaVersion = 1;
		private boolean declaredAtBuildTime = true;

		private Builder(String id, String name, String version, PluginArtifactSource source)
		{
			this.id = id;
			this.name = name;
			this.version = version;
			this.source = source;
		}

		public Builder capabilities(List<String> capabilities)
		{
			this.capabilities = capabilities == null ? Collections.emptyList() : capabilities;
			return this;
		}

		public Builder capabilityRationale(Map<String, String> capabilityRationale)
		{
			this.capabilityRationale = capabilityRationale == null ? Collections.emptyMap() : capabilityRationale;
			return this;
		}

		public Builder permissionSchemaVersion(int permissionSchemaVersion)
		{
			this.permissionSchemaVersion = permissionSchemaVersion;
			return this;
		}

		public Builder declaredAtBuildTime(boolean declaredAtBuildTime)
		{
			this.declaredAtBuildTime = declaredAtBuildTime;
			return this;
		}

		public PluginCapabilityManifest build()
		{
			return new PluginCapabilityManifest(
				id,
				name,
				version,
				source,
				capabilities,
				capabilityRationale,
				permissionSchemaVersion,
				declaredAtBuildTime);
		}
	}
}
