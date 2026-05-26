/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PluginCapabilityPolicyTest
{
	@Test
	public void sourceAwareLifecyclePolicyBlocksOnlyInstallUpdateAndStart() throws Exception
	{
		PluginRuntimeArtifactStatus blockedHub = status(PluginArtifactSource.MICROBOT_HUB, null);
		PluginRuntimeArtifactStatus warnedLocal = status(PluginArtifactSource.LOCAL_DIRECTORY, null);
		PluginRuntimeArtifactStatus normalHub = status(PluginArtifactSource.MICROBOT_HUB,
			manifest(PluginArtifactSource.MICROBOT_HUB, "normal", Collections.singletonList("game_state.read"), 1));
		PluginRuntimeArtifactStatus unknownHub = status(PluginArtifactSource.MICROBOT_HUB,
			manifest(PluginArtifactSource.MICROBOT_HUB, "unknown", Collections.singletonList("future.power"), 1));
		PluginRuntimeArtifactStatus restrictedHub = status(PluginArtifactSource.MICROBOT_HUB,
			manifest(PluginArtifactSource.MICROBOT_HUB, "restricted", Collections.singletonList("credentials.access"), 1));
		PluginRuntimeArtifactStatus unknownLocal = status(PluginArtifactSource.LOCAL_DIRECTORY,
			manifest(PluginArtifactSource.LOCAL_DIRECTORY, "local-unknown", Collections.singletonList("future.power"), 1));
		PluginRuntimeArtifactStatus restrictedLocal = status(PluginArtifactSource.LOCAL_DIRECTORY,
			manifest(PluginArtifactSource.LOCAL_DIRECTORY, "local-restricted", Collections.singletonList("credentials.access"), 1));

		assertFalse(PluginCapabilityPolicy.allowsLifecycleOperation(blockedHub, PluginLifecycleOperation.INSTALL));
		assertFalse(PluginCapabilityPolicy.allowsLifecycleOperation(blockedHub, PluginLifecycleOperation.UPDATE));
		assertFalse(PluginCapabilityPolicy.allowsLifecycleOperation(blockedHub, PluginLifecycleOperation.START));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(blockedHub, PluginLifecycleOperation.STOP));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(blockedHub, PluginLifecycleOperation.REMOVE));

		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(warnedLocal, PluginLifecycleOperation.INSTALL));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(warnedLocal, PluginLifecycleOperation.UPDATE));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(warnedLocal, PluginLifecycleOperation.START));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(warnedLocal, PluginLifecycleOperation.STOP));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(warnedLocal, PluginLifecycleOperation.REMOVE));

		assertLifecycleAllowed(normalHub);
		assertInstallUpdateStartBlocked(unknownHub);
		assertInstallUpdateStartBlocked(restrictedHub);
		assertLifecycleAllowed(unknownLocal);
		assertLifecycleAllowed(restrictedLocal);
	}

	private static void assertLifecycleAllowed(PluginRuntimeArtifactStatus status)
	{
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.INSTALL));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.UPDATE));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.START));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.STOP));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.REMOVE));
	}

	private static void assertInstallUpdateStartBlocked(PluginRuntimeArtifactStatus status)
	{
		assertFalse(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.INSTALL));
		assertFalse(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.UPDATE));
		assertFalse(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.START));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.STOP));
		assertTrue(PluginCapabilityPolicy.allowsLifecycleOperation(status, PluginLifecycleOperation.REMOVE));
	}

	private static PluginCapabilityManifest manifest(PluginArtifactSource source, String id, List<String> capabilities, int schemaVersion)
	{
		return PluginCapabilityManifest.builder(id, id, "1.0.0", source)
			.capabilities(capabilities)
			.permissionSchemaVersion(schemaVersion)
			.build();
	}

	private static PluginRuntimeArtifactStatus status(PluginArtifactSource source, PluginCapabilityManifest manifest) throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(source, source.name())
			.entryClasses("example.Plugin")
			.signature("test")
			.capabilityManifest(manifest)
			.build();
		return new PluginRuntime(Collections.singletonList(new StaticRepository(source, Collections.singletonList(artifact))))
			.discoverStatus()
			.getArtifacts()
			.get(0);
	}

	private static final class StaticRepository implements PluginRepository
	{
		private final PluginArtifactSource source;
		private final java.util.List<PluginArtifact> artifacts;

		private StaticRepository(PluginArtifactSource source, java.util.List<PluginArtifact> artifacts)
		{
			this.source = source;
			this.artifacts = artifacts;
		}

		@Override
		public PluginArtifactSource getSource()
		{
			return source;
		}

		@Override
		public java.util.List<PluginArtifact> discover()
		{
			return artifacts;
		}
	}
}
