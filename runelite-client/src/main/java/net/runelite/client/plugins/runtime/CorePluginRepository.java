/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

public class CorePluginRepository implements PluginRepository
{
	private final Collection<Class<? extends Plugin>> pluginClasses;

	public CorePluginRepository(Collection<Class<? extends Plugin>> pluginClasses)
	{
		this.pluginClasses = pluginClasses;
	}

	@Override
	public PluginArtifactSource getSource()
	{
		return PluginArtifactSource.CORE;
	}

	@Override
	public List<PluginArtifact> discover() throws IOException
	{
		return pluginClasses.stream()
			.map(CorePluginRepository::toArtifact)
			.collect(Collectors.toList());
	}

	private static PluginArtifact toArtifact(Class<? extends Plugin> pluginClass)
	{
		PluginDescriptor descriptor = pluginClass.getAnnotation(PluginDescriptor.class);
		String id = descriptor == null || descriptor.configName().isEmpty()
			? pluginClass.getName()
			: descriptor.configName();
		String displayName = descriptor == null ? pluginClass.getSimpleName() : descriptor.name();

		return PluginArtifact.builder(PluginArtifactSource.CORE, id)
			.displayName(displayName)
			.entryClasses(pluginClass.getName())
			.disabled(descriptor != null && descriptor.disable())
			.build();
	}
}
