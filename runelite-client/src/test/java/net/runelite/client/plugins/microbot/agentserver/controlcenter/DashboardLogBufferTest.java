package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DashboardLogBufferTest
{
	@Test
	public void evictsOldestEventAndRedactsSensitiveValues()
	{
		DashboardLogBuffer buffer = new DashboardLogBuffer(2);
		buffer.append("plugin", 1_000L, "INFO", "example.Logger", "first", null);
		buffer.append("plugin", 2_000L, "WARN", "example.Logger",
			"token=abc123 email person@example.com", null);
		buffer.append("plugin", 3_000L, "ERROR", "example.Logger",
			"Authorization: Bearer secret-value", "Failure: sessionId=private");

		List<DashboardLogBuffer.DashboardLogEvent> events = buffer.get("plugin", 0);
		assertEquals(2, events.size());
		assertEquals("WARN", events.get(0).getLevel());
		assertFalse(events.get(0).getMessage().contains("abc123"));
		assertFalse(events.get(0).getMessage().contains("person@example.com"));
		assertFalse(events.get(1).getMessage().contains("secret-value"));
		assertFalse(events.get(1).getException().contains("private"));
	}

	@Test
	public void afterSequenceReturnsOnlyNewerEvents()
	{
		DashboardLogBuffer buffer = new DashboardLogBuffer(5);
		buffer.append("plugin", 1_000L, "INFO", "logger", "one", null);
		long sequence = buffer.get("plugin", 0).get(0).getSequence();
		buffer.append("plugin", 2_000L, "INFO", "logger", "two", null);

		List<DashboardLogBuffer.DashboardLogEvent> events = buffer.get("plugin", sequence);
		assertEquals(1, events.size());
		assertEquals("two", events.get(0).getMessage());
	}
}
