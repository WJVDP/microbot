/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.health;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
public class PluginHealthRegistry
{
	public static final long DEFAULT_SLOW_CALL_THRESHOLD_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

	private static final PluginHealthRegistry DEFAULT = new PluginHealthRegistry(DEFAULT_SLOW_CALL_THRESHOLD_NANOS);

	private final ConcurrentHashMap<String, MutableHealth> healthByPlugin = new ConcurrentHashMap<>();
	private final AtomicLong slowCallThresholdNanos;

	public PluginHealthRegistry()
	{
		this(DEFAULT_SLOW_CALL_THRESHOLD_NANOS);
	}

	public PluginHealthRegistry(long slowCallThresholdNanos)
	{
		this.slowCallThresholdNanos = new AtomicLong(slowCallThresholdNanos);
	}

	public static PluginHealthRegistry getDefault()
	{
		return DEFAULT;
	}

	public long getSlowCallThresholdNanos()
	{
		return slowCallThresholdNanos.get();
	}

	public void setSlowCallThresholdNanos(long thresholdNanos)
	{
		slowCallThresholdNanos.set(Math.max(0, thresholdNanos));
	}

	public void recordCall(String pluginId, String operation, long durationNanos, @Nullable Throwable failure)
	{
		if (pluginId == null || pluginId.isEmpty())
		{
			return;
		}

		MutableHealth health = healthByPlugin.computeIfAbsent(pluginId, MutableHealth::new);
		health.recordCall(operation, durationNanos, slowCallThresholdNanos.get(), failure);
	}

	public void recordFailure(String pluginId, String operation, Throwable failure)
	{
		recordCall(pluginId, operation, 0, failure);
	}

	public void setDisabledOrBlockedReason(String pluginId, @Nullable String reason)
	{
		if (pluginId == null || pluginId.isEmpty())
		{
			return;
		}

		MutableHealth health = healthByPlugin.computeIfAbsent(pluginId, MutableHealth::new);
		health.setDisabledOrBlockedReason(reason);
	}

	public List<PluginHealthSnapshot> snapshot()
	{
		List<PluginHealthSnapshot> snapshots = new ArrayList<>();
		for (MutableHealth health : healthByPlugin.values())
		{
			snapshots.add(health.snapshot());
		}
		snapshots.sort(Comparator.comparing(PluginHealthSnapshot::getPluginId));
		return Collections.unmodifiableList(snapshots);
	}

	public Map<String, Object> status()
	{
		Map<String, Object> status = new LinkedHashMap<>();
		List<Map<String, Object>> plugins = new ArrayList<>();
		for (PluginHealthSnapshot snapshot : snapshot())
		{
			plugins.add(snapshot.toMap());
		}
		status.put("slowCallThresholdMs", TimeUnit.NANOSECONDS.toMillis(slowCallThresholdNanos.get()));
		status.put("count", plugins.size());
		status.put("plugins", plugins);
		return status;
	}

	public void clear()
	{
		healthByPlugin.clear();
	}

	private static final class MutableHealth
	{
		private final String pluginId;
		private final AtomicInteger exceptionCount = new AtomicInteger();
		private final AtomicInteger slowCallCount = new AtomicInteger();
		private final AtomicLong totalCallCount = new AtomicLong();
		private final AtomicLong totalDurationNanos = new AtomicLong();
		private final AtomicLong maxDurationNanos = new AtomicLong();
		private volatile String lastOperation;
		private volatile String lastFailure;
		private volatile String lastFailureStackTrace;
		private volatile Instant lastFailureTime;
		private volatile String disabledOrBlockedReason;

		private MutableHealth(String pluginId)
		{
			this.pluginId = pluginId;
		}

		private void recordCall(String operation, long durationNanos, long slowCallThresholdNanos, @Nullable Throwable failure)
		{
			lastOperation = operation;
			totalCallCount.incrementAndGet();
			totalDurationNanos.addAndGet(Math.max(0, durationNanos));
			maxDurationNanos.accumulateAndGet(Math.max(0, durationNanos), Math::max);
			if (durationNanos >= slowCallThresholdNanos && slowCallThresholdNanos > 0)
			{
				slowCallCount.incrementAndGet();
			}
			if (failure != null)
			{
				exceptionCount.incrementAndGet();
				lastFailure = failure.getClass().getName() + ": " + Objects.toString(failure.getMessage(), "");
				lastFailureStackTrace = stackTrace(failure);
				lastFailureTime = Instant.now();
			}
		}

		private void setDisabledOrBlockedReason(@Nullable String reason)
		{
			disabledOrBlockedReason = reason;
		}

		private PluginHealthSnapshot snapshot()
		{
			return new PluginHealthSnapshot(
				pluginId,
				exceptionCount.get(),
				slowCallCount.get(),
				totalCallCount.get(),
				totalDurationNanos.get(),
				maxDurationNanos.get(),
				lastOperation,
				lastFailure,
				lastFailureStackTrace,
				lastFailureTime,
				disabledOrBlockedReason);
		}
	}

	public static final class PluginHealthSnapshot
	{
		private final String pluginId;
		private final int exceptionCount;
		private final int slowCallCount;
		private final long totalCallCount;
		private final long totalDurationNanos;
		private final long maxDurationNanos;
		private final String lastOperation;
		private final String lastFailure;
		private final String lastFailureStackTrace;
		private final Instant lastFailureTime;
		private final String disabledOrBlockedReason;

		private PluginHealthSnapshot(
			String pluginId,
			int exceptionCount,
			int slowCallCount,
			long totalCallCount,
			long totalDurationNanos,
			long maxDurationNanos,
			String lastOperation,
			String lastFailure,
			String lastFailureStackTrace,
			Instant lastFailureTime,
			String disabledOrBlockedReason)
		{
			this.pluginId = pluginId;
			this.exceptionCount = exceptionCount;
			this.slowCallCount = slowCallCount;
			this.totalCallCount = totalCallCount;
			this.totalDurationNanos = totalDurationNanos;
			this.maxDurationNanos = maxDurationNanos;
			this.lastOperation = lastOperation;
			this.lastFailure = lastFailure;
			this.lastFailureStackTrace = lastFailureStackTrace;
			this.lastFailureTime = lastFailureTime;
			this.disabledOrBlockedReason = disabledOrBlockedReason;
		}

		public String getPluginId()
		{
			return pluginId;
		}

		public int getExceptionCount()
		{
			return exceptionCount;
		}

		public int getSlowCallCount()
		{
			return slowCallCount;
		}

		public String getLastFailure()
		{
			return lastFailure;
		}

		public String getDisabledOrBlockedReason()
		{
			return disabledOrBlockedReason;
		}

		public Map<String, Object> toMap()
		{
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("pluginId", pluginId);
			map.put("exceptionCount", exceptionCount);
			map.put("slowCallCount", slowCallCount);
			map.put("totalCallCount", totalCallCount);
			map.put("totalDurationMs", TimeUnit.NANOSECONDS.toMillis(totalDurationNanos));
			map.put("maxDurationMs", TimeUnit.NANOSECONDS.toMillis(maxDurationNanos));
			map.put("lastOperation", lastOperation);
			map.put("lastFailure", lastFailure);
			map.put("lastFailureStackTrace", lastFailureStackTrace);
			map.put("lastFailureTime", lastFailureTime == null ? null : lastFailureTime.toString());
			map.put("disabledOrBlockedReason", disabledOrBlockedReason);
			return map;
		}
	}

	private static String stackTrace(Throwable failure)
	{
		StringWriter writer = new StringWriter();
		failure.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
}
