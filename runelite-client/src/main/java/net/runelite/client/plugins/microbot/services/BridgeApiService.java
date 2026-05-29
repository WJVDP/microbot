package net.runelite.client.plugins.microbot.services;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.runtime.PluginRuntimeDiscoveryResult;

public interface BridgeApiService
{
	PluginManager getPluginManager();

	ConfigManager getConfigManager();

	PluginRuntimeDiscoveryResult discoverPluginArtifactStatus() throws IOException;

	boolean installPluginArtifact(String id, String version);

	boolean updatePluginArtifact(String id, String version);

	boolean removePluginArtifact(String id);

	Map<String, Object> getPluginHealthStatus();

	Map<String, Object> getStartupTimingStatus();

	Instant now();

	String getRuneLiteVersion();

	String getMicrobotVersion();
}
