package net.runelite.client.plugins.microbot.services;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.health.PluginHealthRegistry;
import net.runelite.client.plugins.health.StartupTimingRegistry;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManager;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;
import net.runelite.client.plugins.runtime.PluginRuntimeDiscoveryResult;

@Singleton
public class DefaultBridgeApiService implements BridgeApiService
{
	private final PluginManager pluginManager;
	private final ConfigManager configManager;
	private final MicrobotPluginManager microbotPluginManager;
	private final PluginHealthRegistry pluginHealthRegistry;
	private final StartupTimingRegistry startupTimingRegistry;

	@Inject
	DefaultBridgeApiService(
		PluginManager pluginManager,
		ConfigManager configManager,
		MicrobotPluginManager microbotPluginManager,
		PluginHealthRegistry pluginHealthRegistry,
		StartupTimingRegistry startupTimingRegistry)
	{
		this.pluginManager = pluginManager;
		this.configManager = configManager;
		this.microbotPluginManager = microbotPluginManager;
		this.pluginHealthRegistry = pluginHealthRegistry;
		this.startupTimingRegistry = startupTimingRegistry;
	}

	@Override
	public PluginManager getPluginManager()
	{
		return pluginManager;
	}

	@Override
	public ConfigManager getConfigManager()
	{
		return configManager;
	}

	@Override
	public PluginRuntimeDiscoveryResult discoverPluginArtifactStatus() throws IOException
	{
		return microbotPluginManager.discoverPluginArtifactStatus();
	}

	@Override
	public boolean installPluginArtifact(String id, String version)
	{
		MicrobotPluginManifest manifest = getManifest(id);
		if (manifest == null)
		{
			return false;
		}
		microbotPluginManager.installPlugin(manifest, version);
		return true;
	}

	@Override
	public boolean updatePluginArtifact(String id, String version)
	{
		MicrobotPluginManifest manifest = getManifest(id);
		if (manifest == null)
		{
			return false;
		}
		microbotPluginManager.updatePlugin(manifest, version);
		return true;
	}

	@Override
	public boolean removePluginArtifact(String id)
	{
		MicrobotPluginManifest manifest = getManifest(id);
		if (manifest == null)
		{
			return false;
		}
		microbotPluginManager.removePlugin(manifest);
		return true;
	}

	@Override
	public Map<String, Object> getPluginHealthStatus()
	{
		return pluginHealthRegistry.status();
	}

	@Override
	public Map<String, Object> getStartupTimingStatus()
	{
		return startupTimingRegistry.status();
	}

	@Override
	public Instant now()
	{
		return Instant.now();
	}

	@Override
	public String getRuneLiteVersion()
	{
		return RuneLiteProperties.getVersion();
	}

	@Override
	public String getMicrobotVersion()
	{
		return RuneLiteProperties.getMicrobotVersion();
	}

	private MicrobotPluginManifest getManifest(String id)
	{
		if (microbotPluginManager == null)
		{
			return null;
		}
		return microbotPluginManager.getManifestMap().get(id);
	}
}
