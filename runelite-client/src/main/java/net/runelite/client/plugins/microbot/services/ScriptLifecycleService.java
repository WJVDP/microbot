package net.runelite.client.plugins.microbot.services;

import java.util.List;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Script;

public interface ScriptLifecycleService
{
	List<Plugin> getActiveMicrobotPlugins();

	List<Script> getActiveScripts();

	void shutdown(Script script);
}
