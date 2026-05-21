/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PluginRuntimeTest
{
	@Test
	public void discoversArtifactsFromRepositoriesInOrder() throws Exception
	{
		PluginArtifact core = PluginArtifact.builder(PluginArtifactSource.CORE, "core").build();
		PluginArtifact local = PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, "local").build();

		PluginRuntime runtime = new PluginRuntime(Arrays.asList(
			new StaticRepository(PluginArtifactSource.CORE, Collections.singletonList(core)),
			new StaticRepository(PluginArtifactSource.LOCAL_DIRECTORY, Collections.singletonList(local))));

		List<PluginArtifact> artifacts = runtime.discover();

		assertEquals(Arrays.asList(core, local), artifacts);
	}

	@Test
	public void discoveryResultIsImmutable() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "example").build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))));

		List<PluginArtifact> artifacts = runtime.discover();

		try
		{
			artifacts.add(artifact);
		}
		catch (UnsupportedOperationException expected)
		{
			return;
		}

		throw new AssertionError("discovered artifacts should be immutable");
	}

	@Test
	public void repositoriesAreImmutable()
	{
		PluginRepository repository = new StaticRepository(PluginArtifactSource.RUNELITE_HUB, Collections.emptyList());
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(repository));

		assertEquals(Collections.singletonList(repository), runtime.getRepositories());
		try
		{
			runtime.getRepositories().clear();
		}
		catch (UnsupportedOperationException expected)
		{
			return;
		}

		throw new AssertionError("repositories should be immutable");
	}

	@Test
	public void discoveryStatusReportsValidationErrors() throws Exception
	{
		PluginArtifact disabled = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "disabled")
			.entryClasses("disabled.Plugin")
			.disabled(true)
			.build();
		PluginArtifact incompatible = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "incompatible")
			.entryClasses("incompatible.Plugin")
			.minClientVersion("999.0.0")
			.build();
		PluginArtifact missingEntryClasses = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "missing-entry-classes")
			.build();

		PluginRuntime runtime = new PluginRuntime(
			Collections.singletonList(new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Arrays.asList(disabled, incompatible, missingEntryClasses))),
			new PluginArtifactValidator("1.0.0"::equals));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertEquals(3, result.getArtifacts().size());
		assertEquals(Collections.singletonList(PluginArtifactValidator.DISABLED_ERROR), result.getArtifacts().get(0).getErrors());
		assertEquals(Collections.singletonList(PluginArtifactValidator.CLIENT_VERSION_ERROR_PREFIX + "999.0.0"), result.getArtifacts().get(1).getErrors());
		assertEquals(Collections.singletonList(PluginArtifactValidator.MISSING_ENTRY_CLASSES_ERROR), result.getArtifacts().get(2).getErrors());
	}

	@Test
	public void discoveryStatusReportsDuplicateIdsAcrossRepositories() throws Exception
	{
		PluginArtifact first = PluginArtifact.builder(PluginArtifactSource.RUNELITE_HUB, "duplicate")
			.entryClasses("first.Plugin")
			.build();
		PluginArtifact second = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "duplicate")
			.entryClasses("second.Plugin")
			.build();

		PluginRuntime runtime = new PluginRuntime(Arrays.asList(
			new StaticRepository(PluginArtifactSource.RUNELITE_HUB, Collections.singletonList(first)),
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(second))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertEquals(2, result.getArtifacts().size());
		assertEquals(Collections.singletonList(PluginRuntime.DUPLICATE_ID_ERROR_PREFIX + "duplicate"), result.getArtifacts().get(0).getErrors());
		assertEquals(Collections.singletonList(PluginRuntime.DUPLICATE_ID_ERROR_PREFIX + "duplicate"), result.getArtifacts().get(1).getErrors());
	}

	@Test
	public void discoveryStatusAllowsLoadableArtifacts() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, "local")
			.entryClasses("local.Plugin")
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.LOCAL_DIRECTORY, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertFalse(result.hasErrors());
		assertTrue(result.getArtifacts().get(0).isLoadable());
		assertEquals(Collections.emptyList(), result.getArtifacts().get(0).getErrors());
	}

	private static final class StaticRepository implements PluginRepository
	{
		private final PluginArtifactSource source;
		private final List<PluginArtifact> artifacts;

		private StaticRepository(PluginArtifactSource source, List<PluginArtifact> artifacts)
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
		public List<PluginArtifact> discover() throws IOException
		{
			return artifacts;
		}
	}
}
