/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import net.runelite.client.externalplugins.PluginHubManifest;
import net.runelite.client.plugins.microbot.externalplugins.MicrobotPluginManifest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PluginRepositoryTest
{
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void discoversRuneLiteHubArtifactsFromManifestOnly() throws Exception
	{
		PluginHubManifest.JarData jar = new PluginHubManifest.JarData();
		jar.setInternalName("example");
		jar.setDisplayName("Example Plugin");
		jar.setJarHash("hash");
		jar.setJarSize(123);

		PluginHubManifest.DisplayData display = new PluginHubManifest.DisplayData();
		display.setInternalName("example");
		display.setDisplayName("Example Display");
		display.setVersion("1.2.3");

		PluginHubManifest.ManifestFull manifest = new PluginHubManifest.ManifestFull();
		manifest.setJars(Collections.singletonList(jar));
		manifest.setDisplay(Collections.singletonList(display));

		RuneLiteHubPluginRepository repository = new RuneLiteHubPluginRepository(() -> manifest);

		List<PluginArtifact> artifacts = repository.discover();

		assertEquals(1, artifacts.size());
		PluginArtifact artifact = artifacts.get(0);
		assertEquals(PluginArtifactSource.RUNELITE_HUB, artifact.getSource());
		assertEquals("example", artifact.getId());
		assertEquals("Example Display", artifact.getDisplayName());
		assertEquals("1.2.3", artifact.getVersion());
		assertEquals("hash", artifact.getChecksumSha256());
		assertTrue(artifact.getEntryClasses().isEmpty());
	}

	@Test
	public void discoversMicrobotHubArtifactsFromManifestOnly() throws Exception
	{
		MicrobotPluginManifest manifest = new MicrobotPluginManifest();
		manifest.setInternalName("microbot-example");
		manifest.setDisplayName("Microbot Example");
		manifest.setVersion("2.0.0");
		manifest.setSha256("abc123");
		manifest.setMinClientVersion("1.10.0");

		MicrobotHubPluginRepository repository = new MicrobotHubPluginRepository(() -> Collections.singletonList(manifest));

		List<PluginArtifact> artifacts = repository.discover();

		assertEquals(1, artifacts.size());
		PluginArtifact artifact = artifacts.get(0);
		assertEquals(PluginArtifactSource.MICROBOT_HUB, artifact.getSource());
		assertEquals("microbot-example", artifact.getId());
		assertEquals("Microbot Example", artifact.getDisplayName());
		assertEquals("2.0.0", artifact.getVersion());
		assertEquals("abc123", artifact.getChecksumSha256());
		assertEquals("1.10.0", artifact.getMinClientVersion());
		assertTrue(artifact.getEntryClasses().isEmpty());
	}

	@Test
	public void discoversMicrobotHubEntryClassesFromInstalledJarStub() throws Exception
	{
		File directory = temporaryFolder.newFolder();
		File jar = new File(directory, "microbot-example.jar");
		writeJarStub(jar, "example.DoesNotExistPlugin", "example.OtherPlugin");

		MicrobotPluginManifest manifest = new MicrobotPluginManifest();
		manifest.setInternalName("microbot-example");
		manifest.setDisplayName("Microbot Example");
		manifest.setVersion("2.0.0");

		MicrobotHubPluginRepository repository = new MicrobotHubPluginRepository(
			() -> Collections.singletonList(manifest),
			directory);

		List<PluginArtifact> artifacts = repository.discover();

		assertEquals(1, artifacts.size());
		PluginArtifact artifact = artifacts.get(0);
		assertEquals(jar, artifact.getArtifactFile());
		assertEquals(Arrays.asList("example.DoesNotExistPlugin", "example.OtherPlugin"), artifact.getEntryClasses());
	}

	@Test
	public void allowsDuplicateMicrobotHubArtifactIdsForRuntimeStatus() throws Exception
	{
		MicrobotPluginManifest first = new MicrobotPluginManifest();
		first.setInternalName("duplicate");
		first.setDisplayName("First");

		MicrobotPluginManifest second = new MicrobotPluginManifest();
		second.setInternalName("duplicate");
		second.setDisplayName("Second");

		MicrobotHubPluginRepository repository = new MicrobotHubPluginRepository(() -> Arrays.asList(first, second));

		List<PluginArtifact> artifacts = repository.discover();

		assertEquals(2, artifacts.size());
		assertEquals("duplicate", artifacts.get(0).getId());
		assertEquals("duplicate", artifacts.get(1).getId());
	}

	@Test
	public void rejectsInvalidMicrobotHubArtifactMetadata() throws Exception
	{
		MicrobotPluginManifest manifest = new MicrobotPluginManifest();
		manifest.setInternalName(" ");
		manifest.setDisplayName("Invalid");

		MicrobotHubPluginRepository repository = new MicrobotHubPluginRepository(() -> Collections.singletonList(manifest));

		try
		{
			repository.discover();
		}
		catch (IllegalArgumentException ex)
		{
			assertEquals("id is required", ex.getMessage());
			return;
		}

		throw new AssertionError("invalid artifact metadata should be rejected");
	}

	@Test
	public void discoversLocalDirectoryArtifactsWithoutScanningJarContents() throws Exception
	{
		File directory = temporaryFolder.newFolder();
		File jar = new File(directory, "local-plugin.jar");
		writeEmptyJar(jar);
		assertTrue(new File(directory, "ignored.txt").createNewFile());

		LocalDirectoryPluginRepository repository = new LocalDirectoryPluginRepository(directory);

		List<PluginArtifact> artifacts = repository.discover();

		assertEquals(1, artifacts.size());
		PluginArtifact artifact = artifacts.get(0);
		assertEquals(PluginArtifactSource.LOCAL_DIRECTORY, artifact.getSource());
		assertEquals("local-plugin", artifact.getId());
		assertEquals(jar, artifact.getArtifactFile());
		assertTrue(artifact.getEntryClasses().isEmpty());
	}

	@Test
	public void discoversLocalDirectoryEntryClassesFromJarStub() throws Exception
	{
		File directory = temporaryFolder.newFolder();
		File jar = new File(directory, "local-plugin.jar");
		writeJarStub(jar, "local.DoesNotExistPlugin");

		LocalDirectoryPluginRepository repository = new LocalDirectoryPluginRepository(directory);

		List<PluginArtifact> artifacts = repository.discover();

		assertEquals(1, artifacts.size());
		PluginArtifact artifact = artifacts.get(0);
		assertEquals(PluginArtifactSource.LOCAL_DIRECTORY, artifact.getSource());
		assertEquals("local-plugin", artifact.getId());
		assertEquals(Collections.singletonList("local.DoesNotExistPlugin"), artifact.getEntryClasses());
	}

	@Test
	public void validatesDisabledClientVersionAndMissingEntryClasses()
	{
		PluginArtifact disabled = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "disabled")
			.entryClasses("disabled.Plugin")
			.disabled(true)
			.build();
		PluginArtifact incompatible = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "incompatible")
			.entryClasses("incompatible.Plugin")
			.minClientVersion("999.0.0")
			.build();
		PluginArtifact compatible = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "compatible")
			.entryClasses("compatible.Plugin")
			.minClientVersion("1.0.0")
			.build();
		PluginArtifact missingEntryClasses = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "missing-entry-classes")
			.build();

		PluginArtifactValidator validator = new PluginArtifactValidator("1.0.0"::equals);

		assertFalse(validator.validate(disabled).isValid());
		assertFalse(validator.validate(incompatible).isValid());
		assertTrue(validator.validate(compatible).isValid());
		assertFalse(validator.validate(missingEntryClasses).isValid());
	}

	@Test
	public void artifactDefensivelyCopiesEntryClasses()
	{
		List<String> entryClasses = Arrays.asList("a.Plugin", "b.Plugin");

		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.RUNELITE_HUB, "example")
			.entryClasses(entryClasses)
			.build();

		assertEquals(entryClasses, artifact.getEntryClasses());
		assertNull(artifact.getMinClientVersion());
		try
		{
			artifact.getEntryClasses().add("c.Plugin");
		}
		catch (UnsupportedOperationException expected)
		{
			return;
		}

		throw new AssertionError("entry classes should be immutable");
	}

	private static void writeEmptyJar(File jar) throws Exception
	{
		try (JarOutputStream ignored = new JarOutputStream(new FileOutputStream(jar)))
		{
		}
	}

	private static void writeJarStub(File jar, String... entryClasses) throws Exception
	{
		try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(jar)))
		{
			outputStream.putNextEntry(new JarEntry(PluginJarStubReader.STUB_PATH));
			outputStream.write(stubJson(entryClasses).getBytes(StandardCharsets.UTF_8));
			outputStream.closeEntry();
		}
	}

	private static String stubJson(String... entryClasses)
	{
		StringBuilder builder = new StringBuilder("{\"plugins\":[");
		for (int i = 0; i < entryClasses.length; i++)
		{
			if (i > 0)
			{
				builder.append(',');
			}
			builder.append('"').append(entryClasses[i]).append('"');
		}
		return builder.append("]}").toString();
	}
}
