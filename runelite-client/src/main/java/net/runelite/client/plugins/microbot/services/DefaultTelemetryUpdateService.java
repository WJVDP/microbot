package net.runelite.client.plugins.microbot.services;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.MicrobotConfig;

@Singleton
public class DefaultTelemetryUpdateService implements TelemetryUpdateService
{
	private final ConfigManager configManager;
	private final boolean noUpdate;

	@Inject
	DefaultTelemetryUpdateService(ConfigManager configManager, @Named("noupdate") boolean noUpdate)
	{
		this.configManager = configManager;
		this.noUpdate = noUpdate;
	}

	@Override
	public boolean isTelemetryDisabled()
	{
		if (Boolean.getBoolean("microbot.disableTelemetry"))
		{
			return true;
		}

		try
		{
			String value = configManager.getConfiguration(MicrobotConfig.configGroup, MicrobotConfig.keyDisableTelemetry);
			return Boolean.parseBoolean(value);
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	@Override
	public boolean isUpdateCheckDisabled()
	{
		return noUpdate;
	}
}
