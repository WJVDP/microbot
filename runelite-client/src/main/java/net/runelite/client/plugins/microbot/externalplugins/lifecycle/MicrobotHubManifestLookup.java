package net.runelite.client.plugins.microbot.externalplugins.lifecycle;

import javax.annotation.Nullable;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;

public interface MicrobotHubManifestLookup
{
	@Nullable
	MicrobotPluginManifest findByInternalName(String internalName);
}
