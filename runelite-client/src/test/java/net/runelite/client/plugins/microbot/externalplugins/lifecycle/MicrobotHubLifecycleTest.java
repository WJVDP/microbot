package net.runelite.client.plugins.microbot.externalplugins.lifecycle;

import javax.annotation.Nullable;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MicrobotHubLifecycleTest
{
	@Test
	public void installStoresResolvedManifestVersion()
	{
		MicrobotPluginManifest manifest = createManifest("TestPlugin");
		FakeManifestLookup manifests = new FakeManifestLookup(manifest);
		FakeArtifactStore artifacts = new FakeArtifactStore();
		MicrobotHubLifecycle lifecycle = new MicrobotHubLifecycle(manifests, artifacts);

		MicrobotHubLifecycle.InstallResult result = lifecycle.install("TestPlugin", "1.1.0");

		assertTrue(result.isInstalled());
		assertSame(manifest, result.getManifest());
		assertSame(manifest, artifacts.installedManifest);
		assertEquals("1.1.0", artifacts.installedVersion);
		assertEquals(0, artifacts.removedCount);
	}

	@Test
	public void removeDeletesResolvedManifestArtifact()
	{
		MicrobotPluginManifest manifest = createManifest("TestPlugin");
		FakeArtifactStore artifacts = new FakeArtifactStore();
		MicrobotHubLifecycle lifecycle = new MicrobotHubLifecycle(new FakeManifestLookup(manifest), artifacts);

		MicrobotHubLifecycle.RemoveResult result = lifecycle.remove("TestPlugin");

		assertTrue(result.isRemoved());
		assertSame(manifest, result.getManifest());
		assertSame(manifest, artifacts.removedManifest);
		assertEquals(1, artifacts.removedCount);
		assertNull(artifacts.installedManifest);
	}

	@Test
	public void updateRemovesThenInstallsResolvedManifestVersion()
	{
		MicrobotPluginManifest manifest = createManifest("TestPlugin");
		FakeArtifactStore artifacts = new FakeArtifactStore();
		MicrobotHubLifecycle lifecycle = new MicrobotHubLifecycle(new FakeManifestLookup(manifest), artifacts);

		MicrobotHubLifecycle.UpdateResult result = lifecycle.update("TestPlugin", "1.1.0");

		assertTrue(result.isUpdated());
		assertSame(manifest, result.getManifest());
		assertSame(manifest, artifacts.removedManifest);
		assertSame(manifest, artifacts.installedManifest);
		assertEquals("1.1.0", artifacts.installedVersion);
		assertEquals(1, artifacts.removedCount);
	}

	@Test
	public void installRejectsMissingManifestWithoutStoringArtifact()
	{
		MicrobotPluginManifest manifest = createManifest("KnownPlugin");
		FakeArtifactStore artifacts = new FakeArtifactStore();
		MicrobotHubLifecycle lifecycle = new MicrobotHubLifecycle(new FakeManifestLookup(manifest), artifacts);

		MicrobotHubLifecycle.InstallResult result = lifecycle.install("MissingPlugin", "1.1.0");

		assertFalse(result.isInstalled());
		assertNull(result.getManifest());
		assertNull(artifacts.installedManifest);
		assertNull(artifacts.installedVersion);
		assertEquals(0, artifacts.removedCount);
	}

	@Test
	public void updateStopsWhenRemoveFails()
	{
		MicrobotPluginManifest manifest = createManifest("TestPlugin");
		FakeArtifactStore artifacts = new FakeArtifactStore();
		artifacts.removeSucceeds = false;
		MicrobotHubLifecycle lifecycle = new MicrobotHubLifecycle(new FakeManifestLookup(manifest), artifacts);

		MicrobotHubLifecycle.UpdateResult result = lifecycle.update("TestPlugin", "1.1.0");

		assertFalse(result.isUpdated());
		assertSame(manifest, result.getManifest());
		assertSame(manifest, artifacts.removedManifest);
		assertNull(artifacts.installedManifest);
	}

	private static MicrobotPluginManifest createManifest(String internalName)
	{
		MicrobotPluginManifest manifest = new MicrobotPluginManifest();
		manifest.setInternalName(internalName);
		manifest.setVersion("1.0.0");
		return manifest;
	}

	private static class FakeManifestLookup implements MicrobotHubManifestLookup
	{
		private final MicrobotPluginManifest manifest;

		private FakeManifestLookup(MicrobotPluginManifest manifest)
		{
			this.manifest = manifest;
		}

		@Nullable
		@Override
		public MicrobotPluginManifest findByInternalName(String internalName)
		{
			return manifest.getInternalName().equals(internalName) ? manifest : null;
		}
	}

	private static class FakeArtifactStore implements MicrobotHubArtifactStore
	{
		private MicrobotPluginManifest installedManifest;
		private String installedVersion;
		private MicrobotPluginManifest removedManifest;
		private int removedCount;
		private boolean removeSucceeds = true;

		@Override
		public boolean install(MicrobotPluginManifest manifest, @Nullable String versionOverride)
		{
			installedManifest = manifest;
			installedVersion = versionOverride;
			return true;
		}

		@Override
		public boolean remove(MicrobotPluginManifest manifest)
		{
			removedManifest = manifest;
			removedCount++;
			return removeSucceeds;
		}
	}
}
