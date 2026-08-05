package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A bounded immutable plugin-specific status extension. */
public final class ControlCenterStatusSnapshot
{
	private static final int MAX_DETAILS = 20;
	private static final int MAX_KEY_LENGTH = 64;
	private static final int MAX_VALUE_LENGTH = 256;

	private final String currentAction;
	private final Map<String, String> details;

	public ControlCenterStatusSnapshot(String currentAction, Map<String, String> details)
	{
		this.currentAction = bounded(DashboardLogBuffer.redact(currentAction), MAX_VALUE_LENGTH);
		Map<String, String> copy = new LinkedHashMap<>();
		if (details != null)
		{
			for (Map.Entry<String, String> entry : details.entrySet())
			{
				if (copy.size() >= MAX_DETAILS)
				{
					break;
				}
				String key = bounded(entry.getKey(), MAX_KEY_LENGTH);
				if (key != null && !key.isEmpty())
				{
					copy.put(key, bounded(DashboardLogBuffer.redact(entry.getValue()), MAX_VALUE_LENGTH));
				}
			}
		}
		this.details = Collections.unmodifiableMap(copy);
	}

	public String getCurrentAction()
	{
		return currentAction;
	}

	public Map<String, String> getDetails()
	{
		return details;
	}

	private static String bounded(String value, int maximum)
	{
		if (value == null || value.length() <= maximum)
		{
			return value;
		}
		return value.substring(0, maximum);
	}
}
