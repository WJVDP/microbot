package net.runelite.client.plugins.microbot.services;

import java.io.IOException;
import java.util.List;
import net.runelite.client.plugins.runtime.PluginArtifact;
import net.runelite.client.plugins.runtime.PluginRepository;
import net.runelite.client.plugins.runtime.PluginRuntimeDiscoveryResult;

public interface PluginRuntimeService
{
	List<PluginRepository> getRepositories();

	List<PluginArtifact> discover() throws IOException;

	PluginRuntimeDiscoveryResult discoverStatus() throws IOException;
}
