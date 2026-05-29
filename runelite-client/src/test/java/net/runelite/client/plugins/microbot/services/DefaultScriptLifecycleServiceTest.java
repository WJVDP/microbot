package net.runelite.client.plugins.microbot.services;

import java.util.Collections;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Script;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultScriptLifecycleServiceTest
{
	@PluginDescriptor(name = "Service Test")
	public static final class ServiceTestPlugin extends Plugin
	{
		private final TestScript script = new TestScript();
	}

	private static final class TestScript extends Script
	{
		private boolean shutDown;

		@Override
		public void shutdown()
		{
			shutDown = true;
		}
	}

	@Test
	public void discoversScriptsWithoutLaunchingClient()
	{
		ServiceTestPlugin plugin = new ServiceTestPlugin();
		PluginManager pluginManager = mock(PluginManager.class);
		when(pluginManager.getActivePlugins()).thenReturn(Collections.singletonList(plugin));

		DefaultScriptLifecycleService service = new DefaultScriptLifecycleService(pluginManager);

		assertEquals(1, service.getActiveMicrobotPlugins().size());
		assertEquals(1, service.getActiveScripts().size());
		assertSame(plugin.script, service.getActiveScripts().get(0));
	}

	@Test
	public void shutsDownScriptWithoutLaunchingClient()
	{
		TestScript script = new TestScript();
		DefaultScriptLifecycleService service = new DefaultScriptLifecycleService(mock(PluginManager.class));

		service.shutdown(script);

		org.junit.Assert.assertTrue(script.shutDown);
	}
}
