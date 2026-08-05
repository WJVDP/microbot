package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.agentserver.handler.ScriptHeartbeatRegistry;
import net.runelite.client.plugins.microbot.statemachine.StateMachineScript;
import net.runelite.client.plugins.microbot.statemachine.StateSnapshot;
import net.runelite.client.util.Text;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/** Owns eligible plugin discovery, lifecycle serialization, and status aggregation. */
@Slf4j
public final class ControlCenterService implements AutoCloseable
{
	private static final Pattern VALID_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
	private static final long LIFECYCLE_TIMEOUT_SECONDS = 10;

	private final PluginManager pluginManager;
	private final long heartbeatStallMs;
	private final DashboardLogBuffer logBuffer;
	private final DashboardLogAppender logAppender;
	private final Map<String, OperationState> operations = new ConcurrentHashMap<>();
	private final Map<String, Long> startedAt = new ConcurrentHashMap<>();
	private final Set<String> warnedEligibility = ConcurrentHashMap.newKeySet();
	private final Set<String> knownEligibleIds = new LinkedHashSet<>();

	public ControlCenterService(PluginManager pluginManager, long heartbeatStallMs,
		DashboardLogBuffer logBuffer)
	{
		this.pluginManager = pluginManager;
		this.heartbeatStallMs = heartbeatStallMs;
		this.logBuffer = logBuffer;
		this.logAppender = new DashboardLogAppender(logBuffer);

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		logAppender.setName("MICROBOT_CONTROL_CENTER");
		logAppender.setContext(context);
		logAppender.start();
		context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(logAppender);
		refreshEligible();
	}

	public List<Map<String, Object>> listPlugins()
	{
		Map<String, ManagedPlugin> eligible = refreshEligible();
		List<Map<String, Object>> result = new ArrayList<>();
		for (ManagedPlugin plugin : eligible.values())
		{
			result.add(snapshot(plugin));
		}
		result.sort(Comparator.comparing(entry -> (String) entry.get("name"), String.CASE_INSENSITIVE_ORDER));
		return Collections.unmodifiableList(result);
	}

	public Map<String, Object> getPlugin(String pluginId)
	{
		ManagedPlugin plugin = requireEligible(pluginId);
		return snapshot(plugin);
	}

	public Map<String, Object> start(String pluginId) throws ControlCenterException
	{
		return changeLifecycle(requireEligible(pluginId), true);
	}

	public Map<String, Object> stop(String pluginId) throws ControlCenterException
	{
		return changeLifecycle(requireEligible(pluginId), false);
	}

	public Map<String, Object> logs(String pluginId, long afterSequence)
	{
		requireEligible(pluginId);
		List<DashboardLogBuffer.DashboardLogEvent> events = logBuffer.get(pluginId, Math.max(0, afterSequence));
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("pluginId", pluginId);
		result.put("count", events.size());
		result.put("maxEvents", DashboardLogBuffer.DEFAULT_CAPACITY);
		result.put("events", events);
		return result;
	}

	private Map<String, Object> changeLifecycle(ManagedPlugin managed, boolean start) throws ControlCenterException
	{
		OperationState state = operations.computeIfAbsent(managed.id, ignored -> new OperationState());
		synchronized (state.lock)
		{
			boolean active = pluginManager.isActive(managed.plugin);
			if (active == start)
			{
				state.lifecycle = active ? Lifecycle.RUNNING : Lifecycle.STOPPED;
				state.lastError = null;
				state.failureAt = 0;
				return snapshot(managed);
			}
			if ((start && state.lifecycle == Lifecycle.STARTING)
				|| (!start && state.lifecycle == Lifecycle.STOPPING))
			{
				return snapshot(managed);
			}
			if (state.lifecycle == Lifecycle.STARTING || state.lifecycle == Lifecycle.STOPPING)
			{
				throw new ControlCenterException(409, "An opposite lifecycle action is still pending");
			}

			state.lifecycle = start ? Lifecycle.STARTING : Lifecycle.STOPPING;
			state.lastError = null;
			state.failureAt = 0;
			CompletableFuture<Void> completion = new CompletableFuture<>();
			if (SwingUtilities.isEventDispatchThread())
			{
				runLifecycleOnEdt(managed, start, state, completion);
			}
			else
			{
				SwingUtilities.invokeLater(() -> runLifecycleOnEdt(managed, start, state, completion));
			}
			try
			{
				completion.get(LIFECYCLE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			}
			catch (TimeoutException e)
			{
				throw new ControlCenterException(504, "Lifecycle action is still pending");
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new ControlCenterException(503, "Lifecycle action was interrupted");
			}
			catch (ExecutionException e)
			{
				String message = state.lastError != null ? state.lastError : "Lifecycle action failed";
				throw new ControlCenterException(500, message);
			}
			return snapshot(managed);
		}
	}

	private void runLifecycleOnEdt(ManagedPlugin managed, boolean start, OperationState state,
		CompletableFuture<Void> completion)
	{
		try
		{
			if (start)
			{
				startedAt.put(managed.id, System.currentTimeMillis());
				pluginManager.setPluginEnabled(managed.plugin, true);
				if (!pluginManager.isActive(managed.plugin))
				{
					pluginManager.startPlugin(managed.plugin);
				}
				if (!pluginManager.isActive(managed.plugin))
				{
					pluginManager.setPluginEnabled(managed.plugin, false);
					throw new PluginInstantiationException("Plugin did not enter the active state");
				}
				state.lifecycle = Lifecycle.RUNNING;
			}
			else
			{
				pluginManager.setPluginEnabled(managed.plugin, false);
				if (pluginManager.isActive(managed.plugin))
				{
					pluginManager.stopPlugin(managed.plugin);
				}
				if (pluginManager.isActive(managed.plugin))
				{
					pluginManager.setPluginEnabled(managed.plugin, true);
					throw new PluginInstantiationException("Plugin remained active after stop");
				}
				startedAt.remove(managed.id);
				ControlCenterStatusRegistry.unregister(managed.id);
				state.lifecycle = Lifecycle.STOPPED;
			}
			state.lastError = null;
			state.failureAt = 0;
			completion.complete(null);
		}
		catch (Exception error)
		{
			String message = safeError(error);
			state.lastError = message;
			state.failureAt = System.currentTimeMillis();
			state.lifecycle = Lifecycle.FAILED;
			logBuffer.appendLifecycleError(managed.id, message);
			completion.completeExceptionally(error);
		}
	}

	private Map<String, Object> snapshot(ManagedPlugin managed)
	{
		long now = System.currentTimeMillis();
		boolean active = pluginManager.isActive(managed.plugin);
		OperationState operation = operations.computeIfAbsent(managed.id, ignored -> new OperationState());
		Lifecycle lifecycle = operation.lifecycle;
		if (lifecycle != Lifecycle.STARTING && lifecycle != Lifecycle.STOPPING && lifecycle != Lifecycle.FAILED)
		{
			lifecycle = active ? Lifecycle.RUNNING : Lifecycle.STOPPED;
			operation.lifecycle = lifecycle;
		}

		ScriptHeartbeatRegistry.HeartbeatSnapshot heartbeat = findHeartbeat(managed);
		if (lifecycle == Lifecycle.FAILED && active && heartbeat != null
			&& heartbeat.getLastHeartbeatMs() > operation.failureAt)
		{
			operation.lifecycle = Lifecycle.RUNNING;
			operation.lastError = null;
			operation.failureAt = 0;
			lifecycle = Lifecycle.RUNNING;
		}
		Long start = active ? startedAt.computeIfAbsent(managed.id, ignored -> now) : null;
		if (active && heartbeat != null)
		{
			start = heartbeat.getFirstHeartbeatMs();
			startedAt.put(managed.id, start);
		}
		if (!active && lifecycle != Lifecycle.STARTING)
		{
			startedAt.remove(managed.id);
			ControlCenterStatusRegistry.unregister(managed.id);
		}

		DashboardLogBuffer.DashboardLogEvent scriptError = active && start != null
			? logBuffer.lastErrorAfter(managed.id, start)
			: null;
		Health health;
		if (lifecycle == Lifecycle.FAILED || scriptError != null)
		{
			health = Health.FAILED;
			if (lifecycle == Lifecycle.RUNNING)
			{
				lifecycle = Lifecycle.FAILED;
			}
		}
		else if (!active || heartbeat == null)
		{
			health = Health.UNKNOWN;
		}
		else
		{
			health = now - heartbeat.getLastHeartbeatMs() > heartbeatStallMs ? Health.STALLED : Health.HEALTHY;
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", managed.id);
		result.put("name", managed.name);
		result.put("lifecycle", lifecycle.name());
		result.put("health", health.name());
		result.put("active", active);
		result.put("startedAt", start != null ? Instant.ofEpochMilli(start).toString() : null);
		result.put("runtimeMs", start != null ? Math.max(0, now - start) : null);
		result.put("lastHeartbeatAt", heartbeat != null
			? Instant.ofEpochMilli(heartbeat.getLastHeartbeatMs()).toString() : null);
		result.put("heartbeatAgeMs", heartbeat != null ? Math.max(0, now - heartbeat.getLastHeartbeatMs()) : null);
		result.put("loopCount", heartbeat != null ? heartbeat.getLoopCount() : 0);

		StateSnapshot<?> state = findStateSnapshot(managed, heartbeat);
		addStateMachineStatus(result, state, now);
		ControlCenterStatusSnapshot extension = safeExtension(managed.id);
		String currentAction = extension != null ? extension.getCurrentAction() : null;
		if (currentAction == null && state != null && state.currentState() != null)
		{
			currentAction = state.currentState().name();
		}
		result.put("currentAction", currentAction);
		result.put("details", extension != null ? extension.getDetails() : Collections.emptyMap());
		String lastError = operation.lastError;
		if (lastError == null && scriptError != null)
		{
			lastError = scriptError.getMessage();
			if (scriptError.getException() != null)
			{
				lastError += " (" + scriptError.getException() + ")";
			}
		}
		result.put("lastError", lastError);
		return result;
	}

	private static void addStateMachineStatus(Map<String, Object> result, StateSnapshot<?> state, long now)
	{
		if (state == null)
		{
			result.put("currentState", null);
			result.put("stateEnteredAt", null);
			result.put("msInCurrentState", null);
			result.put("transitionCount", 0);
			result.put("previousState", null);
			result.put("lastTransitionReason", null);
			result.put("lastTransitionAt", null);
			result.put("phases", Collections.emptyList());
			return;
		}

		Enum<?> current = state.currentState();
		result.put("currentState", current != null ? current.name() : null);
		result.put("stateEnteredAt", state.stateEnteredAt() != null ? state.stateEnteredAt().toString() : null);
		result.put("msInCurrentState", state.stateEnteredAt() != null
			? Math.max(0, now - state.stateEnteredAt().toEpochMilli()) : null);
		result.put("transitionCount", state.transitionCount());
		result.put("previousState", state.previousState() != null ? state.previousState().name() : null);
		result.put("lastTransitionReason", boundedStatus(state.lastTransitionReason(), 500));
		result.put("lastTransitionAt", state.lastTransitionAt() != null ? state.lastTransitionAt().toString() : null);
		List<String> phases = new ArrayList<>();
		if (current != null)
		{
			for (Object phase : current.getDeclaringClass().getEnumConstants())
			{
				phases.add(((Enum<?>) phase).name());
			}
		}
		result.put("phases", phases);
	}

	private ScriptHeartbeatRegistry.HeartbeatSnapshot findHeartbeat(ManagedPlugin managed)
	{
		ScriptHeartbeatRegistry.HeartbeatSnapshot newest = null;
		for (ScriptHeartbeatRegistry.HeartbeatSnapshot heartbeat : ScriptHeartbeatRegistry.getSnapshots())
		{
			if (isInPluginNamespace(managed, heartbeat.getScriptClassName())
				&& (newest == null || heartbeat.getLastHeartbeatMs() > newest.getLastHeartbeatMs()))
			{
				newest = heartbeat;
			}
		}
		return newest;
	}

	private StateSnapshot<?> findStateSnapshot(ManagedPlugin managed,
		ScriptHeartbeatRegistry.HeartbeatSnapshot heartbeat)
	{
		StateSnapshot<?> fallback = null;
		for (StateMachineScript<?> script : StateMachineScript.getRegistry().values())
		{
			String className = script.getClass().getName();
			if (!isInPluginNamespace(managed, className))
			{
				continue;
			}
			StateSnapshot<?> snapshot = script.getSnapshot();
			if (heartbeat != null && heartbeat.getScriptClassName().equals(className))
			{
				return snapshot;
			}
			if (fallback == null && snapshot != null)
			{
				fallback = snapshot;
			}
		}
		return fallback;
	}

	private static boolean isInPluginNamespace(ManagedPlugin managed, String className)
	{
		return className.equals(managed.packageName) || className.startsWith(managed.packageName + ".");
	}

	private ControlCenterStatusSnapshot safeExtension(String pluginId)
	{
		try
		{
			return ControlCenterStatusRegistry.snapshot(pluginId);
		}
		catch (RuntimeException error)
		{
			if (warnedEligibility.add("provider:" + pluginId))
			{
				log.warn("Control-center status provider failed for {}", pluginId);
			}
			return null;
		}
	}

	private ManagedPlugin requireEligible(String pluginId)
	{
		ManagedPlugin plugin = refreshEligible().get(pluginId);
		if (plugin == null)
		{
			throw new UnknownPluginException();
		}
		return plugin;
	}

	private synchronized Map<String, ManagedPlugin> refreshEligible()
	{
		Collection<Plugin> plugins = pluginManager.getPlugins();
		Map<String, ManagedPlugin> result = new LinkedHashMap<>();
		Set<String> conflicts = new LinkedHashSet<>();
		Map<String, List<String>> prefixes = new LinkedHashMap<>();
		for (Plugin plugin : plugins)
		{
			ControlCenterPlugin marker = plugin.getClass().getAnnotation(ControlCenterPlugin.class);
			PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
			if (marker == null || descriptor == null)
			{
				continue;
			}
			String id = marker.id();
			if (id == null || id.length() > 64 || !VALID_ID.matcher(id).matches())
			{
				warnOnce("invalid:" + plugin.getClass().getName(), "Ignoring a plugin with an invalid control-center id");
				continue;
			}
			if (result.containsKey(id) || conflicts.contains(id))
			{
				result.remove(id);
				prefixes.remove(id);
				conflicts.add(id);
				warnOnce("duplicate:" + id, "Ignoring duplicate control-center id " + id);
				continue;
			}
			String packageName = plugin.getClass().getPackageName();
			List<String> loggerPrefixes = marker.loggerPrefixes().length == 0
				? Collections.singletonList(packageName)
				: sanitizePrefixes(Arrays.asList(marker.loggerPrefixes()), packageName);
			if (loggerPrefixes.isEmpty())
			{
				warnOnce("loggers:" + id, "Ignoring a plugin with no valid control-center logger namespace");
				continue;
			}
			String displayName = boundedStatus(Text.removeTags(descriptor.name()).trim(), 128);
			result.put(id, new ManagedPlugin(id, displayName, packageName, plugin));
			prefixes.put(id, loggerPrefixes);
		}
		Set<String> removed = new LinkedHashSet<>(knownEligibleIds);
		removed.removeAll(result.keySet());
		for (String pluginId : removed)
		{
			operations.remove(pluginId);
			startedAt.remove(pluginId);
			logBuffer.remove(pluginId);
			ControlCenterStatusRegistry.unregister(pluginId);
		}
		knownEligibleIds.clear();
		knownEligibleIds.addAll(result.keySet());
		logAppender.updateEligible(prefixes);
		return result;
	}

	private static List<String> sanitizePrefixes(List<String> values, String packageName)
	{
		List<String> result = new ArrayList<>();
		for (String value : values)
		{
			if (value != null && !value.isBlank() && value.length() <= 256)
			{
				String prefix = value.trim();
				if (prefix.equals(packageName) || prefix.startsWith(packageName + "."))
				{
					result.add(prefix);
				}
			}
		}
		return result;
	}

	private void warnOnce(String key, String message)
	{
		if (warnedEligibility.add(key))
		{
			log.warn(message);
		}
	}

	private static String safeError(Throwable error)
	{
		Throwable cause = error;
		while (cause.getCause() != null && cause.getCause() != cause)
		{
			cause = cause.getCause();
		}
		String message = cause.getMessage();
		String summary = cause.getClass().getSimpleName() + (message == null ? "" : ": " + message);
		return boundedStatus(summary, 500);
	}

	private static String boundedStatus(String value, int maximum)
	{
		String redacted = DashboardLogBuffer.redact(value);
		if (redacted == null || redacted.length() <= maximum)
		{
			return redacted;
		}
		return redacted.substring(0, maximum);
	}

	@Override
	public void close()
	{
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(logAppender);
		logAppender.stop();
	}

	private enum Lifecycle
	{
		STOPPED,
		STARTING,
		RUNNING,
		STOPPING,
		FAILED
	}

	private enum Health
	{
		UNKNOWN,
		HEALTHY,
		STALLED,
		FAILED
	}

	private static final class OperationState
	{
		private final Object lock = new Object();
		private volatile Lifecycle lifecycle = Lifecycle.STOPPED;
		private volatile String lastError;
		private volatile long failureAt;
	}

	private static final class ManagedPlugin
	{
		private final String id;
		private final String name;
		private final String packageName;
		private final Plugin plugin;

		private ManagedPlugin(String id, String name, String packageName, Plugin plugin)
		{
			this.id = id;
			this.name = name;
			this.packageName = packageName;
			this.plugin = plugin;
		}
	}

	public static class ControlCenterException extends Exception
	{
		private final int statusCode;

		private ControlCenterException(int statusCode, String message)
		{
			super(message);
			this.statusCode = statusCode;
		}

		public int getStatusCode()
		{
			return statusCode;
		}
	}

	public static final class UnknownPluginException extends IllegalArgumentException
	{
		private UnknownPluginException()
		{
			super("Eligible plugin not found");
		}
	}
}
