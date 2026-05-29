/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PluginCapabilityPolicy
{
	static final String OK_REASON = "Plugin capability metadata is present.";
	static final String MISSING_REASON = "Plugin does not declare capabilities.";
	static final String UNKNOWN_REASON = "Plugin declares capabilities this client does not recognize.";
	static final String RESTRICTED_REASON = "Plugin requests restricted capabilities.";
	static final String LOCAL_WARNING_REASON = "Allowed because this is a local development plugin.";
	static final String BLOCKED_FOR_SOURCE_REASON = "This plugin source requires valid capability metadata.";
	private static final int SUPPORTED_SCHEMA_VERSION = 1;
	private static final Set<String> KNOWN_CAPABILITIES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
		"game_state.read",
		"game_input.control",
		"movement.control",
		"inventory.control",
		"combat.control",
		"network.local",
		"network.remote",
		"filesystem.read",
		"filesystem.write",
		"process.launch",
		"credentials.access",
		"settings.modify"
	)));
	private static final Set<String> DEFAULT_RESTRICTED_CAPABILITIES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
		"credentials.access",
		"process.launch"
	)));

	private PluginCapabilityPolicy()
	{
	}

	static CapabilityStatus evaluate(PluginArtifact artifact)
	{
		PluginCapabilityManifest manifest = artifact.getCapabilityManifest();
		if (manifest == null)
		{
			if (artifact.getSource() == PluginArtifactSource.LOCAL_DIRECTORY)
			{
				return new CapabilityStatus(
					PluginCapabilityState.MISSING,
					Collections.emptyList(),
					Collections.emptyList(),
					"warn",
					"capabilities_local_warning",
					LOCAL_WARNING_REASON,
					true);
			}
			return new CapabilityStatus(
				PluginCapabilityState.MISSING,
				Collections.emptyList(),
				Collections.emptyList(),
				"block",
				"capabilities_blocked_for_source",
				BLOCKED_FOR_SOURCE_REASON,
				false);
		}

		List<String> capabilities = manifest.getCapabilities();
		List<String> restrictedCapabilities = restrictedCapabilities(capabilities);
		boolean unknown = manifest.getPermissionSchemaVersion() != SUPPORTED_SCHEMA_VERSION
			|| capabilities.stream().anyMatch(capability -> !KNOWN_CAPABILITIES.contains(capability));
		if (unknown)
		{
			return forNonNormalState(artifact, PluginCapabilityState.UNKNOWN, capabilities, restrictedCapabilities,
				"capabilities_unknown", UNKNOWN_REASON);
		}
		if (!restrictedCapabilities.isEmpty())
		{
			return forNonNormalState(artifact, PluginCapabilityState.RESTRICTED, capabilities, restrictedCapabilities,
				"capabilities_restricted", RESTRICTED_REASON);
		}
		return new CapabilityStatus(
			PluginCapabilityState.NORMAL,
			capabilities,
			Collections.emptyList(),
			"allow",
			"capabilities_ok",
			OK_REASON,
			false);
	}

	public static boolean allowsLifecycleOperation(PluginRuntimeArtifactStatus status, PluginLifecycleOperation operation)
	{
		if (operation == PluginLifecycleOperation.STOP || operation == PluginLifecycleOperation.REMOVE)
		{
			return true;
		}
		return !"block".equals(status.getCapabilityPolicyAction());
	}

	private static CapabilityStatus forNonNormalState(
		PluginArtifact artifact,
		PluginCapabilityState state,
		List<String> capabilities,
		List<String> restrictedCapabilities,
		String reasonCode,
		String reason)
	{
		if (artifact.getSource() == PluginArtifactSource.LOCAL_DIRECTORY)
		{
			return new CapabilityStatus(
				state,
				capabilities,
				restrictedCapabilities,
				"warn",
				"capabilities_local_warning",
				LOCAL_WARNING_REASON,
				true);
		}
		return new CapabilityStatus(state, capabilities, restrictedCapabilities, "block", reasonCode, reason, false);
	}

	private static List<String> restrictedCapabilities(List<String> capabilities)
	{
		List<String> restricted = new ArrayList<>();
		for (String capability : capabilities)
		{
			if (DEFAULT_RESTRICTED_CAPABILITIES.contains(capability))
			{
				restricted.add(capability);
			}
		}
		return restricted;
	}
}
