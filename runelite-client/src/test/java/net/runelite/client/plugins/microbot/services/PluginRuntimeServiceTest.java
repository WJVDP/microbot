package net.runelite.client.plugins.microbot.services;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.runelite.client.plugins.runtime.PluginArtifact;
import net.runelite.client.plugins.runtime.PluginArtifactSource;
import net.runelite.client.plugins.runtime.PluginRepository;
import net.runelite.client.plugins.runtime.PluginRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PluginRuntimeServiceTest
{
	@Test
	public void pluginRuntimeCanBeUsedThroughServiceInterface() throws IOException
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, "local-test")
			.displayName("Local Test")
			.version("1.0.0")
			.entryClasses("example.LocalTestPlugin")
			.minClientVersion("1.0.0")
			.build();
		PluginRepository repository = new PluginRepository()
		{
			@Override
			public PluginArtifactSource getSource()
			{
				return PluginArtifactSource.LOCAL_DIRECTORY;
			}

			@Override
			public List<PluginArtifact> discover()
			{
				return Collections.singletonList(artifact);
			}
		};

		PluginRuntimeService service = new PluginRuntime(Collections.singletonList(repository));

		assertEquals(1, service.getRepositories().size());
		assertEquals(1, service.discover().size());
		assertFalse(service.discoverStatus().hasErrors());
	}
}
