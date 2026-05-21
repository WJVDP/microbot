/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import net.runelite.client.externalplugins.PluginHubManifest;

final class PluginJarStubReader
{
	static final String STUB_PATH = "runelite_plugin.json";

	private static final Gson GSON = new Gson();

	private PluginJarStubReader()
	{
	}

	static List<String> readEntryClasses(File jarFile) throws IOException
	{
		try (JarFile jar = new JarFile(jarFile))
		{
			JarEntry entry = jar.getJarEntry(STUB_PATH);
			if (entry == null)
			{
				return Collections.emptyList();
			}

			try (InputStream inputStream = jar.getInputStream(entry);
				InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8))
			{
				PluginHubManifest.Stub stub = GSON.fromJson(reader, PluginHubManifest.Stub.class);
				if (stub == null || stub.getPlugins() == null)
				{
					return Collections.emptyList();
				}

				return Arrays.stream(stub.getPlugins())
					.filter(Objects::nonNull)
					.map(String::trim)
					.filter(plugin -> !plugin.isEmpty())
					.collect(Collectors.toList());
			}
		}
	}

	static List<String> readEntryClassesOrEmpty(File jarFile)
	{
		try
		{
			return readEntryClasses(jarFile);
		}
		catch (IOException ex)
		{
			return Collections.emptyList();
		}
		catch (JsonParseException ex)
		{
			return Collections.emptyList();
		}
	}

	static boolean hasMalformedManifest(File jarFile)
	{
		try
		{
			readEntryClasses(jarFile);
			return false;
		}
		catch (JsonParseException ex)
		{
			return true;
		}
		catch (IOException ex)
		{
			return false;
		}
	}
}
