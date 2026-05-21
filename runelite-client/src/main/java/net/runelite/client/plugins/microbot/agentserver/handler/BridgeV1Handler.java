package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManager;
import net.runelite.client.plugins.runtime.PluginArtifact;
import net.runelite.client.plugins.runtime.PluginRuntimeArtifactStatus;
import net.runelite.client.plugins.runtime.PluginRuntimeDiscoveryResult;

@Slf4j
public class BridgeV1Handler extends AgentHandler
{
	private static final String BASE_PATH = "/bridge/v1";

	private final MicrobotPluginManager microbotPluginManager;

	public BridgeV1Handler(Gson gson, MicrobotPluginManager microbotPluginManager)
	{
		super(gson);
		this.microbotPluginManager = microbotPluginManager;
	}

	@Override
	public String getPath()
	{
		return BASE_PATH;
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException
	{
		String subPath = getSubPath(exchange, BASE_PATH);
		if ("".equals(subPath) || "/".equals(subPath) || "/status".equals(subPath))
		{
			handleStatus(exchange);
			return;
		}

		if ("/plugins".equals(subPath))
		{
			handlePluginList(exchange);
			return;
		}

		if ("/plugin-artifacts".equals(subPath))
		{
			handlePluginArtifacts(exchange);
			return;
		}

		if (subPath.startsWith("/plugins/") && subPath.endsWith("/start"))
		{
			handlePluginCommand(exchange, subPath, true);
			return;
		}

		if (subPath.startsWith("/plugins/") && subPath.endsWith("/stop"))
		{
			handlePluginCommand(exchange, subPath, false);
			return;
		}

		sendJson(exchange, 404, errorResponse("Unknown endpoint: " + BASE_PATH + subPath));
	}

	private void handleStatus(HttpExchange exchange) throws IOException
	{
		try
		{
			requireGet(exchange);
		}
		catch (HttpMethodException ex)
		{
			sendJson(exchange, 405, errorResponse(ex.getMessage()));
			return;
		}

		PluginManager pluginManager = getPluginManager();
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("bridgeVersion", "1");
		response.put("serverTime", Instant.now().toString());
		response.put("runeliteVersion", RuneLiteProperties.getVersion());
		response.put("microbotVersion", RuneLiteProperties.getMicrobotVersion());
		response.put("pluginManagerAvailable", pluginManager != null);
		response.put("pluginCount", pluginManager == null ? 0 : pluginManager.getPlugins().size());
		sendJson(exchange, 200, response);
	}

	private void handlePluginArtifacts(HttpExchange exchange) throws IOException
	{
		try
		{
			requireGet(exchange);
		}
		catch (HttpMethodException ex)
		{
			sendJson(exchange, 405, errorResponse(ex.getMessage()));
			return;
		}

		PluginRuntimeDiscoveryResult result = microbotPluginManager.discoverPluginArtifactStatus();
		List<Map<String, Object>> artifacts = new ArrayList<>();
		for (PluginRuntimeArtifactStatus status : result.getArtifacts())
		{
			artifacts.add(toArtifactDto(status));
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", artifacts.size());
		response.put("hasErrors", result.hasErrors());
		response.put("artifacts", artifacts);
		sendJson(exchange, 200, response);
	}

	private void handlePluginList(HttpExchange exchange) throws IOException
	{
		try
		{
			requireGet(exchange);
		}
		catch (HttpMethodException ex)
		{
			sendJson(exchange, 405, errorResponse(ex.getMessage()));
			return;
		}

		PluginManager pluginManager = getPluginManager();
		if (pluginManager == null)
		{
			sendJson(exchange, 503, errorResponse("PluginManager not available"));
			return;
		}

		List<Map<String, Object>> plugins = new ArrayList<>();
		for (Plugin plugin : pluginManager.getPlugins())
		{
			plugins.add(toPluginDto(pluginManager, plugin));
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", plugins.size());
		response.put("plugins", plugins);
		sendJson(exchange, 200, response);
	}

	private void handlePluginCommand(HttpExchange exchange, String subPath, boolean start) throws IOException
	{
		try
		{
			requirePost(exchange);
		}
		catch (HttpMethodException ex)
		{
			sendJson(exchange, 405, errorResponse(ex.getMessage()));
			return;
		}

		PluginManager pluginManager = getPluginManager();
		if (pluginManager == null)
		{
			sendJson(exchange, 503, errorResponse("PluginManager not available"));
			return;
		}

		String id = extractPluginId(subPath, start ? "/start" : "/stop");
		Plugin plugin = findPlugin(pluginManager, id);
		if (plugin == null)
		{
			sendJson(exchange, 404, errorResponse("Plugin not found: " + id));
			return;
		}

		AtomicBoolean changed = new AtomicBoolean(false);
		AtomicReference<String> error = new AtomicReference<>();
		try
		{
			SwingUtilities.invokeAndWait(() -> {
				try
				{
					pluginManager.setPluginEnabled(plugin, start);
					changed.set(start ? pluginManager.startPlugin(plugin) : pluginManager.stopPlugin(plugin));
				}
				catch (PluginInstantiationException ex)
				{
					log.warn("Bridge plugin command failed for {}", plugin.getClass().getName(), ex);
					error.set(ex.getMessage());
				}
			});
		}
		catch (Exception ex)
		{
			sendJson(exchange, 500, errorResponse("Plugin command failed: " + ex.getMessage()));
			return;
		}

		if (error.get() != null)
		{
			sendJson(exchange, 500, errorResponse(error.get()));
			return;
		}

		Map<String, Object> response = toPluginDto(pluginManager, plugin);
		response.put("changed", changed.get());
		sendJson(exchange, 200, response);
	}

	private static Map<String, Object> toPluginDto(PluginManager pluginManager, Plugin plugin)
	{
		PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
		Map<String, Object> dto = new LinkedHashMap<>();
		dto.put("id", plugin.getClass().getName());
		dto.put("displayName", descriptor == null ? plugin.getClass().getSimpleName() : descriptor.name());
		dto.put("className", plugin.getClass().getName());
		dto.put("enabled", pluginManager.isPluginEnabled(plugin));
		dto.put("active", pluginManager.isActive(plugin));
		dto.put("hidden", descriptor != null && descriptor.hidden());
		dto.put("external", descriptor != null && descriptor.isExternal());
		dto.put("description", descriptor == null ? "" : descriptor.description());
		return dto;
	}

	private static Map<String, Object> toArtifactDto(PluginRuntimeArtifactStatus status)
	{
		PluginArtifact artifact = status.getArtifact();
		Map<String, Object> dto = new LinkedHashMap<>();
		dto.put("id", artifact.getId());
		dto.put("displayName", artifact.getDisplayName());
		dto.put("version", artifact.getVersion());
		dto.put("source", artifact.getSource().name());
		dto.put("entryClasses", artifact.getEntryClasses());
		dto.put("minClientVersion", artifact.getMinClientVersion());
		dto.put("checksumSha256", artifact.getChecksumSha256());
		dto.put("signature", artifact.getSignature());
		dto.put("installed", artifact.getArtifactFile() != null);
		dto.put("loadable", status.isLoadable());
		dto.put("errors", status.getErrors());
		return dto;
	}

	private static String extractPluginId(String subPath, String suffix)
	{
		return subPath.substring("/plugins/".length(), subPath.length() - suffix.length());
	}

	private static Plugin findPlugin(PluginManager pluginManager, String id)
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin.getClass().getName().equals(id))
			{
				return plugin;
			}
		}
		return null;
	}

	private static PluginManager getPluginManager()
	{
		try
		{
			return Microbot.getPluginManager();
		}
		catch (Exception ex)
		{
			return null;
		}
	}
}
