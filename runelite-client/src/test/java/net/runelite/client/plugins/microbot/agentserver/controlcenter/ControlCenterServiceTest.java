package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.agentserver.handler.ScriptHeartbeatRegistry;
import net.runelite.client.plugins.microbot.statemachine.StateMachineScript;
import net.runelite.client.plugins.microbot.statemachine.Transition;
import org.junit.After;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ControlCenterServiceTest
{
	private TestStateMachine stateMachine;

	@After
	public void cleanUp()
	{
		if (stateMachine != null)
		{
			stateMachine.shutdown();
		}
		ControlCenterStatusRegistry.clear();
	}

	@Test
	public void listsOnlyExplicitlyEligiblePluginsAndAggregatesStateMachine()
		throws Exception
	{
		PluginManager pluginManager = mock(PluginManager.class);
		EligiblePlugin eligible = new EligiblePlugin();
		UnmarkedPlugin unmarked = new UnmarkedPlugin();
		when(pluginManager.getPlugins()).thenReturn(Arrays.asList(eligible, unmarked));
		when(pluginManager.isActive(eligible)).thenReturn(true);

		stateMachine = new TestStateMachine();
		Method initialize = StateMachineScript.class.getDeclaredMethod("initialize");
		initialize.setAccessible(true);
		initialize.invoke(stateMachine);
		stateMachine.forceState(TestState.RUNNING, "test");
		ScriptHeartbeatRegistry.recordHeartbeat(stateMachine.getClass().getName());

		try (ControlCenterService service = new ControlCenterService(
			pluginManager, 10_000L, new DashboardLogBuffer()))
		{
			List<Map<String, Object>> plugins = service.listPlugins();
			assertEquals(1, plugins.size());
			Map<String, Object> status = plugins.get(0);
			assertEquals("eligible-plugin", status.get("id"));
			assertEquals("RUNNING", status.get("lifecycle"));
			assertEquals("HEALTHY", status.get("health"));
			assertEquals("RUNNING", status.get("currentState"));
			assertEquals("RUNNING", status.get("currentAction"));
			assertEquals(1L, status.get("transitionCount"));
			assertEquals(Arrays.asList("IDLE", "RUNNING"), status.get("phases"));
		}
	}

	@Test
	public void startAndStopAreIdempotentAndRunOnEdt() throws Exception
	{
		PluginManager pluginManager = mock(PluginManager.class);
		EligiblePlugin eligible = new EligiblePlugin();
		AtomicBoolean active = new AtomicBoolean();
		AtomicBoolean lifecycleRanOnEdt = new AtomicBoolean(true);
		when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(eligible));
		when(pluginManager.isActive(eligible)).thenAnswer(ignored -> active.get());
		when(pluginManager.startPlugin(eligible)).thenAnswer(ignored ->
		{
			lifecycleRanOnEdt.compareAndSet(true, SwingUtilities.isEventDispatchThread());
			active.set(true);
			return true;
		});
		when(pluginManager.stopPlugin(eligible)).thenAnswer(ignored ->
		{
			lifecycleRanOnEdt.compareAndSet(true, SwingUtilities.isEventDispatchThread());
			active.set(false);
			return true;
		});

		try (ControlCenterService service = new ControlCenterService(
			pluginManager, 10_000L, new DashboardLogBuffer()))
		{
			assertEquals("RUNNING", service.start("eligible-plugin").get("lifecycle"));
			assertEquals("RUNNING", service.start("eligible-plugin").get("lifecycle"));
			assertEquals("STOPPED", service.stop("eligible-plugin").get("lifecycle"));
			assertEquals("STOPPED", service.stop("eligible-plugin").get("lifecycle"));
		}

		assertTrue(lifecycleRanOnEdt.get());
		verify(pluginManager, times(1)).startPlugin(eligible);
		verify(pluginManager, times(1)).stopPlugin(eligible);
	}

	@Test
	public void duplicateStableIdsAreNotExposed()
	{
		PluginManager pluginManager = mock(PluginManager.class);
		when(pluginManager.getPlugins()).thenReturn(Arrays.asList(new EligiblePlugin(), new DuplicatePlugin()));

		try (ControlCenterService service = new ControlCenterService(
			pluginManager, 10_000L, new DashboardLogBuffer()))
		{
			assertTrue(service.listPlugins().isEmpty());
		}
	}

	@ControlCenterPlugin(id = "eligible-plugin")
	@PluginDescriptor(name = "Eligible Plugin", description = "test")
	private static class EligiblePlugin extends Plugin
	{
	}

	@ControlCenterPlugin(id = "eligible-plugin")
	@PluginDescriptor(name = "Duplicate Plugin", description = "test")
	private static class DuplicatePlugin extends Plugin
	{
	}

	@PluginDescriptor(name = "Unmarked Plugin", description = "test")
	private static class UnmarkedPlugin extends Plugin
	{
	}

	private enum TestState
	{
		IDLE,
		RUNNING
	}

	private static class TestStateMachine extends StateMachineScript<TestState>
	{
		@Override
		protected TestState initialState()
		{
			return TestState.IDLE;
		}

		@Override
		protected List<Transition<TestState>> defineTransitions()
		{
			return Collections.emptyList();
		}

		@Override
		protected void onState(TestState state)
		{
		}
	}
}
