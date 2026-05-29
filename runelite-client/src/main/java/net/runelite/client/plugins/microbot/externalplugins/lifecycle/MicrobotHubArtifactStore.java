package net.runelite.client.plugins.microbot.externalplugins.lifecycle;

import javax.annotation.Nullable;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;

public interface MicrobotHubArtifactStore
{
	boolean install(MicrobotPluginManifest manifest, @Nullable String versionOverride);

	boolean remove(MicrobotPluginManifest manifest);
}
