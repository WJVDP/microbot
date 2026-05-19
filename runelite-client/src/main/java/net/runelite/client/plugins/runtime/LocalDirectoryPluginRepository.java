/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LocalDirectoryPluginRepository implements PluginRepository
{
	private final File directory;

	public LocalDirectoryPluginRepository(File directory)
	{
		this.directory = directory;
	}

	@Override
	public PluginArtifactSource getSource()
	{
		return PluginArtifactSource.LOCAL_DIRECTORY;
	}

	@Override
	public List<PluginArtifact> discover() throws IOException
	{
		File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));
		if (files == null)
		{
			return Collections.emptyList();
		}

		return Arrays.stream(files)
			.map(LocalDirectoryPluginRepository::toArtifact)
			.collect(Collectors.toList());
	}

	private static PluginArtifact toArtifact(File file)
	{
		String name = file.getName();
		String id = name.endsWith(".jar") ? name.substring(0, name.length() - 4) : name;
		return PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, id)
			.displayName(id)
			.artifactFile(file)
			.build();
	}
}
