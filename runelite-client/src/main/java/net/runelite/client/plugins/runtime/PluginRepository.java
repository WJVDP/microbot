/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.IOException;
import java.util.List;

public interface PluginRepository
{
	PluginArtifactSource getSource();

	List<PluginArtifact> discover() throws IOException;
}
