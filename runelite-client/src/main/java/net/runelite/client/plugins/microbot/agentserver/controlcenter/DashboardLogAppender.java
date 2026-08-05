package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Routes only explicitly eligible plugin loggers into dashboard ring buffers. */
public final class DashboardLogAppender extends AppenderBase<ILoggingEvent>
{
	private final DashboardLogBuffer buffer;
	private volatile Map<String, List<String>> loggerPrefixes = Collections.emptyMap();

	public DashboardLogAppender(DashboardLogBuffer buffer)
	{
		this.buffer = buffer;
	}

	void updateEligible(Map<String, List<String>> eligible)
	{
		Map<String, List<String>> copy = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : eligible.entrySet())
		{
			List<String> prefixes = new ArrayList<>(entry.getValue());
			prefixes.sort(Comparator.comparingInt(String::length).reversed());
			copy.put(entry.getKey(), Collections.unmodifiableList(prefixes));
		}
		loggerPrefixes = Collections.unmodifiableMap(copy);
	}

	@Override
	protected void append(ILoggingEvent event)
	{
		String pluginId = resolvePlugin(event.getLoggerName());
		if (pluginId == null)
		{
			return;
		}
		IThrowableProxy throwable = event.getThrowableProxy();
		String exception = throwable == null
			? null
			: throwable.getClassName() + (throwable.getMessage() == null ? "" : ": " + throwable.getMessage());
		buffer.append(pluginId, event.getTimeStamp(), event.getLevel().toString(),
			event.getLoggerName(), event.getFormattedMessage(), exception);
	}

	private String resolvePlugin(String logger)
	{
		String bestId = null;
		int bestLength = -1;
		for (Map.Entry<String, List<String>> entry : loggerPrefixes.entrySet())
		{
			for (String prefix : entry.getValue())
			{
				if (matches(logger, prefix) && prefix.length() > bestLength)
				{
					bestId = entry.getKey();
					bestLength = prefix.length();
				}
			}
		}
		return bestId;
	}

	private static boolean matches(String logger, String prefix)
	{
		return logger != null && prefix != null && (logger.equals(prefix)
			|| logger.startsWith(prefix + ".") || logger.startsWith(prefix + "$"));
	}
}
