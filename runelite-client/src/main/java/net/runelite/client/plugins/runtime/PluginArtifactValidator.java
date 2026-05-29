/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class PluginArtifactValidator
{
	public static final String DISABLED_ERROR = "Plugin is disabled";
	public static final String CLIENT_VERSION_ERROR_PREFIX = "Plugin requires client version ";
	public static final String MISSING_ENTRY_CLASSES_ERROR = "Plugin entry classes are missing";
	public static final int CURRENT_PLUGIN_API_VERSION = 1;
	public static final int LEGACY_PLUGIN_API_VERSION = 1;
	public static final String PLUGIN_API_COMPATIBLE = "plugin_api_compatible";
	public static final String PLUGIN_API_TOO_NEW = "plugin_api_too_new";
	public static final String PLUGIN_API_RETIRED = "plugin_api_retired";
	public static final String CLIENT_VERSION_TOO_OLD = "client_version_too_old";
	public static final String PLUGIN_API_MISSING = "plugin_api_missing";
	public static final String PLUGIN_API_MALFORMED = "plugin_api_malformed";
	public static final String PLUGIN_API_SUPPORTED_REASON = "Plugin API version is supported.";
	public static final String PLUGIN_API_TOO_NEW_REASON = "Plugin targets a newer Plugin API Compatibility Version "
		+ "than this client supports.";
	public static final String PLUGIN_API_RETIRED_REASON = "Plugin targets a retired Plugin API Compatibility Version.";
	public static final String CLIENT_VERSION_TOO_OLD_REASON = "Plugin requires a newer Microbot client version.";
	public static final String PLUGIN_API_MISSING_REASON = "Plugin does not declare pluginApiVersion; using legacy "
		+ "Plugin API Compatibility version 1 during the migration window.";
	public static final String PLUGIN_API_MALFORMED_REASON = "Plugin declared pluginApiVersion, but the value could not be parsed.";

	private final Predicate<String> clientVersionCompatible;
	private final int clientPluginApiVersion;
	private final Set<Integer> retiredPluginApiVersions;

	public PluginArtifactValidator(Predicate<String> clientVersionCompatible)
	{
		this(clientVersionCompatible, CURRENT_PLUGIN_API_VERSION, Collections.emptySet());
	}

	public PluginArtifactValidator(
		Predicate<String> clientVersionCompatible,
		int clientPluginApiVersion,
		Set<Integer> retiredPluginApiVersions)
	{
		this.clientVersionCompatible = clientVersionCompatible;
		this.clientPluginApiVersion = clientPluginApiVersion;
		this.retiredPluginApiVersions = Collections.unmodifiableSet(new HashSet<>(retiredPluginApiVersions));
	}

	public PluginArtifactValidationResult validate(PluginArtifact artifact)
	{
		List<String> errors = new ArrayList<>();
		if (artifact.isDisabled())
		{
			errors.add(DISABLED_ERROR);
		}

		String minClientVersion = artifact.getMinClientVersion();
		if (minClientVersion != null && !clientVersionCompatible.test(minClientVersion))
		{
			errors.add(CLIENT_VERSION_ERROR_PREFIX + minClientVersion);
		}

		if (artifact.getSource() != PluginArtifactSource.CORE && artifact.getEntryClasses().isEmpty())
		{
			errors.add(MISSING_ENTRY_CLASSES_ERROR);
		}

		return errors.isEmpty()
			? PluginArtifactValidationResult.valid()
			: PluginArtifactValidationResult.invalid(errors);
	}

	public PluginArtifactValidationResult validateCompatibility(PluginArtifact artifact)
	{
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		Integer pluginApiVersion = null;
		String action = "allow";
		String reasonCode = PLUGIN_API_COMPATIBLE;
		String reason = PLUGIN_API_SUPPORTED_REASON;

		String rawPluginApiVersion = artifact.getPluginApiVersion();
		if (rawPluginApiVersion == null)
		{
			pluginApiVersion = LEGACY_PLUGIN_API_VERSION;
			action = "warn";
			reasonCode = PLUGIN_API_MISSING;
			reason = PLUGIN_API_MISSING_REASON;
			warnings.add(reason);
		}
		else
		{
			try
			{
				pluginApiVersion = Integer.parseInt(rawPluginApiVersion);
				if (pluginApiVersion > clientPluginApiVersion)
				{
					action = "block";
					reasonCode = PLUGIN_API_TOO_NEW;
					reason = PLUGIN_API_TOO_NEW_REASON;
					errors.add(reason);
				}
				else if (retiredPluginApiVersions.contains(pluginApiVersion))
				{
					action = "block";
					reasonCode = PLUGIN_API_RETIRED;
					reason = PLUGIN_API_RETIRED_REASON;
					errors.add(reason);
				}
			}
			catch (NumberFormatException ex)
			{
				action = "block";
				reasonCode = PLUGIN_API_MALFORMED;
				reason = PLUGIN_API_MALFORMED_REASON;
				errors.add(reason);
			}
		}

		String minClientVersion = artifact.getMinClientVersion();
		if (minClientVersion != null && !clientVersionCompatible.test(minClientVersion))
		{
			action = "block";
			reasonCode = CLIENT_VERSION_TOO_OLD;
			reason = CLIENT_VERSION_TOO_OLD_REASON;
			errors.add(CLIENT_VERSION_ERROR_PREFIX + minClientVersion);
		}

		return PluginArtifactValidationResult.compatibility(
			errors,
			warnings,
			pluginApiVersion,
			clientPluginApiVersion,
			action,
			reasonCode,
			reason);
	}
}
