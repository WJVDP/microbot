package net.runelite.client.plugins.microbot.services;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Script;

@Slf4j
@Singleton
public class DefaultScriptLifecycleService implements ScriptLifecycleService
{
	private final PluginManager pluginManager;

	@Inject
	DefaultScriptLifecycleService(PluginManager pluginManager)
	{
		this.pluginManager = pluginManager;
	}

	@Override
	public List<Plugin> getActiveMicrobotPlugins()
	{
		return pluginManager.getActivePlugins().stream()
			.filter(plugin -> plugin.getClass().getPackage().getName().toLowerCase().contains("microbot"))
			.filter(plugin -> !plugin.getClass().getSimpleName().equalsIgnoreCase("QuestHelperPlugin")
				&& !plugin.getClass().getSimpleName().equalsIgnoreCase("MInventorySetupsPlugin")
				&& !plugin.getClass().getSimpleName().equalsIgnoreCase("MicrobotPlugin")
				&& !plugin.getClass().getSimpleName().equalsIgnoreCase("ShortestPathPlugin")
				&& !plugin.getClass().getSimpleName().equalsIgnoreCase("AntibanPlugin")
				&& !plugin.getClass().getSimpleName().equalsIgnoreCase("ExamplePlugin"))
			.collect(Collectors.toList());
	}

	@Override
	public List<Script> getActiveScripts()
	{
		return getActiveMicrobotPlugins().stream()
			.flatMap(plugin -> Arrays.stream(plugin.getClass().getDeclaredFields())
				.filter(field -> Script.class.isAssignableFrom(field.getType()))
				.map(field -> getScript(plugin, field)))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Override
	public void shutdown(Script script)
	{
		if (script != null)
		{
			script.shutdown();
		}
	}

	private static Script getScript(Plugin plugin, Field field)
	{
		field.setAccessible(true);
		try
		{
			return (Script) field.get(plugin);
		}
		catch (IllegalAccessException ex)
		{
			log.error("Error getting active script", ex);
			return null;
		}
	}
}
