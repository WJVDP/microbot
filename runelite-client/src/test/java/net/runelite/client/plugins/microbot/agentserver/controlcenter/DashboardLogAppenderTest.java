package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DashboardLogAppenderTest
{
	@Test
	public void capturesOnlyTheEligibleLoggerNamespace()
	{
		DashboardLogBuffer buffer = new DashboardLogBuffer(10);
		DashboardLogAppender appender = new DashboardLogAppender(buffer);
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		appender.setContext(context);
		appender.updateEligible(Collections.singletonMap(
			"eligible", Collections.singletonList("example.eligible")));
		appender.start();
		try
		{
			appender.doAppend(new LoggingEvent(getClass().getName(),
				context.getLogger("example.eligible.Script"), Level.INFO, "included", null, null));
			appender.doAppend(new LoggingEvent(getClass().getName(),
				context.getLogger("example.other.Script"), Level.ERROR, "excluded", null, null));
			assertEquals(1, buffer.get("eligible", 0).size());
			assertEquals("included", buffer.get("eligible", 0).get(0).getMessage());
		}
		finally
		{
			appender.stop();
		}
	}
}
