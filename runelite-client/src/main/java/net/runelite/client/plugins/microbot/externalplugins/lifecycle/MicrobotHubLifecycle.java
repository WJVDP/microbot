package net.runelite.client.plugins.microbot.externalplugins.lifecycle;

import com.google.common.base.Strings;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;

public class MicrobotHubLifecycle
{
	private final MicrobotHubManifestLookup manifestLookup;
	private final MicrobotHubArtifactStore artifactStore;

	public MicrobotHubLifecycle(
		MicrobotHubManifestLookup manifestLookup,
		MicrobotHubArtifactStore artifactStore)
	{
		this.manifestLookup = manifestLookup;
		this.artifactStore = artifactStore;
	}

	public InstallResult install(String internalName, @Nullable String versionOverride)
	{
		MicrobotPluginManifest manifest = findManifest(internalName);
		if (manifest == null)
		{
			return InstallResult.rejected(null);
		}

		if (!artifactStore.install(manifest, versionOverride))
		{
			return InstallResult.rejected(manifest);
		}

		return InstallResult.installed(manifest);
	}

	public RemoveResult remove(String internalName)
	{
		MicrobotPluginManifest manifest = findManifest(internalName);
		if (manifest == null)
		{
			return RemoveResult.rejected(null);
		}

		if (!artifactStore.remove(manifest))
		{
			return RemoveResult.rejected(manifest);
		}

		return RemoveResult.removed(manifest);
	}

	public UpdateResult update(String internalName, @Nullable String versionOverride)
	{
		MicrobotPluginManifest manifest = findManifest(internalName);
		if (manifest == null)
		{
			return UpdateResult.rejected(null);
		}

		if (!artifactStore.remove(manifest))
		{
			return UpdateResult.rejected(manifest);
		}

		if (!artifactStore.install(manifest, versionOverride))
		{
			return UpdateResult.rejected(manifest);
		}

		return UpdateResult.updated(manifest);
	}

	@Nullable
	private MicrobotPluginManifest findManifest(String internalName)
	{
		if (Strings.isNullOrEmpty(internalName))
		{
			return null;
		}

		return manifestLookup.findByInternalName(internalName);
	}

	@Value
	public static class InstallResult
	{
		boolean installed;
		@Nullable
		MicrobotPluginManifest manifest;

		static InstallResult installed(MicrobotPluginManifest manifest)
		{
			return new InstallResult(true, manifest);
		}

		static InstallResult rejected(@Nullable MicrobotPluginManifest manifest)
		{
			return new InstallResult(false, manifest);
		}
	}

	@Value
	public static class RemoveResult
	{
		boolean removed;
		@Nullable
		MicrobotPluginManifest manifest;

		static RemoveResult removed(MicrobotPluginManifest manifest)
		{
			return new RemoveResult(true, manifest);
		}

		static RemoveResult rejected(@Nullable MicrobotPluginManifest manifest)
		{
			return new RemoveResult(false, manifest);
		}
	}

	@Value
	public static class UpdateResult
	{
		boolean updated;
		@Nullable
		MicrobotPluginManifest manifest;

		static UpdateResult updated(MicrobotPluginManifest manifest)
		{
			return new UpdateResult(true, manifest);
		}

		static UpdateResult rejected(@Nullable MicrobotPluginManifest manifest)
		{
			return new UpdateResult(false, manifest);
		}
	}
}
