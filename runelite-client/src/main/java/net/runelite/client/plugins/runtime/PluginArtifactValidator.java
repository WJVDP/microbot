/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PluginArtifactValidator
{
	public static final String DISABLED_ERROR = "Plugin is disabled";
	public static final String CLIENT_VERSION_ERROR_PREFIX = "Plugin requires client version ";
	public static final String MISSING_ENTRY_CLASSES_ERROR = "Plugin entry classes are missing";

	private final Predicate<String> clientVersionCompatible;

	public PluginArtifactValidator(Predicate<String> clientVersionCompatible)
	{
		this.clientVersionCompatible = clientVersionCompatible;
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
}
