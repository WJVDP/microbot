/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.Collections;
import java.util.List;

public final class PluginArtifactValidationResult
{
	private static final PluginArtifactValidationResult VALID = new PluginArtifactValidationResult(Collections.emptyList());

	private final List<String> errors;

	private PluginArtifactValidationResult(List<String> errors)
	{
		this.errors = Collections.unmodifiableList(errors);
	}

	public static PluginArtifactValidationResult valid()
	{
		return VALID;
	}

	public static PluginArtifactValidationResult invalid(List<String> errors)
	{
		return new PluginArtifactValidationResult(errors);
	}

	public boolean isValid()
	{
		return errors.isEmpty();
	}

	public List<String> getErrors()
	{
		return errors;
	}
}
