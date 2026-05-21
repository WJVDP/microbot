/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PluginRuntimeTest
{
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void discoversArtifactsFromRepositoriesInOrder() throws Exception
	{
		PluginArtifact core = PluginArtifact.builder(PluginArtifactSource.CORE, "core").build();
		PluginArtifact local = PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, "local").build();

		PluginRuntime runtime = new PluginRuntime(Arrays.asList(
			new StaticRepository(PluginArtifactSource.CORE, Collections.singletonList(core)),
			new StaticRepository(PluginArtifactSource.LOCAL_DIRECTORY, Collections.singletonList(local))));

		List<PluginArtifact> artifacts = runtime.discover();

		assertEquals(Arrays.asList(core, local), artifacts);
	}

	@Test
	public void discoveryResultIsImmutable() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "example").build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))));

		List<PluginArtifact> artifacts = runtime.discover();

		try
		{
			artifacts.add(artifact);
		}
		catch (UnsupportedOperationException expected)
		{
			return;
		}

		throw new AssertionError("discovered artifacts should be immutable");
	}

	@Test
	public void repositoriesAreImmutable()
	{
		PluginRepository repository = new StaticRepository(PluginArtifactSource.RUNELITE_HUB, Collections.emptyList());
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(repository));

		assertEquals(Collections.singletonList(repository), runtime.getRepositories());
		try
		{
			runtime.getRepositories().clear();
		}
		catch (UnsupportedOperationException expected)
		{
			return;
		}

		throw new AssertionError("repositories should be immutable");
	}

	@Test
	public void discoveryStatusReportsValidationErrors() throws Exception
	{
		PluginArtifact disabled = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "disabled")
			.entryClasses("disabled.Plugin")
			.disabled(true)
			.build();
		PluginArtifact incompatible = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "incompatible")
			.entryClasses("incompatible.Plugin")
			.minClientVersion("999.0.0")
			.build();
		PluginArtifact missingEntryClasses = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "missing-entry-classes")
			.build();

		PluginRuntime runtime = new PluginRuntime(
			Collections.singletonList(new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Arrays.asList(disabled, incompatible, missingEntryClasses))),
			new PluginArtifactValidator("1.0.0"::equals));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertEquals(3, result.getArtifacts().size());
		assertEquals(Collections.singletonList(PluginArtifactValidator.DISABLED_ERROR), result.getArtifacts().get(0).getErrors());
		assertEquals(Collections.singletonList(PluginArtifactValidator.CLIENT_VERSION_ERROR_PREFIX + "999.0.0"), result.getArtifacts().get(1).getErrors());
		assertEquals(Collections.singletonList(PluginArtifactValidator.MISSING_ENTRY_CLASSES_ERROR), result.getArtifacts().get(2).getErrors());
	}

	@Test
	public void discoveryStatusReportsDuplicateIdsAcrossRepositories() throws Exception
	{
		PluginArtifact first = PluginArtifact.builder(PluginArtifactSource.RUNELITE_HUB, "duplicate")
			.entryClasses("first.Plugin")
			.build();
		PluginArtifact second = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "duplicate")
			.entryClasses("second.Plugin")
			.build();

		PluginRuntime runtime = new PluginRuntime(Arrays.asList(
			new StaticRepository(PluginArtifactSource.RUNELITE_HUB, Collections.singletonList(first)),
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(second))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertEquals(2, result.getArtifacts().size());
		assertEquals(Collections.singletonList(PluginRuntime.DUPLICATE_ID_ERROR_PREFIX + "duplicate"), result.getArtifacts().get(0).getErrors());
		assertEquals(Collections.singletonList(PluginRuntime.DUPLICATE_ID_ERROR_PREFIX + "duplicate"), result.getArtifacts().get(1).getErrors());
	}

	@Test
	public void discoveryStatusAllowsLoadableArtifacts() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, "local")
			.entryClasses("local.Plugin")
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.LOCAL_DIRECTORY, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertFalse(result.hasErrors());
		assertTrue(result.getArtifacts().get(0).isLoadable());
		assertEquals(Collections.emptyList(), result.getArtifacts().get(0).getErrors());
	}

	@Test
	public void discoveryStatusAllowsValidChecksum() throws Exception
	{
		File jar = temporaryFolder.newFile("valid.jar");
		writeFile(jar, "valid");
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "valid")
			.artifactFile(jar)
			.checksumSha256(sha256(jar))
			.entryClasses("valid.Plugin")
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertFalse(result.hasErrors());
		assertTrue(result.getArtifacts().get(0).isLoadable());
	}

	@Test
	public void discoveryStatusBlocksChecksumMismatch() throws Exception
	{
		File jar = temporaryFolder.newFile("mismatch.jar");
		writeFile(jar, "actual");
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "mismatch")
			.artifactFile(jar)
			.checksumSha256(sha256("expected"))
			.entryClasses("mismatch.Plugin")
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertFalse(result.getArtifacts().get(0).isLoadable());
		assertTrue(result.getArtifacts().get(0).getErrors().get(0).startsWith(PluginArtifactVerifier.CHECKSUM_MISMATCH_ERROR_PREFIX));
	}

	@Test
	public void discoveryStatusBlocksMissingArtifactFileWhenChecksumIsDeclared() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "missing")
			.checksumSha256(sha256("expected"))
			.entryClasses("missing.Plugin")
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertEquals(Collections.singletonList(PluginArtifactVerifier.MISSING_ARTIFACT_FILE_ERROR), result.getArtifacts().get(0).getErrors());
	}

	@Test
	public void discoveryStatusBlocksMalformedJarManifest() throws Exception
	{
		File directory = temporaryFolder.newFolder();
		File jar = new File(directory, "malformed.jar");
		writeJarStub(jar, "{not-json");
		LocalDirectoryPluginRepository repository = new LocalDirectoryPluginRepository(directory);
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(repository));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertTrue(result.getArtifacts().get(0).getErrors().contains(PluginArtifactVerifier.MALFORMED_MANIFEST_ERROR));
	}

	@Test
	public void discoveryStatusRunsSignatureVerifierExtension() throws Exception
	{
		File jar = temporaryFolder.newFile("signed.jar");
		writeFile(jar, "signed");
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "signed")
			.artifactFile(jar)
			.signature("signature")
			.entryClasses("signed.Plugin")
			.build();
		PluginRuntime runtime = new PluginRuntime(
			Collections.singletonList(new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))),
			new PluginArtifactValidator(version -> true),
			new PluginArtifactVerifier((signedArtifact, artifactFile) -> false));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertEquals(Collections.singletonList(PluginArtifactVerifier.SIGNATURE_INVALID_ERROR), result.getArtifacts().get(0).getErrors());
	}

	private static void writeFile(File file, String content) throws Exception
	{
		try (FileOutputStream outputStream = new FileOutputStream(file))
		{
			outputStream.write(content.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static void writeJarStub(File jar, String stubJson) throws Exception
	{
		try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(jar)))
		{
			outputStream.putNextEntry(new JarEntry(PluginJarStubReader.STUB_PATH));
			outputStream.write(stubJson.getBytes(StandardCharsets.UTF_8));
			outputStream.closeEntry();
		}
	}

	private static String sha256(File file) throws Exception
	{
		return sha256(new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
	}

	private static String sha256(String content) throws Exception
	{
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
		StringBuilder builder = new StringBuilder(hash.length * 2);
		for (byte value : hash)
		{
			builder.append(String.format("%02x", value));
		}
		return builder.toString();
	}

	private static final class StaticRepository implements PluginRepository
	{
		private final PluginArtifactSource source;
		private final List<PluginArtifact> artifacts;

		private StaticRepository(PluginArtifactSource source, List<PluginArtifact> artifacts)
		{
			this.source = source;
			this.artifacts = artifacts;
		}

		@Override
		public PluginArtifactSource getSource()
		{
			return source;
		}

		@Override
		public List<PluginArtifact> discover() throws IOException
		{
			return artifacts;
		}
	}
}
