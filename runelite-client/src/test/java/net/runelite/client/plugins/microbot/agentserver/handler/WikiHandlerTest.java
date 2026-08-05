package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WikiHandlerTest
{
	private static final Gson GSON = new Gson();

	@Rule
	public final MockWebServer wikiServer = new MockWebServer();

	private HttpServer agentServer;
	private ExecutorService executor;
	private int agentPort;

	@Before
	public void setUp() throws IOException
	{
		AgentHandler.setTokenSupplier(() -> null);
		executor = Executors.newSingleThreadExecutor();
		agentServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		agentServer.setExecutor(executor);
		WikiHandler handler = new WikiHandler(GSON, new OkHttpClient(), wikiServer.url("/api.php"));
		agentServer.createContext(handler.getPath(), handler);
		agentServer.start();
		agentPort = agentServer.getAddress().getPort();
	}

	@After
	public void tearDown()
	{
		agentServer.stop(0);
		executor.shutdownNow();
	}

	@Test
	public void searchesWikiAndReturnsRankedSummaries() throws Exception
	{
		wikiServer.enqueue(new MockResponse().setBody("{\"query\":{\"pages\":["
			+ "{\"pageid\":2,\"title\":\"Other\",\"index\":2,\"extract\":\"Second\",\"fullurl\":\"https://example/other\"},"
			+ "{\"pageid\":1,\"title\":\"Abyssal whip\",\"index\":1,\"extract\":\"A weapon.\",\"fullurl\":\"https://example/whip\"}"
			+ "]}}"));

		Response response = get("/wiki?q=Abyssal%20whip&limit=2");

		assertEquals(200, response.code);
		assertEquals("Abyssal whip", response.body.get("query"));
		assertEquals(2.0, response.body.get("count"));
		List<Map<String, Object>> results = results(response);
		assertEquals("Abyssal whip", results.get(0).get("title"));
		assertEquals("A weapon.", results.get(0).get("summary"));

		RecordedRequest request = wikiServer.takeRequest();
		assertEquals("Abyssal whip", request.getRequestUrl().queryParameter("gsrsearch"));
		assertEquals("2", request.getRequestUrl().queryParameter("gsrlimit"));
		assertTrue(request.getHeader("User-Agent").startsWith("Microbot wiki lookup"));
	}

	@Test
	public void missingQueryReturns400WithoutCallingWiki() throws IOException
	{
		Response response = get("/wiki");

		assertEquals(400, response.code);
		assertEquals(0, wikiServer.getRequestCount());
	}

	@Test
	public void upstreamFailureReturns502() throws IOException
	{
		wikiServer.enqueue(new MockResponse().setResponseCode(503));

		Response response = get("/wiki?q=Goblin");

		assertEquals(502, response.code);
		assertTrue(response.body.get("error").toString().contains("503"));
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> results(Response response)
	{
		return (List<Map<String, Object>>) response.body.get("results");
	}

	private Response get(String path) throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection) new URL(
			"http://127.0.0.1:" + agentPort + path).openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(5000);
		int code = connection.getResponseCode();
		InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
		String json;
		try (Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()))
		{
			json = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "{}";
		}
		Map<String, Object> body = GSON.fromJson(json,
			new TypeToken<Map<String, Object>>() { }.getType());
		return new Response(code, body);
	}

	private static class Response
	{
		private final int code;
		private final Map<String, Object> body;

		private Response(int code, Map<String, Object> body)
		{
			this.code = code;
			this.body = body;
		}
	}
}
