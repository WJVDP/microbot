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
import net.runelite.client.plugins.health.PluginHealthRegistry;
import net.runelite.client.plugins.health.StartupTimingRegistry;

public class PluginRuntime
{
	public static final String DUPLICATE_ID_ERROR_PREFIX = "Duplicate plugin artifact id: ";

	private final List<PluginRepository> repositories;
	private final PluginArtifactValidator validator;
	private final PluginArtifactVerifier verifier;
	private final PluginHealthRegistry pluginHealthRegistry;
	private final StartupTimingRegistry startupTimingRegistry;

	public PluginRuntime(Collection<PluginRepository> repositories)
	{
		this(repositories, new PluginArtifactValidator(version -> true));
	}

	public PluginRuntime(Collection<PluginRepository> repositories, PluginArtifactValidator validator)
	{
		this(repositories, validator, new PluginArtifactVerifier());
	}

	public PluginRuntime(Collection<PluginRepository> repositories, PluginArtifactValidator validator, PluginArtifactVerifier verifier)
	{
		this(repositories, validator, verifier, StartupTimingRegistry.getDefault());
	}

	public PluginRuntime(
		Collection<PluginRepository> repositories,
		PluginArtifactValidator validator,
		PluginArtifactVerifier verifier,
		StartupTimingRegistry startupTimingRegistry)
	{
		this(repositories, validator, verifier, PluginHealthRegistry.getDefault(), startupTimingRegistry);
	}

	public PluginRuntime(
		Collection<PluginRepository> repositories,
		PluginArtifactValidator validator,
		PluginArtifactVerifier verifier,
		PluginHealthRegistry pluginHealthRegistry,
		StartupTimingRegistry startupTimingRegistry)
	{
		this.repositories = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(repositories, "repositories")));
		this.validator = Objects.requireNonNull(validator, "validator");
		this.verifier = Objects.requireNonNull(verifier, "verifier");
		this.pluginHealthRegistry = Objects.requireNonNull(pluginHealthRegistry, "pluginHealthRegistry");
		this.startupTimingRegistry = Objects.requireNonNull(startupTimingRegistry, "startupTimingRegistry");
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
			try
			{
				artifacts.addAll(startupTimingRegistry.time("plugin.discovery", repository.getSource().name(), repository::discover));
			}
			catch (IOException ex)
			{
				throw ex;
			}
			catch (Exception ex)
			{
				throw new IOException(ex);
			}
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
			List<String> errors = new ArrayList<>(verifier.verify(artifact).getErrors());
			errors.addAll(validator.validate(artifact).getErrors());
			if (idCounts.getOrDefault(artifact.getId(), 0) > 1)
			{
				errors.add(DUPLICATE_ID_ERROR_PREFIX + artifact.getId());
			}
			pluginHealthRegistry.setDisabledOrBlockedReason(artifact.getId(), errors.isEmpty() ? null : String.join("; ", errors));
			statuses.add(new PluginRuntimeArtifactStatus(artifact, errors));
		}
		return new PluginRuntimeDiscoveryResult(statuses);
	}
}
