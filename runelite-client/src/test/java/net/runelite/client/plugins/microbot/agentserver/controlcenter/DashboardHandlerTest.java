package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpServer;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.DrawManager;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardHandlerTest
{
	private static final Gson GSON = new GsonBuilder().create();

	@Test
	public void bootstrapSessionProtectsApiAndRejectsCrossOriginRequests() throws Exception
	{
		PluginManager pluginManager = mock(PluginManager.class);
		when(pluginManager.getPlugins()).thenReturn(Collections.emptyList());
		DashboardSessionManager sessions = new DashboardSessionManager();
		sessions.issueBootstrap();
		ControlCenterService service = new ControlCenterService(pluginManager, 10_000L, new DashboardLogBuffer());
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		server.setExecutor(executor);
		int port = server.getAddress().getPort();
		server.createContext(DashboardHandler.PATH, new DashboardHandler(GSON, port, sessions,
			service, new FrameCaptureService(new DrawManager())));
		server.start();

		try
		{
			HttpURLConnection index = open(port, "/dashboard/");
			assertEquals(200, index.getResponseCode());
			assertEquals("no-store", index.getHeaderField("Cache-Control"));
			assertTrue(index.getHeaderField("Content-Security-Policy").contains("frame-ancestors 'none'"));
			String bootstrapCookie = findCookie(index, "microbot_dashboard_bootstrap");
			assertNotNull(bootstrapCookie);

			HttpClient client = HttpClient.newHttpClient();
			HttpResponse<String> bootstrap = client.send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/dashboard/api/session/bootstrap"))
				.header("Origin", "http://127.0.0.1:" + port)
				.header("Cookie", bootstrapCookie)
				.POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
			assertEquals(200, bootstrap.statusCode());
			String sessionCookie = findCookie(bootstrap.headers().allValues("Set-Cookie"), "microbot_dashboard");
			assertNotNull(sessionCookie);
			Map<String, Object> bootstrapBody = GSON.fromJson(bootstrap.body(),
				new TypeToken<Map<String, Object>>() { }.getType());
			assertNotNull(bootstrapBody.get("csrfToken"));
			assertFalse(bootstrapBody.containsKey("sessionId"));

			HttpURLConnection plugins = open(port, "/dashboard/api/plugins");
			plugins.setRequestProperty("Cookie", sessionCookie);
			assertEquals(200, plugins.getResponseCode());

			HttpResponse<String> unauthenticatedMutation = client.send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/dashboard/api/plugins/missing/start"))
				.header("Origin", "http://127.0.0.1:" + port)
				.POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
			assertEquals(403, unauthenticatedMutation.statusCode());

			HttpResponse<String> crossOrigin = client.send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/dashboard/api/plugins"))
				.header("Origin", "https://evil.example")
				.GET().build(), HttpResponse.BodyHandlers.ofString());
			assertEquals(404, crossOrigin.statusCode());
		}
		finally
		{
			server.stop(0);
			executor.shutdownNow();
			service.close();
			sessions.clear();
		}
	}

	private static HttpURLConnection open(int port, String path) throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
		connection.setConnectTimeout(5_000);
		connection.setReadTimeout(5_000);
		return connection;
	}

	private static String findCookie(HttpURLConnection connection, String name)
	{
		for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet())
		{
			if (header.getKey() == null || !"Set-Cookie".equalsIgnoreCase(header.getKey()))
			{
				continue;
			}
			for (String value : header.getValue())
			{
				if (value.startsWith(name + "="))
				{
					return value.substring(0, value.indexOf(';'));
				}
			}
		}
		return null;
	}

	private static String findCookie(List<String> values, String name)
	{
		for (String value : values)
		{
			if (value.startsWith(name + "="))
			{
				return value.substring(0, value.indexOf(';'));
			}
		}
		return null;
	}

}
