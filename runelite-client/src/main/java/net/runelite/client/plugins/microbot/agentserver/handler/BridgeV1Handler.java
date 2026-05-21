package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigSectionDescriptor;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManager;
import net.runelite.client.plugins.runtime.PluginArtifact;
import net.runelite.client.plugins.runtime.PluginRuntimeArtifactStatus;
import net.runelite.client.plugins.runtime.PluginRuntimeDiscoveryResult;

@Slf4j
public class BridgeV1Handler extends AgentHandler
{
	private static final String BASE_PATH = "/bridge/v1";
	private static final int MAX_EVENTS = 200;

	private final BridgeServices services;
	private final Deque<Map<String, Object>> events = new LinkedList<>();

	public BridgeV1Handler(Gson gson, MicrobotPluginManager microbotPluginManager)
	{
		this(gson, new DefaultBridgeServices(microbotPluginManager));
	}

	BridgeV1Handler(Gson gson, BridgeServices services)
	{
		super(gson);
		this.services = services;
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

		if ("/events".equals(subPath))
		{
			handleEvents(exchange);
			return;
		}

		if ("/runtime-health".equals(subPath))
		{
			handleRuntimeHealth(exchange);
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

		if (subPath.startsWith("/plugins/") && subPath.endsWith("/config/schema"))
		{
			handlePluginConfigSchema(exchange, subPath);
			return;
		}

		if (subPath.startsWith("/plugins/") && subPath.endsWith("/config"))
		{
			handlePluginConfig(exchange, subPath);
			return;
		}

		if (subPath.startsWith("/plugin-artifacts/") && subPath.endsWith("/install"))
		{
			handleArtifactCommand(exchange, subPath, "install");
			return;
		}

		if (subPath.startsWith("/plugin-artifacts/") && subPath.endsWith("/update"))
		{
			handleArtifactCommand(exchange, subPath, "update");
			return;
		}

		if (subPath.startsWith("/plugin-artifacts/") && subPath.endsWith("/remove"))
		{
			handleArtifactCommand(exchange, subPath, "remove");
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

		PluginManager pluginManager = services.getPluginManager();
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("bridgeVersion", "1");
		response.put("serverTime", services.now().toString());
		response.put("runeliteVersion", services.getRuneLiteVersion());
		response.put("microbotVersion", services.getMicrobotVersion());
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

		PluginRuntimeDiscoveryResult result = services.discoverPluginArtifactStatus();
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

		PluginManager pluginManager = services.getPluginManager();
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

		PluginManager pluginManager = services.getPluginManager();
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
		recordEvent("plugin.state", "info", id, start ? "start" : "stop", "completed",
			"Plugin " + (start ? "started" : "stopped"));
		sendJson(exchange, 200, response);
	}

	private void handleArtifactCommand(HttpExchange exchange, String subPath, String action) throws IOException
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

		String id = extractArtifactId(subPath, "/" + action);
		PluginRuntimeArtifactStatus artifactStatus = findArtifactStatus(id);
		if (artifactStatus == null)
		{
			sendJson(exchange, 404, errorResponse("Plugin artifact not found: " + id));
			return;
		}

		String version = null;
		if (!"remove".equals(action))
		{
			Map<String, Object> request;
			try
			{
				request = readOptionalJsonBody(exchange);
			}
			catch (IOException ex)
			{
				sendJson(exchange, 400, errorResponse("Invalid JSON body"));
				return;
			}
			Object versionValue = request.get("version");
			if (versionValue instanceof String && !((String) versionValue).isEmpty())
			{
				version = (String) versionValue;
			}
		}

		boolean accepted;
		if ("install".equals(action))
		{
			accepted = services.installPluginArtifact(id, version);
		}
		else if ("update".equals(action))
		{
			accepted = services.updatePluginArtifact(id, version);
		}
		else
		{
			accepted = services.removePluginArtifact(id);
		}

		if (!accepted)
		{
			sendJson(exchange, 404, errorResponse("Plugin artifact command could not be queued: " + id));
			return;
		}

		Map<String, Object> response = commandResponse(action, "pluginArtifact", id, "queued", true);
		response.put("version", version);
		recordEvent(artifactEventType(action), "info", id, action, "queued",
			"Plugin artifact " + action + " queued");
		sendJson(exchange, 202, response);
	}

	private void handlePluginConfigSchema(HttpExchange exchange, String subPath) throws IOException
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

		ConfigDescriptor descriptor = getPluginConfigDescriptor(extractPluginId(subPath, "/config/schema"));
		if (descriptor == null)
		{
			sendJson(exchange, 404, errorResponse("Plugin config not found"));
			return;
		}

		sendJson(exchange, 200, toConfigSchemaDto(descriptor));
	}

	private void handlePluginConfig(HttpExchange exchange, String subPath) throws IOException
	{
		String id = extractPluginId(subPath, "/config");
		ConfigDescriptor descriptor = getPluginConfigDescriptor(id);
		ConfigManager configManager = services.getConfigManager();
		if (descriptor == null || configManager == null)
		{
			sendJson(exchange, 404, errorResponse("Plugin config not found"));
			return;
		}

		if ("GET".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			sendJson(exchange, 200, toConfigValuesDto(configManager, descriptor));
			return;
		}

		try
		{
			requirePost(exchange);
		}
		catch (HttpMethodException ex)
		{
			sendJson(exchange, 405, errorResponse(ex.getMessage()));
			return;
		}

		Map<String, Object> request;
		try
		{
			request = readJsonBody(exchange);
		}
		catch (Exception ex)
		{
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		Map<String, Object> values = extractConfigValues(request);
		if (values.isEmpty())
		{
			sendJson(exchange, 400, errorResponse("Required: key and value, or values object"));
			return;
		}

		Map<String, ConfigItemDescriptor> writable = writableItemsByKey(descriptor);
		Set<String> changed = new HashSet<>();
		for (Map.Entry<String, Object> entry : values.entrySet())
		{
			ConfigItemDescriptor item = writable.get(entry.getKey());
			if (item == null)
			{
				sendJson(exchange, 400, errorResponse("Unknown or secret config key: " + entry.getKey()));
				return;
			}

			Object value = entry.getValue();
			if (value == null)
			{
				configManager.unsetConfiguration(descriptor.getGroup().value(), entry.getKey());
			}
			else
			{
				configManager.setConfiguration(descriptor.getGroup().value(), entry.getKey(), value.toString());
			}
			changed.add(entry.getKey());
		}

		Map<String, Object> response = toConfigValuesDto(configManager, descriptor);
		response.put("success", true);
		response.put("changed", changed);
		recordEvent("plugin.config", "info", id, "write", "completed",
			"Plugin config updated");
		sendJson(exchange, 200, response);
	}

	private void handleEvents(HttpExchange exchange) throws IOException
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

		List<Map<String, Object>> snapshot;
		synchronized (events)
		{
			snapshot = new ArrayList<>(events);
		}
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", snapshot.size());
		response.put("events", snapshot);
		sendJson(exchange, 200, response);
	}

	private void handleRuntimeHealth(HttpExchange exchange) throws IOException
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

		PluginManager pluginManager = services.getPluginManager();
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("serverTime", services.now().toString());
		response.put("pluginManagerAvailable", pluginManager != null);
		response.put("configManagerAvailable", services.getConfigManager() != null);
		response.put("pluginCount", pluginManager == null ? 0 : pluginManager.getPlugins().size());

		try
		{
			PluginRuntimeDiscoveryResult result = services.discoverPluginArtifactStatus();
			response.put("artifactStatusAvailable", true);
			response.put("artifactCount", result.getArtifacts().size());
			response.put("artifactErrors", result.hasErrors());
		}
		catch (Exception ex)
		{
			response.put("artifactStatusAvailable", false);
			response.put("artifactError", ex.getMessage());
		}

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
		dto.put("metadataSource", artifact.getMetadataSource().name());
		dto.put("entryClasses", artifact.getEntryClasses());
		dto.put("minClientVersion", artifact.getMinClientVersion());
		dto.put("checksumSha256", artifact.getChecksumSha256());
		dto.put("signature", artifact.getSignature());
		dto.put("installed", artifact.getArtifactFile() != null);
		dto.put("loadable", status.isLoadable());
		dto.put("errors", status.getErrors());
		return dto;
	}

	private ConfigDescriptor getPluginConfigDescriptor(String id)
	{
		PluginManager pluginManager = services.getPluginManager();
		ConfigManager configManager = services.getConfigManager();
		if (pluginManager == null || configManager == null)
		{
			return null;
		}

		Plugin plugin = findPlugin(pluginManager, id);
		if (plugin == null)
		{
			return null;
		}

		Config config = pluginManager.getPluginConfigProxy(plugin);
		if (config == null)
		{
			return null;
		}

		return configManager.getConfigDescriptor(config);
	}

	private static Map<String, Object> toConfigSchemaDto(ConfigDescriptor descriptor)
	{
		Map<String, Object> dto = new LinkedHashMap<>();
		dto.put("group", descriptor.getGroup().value());

		List<Map<String, Object>> sections = new ArrayList<>();
		for (ConfigSectionDescriptor section : descriptor.getSections())
		{
			Map<String, Object> sectionDto = new LinkedHashMap<>();
			sectionDto.put("key", section.key());
			sectionDto.put("name", section.name());
			sectionDto.put("description", section.getSection().description());
			sectionDto.put("position", section.position());
			sectionDto.put("closedByDefault", section.getSection().closedByDefault());
			sections.add(sectionDto);
		}
		dto.put("sections", sections);

		List<Map<String, Object>> items = new ArrayList<>();
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			ConfigItem configItem = item.getItem();
			Map<String, Object> itemDto = new LinkedHashMap<>();
			itemDto.put("key", configItem.keyName());
			itemDto.put("name", configItem.name());
			itemDto.put("description", configItem.description());
			itemDto.put("type", typeName(item.getType()));
			itemDto.put("position", configItem.position());
			itemDto.put("hidden", configItem.hidden());
			itemDto.put("secret", configItem.secret());
			itemDto.put("section", configItem.section());
			itemDto.put("warning", configItem.warning());
			Range range = item.getRange();
			if (range != null)
			{
				Map<String, Object> rangeDto = new LinkedHashMap<>();
				rangeDto.put("min", range.min());
				rangeDto.put("max", range.max());
				itemDto.put("range", rangeDto);
			}
			items.add(itemDto);
		}
		dto.put("items", items);
		return dto;
	}

	private static Map<String, Object> toConfigValuesDto(ConfigManager configManager, ConfigDescriptor descriptor)
	{
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("group", descriptor.getGroup().value());
		Map<String, Object> values = new LinkedHashMap<>();
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			String key = item.getItem().keyName();
			values.put(key, item.getItem().secret() ? null : configManager.getConfiguration(descriptor.getGroup().value(), key));
		}
		response.put("values", values);
		return response;
	}

	private static String typeName(Type type)
	{
		String name = type.getTypeName();
		int lastDot = name.lastIndexOf('.');
		return lastDot >= 0 ? name.substring(lastDot + 1) : name;
	}

	private static Map<String, ConfigItemDescriptor> writableItemsByKey(ConfigDescriptor descriptor)
	{
		Map<String, ConfigItemDescriptor> items = new LinkedHashMap<>();
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			if (!item.getItem().secret())
			{
				items.put(item.getItem().keyName(), item);
			}
		}
		return items;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> extractConfigValues(Map<String, Object> request)
	{
		Object values = request.get("values");
		if (values instanceof Map)
		{
			return (Map<String, Object>) values;
		}

		Object key = request.get("key");
		if (key instanceof String && request.containsKey("value"))
		{
			Map<String, Object> single = new LinkedHashMap<>();
			single.put((String) key, request.get("value"));
			return single;
		}
		return Collections.emptyMap();
	}

	private Map<String, Object> readOptionalJsonBody(HttpExchange exchange) throws IOException
	{
		String cl = exchange.getRequestHeaders().getFirst("Content-Length");
		if (cl == null || "0".equals(cl.trim()))
		{
			return Collections.emptyMap();
		}

		try
		{
			Map<String, Object> body = readJsonBody(exchange);
			return body == null ? Collections.emptyMap() : body;
		}
		catch (Exception ex)
		{
			throw new IOException("Invalid JSON body", ex);
		}
	}

	private PluginRuntimeArtifactStatus findArtifactStatus(String id) throws IOException
	{
		for (PluginRuntimeArtifactStatus status : services.discoverPluginArtifactStatus().getArtifacts())
		{
			if (Objects.equals(status.getArtifact().getId(), id))
			{
				return status;
			}
		}
		return null;
	}

	private static Map<String, Object> commandResponse(String action, String targetType, String id, String status, boolean accepted)
	{
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("commandId", UUID.randomUUID().toString());
		response.put("action", action);
		response.put("targetType", targetType);
		response.put("id", id);
		response.put("accepted", accepted);
		response.put("status", status);
		return response;
	}

	private static String artifactEventType(String action)
	{
		if ("update".equals(action))
		{
			return "plugin.update";
		}
		if ("remove".equals(action))
		{
			return "plugin.remove";
		}
		return "plugin.install";
	}

	private void recordEvent(String type, String level, String pluginId, String action, String status, String message)
	{
		Map<String, Object> event = new LinkedHashMap<>();
		event.put("id", UUID.randomUUID().toString());
		event.put("time", services.now().toString());
		event.put("type", type);
		event.put("level", level);
		event.put("source", "bridge-v1");
		event.put("pluginId", pluginId);
		event.put("action", action);
		event.put("status", status);
		event.put("message", message);
		synchronized (events)
		{
			events.addLast(event);
			while (events.size() > MAX_EVENTS)
			{
				events.removeFirst();
			}
		}
	}

	private static String extractPluginId(String subPath, String suffix)
	{
		return subPath.substring("/plugins/".length(), subPath.length() - suffix.length());
	}

	private static String extractArtifactId(String subPath, String suffix)
	{
		return subPath.substring("/plugin-artifacts/".length(), subPath.length() - suffix.length());
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

	interface BridgeServices
	{
		PluginManager getPluginManager();

		ConfigManager getConfigManager();

		PluginRuntimeDiscoveryResult discoverPluginArtifactStatus() throws IOException;

		boolean installPluginArtifact(String id, String version);

		boolean updatePluginArtifact(String id, String version);

		boolean removePluginArtifact(String id);

		default Instant now()
		{
			return Instant.now();
		}

		default String getRuneLiteVersion()
		{
			return RuneLiteProperties.getVersion();
		}

		default String getMicrobotVersion()
		{
			return RuneLiteProperties.getMicrobotVersion();
		}
	}

	private static final class DefaultBridgeServices implements BridgeServices
	{
		private final MicrobotPluginManager microbotPluginManager;

		private DefaultBridgeServices(MicrobotPluginManager microbotPluginManager)
		{
			this.microbotPluginManager = microbotPluginManager;
		}

		@Override
		public PluginManager getPluginManager()
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

		@Override
		public ConfigManager getConfigManager()
		{
			try
			{
				return Microbot.getConfigManager();
			}
			catch (Exception ex)
			{
				return null;
			}
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

		private MicrobotPluginManifest getManifest(String id)
		{
			if (microbotPluginManager == null)
			{
				return null;
			}
			return microbotPluginManager.getManifestMap().get(id);
		}
	}
}
