package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/** Browser-only dashboard surface with session and CSRF authentication. */
@Slf4j
public final class DashboardHandler implements HttpHandler
{
	public static final String PATH = "/dashboard";
	private static final String RESOURCE_ROOT =
		"/net/runelite/client/plugins/microbot/agentserver/dashboard/";
	private static final String SESSION_COOKIE = "microbot_dashboard";
	private static final String BOOTSTRAP_COOKIE = "microbot_dashboard_bootstrap";
	private static final String CSRF_HEADER = "X-CSRF-Token";

	private final Gson gson;
	private final String expectedHost;
	private final String expectedOrigin;
	private final DashboardSessionManager sessions;
	private final ControlCenterService service;
	private final FrameCaptureService frameCaptureService;

	public DashboardHandler(Gson gson, int port, DashboardSessionManager sessions,
		ControlCenterService service, FrameCaptureService frameCaptureService)
	{
		this.gson = gson;
		this.expectedHost = "127.0.0.1:" + port;
		this.expectedOrigin = "http://" + expectedHost;
		this.sessions = sessions;
		this.service = service;
		this.frameCaptureService = frameCaptureService;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException
	{
		applySecurityHeaders(exchange);
		if (!isAllowedRequest(exchange))
		{
			sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
			return;
		}

		try
		{
			route(exchange);
		}
		catch (ControlCenterService.UnknownPluginException e)
		{
			sendJson(exchange, 404, error("Eligible plugin not found"));
		}
		catch (ControlCenterService.ControlCenterException e)
		{
			sendJson(exchange, e.getStatusCode(), error(e.getMessage()));
		}
		catch (Exception e)
		{
			if (isClientDisconnect(e))
			{
				return;
			}
			log.warn("Dashboard request failed for {}", exchange.getRequestURI().getPath());
			sendJson(exchange, 500, error("Internal server error"));
		}
	}

	private void route(HttpExchange exchange) throws Exception
	{
		String path = exchange.getRequestURI().getPath();
		if (PATH.equals(path))
		{
			if (!requireMethod(exchange, "GET"))
			{
				return;
			}
			exchange.getResponseHeaders().set("Location", PATH + "/");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
			return;
		}
		if ((PATH + "/").equals(path))
		{
			serveIndex(exchange);
			return;
		}
		if ((PATH + "/styles.css").equals(path))
		{
			serveResource(exchange, "styles.css", "text/css; charset=utf-8");
			return;
		}
		if ((PATH + "/app.js").equals(path))
		{
			serveResource(exchange, "app.js", "text/javascript; charset=utf-8");
			return;
		}
		if ((PATH + "/api/session/bootstrap").equals(path))
		{
			handleBootstrap(exchange);
			return;
		}
		if ((PATH + "/api/plugins").equals(path))
		{
			if (!requireAuthenticatedGet(exchange))
			{
				return;
			}
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("plugins", service.listPlugins());
			sendJson(exchange, 200, response);
			return;
		}
		if ((PATH + "/api/screenshot").equals(path))
		{
			handleScreenshot(exchange);
			return;
		}
		if (path.startsWith(PATH + "/api/plugins/"))
		{
			handlePluginRoute(exchange, path.substring((PATH + "/api/plugins/").length()));
			return;
		}
		sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
	}

	private void serveIndex(HttpExchange exchange) throws IOException
	{
		if (!requireMethod(exchange, "GET"))
		{
			return;
		}
		if (!sessions.authenticate(cookie(exchange, SESSION_COOKIE)))
		{
			String bootstrap = sessions.claimBootstrapForBrowser();
			if (bootstrap != null)
			{
				addCookie(exchange, BOOTSTRAP_COOKIE, bootstrap,
					"/dashboard/api/session/bootstrap", DashboardSessionManager.BOOTSTRAP_TTL_MS / 1000);
			}
		}
		serveResource(exchange, "index.html", "text/html; charset=utf-8");
	}

	private void handleBootstrap(HttpExchange exchange) throws IOException
	{
		if (!requireMethod(exchange, "POST") || !requireExactOrigin(exchange))
		{
			return;
		}
		DashboardSessionManager.SessionGrant grant = sessions.resume(cookie(exchange, SESSION_COOKIE));
		if (grant == null)
		{
			grant = sessions.exchangeBootstrap(cookie(exchange, BOOTSTRAP_COOKIE));
		}
		if (grant == null)
		{
			sendJson(exchange, 401, error("Open the dashboard from Agent Server settings"));
			return;
		}

		addCookie(exchange, SESSION_COOKIE, grant.getSessionId(), "/dashboard",
			Math.max(0, (grant.getExpiresAt() - System.currentTimeMillis()) / 1000));
		clearCookie(exchange, BOOTSTRAP_COOKIE, "/dashboard/api/session/bootstrap");
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("csrfToken", grant.getCsrfToken());
		response.put("expiresAt", grant.getExpiresAt());
		sendJson(exchange, 200, response);
	}

	private void handlePluginRoute(HttpExchange exchange, String subPath) throws Exception
	{
		String[] parts = subPath.split("/", -1);
		if (parts.length == 1 && !parts[0].isEmpty())
		{
			if (!requireAuthenticatedGet(exchange))
			{
				return;
			}
			sendJson(exchange, 200, service.getPlugin(parts[0]));
			return;
		}
		if (parts.length != 2 || parts[0].isEmpty())
		{
			sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
			return;
		}

		String pluginId = parts[0];
		String action = parts[1];
		if ("logs".equals(action))
		{
			if (!requireAuthenticatedGet(exchange))
			{
				return;
			}
			long after = parseAfter(exchange.getRequestURI().getRawQuery());
			sendJson(exchange, 200, service.logs(pluginId, after));
			return;
		}
		if (!"start".equals(action) && !"stop".equals(action))
		{
			sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
			return;
		}
		if (!requireAuthenticatedMutation(exchange))
		{
			return;
		}
		sendJson(exchange, 200, "start".equals(action) ? service.start(pluginId) : service.stop(pluginId));
	}

	private void handleScreenshot(HttpExchange exchange) throws IOException
	{
		if (!requireAuthenticatedGet(exchange))
		{
			return;
		}
		try
		{
			FrameCaptureService.Capture capture = frameCaptureService.capture();
			exchange.getResponseHeaders().set("Content-Type", "image/png");
			exchange.getResponseHeaders().set("X-Image-Width", Integer.toString(capture.getWidth()));
			exchange.getResponseHeaders().set("X-Image-Height", Integer.toString(capture.getHeight()));
			exchange.sendResponseHeaders(200, capture.getPng().length);
			try (OutputStream output = exchange.getResponseBody())
			{
				output.write(capture.getPng());
			}
		}
		catch (FrameCaptureService.CaptureBusyException e)
		{
			sendJson(exchange, 409, error(e.getMessage()));
		}
		catch (TimeoutException e)
		{
			sendJson(exchange, 504, error("Timed out waiting for the next client frame"));
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			sendJson(exchange, 503, error("Frame capture interrupted"));
		}
	}

	private boolean requireAuthenticatedGet(HttpExchange exchange) throws IOException
	{
		if (!requireMethod(exchange, "GET"))
		{
			return false;
		}
		if (!sessions.authenticate(cookie(exchange, SESSION_COOKIE)))
		{
			sendJson(exchange, 401, error("Dashboard session required"));
			return false;
		}
		return true;
	}

	private boolean requireAuthenticatedMutation(HttpExchange exchange) throws IOException
	{
		if (!requireMethod(exchange, "POST") || !requireExactOrigin(exchange))
		{
			return false;
		}
		String sessionId = cookie(exchange, SESSION_COOKIE);
		if (!sessions.authenticateMutation(sessionId, exchange.getRequestHeaders().getFirst(CSRF_HEADER)))
		{
			sendJson(exchange, 403, error("Dashboard session and CSRF token required"));
			return false;
		}
		return true;
	}

	private boolean requireExactOrigin(HttpExchange exchange) throws IOException
	{
		if (!expectedOrigin.equals(exchange.getRequestHeaders().getFirst("Origin")))
		{
			sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
			return false;
		}
		return true;
	}

	private boolean requireMethod(HttpExchange exchange, String expected) throws IOException
	{
		if (!expected.equalsIgnoreCase(exchange.getRequestMethod()))
		{
			exchange.getResponseHeaders().set("Allow", expected);
			sendJson(exchange, 405, error("Method not allowed"));
			return false;
		}
		return true;
	}

	private boolean isAllowedRequest(HttpExchange exchange)
	{
		InetAddress remote = exchange.getRemoteAddress().getAddress();
		if (remote == null || !remote.isLoopbackAddress())
		{
			return false;
		}
		if (!expectedHost.equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Host")))
		{
			return false;
		}
		String origin = exchange.getRequestHeaders().getFirst("Origin");
		return origin == null || expectedOrigin.equals(origin);
	}

	private void serveResource(HttpExchange exchange, String name, String contentType) throws IOException
	{
		if (!requireMethod(exchange, "GET"))
		{
			return;
		}
		try (InputStream input = DashboardHandler.class.getResourceAsStream(RESOURCE_ROOT + name))
		{
			if (input == null)
			{
				sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
				return;
			}
			byte[] bytes = input.readAllBytes();
			exchange.getResponseHeaders().set("Content-Type", contentType);
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream output = exchange.getResponseBody())
			{
				output.write(bytes);
			}
		}
	}

	private void applySecurityHeaders(HttpExchange exchange)
	{
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		exchange.getResponseHeaders().set("Content-Security-Policy",
			"default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' blob:; "
				+ "connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'");
		exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
		exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
		exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
	}

	private void sendJson(HttpExchange exchange, int status, Object body) throws IOException
	{
		sendBytes(exchange, status, gson.toJson(body).getBytes(StandardCharsets.UTF_8),
			"application/json; charset=utf-8");
	}

	private void sendText(HttpExchange exchange, int status, String body, String contentType) throws IOException
	{
		sendBytes(exchange, status, body.getBytes(StandardCharsets.UTF_8), contentType);
	}

	private void sendBytes(HttpExchange exchange, int status, byte[] body, String contentType) throws IOException
	{
		exchange.getResponseHeaders().set("Content-Type", contentType);
		exchange.sendResponseHeaders(status, body.length);
		try (OutputStream output = exchange.getResponseBody())
		{
			output.write(body);
		}
	}

	private static Map<String, Object> error(String message)
	{
		return Collections.singletonMap("error", message);
	}

	private static long parseAfter(String query)
	{
		if (query == null)
		{
			return 0;
		}
		for (String part : query.split("&"))
		{
			if (part.startsWith("after="))
			{
				try
				{
					return Math.max(0, Long.parseLong(part.substring("after=".length())));
				}
				catch (NumberFormatException ignored)
				{
					return 0;
				}
			}
		}
		return 0;
	}

	private static String cookie(HttpExchange exchange, String name)
	{
		String header = exchange.getRequestHeaders().getFirst("Cookie");
		if (header == null)
		{
			return null;
		}
		for (String part : header.split(";"))
		{
			String[] pair = part.trim().split("=", 2);
			if (pair.length == 2 && name.equals(pair[0]))
			{
				return pair[1];
			}
		}
		return null;
	}

	private static void addCookie(HttpExchange exchange, String name, String value, String path, long maxAgeSeconds)
	{
		exchange.getResponseHeaders().add("Set-Cookie", name + "=" + value + "; Path=" + path
			+ "; Max-Age=" + maxAgeSeconds + "; HttpOnly; SameSite=Strict");
	}

	private static void clearCookie(HttpExchange exchange, String name, String path)
	{
		exchange.getResponseHeaders().add("Set-Cookie", name + "=; Path=" + path
			+ "; Max-Age=0; HttpOnly; SameSite=Strict");
	}

	private static boolean isClientDisconnect(Throwable error)
	{
		Throwable current = error;
		while (current != null)
		{
			if (current instanceof IOException && current.getMessage() != null)
			{
				String message = current.getMessage().toLowerCase();
				if (message.contains("broken pipe") || message.contains("connection reset")
					|| message.contains("connection closed"))
				{
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}
}
