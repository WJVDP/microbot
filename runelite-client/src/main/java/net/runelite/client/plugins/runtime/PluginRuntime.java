/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PluginRuntime
{
	public static final String DUPLICATE_ID_ERROR_PREFIX = "Duplicate plugin artifact id: ";

	private final List<PluginRepository> repositories;
	private final PluginArtifactValidator validator;

	public PluginRuntime(Collection<PluginRepository> repositories)
	{
		this(repositories, new PluginArtifactValidator(version -> true));
	}

	public PluginRuntime(Collection<PluginRepository> repositories, PluginArtifactValidator validator)
	{
		this.repositories = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(repositories, "repositories")));
		this.validator = Objects.requireNonNull(validator, "validator");
	}

	public List<PluginRepository> getRepositories()
	{
		return repositories;
	}

	public List<PluginArtifact> discover() throws IOException
	{
		List<PluginArtifact> artifacts = new ArrayList<>();
		for (PluginRepository repository : repositories)
		{
			artifacts.addAll(repository.discover());
		}
		return Collections.unmodifiableList(artifacts);
	}

	public PluginRuntimeDiscoveryResult discoverStatus() throws IOException
	{
		List<PluginArtifact> artifacts = discover();
		Map<String, Integer> idCounts = new HashMap<>();
		for (PluginArtifact artifact : artifacts)
		{
			idCounts.merge(artifact.getId(), 1, Integer::sum);
		}

		List<PluginRuntimeArtifactStatus> statuses = new ArrayList<>(artifacts.size());
		for (PluginArtifact artifact : artifacts)
		{
			List<String> errors = new ArrayList<>(validator.validate(artifact).getErrors());
			if (idCounts.getOrDefault(artifact.getId(), 0) > 1)
			{
				errors.add(DUPLICATE_ID_ERROR_PREFIX + artifact.getId());
			}
			statuses.add(new PluginRuntimeArtifactStatus(artifact, errors));
		}
		return new PluginRuntimeDiscoveryResult(statuses);
	}
}
