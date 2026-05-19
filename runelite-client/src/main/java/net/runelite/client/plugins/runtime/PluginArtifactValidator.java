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
			errors.add("Plugin is disabled");
		}

		String minClientVersion = artifact.getMinClientVersion();
		if (minClientVersion != null && !clientVersionCompatible.test(minClientVersion))
		{
			errors.add("Plugin requires client version " + minClientVersion);
		}

		return errors.isEmpty()
			? PluginArtifactValidationResult.valid()
			: PluginArtifactValidationResult.invalid(errors);
	}
}
