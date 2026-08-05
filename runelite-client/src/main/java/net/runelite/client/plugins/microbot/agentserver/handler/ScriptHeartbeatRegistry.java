package net.runelite.client.plugins.microbot.agentserver.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ScriptHeartbeatRegistry {

	private ScriptHeartbeatRegistry() {}

	private static final ConcurrentHashMap<String, HeartbeatRecord> registry = new ConcurrentHashMap<>();

	public static void recordHeartbeat(String scriptClassName) {
		registry.computeIfAbsent(scriptClassName, HeartbeatRecord::new).beat();
	}

	public static void remove(String scriptClassName) {
		registry.remove(scriptClassName);
	}

	public static Map<String, Object> getHealth(String scriptClassName) {
		HeartbeatRecord record = registry.get(scriptClassName);
		if (record == null) {
			return null;
		}
		return record.toMap();
	}

	public static Map<String, Map<String, Object>> getAllHealth() {
		Map<String, Map<String, Object>> all = new LinkedHashMap<>();
		for (Map.Entry<String, HeartbeatRecord> entry : registry.entrySet()) {
			all.put(entry.getKey(), entry.getValue().toMap());
		}
		return all;
	}

	/** Returns immutable typed snapshots for in-process status consumers. */
	public static List<HeartbeatSnapshot> getSnapshots() {
		List<HeartbeatSnapshot> snapshots = new ArrayList<>();
		for (HeartbeatRecord record : registry.values()) {
			snapshots.add(record.snapshot());
		}
		return Collections.unmodifiableList(snapshots);
	}

	public static final class HeartbeatSnapshot {
		private final String scriptClassName;
		private final long firstHeartbeatMs;
		private final long lastHeartbeatMs;
		private final long loopCount;

		private HeartbeatSnapshot(String scriptClassName, long firstHeartbeatMs, long lastHeartbeatMs, long loopCount) {
			this.scriptClassName = scriptClassName;
			this.firstHeartbeatMs = firstHeartbeatMs;
			this.lastHeartbeatMs = lastHeartbeatMs;
			this.loopCount = loopCount;
		}

		public String getScriptClassName() {
			return scriptClassName;
		}

		public long getFirstHeartbeatMs() {
			return firstHeartbeatMs;
		}

		public long getLastHeartbeatMs() {
			return lastHeartbeatMs;
		}

		public long getLoopCount() {
			return loopCount;
		}
	}

	static final class HeartbeatRecord {
		private final String scriptClassName;
		private final AtomicLong loopCount = new AtomicLong(0);
		private final long firstHeartbeatMs = System.currentTimeMillis();
		private final AtomicLong lastHeartbeatMs = new AtomicLong(firstHeartbeatMs);

		HeartbeatRecord(String scriptClassName) {
			this.scriptClassName = scriptClassName;
		}

		void beat() {
			loopCount.incrementAndGet();
			lastHeartbeatMs.set(System.currentTimeMillis());
		}

		Map<String, Object> toMap() {
			HeartbeatSnapshot snapshot = snapshot();
			long lastMs = snapshot.getLastHeartbeatMs();
			long stalledMs = System.currentTimeMillis() - lastMs;
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("scriptClassName", snapshot.getScriptClassName());
			map.put("loopCount", snapshot.getLoopCount());
			map.put("firstHeartbeatAt", Instant.ofEpochMilli(snapshot.getFirstHeartbeatMs()).toString());
			map.put("lastHeartbeatAt", Instant.ofEpochMilli(lastMs).toString());
			map.put("stalledMs", stalledMs);
			return map;
		}

		HeartbeatSnapshot snapshot() {
			return new HeartbeatSnapshot(scriptClassName, firstHeartbeatMs, lastHeartbeatMs.get(), loopCount.get());
		}
	}
}
