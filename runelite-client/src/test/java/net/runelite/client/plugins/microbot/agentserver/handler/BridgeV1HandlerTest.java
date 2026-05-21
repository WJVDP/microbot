package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.runtime.PluginArtifact;
import net.runelite.client.plugins.runtime.PluginArtifactSource;
import net.runelite.client.plugins.runtime.PluginRepository;
import net.runelite.client.plugins.runtime.PluginRuntime;
import net.runelite.client.plugins.runtime.PluginRuntimeDiscoveryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BridgeV1HandlerTest
{
	private static final String TOKEN = "bridge-token";

	private final Gson gson = new Gson();
	private HttpServer server;
	private int port;
	private FakeBridgeServices services;
	private PluginManager pluginManager;
	private ConfigManager configManager;
	private TestPlugin plugin;

	@ConfigGroup("bridgetest")
	public interface TestConfig extends Config
	{
		@ConfigItem(
			keyName = "enabled",
			name = "Enabled",
			description = "Enable test setting"
		)
		default boolean enabled()
		{
			return false;
		}
	}

	@PluginDescriptor(
		name = "Bridge Test",
		description = "Bridge contract test plugin",
		enabledByDefault = false
	)
	public static final class TestPlugin extends Plugin
	{
	}

	@Before
	public void setUp() throws Exception
	{
		plugin = new TestPlugin();
		pluginManager = mock(PluginManager.class);
		configManager = mock(ConfigManager.class);
		services = new FakeBridgeServices(pluginManager, configManager);

		when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(plugin));
		when(pluginManager.isPluginEnabled(plugin)).thenReturn(true);
		when(pluginManager.isActive(plugin)).thenReturn(true);
		when(pluginManager.startPlugin(plugin)).thenReturn(true);
		when(pluginManager.stopPlugin(plugin)).thenReturn(true);

		Config config = mock(TestConfig.class);
		ConfigDescriptor descriptor = testConfigDescriptor();
		when(pluginManager.getPluginConfigProxy(plugin)).thenReturn(config);
		when(configManager.getConfigDescriptor(config)).thenReturn(descriptor);
		when(configManager.getConfiguration("bridgetest", "enabled")).thenReturn("true");

		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/bridge/v1", new BridgeV1Handler(gson, services));
		AgentHandler.setTokenSupplier(() -> TOKEN);
		server.start();
		port = server.getAddress().getPort();
	}

	@After
	public void tearDown()
	{
		if (server != null)
		{
			server.stop(0);
		}
		AgentHandler.setTokenSupplier(null);
	}

	@Test
	public void bridgeRequiresToken() throws Exception
	{
		Response response = request("GET", "/bridge/v1/status", null, false);

		assertEquals(404, response.code);
		assertEquals("Not Found", response.body);
	}

	@Test
	public void statusShapeIsStable() throws Exception
	{
		JsonObject json = requestJson("GET", "/bridge/v1/status", null);

		assertEquals("1", json.get("bridgeVersion").getAsString());
		assertEquals("2026-05-21T00:00:00Z", json.get("serverTime").getAsString());
		assertEquals("test-runelite", json.get("runeliteVersion").getAsString());
		assertEquals("test-microbot", json.get("microbotVersion").getAsString());
		assertTrue(json.get("pluginManagerAvailable").getAsBoolean());
		assertEquals(1, json.get("pluginCount").getAsInt());
	}

	@Test
	public void pluginListShapeIsStable() throws Exception
	{
		JsonObject json = requestJson("GET", "/bridge/v1/plugins", null);
		JsonObject dto = json.getAsJsonArray("plugins").get(0).getAsJsonObject();

		assertEquals(1, json.get("count").getAsInt());
		assertEquals(TestPlugin.class.getName(), dto.get("id").getAsString());
		assertEquals("Bridge Test", dto.get("displayName").getAsString());
		assertEquals(TestPlugin.class.getName(), dto.get("className").getAsString());
		assertTrue(dto.get("enabled").getAsBoolean());
		assertTrue(dto.get("active").getAsBoolean());
		assertFalse(dto.get("hidden").getAsBoolean());
		assertFalse(dto.get("external").getAsBoolean());
		assertEquals("Bridge contract test plugin", dto.get("description").getAsString());
	}

	@Test
	public void artifactStatusShapeIsStable() throws Exception
	{
		JsonObject json = requestJson("GET", "/bridge/v1/plugin-artifacts", null);
		JsonObject dto = json.getAsJsonArray("artifacts").get(0).getAsJsonObject();

		assertEquals(1, json.get("count").getAsInt());
		assertTrue(json.get("hasErrors").getAsBoolean());
		assertEquals("test-artifact", dto.get("id").getAsString());
		assertEquals("Test Artifact", dto.get("displayName").getAsString());
		assertEquals("1.0.0", dto.get("version").getAsString());
		assertEquals("MICROBOT_HUB", dto.get("source").getAsString());
		assertEquals("HUB_MANIFEST", dto.get("metadataSource").getAsString());
		assertEquals("example.TestPlugin", dto.getAsJsonArray("entryClasses").get(0).getAsString());
		assertEquals("1.0.0", dto.get("minClientVersion").getAsString());
		assertEquals("abc123", dto.get("checksumSha256").getAsString());
		assertFalse(dto.get("installed").getAsBoolean());
		assertFalse(dto.get("loadable").getAsBoolean());
		assertEquals("Plugin artifact file is missing", dto.getAsJsonArray("errors").get(0).getAsString());
	}

	@Test
	public void pluginCommandResponseShapeIsStable() throws Exception
	{
		JsonObject json = requestJson("POST", "/bridge/v1/plugins/" + TestPlugin.class.getName() + "/start", "{}");

		assertEquals(TestPlugin.class.getName(), json.get("id").getAsString());
		assertTrue(json.get("changed").getAsBoolean());
		verify(pluginManager).setPluginEnabled(plugin, true);
		verify(pluginManager).startPlugin(plugin);
	}

	@Test
	public void artifactInstallUpdateRemoveCommandsAreQueued() throws Exception
	{
		JsonObject install = requestJson("POST", "/bridge/v1/plugin-artifacts/test-artifact/install", "{\"version\":\"1.0.0\"}", 202);
		JsonObject update = requestJson("POST", "/bridge/v1/plugin-artifacts/test-artifact/update", "{}", 202);
		JsonObject remove = requestJson("POST", "/bridge/v1/plugin-artifacts/test-artifact/remove", "{}", 202);

		assertEquals("install", install.get("action").getAsString());
		assertEquals("pluginArtifact", install.get("targetType").getAsString());
		assertEquals("test-artifact", install.get("id").getAsString());
		assertTrue(install.get("accepted").getAsBoolean());
		assertEquals("queued", install.get("status").getAsString());
		assertNotNull(install.get("commandId").getAsString());
		assertEquals(List.of("install:test-artifact:1.0.0", "update:test-artifact:null", "remove:test-artifact"), services.commands);

		assertEquals("update", update.get("action").getAsString());
		assertEquals("remove", remove.get("action").getAsString());
	}

	@Test
	public void configSchemaAndSettingsCanBeReadAndWritten() throws Exception
	{
		JsonObject schema = requestJson("GET", "/bridge/v1/plugins/" + TestPlugin.class.getName() + "/config/schema", null);
		JsonObject values = requestJson("GET", "/bridge/v1/plugins/" + TestPlugin.class.getName() + "/config", null);
		JsonObject write = requestJson("POST", "/bridge/v1/plugins/" + TestPlugin.class.getName() + "/config", "{\"key\":\"enabled\",\"value\":false}");

		assertEquals("bridgetest", schema.get("group").getAsString());
		JsonObject item = schema.getAsJsonArray("items").get(0).getAsJsonObject();
		assertEquals("enabled", item.get("key").getAsString());
		assertEquals("Enabled", item.get("name").getAsString());
		assertEquals("boolean", item.get("type").getAsString());
		assertTrue(values.getAsJsonObject("values").get("enabled").getAsBoolean());
		assertTrue(write.get("success").getAsBoolean());
		verify(configManager).setConfiguration(eq("bridgetest"), eq("enabled"), eq("false"));
	}

	@Test
	public void eventsAndRuntimeHealthAreReadable() throws Exception
	{
		requestJson("POST", "/bridge/v1/plugin-artifacts/test-artifact/install", "{}", 202);

		JsonObject events = requestJson("GET", "/bridge/v1/events", null);
		JsonArray eventList = events.getAsJsonArray("events");
		assertEquals(1, events.get("count").getAsInt());
		assertEquals("plugin.install", eventList.get(0).getAsJsonObject().get("type").getAsString());

		JsonObject health = requestJson("GET", "/bridge/v1/runtime-health", null);
		assertTrue(health.get("pluginManagerAvailable").getAsBoolean());
		assertTrue(health.get("configManagerAvailable").getAsBoolean());
		assertTrue(health.get("artifactStatusAvailable").getAsBoolean());
		assertEquals(1, health.get("artifactCount").getAsInt());
	}

	private JsonObject requestJson(String method, String path, String body) throws Exception
	{
		return requestJson(method, path, body, 200);
	}

	private JsonObject requestJson(String method, String path, String body, int expectedCode) throws Exception
	{
		Response response = request(method, path, body, true);
		assertEquals(response.body, expectedCode, response.code);
		return new JsonParser().parse(response.body).getAsJsonObject();
	}

	private Response request(String method, String path, String body, boolean token) throws Exception
	{
		HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
		conn.setRequestMethod(method);
		if (token)
		{
			conn.setRequestProperty("X-Agent-Token", TOKEN);
		}
		if (body != null)
		{
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/json");
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			conn.setFixedLengthStreamingMode(bytes.length);
			try (OutputStream os = conn.getOutputStream())
			{
				os.write(bytes);
			}
		}

		try
		{
			int code = conn.getResponseCode();
			byte[] bytes = code >= 400 ? conn.getErrorStream().readAllBytes() : conn.getInputStream().readAllBytes();
			return new Response(code, new String(bytes, StandardCharsets.UTF_8));
		}
		finally
		{
			conn.disconnect();
		}
	}

	private static ConfigDescriptor testConfigDescriptor() throws Exception
	{
		Method method = TestConfig.class.getMethod("enabled");
		ConfigItem item = method.getAnnotation(ConfigItem.class);
		ConfigItemDescriptor itemDescriptor = new ConfigItemDescriptor(
			item,
			method.getGenericReturnType(),
			null,
			null,
			null
		);
		return new ConfigDescriptor(
			TestConfig.class.getAnnotation(ConfigGroup.class),
			Collections.emptyList(),
			Collections.singletonList(itemDescriptor),
			null
		);
	}

	private static final class Response
	{
		private final int code;
		private final String body;

		private Response(int code, String body)
		{
			this.code = code;
			this.body = body;
		}
	}

	private static final class FakeBridgeServices implements BridgeV1Handler.BridgeServices
	{
		private final PluginManager pluginManager;
		private final ConfigManager configManager;
		private final List<String> commands = new java.util.ArrayList<>();

		private FakeBridgeServices(PluginManager pluginManager, ConfigManager configManager)
		{
			this.pluginManager = pluginManager;
			this.configManager = configManager;
		}

		@Override
		public PluginManager getPluginManager()
		{
			return pluginManager;
		}

		@Override
		public ConfigManager getConfigManager()
		{
			return configManager;
		}

		@Override
		public PluginRuntimeDiscoveryResult discoverPluginArtifactStatus() throws IOException
		{
			PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "test-artifact")
				.displayName("Test Artifact")
				.version("1.0.0")
				.entryClasses("example.TestPlugin")
				.minClientVersion("1.0.0")
				.checksumSha256("abc123")
				.build();
			PluginRepository repository = new PluginRepository()
			{
				@Override
				public PluginArtifactSource getSource()
				{
					return PluginArtifactSource.MICROBOT_HUB;
				}

				@Override
				public List<PluginArtifact> discover()
				{
					return Collections.singletonList(artifact);
				}
			};
			return new PluginRuntime(Collections.singletonList(repository)).discoverStatus();
		}

		@Override
		public boolean installPluginArtifact(String id, String version)
		{
			commands.add("install:" + id + ":" + version);
			return "test-artifact".equals(id);
		}

		@Override
		public boolean updatePluginArtifact(String id, String version)
		{
			commands.add("update:" + id + ":" + version);
			return "test-artifact".equals(id);
		}

		@Override
		public boolean removePluginArtifact(String id)
		{
			commands.add("remove:" + id);
			return "test-artifact".equals(id);
		}

		@Override
		public Instant now()
		{
			return Instant.parse("2026-05-21T00:00:00Z");
		}

		@Override
		public String getRuneLiteVersion()
		{
			return "test-runelite";
		}

		@Override
		public String getMicrobotVersion()
		{
			return "test-microbot";
		}
	}
}
