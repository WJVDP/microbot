package net.runelite.client.plugins.microbot.services;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.MicrobotConfig;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultTelemetryUpdateServiceTest
{
	@After
	public void tearDown()
	{
		System.clearProperty("microbot.disableTelemetry");
	}

	@Test
	public void readsTelemetryConfigWithoutLaunchingClient()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(MicrobotConfig.configGroup, MicrobotConfig.keyDisableTelemetry)).thenReturn("true");

		DefaultTelemetryUpdateService service = new DefaultTelemetryUpdateService(configManager, false);

		assertTrue(service.isTelemetryDisabled());
		assertFalse(service.isUpdateCheckDisabled());
	}

	@Test
	public void systemPropertyOverridesTelemetryConfig()
	{
		System.setProperty("microbot.disableTelemetry", "true");
		DefaultTelemetryUpdateService service = new DefaultTelemetryUpdateService(mock(ConfigManager.class), true);

		assertTrue(service.isTelemetryDisabled());
		assertTrue(service.isUpdateCheckDisabled());
	}
}
