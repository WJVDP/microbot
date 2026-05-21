/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PluginArtifacts
{
	private PluginArtifacts()
	{
	}

	static List<PluginArtifact> requireUniqueIds(List<PluginArtifact> artifacts)
	{
		Set<String> ids = new HashSet<>();
		for (PluginArtifact artifact : artifacts)
		{
			if (!ids.add(artifact.getId()))
			{
				throw new IllegalArgumentException("Duplicate plugin artifact id: " + artifact.getId());
			}
		}
		return artifacts;
	}
}
