package net.runelite.client.plugins.microbot.agentserver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigButton;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(AgentServerConfig.GROUP)
public interface AgentServerConfig extends Config {

	String GROUP = "agentServer";
	String KEY_TOKEN = "authToken";
	String KEY_PORT = "port";
	String KEY_OPEN_DASHBOARD = "openDashboard";

	@ConfigItem(
			keyName = "port",
			name = "Port",
			description = "HTTP server port for agent communication (bound to 127.0.0.1 only)",
			position = 0
	)
	@Range(min = 1024, max = 65535)
	default int port() {
		return 8081;
	}

	@ConfigItem(
			keyName = "maxResults",
			name = "Max Results",
			description = "Default max results for list/query endpoints",
			position = 1
	)
	@Range(min = 10, max = 5000)
	default int maxResults() {
		return 200;
	}

	@ConfigItem(
			keyName = "heartbeatStallSeconds",
			name = "Heartbeat stall threshold",
			description = "Seconds without a script heartbeat before dashboard health becomes STALLED",
			position = 2
	)
	@Range(min = 2, max = 300)
	default int heartbeatStallSeconds() {
		return 10;
	}

	@ConfigItem(
			keyName = KEY_TOKEN,
			name = "Auth token",
			description = "Shared secret required in the X-Agent-Token header. Auto-generated on first start. Use 'Regenerate token' to rotate.",
			secret = true,
			position = 3
	)
	default String authToken() {
		return "";
	}

	@ConfigItem(
			keyName = KEY_TOKEN,
			name = "",
			description = ""
	)
	void authToken(String token);

	@ConfigItem(
			keyName = "bindOnlyWhileScriptsActive",
			name = "Stealth bind",
			description = "Only open the listening socket while a Microbot script is actively running. Eliminates the localhost port fingerprint during manual play.",
			position = 4
	)
	default boolean bindOnlyWhileScriptsActive() {
		return false;
	}

	enum BindMode { TCP, UDS }

	@ConfigItem(
			keyName = "bindMode",
			name = "Bind mode",
			description = "TCP: classic 127.0.0.1:<port>. UDS: Unix domain socket at ~/.runelite/.agent.sock — invisible to port scanners but clients must speak UDS.",
			position = 5
	)
	default BindMode bindMode() {
		return BindMode.TCP;
	}

	@ConfigItem(
			keyName = KEY_OPEN_DASHBOARD,
			name = "Open dashboard",
			description = "Open the local plugin control center in your browser (TCP mode only)",
			position = 6
	)
	default ConfigButton openDashboard() {
		return new ConfigButton();
	}
}
