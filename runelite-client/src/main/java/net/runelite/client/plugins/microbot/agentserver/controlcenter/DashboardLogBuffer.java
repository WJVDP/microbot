package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/** Fixed-size, per-plugin dashboard log storage with mandatory redaction. */
public final class DashboardLogBuffer
{
	public static final int DEFAULT_CAPACITY = 500;
	private static final int MAX_MESSAGE_LENGTH = 1_000;
	private static final int MAX_EXCEPTION_LENGTH = 500;
	private static final Pattern BEARER = Pattern.compile("(?i)\\bbearer\\s+[A-Za-z0-9._~+/=-]+");
	private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
	private static final Pattern SENSITIVE_VALUE = Pattern.compile(
		"(?i)\\b(token|authorization|cookie|session(?:id)?|password|passwd|secret|api[-_]?key|account|profile|username|login)\\b(\\s*[:=]\\s*)([^\\s,;]+)");

	private final int capacity;
	private final AtomicLong sequence = new AtomicLong();
	private final Map<String, ArrayDeque<DashboardLogEvent>> events = new ConcurrentHashMap<>();

	public DashboardLogBuffer()
	{
		this(DEFAULT_CAPACITY);
	}

	DashboardLogBuffer(int capacity)
	{
		if (capacity < 1)
		{
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.capacity = capacity;
	}

	void append(String pluginId, long timestamp, String level, String logger, String message, String exception)
	{
		DashboardLogEvent event = new DashboardLogEvent(
			sequence.incrementAndGet(),
			Instant.ofEpochMilli(timestamp).toString(),
			level,
			bounded(redact(logger), 256),
			bounded(redact(message), MAX_MESSAGE_LENGTH),
			bounded(redact(exception), MAX_EXCEPTION_LENGTH));
		ArrayDeque<DashboardLogEvent> buffer = events.computeIfAbsent(pluginId, ignored -> new ArrayDeque<>(capacity));
		synchronized (buffer)
		{
			while (buffer.size() >= capacity)
			{
				buffer.removeFirst();
			}
			buffer.addLast(event);
		}
	}

	void appendLifecycleError(String pluginId, String message)
	{
		append(pluginId, System.currentTimeMillis(), "ERROR", "control-center.lifecycle", message, null);
	}

	public List<DashboardLogEvent> get(String pluginId, long afterSequence)
	{
		ArrayDeque<DashboardLogEvent> buffer = events.get(pluginId);
		if (buffer == null)
		{
			return Collections.emptyList();
		}
		List<DashboardLogEvent> result = new ArrayList<>();
		synchronized (buffer)
		{
			for (DashboardLogEvent event : buffer)
			{
				if (event.getSequence() > afterSequence)
				{
					result.add(event);
				}
			}
		}
		return Collections.unmodifiableList(result);
	}

	DashboardLogEvent lastErrorAfter(String pluginId, long timestamp)
	{
		ArrayDeque<DashboardLogEvent> buffer = events.get(pluginId);
		if (buffer == null)
		{
			return null;
		}
		synchronized (buffer)
		{
			DashboardLogEvent last = null;
			for (DashboardLogEvent event : buffer)
			{
				if ("ERROR".equals(event.getLevel()) && event.getTimestampMs() >= timestamp)
				{
					last = event;
				}
			}
			return last;
		}
	}

	void remove(String pluginId)
	{
		events.remove(pluginId);
	}

	static String redact(String value)
	{
		if (value == null)
		{
			return null;
		}
		String redacted = BEARER.matcher(value).replaceAll("Bearer [REDACTED]");
		redacted = SENSITIVE_VALUE.matcher(redacted).replaceAll("$1$2[REDACTED]");
		return EMAIL.matcher(redacted).replaceAll("[REDACTED_EMAIL]");
	}

	private static String bounded(String value, int maximum)
	{
		if (value == null || value.length() <= maximum)
		{
			return value;
		}
		return value.substring(0, maximum);
	}

	public static final class DashboardLogEvent
	{
		private final long sequence;
		private final String timestamp;
		private final long timestampMs;
		private final String level;
		private final String logger;
		private final String message;
		private final String exception;

		private DashboardLogEvent(long sequence, String timestamp, String level, String logger, String message, String exception)
		{
			this.sequence = sequence;
			this.timestamp = timestamp;
			this.timestampMs = Instant.parse(timestamp).toEpochMilli();
			this.level = level;
			this.logger = logger;
			this.message = message;
			this.exception = exception;
		}

		public long getSequence()
		{
			return sequence;
		}

		public String getTimestamp()
		{
			return timestamp;
		}

		long getTimestampMs()
		{
			return timestampMs;
		}

		public String getLevel()
		{
			return level;
		}

		public String getLogger()
		{
			return logger;
		}

		public String getMessage()
		{
			return message;
		}

		public String getException()
		{
			return exception;
		}
	}
}
