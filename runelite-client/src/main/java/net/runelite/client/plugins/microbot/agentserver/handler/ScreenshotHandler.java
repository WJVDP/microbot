package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.api.Client;
import net.runelite.client.plugins.microbot.agentserver.controlcenter.FrameCaptureService;
import net.runelite.client.ui.DrawManager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class ScreenshotHandler extends AgentHandler {

	private final FrameCaptureService frameCaptureService;

	public ScreenshotHandler(Gson gson, Client client, DrawManager drawManager) {
		this(gson, new FrameCaptureService(drawManager));
	}

	public ScreenshotHandler(Gson gson, FrameCaptureService frameCaptureService) {
		super(gson);
		this.frameCaptureService = frameCaptureService;
	}

	@Override
	public String getPath() {
		return "/screenshot";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		try {
			requireGet(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, String> params = parseQuery(exchange.getRequestURI());
		boolean save = "true".equalsIgnoreCase(params.getOrDefault("save", "false"));

		FrameCaptureService.Capture screenshot;
		try {
			screenshot = frameCaptureService.capture();
		} catch (FrameCaptureService.CaptureBusyException e) {
			sendJson(exchange, 409, errorResponse(e.getMessage()));
			return;
		} catch (TimeoutException e) {
			sendJson(exchange, 500, errorResponse("Failed to capture screenshot - no frame available"));
			return;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			sendJson(exchange, 500, errorResponse("Frame capture interrupted"));
			return;
		}

		if (save) {
			String dir = params.getOrDefault("dir", System.getProperty("user.home") + "/.runelite/test-results/screenshots");
			String label = params.getOrDefault("label", "screenshot");
			File outDir = new File(dir);
			outDir.mkdirs();
			String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
			File outFile = new File(outDir, label + "-" + timestamp + ".png");
			Files.write(outFile.toPath(), screenshot.getPng());

			Map<String, Object> result = new LinkedHashMap<>();
			result.put("success", true);
			result.put("path", outFile.getAbsolutePath());
			result.put("width", screenshot.getWidth());
			result.put("height", screenshot.getHeight());
			sendJson(exchange, 200, result);
			return;
		}

		byte[] bytes = screenshot.getPng();
		exchange.getResponseHeaders().set("Content-Type", "image/png");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

}
