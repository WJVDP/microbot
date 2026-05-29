/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.Map;

public interface PluginArtifactStatusProjection
{
	Map<String, Object> toBridgeV1ArtifactDto();
}
