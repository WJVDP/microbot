/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
	public void discoversLocalDirectoryArtifactsWithoutScanningJarContents() throws Exception
	{
		File directory = temporaryFolder.newFolder();
		File jar = new File(directory, "local-plugin.jar");
		assertTrue(jar.createNewFile());
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
	public void validatesDisabledAndClientVersionChecks()
	{
		PluginArtifact disabled = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "disabled")
			.disabled(true)
			.build();
		PluginArtifact incompatible = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "incompatible")
			.minClientVersion("999.0.0")
			.build();
		PluginArtifact compatible = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "compatible")
			.minClientVersion("1.0.0")
			.build();

		PluginArtifactValidator validator = new PluginArtifactValidator("1.0.0"::equals);

		assertFalse(validator.validate(disabled).isValid());
		assertFalse(validator.validate(incompatible).isValid());
		assertTrue(validator.validate(compatible).isValid());
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
}
