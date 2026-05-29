/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import net.runelite.client.externalplugins.PluginHubManifest;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
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
		assertEquals(PluginArtifactMetadataSource.HUB_MANIFEST, artifact.getMetadataSource());
		assertTrue(artifact.getEntryClasses().isEmpty());
	}

	@Test
	public void discoversRuneLiteHubEntryClassesFromInstalledJarStub() throws Exception
	{
		File directory = temporaryFolder.newFolder();
		File jar = new File(directory, "example.jar");
		writeJarStub(jar, "example.DoesNotExistPlugin");

		PluginHubManifest.JarData jarData = new PluginHubManifest.JarData();
		jarData.setInternalName("example");
		jarData.setDisplayName("Example Plugin");
		jarData.setJarHash(base64Sha256(jar));
		jarData.setJarSize((int) jar.length());
		assertTrue(jar.renameTo(new File(directory, jarData.getInternalName() + "_" + jarData.getJarHash() + ".jar")));

		PluginHubManifest.ManifestFull manifest = new PluginHubManifest.ManifestFull();
		manifest.setJars(Collections.singletonList(jarData));

		RuneLiteHubPluginRepository repository = new RuneLiteHubPluginRepository(() -> manifest, directory);

		List<PluginArtifact> artifacts = repository.discover();

		assertEquals(1, artifacts.size());
		PluginArtifact artifact = artifacts.get(0);
		assertEquals(PluginArtifactSource.RUNELITE_HUB, artifact.getSource());
		assertEquals(Collections.singletonList("example.DoesNotExistPlugin"), artifact.getEntryClasses());
		assertEquals(PluginArtifactMetadataSource.JAR_STUB, artifact.getMetadataSource());
		assertEquals(hexSha256(artifact.getArtifactFile()), artifact.getChecksumSha256());
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
		manifest.setPluginApiVersion("1");

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
		assertEquals("1", artifact.getPluginApiVersion());
		assertEquals(PluginArtifactMetadataSource.HUB_MANIFEST, artifact.getMetadataSource());
		assertTrue(artifact.getEntryClasses().isEmpty());
	}

	@Test
	public void microbotHubManifestPluginApiVersionIsAuthoritativeWhenJarMetadataExists() throws Exception
	{
		File directory = temporaryFolder.newFolder();
		File jar = new File(directory, "microbot-example.jar");
		writeJarClass(jar, FutureApiRuntimePlugin.class);

		MicrobotPluginManifest manifest = new MicrobotPluginManifest();
		manifest.setInternalName("microbot-example");
		manifest.setDisplayName("Microbot Example");
		manifest.setPluginApiVersion("1");

		MicrobotHubPluginRepository repository = new MicrobotHubPluginRepository(
			() -> Collections.singletonList(manifest),
			directory);

		PluginArtifact artifact = repository.discover().get(0);

		assertEquals("1", artifact.getPluginApiVersion());
		assertEquals(Collections.singletonList(FutureApiRuntimePlugin.class.getName()), artifact.getEntryClasses());
		assertEquals(PluginArtifactMetadataSource.LEGACY_PLUGIN_DESCRIPTOR_SCAN, artifact.getMetadataSource());
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
		assertEquals(PluginArtifactMetadataSource.JAR_STUB, artifact.getMetadataSource());
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
		assertEquals(PluginArtifactMetadataSource.FILE_NAME, artifact.getMetadataSource());
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
		assertEquals(PluginArtifactMetadataSource.JAR_STUB, artifact.getMetadataSource());
	}

	@Test
	public void discoversLocalDirectoryEntryClassesFromLegacyDescriptorScan() throws Exception
	{
		File directory = temporaryFolder.newFolder();
		File jar = new File(directory, "legacy-plugin.jar");
		writeJarClass(jar, LegacyRuntimePlugin.class);

		LocalDirectoryPluginRepository repository = new LocalDirectoryPluginRepository(directory);

		List<PluginArtifact> artifacts = repository.discover();

		assertEquals(1, artifacts.size());
		PluginArtifact artifact = artifacts.get(0);
		assertEquals("legacy-plugin", artifact.getId());
		assertEquals(Collections.singletonList(LegacyRuntimePlugin.class.getName()), artifact.getEntryClasses());
		assertEquals(PluginArtifactMetadataSource.LEGACY_PLUGIN_DESCRIPTOR_SCAN, artifact.getMetadataSource());
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

	private static void writeJarClass(File jar, Class<?> clazz) throws Exception
	{
		String classFile = clazz.getName().replace('.', '/') + ".class";
		try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(jar));
			InputStream inputStream = clazz.getClassLoader().getResourceAsStream(classFile))
		{
			assertTrue("Missing class resource " + classFile, inputStream != null);
			outputStream.putNextEntry(new JarEntry(classFile));
			byte[] buffer = new byte[8192];
			int read;
			while ((read = inputStream.read(buffer)) != -1)
			{
				outputStream.write(buffer, 0, read);
			}
			outputStream.closeEntry();
		}
	}

	private static String base64Sha256(File file) throws Exception
	{
		return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(file));
	}

	private static String hexSha256(File file) throws Exception
	{
		byte[] hash = sha256(file);
		StringBuilder builder = new StringBuilder(hash.length * 2);
		for (byte value : hash)
		{
			builder.append(String.format("%02x", value));
		}
		return builder.toString();
	}

	private static byte[] sha256(File file) throws Exception
	{
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return digest.digest(java.nio.file.Files.readAllBytes(file.toPath()));
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

	@PluginDescriptor(name = "Legacy Runtime Plugin")
	public static final class LegacyRuntimePlugin extends Plugin
	{
	}

	@PluginDescriptor(name = "Future API Runtime Plugin", pluginApiVersion = 2)
	public static final class FutureApiRuntimePlugin extends Plugin
	{
	}
}
