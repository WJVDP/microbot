/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.Collections;
import java.util.List;

public final class PluginRuntimeArtifactStatus
{
	private final PluginArtifact artifact;
	private final List<String> errors;

	PluginRuntimeArtifactStatus(PluginArtifact artifact, List<String> errors)
	{
		this.artifact = artifact;
		this.errors = Collections.unmodifiableList(errors);
	}

	public PluginArtifact getArtifact()
	{
		return artifact;
	}

	public boolean isLoadable()
	{
		return errors.isEmpty();
	}

	public List<String> getErrors()
	{
		return errors;
	}
}
