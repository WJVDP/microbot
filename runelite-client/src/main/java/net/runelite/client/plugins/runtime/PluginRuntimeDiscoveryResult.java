/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.Collections;
import java.util.List;

public final class PluginRuntimeDiscoveryResult
{
	private final List<PluginRuntimeArtifactStatus> artifacts;

	PluginRuntimeDiscoveryResult(List<PluginRuntimeArtifactStatus> artifacts)
	{
		this.artifacts = Collections.unmodifiableList(artifacts);
	}

	public List<PluginRuntimeArtifactStatus> getArtifacts()
	{
		return artifacts;
	}

	public boolean hasErrors()
	{
		return artifacts.stream().anyMatch(status -> !status.isLoadable());
	}
}
