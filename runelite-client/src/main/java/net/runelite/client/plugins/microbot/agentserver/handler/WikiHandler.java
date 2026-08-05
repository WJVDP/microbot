package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only OSRS Wiki search for agents that need to identify game entities.
 */
public class WikiHandler extends AgentHandler
{
	static final String DEFAULT_API_URL = "https://oldschool.runescape.wiki/api.php";
	private static final String USER_AGENT = "Microbot wiki lookup (https://github.com/chsami/microbot)";
	private static final int DEFAULT_LIMIT = 5;
	private static final int MAX_LIMIT = 10;
	private static final int MAX_QUERY_LENGTH = 200;

	private final OkHttpClient httpClient;
	private final HttpUrl apiUrl;

	public WikiHandler(Gson gson, OkHttpClient httpClient)
	{
		this(gson, httpClient, HttpUrl.parse(DEFAULT_API_URL));
	}

	WikiHandler(Gson gson, OkHttpClient httpClient, HttpUrl apiUrl)
	{
		super(gson);
		this.httpClient = httpClient;
		this.apiUrl = apiUrl;
	}

	@Override
	public String getPath()
	{
		return "/wiki";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			requireGet(exchange);
		}
		catch (HttpMethodException e)
		{
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, String> params = parseQuery(exchange.getRequestURI());
		String query = params.get("q");
		if (query == null || query.trim().isEmpty())
		{
			sendJson(exchange, 400, errorResponse("Missing required parameter: q"));
			return;
		}
		query = query.trim();
		if (query.length() > MAX_QUERY_LENGTH)
		{
			sendJson(exchange, 400, errorResponse("Query must be 200 characters or fewer"));
			return;
		}

		int limit = Math.max(1, Math.min(getIntParam(params, "limit", DEFAULT_LIMIT), MAX_LIMIT));
		HttpUrl requestUrl = apiUrl.newBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("generator", "search")
			.addQueryParameter("gsrsearch", query)
			.addQueryParameter("gsrnamespace", "0")
			.addQueryParameter("gsrlimit", Integer.toString(limit))
			.addQueryParameter("prop", "extracts|info")
			.addQueryParameter("exintro", "1")
			.addQueryParameter("explaintext", "1")
			.addQueryParameter("exsentences", "3")
			.addQueryParameter("inprop", "url")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.build();

		Request request = new Request.Builder()
			.url(requestUrl)
			.header("User-Agent", USER_AGENT)
			.header("Accept", "application/json")
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			ResponseBody body = response.body();
			if (!response.isSuccessful() || body == null)
			{
				sendJson(exchange, 502, errorResponse("OSRS Wiki request failed with status " + response.code()));
				return;
			}

			JsonObject root;
			try
			{
				root = gson.fromJson(body.charStream(), JsonObject.class);
			}
			catch (RuntimeException e)
			{
				sendJson(exchange, 502, errorResponse("OSRS Wiki returned an invalid response"));
				return;
			}

			List<JsonObject> pages = getPages(root);
			pages.sort(Comparator.comparingInt(page -> getInt(page, "index", Integer.MAX_VALUE)));
			List<Map<String, Object>> results = new ArrayList<>();
			for (JsonObject page : pages)
			{
				Map<String, Object> result = new LinkedHashMap<>();
				result.put("title", getString(page, "title"));
				result.put("pageId", getInt(page, "pageid", -1));
				result.put("url", getString(page, "fullurl"));
				result.put("summary", getString(page, "extract"));
				results.add(result);
			}

			Map<String, Object> output = new LinkedHashMap<>();
			output.put("query", query);
			output.put("count", results.size());
			output.put("results", results);
			sendJson(exchange, 200, output);
		}
		catch (IOException e)
		{
			sendJson(exchange, 502, errorResponse("Could not reach the OSRS Wiki"));
		}
	}

	private static List<JsonObject> getPages(JsonObject root)
	{
		List<JsonObject> pages = new ArrayList<>();
		if (root == null || !root.has("query") || !root.get("query").isJsonObject())
		{
			return pages;
		}
		JsonElement pagesElement = root.getAsJsonObject("query").get("pages");
		if (pagesElement == null || !pagesElement.isJsonArray())
		{
			return pages;
		}
		JsonArray array = pagesElement.getAsJsonArray();
		for (JsonElement element : array)
		{
			if (element.isJsonObject())
			{
				pages.add(element.getAsJsonObject());
			}
		}
		return pages;
	}

	private static String getString(JsonObject object, String key)
	{
		JsonElement value = object.get(key);
		return value == null || value.isJsonNull() ? "" : value.getAsString();
	}

	private static int getInt(JsonObject object, String key, int defaultValue)
	{
		JsonElement value = object.get(key);
		return value == null || value.isJsonNull() ? defaultValue : value.getAsInt();
	}
}
