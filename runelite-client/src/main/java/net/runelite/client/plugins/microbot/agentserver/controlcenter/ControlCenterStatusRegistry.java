package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for optional plugin status providers. Plugins must unregister
 * during shutdown; the control-center service also removes providers whenever it
 * observes that their plugin is stopped.
 */
public final class ControlCenterStatusRegistry
{
	private static final ConcurrentHashMap<String, ControlCenterStatusProvider> PROVIDERS = new ConcurrentHashMap<>();

	private ControlCenterStatusRegistry()
	{
	}

	public static void register(String pluginId, ControlCenterStatusProvider provider)
	{
		if (pluginId == null || provider == null)
		{
			throw new IllegalArgumentException("pluginId and provider are required");
		}
		PROVIDERS.put(pluginId, provider);
	}

	public static void unregister(String pluginId)
	{
		if (pluginId != null)
		{
			PROVIDERS.remove(pluginId);
		}
	}

	static ControlCenterStatusSnapshot snapshot(String pluginId)
	{
		ControlCenterStatusProvider provider = PROVIDERS.get(pluginId);
		return provider != null ? provider.snapshot() : null;
	}

	static void clear()
	{
		PROVIDERS.clear();
	}
}
