# Runtime Service Boundaries

Milestone 6 introduces injectable services for new Microbot code while keeping
the existing static `Microbot` API as a compatibility shim for scripts and
plugins that already depend on it.

## Services

- `PluginRuntimeService` wraps runtime artifact discovery and status checks.
- `BridgeApiService` is the Bridge V1 backend contract used by
  `BridgeV1Handler`.
- `ScriptLifecycleService` exposes active Microbot plugins and scripts.
- `GameStateCacheService` exposes login state and Microbot cache access.
- `TelemetryUpdateService` exposes telemetry and update-check policy.

## Migration Examples

Prefer constructor or field injection for new plugins:

```java
@Inject
private GameStateCacheService gameStateCacheService;

if (gameStateCacheService.isLoggedIn())
{
	int runEnergyVarbit = gameStateCacheService.getVarbitValue(varbitId);
}
```

Existing static calls continue to compile:

```java
if (Microbot.isLoggedIn())
{
	int runEnergyVarbit = Microbot.getVarbitValue(varbitId);
}
```

For script lifecycle queries, inject `ScriptLifecycleService` instead of
reflecting through `Microbot`:

```java
@Inject
private ScriptLifecycleService scriptLifecycleService;

for (Script script : scriptLifecycleService.getActiveScripts())
{
	scriptLifecycleService.shutdown(script);
}
```

The compatibility form remains available:

```java
for (Script script : Microbot.getActiveScripts())
{
	script.shutdown();
}
```

Bridge code should depend on `BridgeApiService`:

```java
@Inject
private BridgeApiService bridgeApiService;

PluginRuntimeDiscoveryResult result = bridgeApiService.discoverPluginArtifactStatus();
```

Runtime tests and tools can use `PluginRuntimeService` without launching the
client:

```java
PluginRuntimeService runtime = new PluginRuntime(repositories);
PluginRuntimeDiscoveryResult status = runtime.discoverStatus();
```
