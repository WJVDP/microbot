/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

final class LegacyPluginDescriptorJarScanner
{
	private LegacyPluginDescriptorJarScanner()
	{
	}

	static List<String> scanEntryClassesOrEmpty(File jarFile)
	{
		try
		{
			return scanEntryClasses(jarFile);
		}
		catch (IOException | LinkageError | RuntimeException ex)
		{
			return Collections.emptyList();
		}
	}

	private static List<String> scanEntryClasses(File jarFile) throws IOException
	{
		List<String> classNames;
		try (JarFile jar = new JarFile(jarFile))
		{
			classNames = jar.stream()
				.map(JarEntry::getName)
				.filter(name -> name.endsWith(".class"))
				.map(LegacyPluginDescriptorJarScanner::toClassName)
				.collect(Collectors.toList());
		}

		List<String> plugins = new ArrayList<>();
		try (URLClassLoader classLoader = new URLClassLoader(
			new URL[]{jarFile.toURI().toURL()},
			LegacyPluginDescriptorJarScanner.class.getClassLoader()))
		{
			for (String className : classNames)
			{
				Class<?> clazz;
				try
				{
					clazz = Class.forName(className, false, classLoader);
				}
				catch (ClassNotFoundException | LinkageError ex)
				{
					continue;
				}

				if (Plugin.class.isAssignableFrom(clazz) && clazz.getAnnotation(PluginDescriptor.class) != null)
				{
					plugins.add(className);
				}
			}
		}
		return plugins;
	}

	private static String toClassName(String entryName)
	{
		return entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
	}
}
