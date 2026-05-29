/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.health;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PluginHealthRegistryTest
{
	@Test
	public void recordsExceptionsSlowCallsAndBlockedReason()
	{
		PluginHealthRegistry registry = new PluginHealthRegistry(TimeUnit.MILLISECONDS.toNanos(10));
		RuntimeException failure = new RuntimeException("boom");

		registry.recordCall("plugin.one", "event-handler", TimeUnit.MILLISECONDS.toNanos(5), null);
		registry.recordCall("plugin.one", "overlay-render", TimeUnit.MILLISECONDS.toNanos(20), failure);
		registry.setDisabledOrBlockedReason("plugin.one", "disabled");

		List<PluginHealthRegistry.PluginHealthSnapshot> snapshots = registry.snapshot();
		assertEquals(1, snapshots.size());
		PluginHealthRegistry.PluginHealthSnapshot snapshot = snapshots.get(0);
		assertEquals("plugin.one", snapshot.getPluginId());
		assertEquals(1, snapshot.getExceptionCount());
		assertEquals(1, snapshot.getSlowCallCount());
		assertNotNull(snapshot.getLastFailure());
		assertEquals("disabled", snapshot.getDisabledOrBlockedReason());

		Map<String, Object> status = registry.status();
		assertEquals(1, status.get("count"));
		assertEquals(10L, status.get("slowCallThresholdMs"));
	}
}
