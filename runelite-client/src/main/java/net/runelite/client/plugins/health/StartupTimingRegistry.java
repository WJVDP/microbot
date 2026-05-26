/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.health;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
public class StartupTimingRegistry
{
	private static final int MAX_TIMINGS = 1000;
	private static final StartupTimingRegistry DEFAULT = new StartupTimingRegistry();

	private final Deque<TimingSnapshot> timings = new LinkedList<>();
	private long lastSplashStageNanos = System.nanoTime();

	public static StartupTimingRegistry getDefault()
	{
		return DEFAULT;
	}

	public <T> T time(String stage, @Nullable String detail, Callable<T> callable) throws Exception
	{
		long start = System.nanoTime();
		try
		{
			return callable.call();
		}
		finally
		{
			record(stage, detail, System.nanoTime() - start);
		}
	}

	public void timeUnchecked(String stage, @Nullable String detail, Runnable runnable)
	{
		long start = System.nanoTime();
		try
		{
			runnable.run();
		}
		finally
		{
			record(stage, detail, System.nanoTime() - start);
		}
	}

	public void record(String stage, @Nullable String detail, long durationNanos)
	{
		synchronized (timings)
		{
			timings.addLast(new TimingSnapshot(Instant.now(), stage, detail, Math.max(0, durationNanos)));
			while (timings.size() > MAX_TIMINGS)
			{
				timings.removeFirst();
			}
		}
	}

	public void recordSplashStage(@Nullable String actionText, String subActionText)
	{
		long now = System.nanoTime();
		long elapsed = now - lastSplashStageNanos;
		lastSplashStageNanos = now;
		String detail = (actionText == null || actionText.isEmpty() ? "" : actionText + ": ") + subActionText;
		record("splash.stage", detail, elapsed);
	}

	public List<TimingSnapshot> snapshot()
	{
		synchronized (timings)
		{
			return Collections.unmodifiableList(new ArrayList<>(timings));
		}
	}

	public Map<String, Object> status()
	{
		List<Map<String, Object>> entries = new ArrayList<>();
		for (TimingSnapshot snapshot : snapshot())
		{
			entries.add(snapshot.toMap());
		}

		Map<String, Object> status = new LinkedHashMap<>();
		status.put("count", entries.size());
		status.put("timings", entries);
		return status;
	}

	public void clear()
	{
		synchronized (timings)
		{
			timings.clear();
		}
		lastSplashStageNanos = System.nanoTime();
	}

	public static final class TimingSnapshot
	{
		private final Instant time;
		private final String stage;
		private final String detail;
		private final long durationNanos;

		private TimingSnapshot(Instant time, String stage, String detail, long durationNanos)
		{
			this.time = time;
			this.stage = stage;
			this.detail = detail;
			this.durationNanos = durationNanos;
		}

		public String getStage()
		{
			return stage;
		}

		public long getDurationNanos()
		{
			return durationNanos;
		}

		public Map<String, Object> toMap()
		{
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("time", time.toString());
			map.put("stage", stage);
			map.put("detail", detail);
			map.put("durationMs", TimeUnit.NANOSECONDS.toMillis(durationNanos));
			map.put("durationNanos", durationNanos);
			return map;
		}
	}
}
