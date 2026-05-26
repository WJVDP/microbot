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
			.signature("test")
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "disabled"))
			.disabled(true)
			.build();
		PluginArtifact incompatible = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "incompatible")
			.entryClasses("incompatible.Plugin")
			.signature("test")
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "incompatible"))
			.minClientVersion("999.0.0")
			.build();
		PluginArtifact missingEntryClasses = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "missing-entry-classes")
			.signature("test")
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "missing-entry-classes"))
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
			.capabilityManifest(capabilities(PluginArtifactSource.RUNELITE_HUB, "duplicate"))
			.build();
		PluginArtifact second = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "duplicate")
			.entryClasses("second.Plugin")
			.signature("test")
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "duplicate"))
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
			.capabilityManifest(capabilities(PluginArtifactSource.LOCAL_DIRECTORY, "local"))
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
			.signature("test")
			.entryClasses("valid.Plugin")
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "valid"))
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
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "mismatch"))
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
		assertTrue(result.getArtifacts().get(0).getErrors().contains(PluginArtifactVerifier.MISSING_ARTIFACT_FILE_ERROR));
		assertTrue(result.getArtifacts().get(0).getErrors().contains("Unsigned plugin is not allowed from this source."));
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
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "signed"))
			.build();
		PluginRuntime runtime = new PluginRuntime(
			Collections.singletonList(new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))),
			new PluginArtifactValidator(version -> true),
			new PluginArtifactVerifier((signedArtifact, artifactFile) -> PluginArtifactSignatureVerification.INVALID));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();

		assertTrue(result.hasErrors());
		assertEquals(PluginArtifactSignatureClassification.INVALID_SIGNATURE, result.getArtifacts().get(0).getSignatureClassification());
		assertEquals("invalid_signature", result.getArtifacts().get(0).getSignatureReasonCode());
		assertEquals(Collections.singletonList("Plugin signature did not match the artifact."), result.getArtifacts().get(0).getErrors());
	}

	@Test
	public void discoveryStatusBlocksUnsignedMicrobotHubArtifacts() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "unsigned")
			.entryClasses("unsigned.Plugin")
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();
		PluginRuntimeArtifactStatus status = result.getArtifacts().get(0);

		assertTrue(result.hasErrors());
		assertFalse(status.isLoadable());
		assertEquals(PluginArtifactSignatureClassification.UNSIGNED_BLOCKED, status.getSignatureClassification());
		assertEquals("block", status.getSignaturePolicyAction());
		assertEquals("unsigned_blocked", status.getSignatureReasonCode());
		assertEquals("Unsigned plugin is not allowed from this source.", status.getSignatureReason());
		assertTrue(status.getErrors().contains("Unsigned plugin is not allowed from this source."));
	}

	@Test
	public void discoveryStatusAllowsUnsignedLocalArtifactsWithWarning() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, "local-unsigned")
			.entryClasses("local.Plugin")
			.capabilityManifest(capabilities(PluginArtifactSource.LOCAL_DIRECTORY, "local-unsigned"))
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.LOCAL_DIRECTORY, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();
		PluginRuntimeArtifactStatus status = result.getArtifacts().get(0);

		assertFalse(result.hasErrors());
		assertTrue(status.isLoadable());
		assertEquals(PluginArtifactSignatureClassification.UNSIGNED_LOCAL, status.getSignatureClassification());
		assertEquals("warn", status.getSignaturePolicyAction());
		assertEquals("unsigned_local", status.getSignatureReasonCode());
		assertEquals("Unsigned local plugin. Allowed for development.", status.getSignatureReason());
		assertEquals(Collections.singletonList("Unsigned local plugin. Allowed for development."), status.getWarnings());
		assertEquals(Collections.emptyList(), status.getErrors());
	}

	@Test
	public void discoveryStatusWarnsForLocalMissingCapabilityManifest() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, "local-missing-capabilities")
			.entryClasses("local.Plugin")
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.LOCAL_DIRECTORY, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();
		PluginRuntimeArtifactStatus status = result.getArtifacts().get(0);

		assertFalse(result.hasErrors());
		assertTrue(status.isLoadable());
		assertEquals(PluginCapabilityState.MISSING, status.getCapabilityState());
		assertEquals("warn", status.getCapabilityPolicyAction());
		assertEquals("capabilities_local_warning", status.getCapabilityReasonCode());
		assertEquals("Allowed because this is a local development plugin.", status.getCapabilityReason());
		assertTrue(status.getWarnings().contains("Allowed because this is a local development plugin."));
		assertEquals(Collections.emptyList(), status.getCapabilities());
		assertEquals(Collections.emptyList(), status.getRestrictedCapabilities());
	}

	@Test
	public void discoveryStatusBlocksHubMissingCapabilityManifest() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "hub-missing-capabilities")
			.entryClasses("hub.Plugin")
			.signature("test")
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();
		PluginRuntimeArtifactStatus status = result.getArtifacts().get(0);

		assertTrue(result.hasErrors());
		assertFalse(status.isLoadable());
		assertEquals(PluginCapabilityState.MISSING, status.getCapabilityState());
		assertEquals("block", status.getCapabilityPolicyAction());
		assertEquals("capabilities_blocked_for_source", status.getCapabilityReasonCode());
		assertEquals("This plugin source requires valid capability metadata.", status.getCapabilityReason());
		assertTrue(status.getErrors().contains("This plugin source requires valid capability metadata."));
	}

	@Test
	public void discoveryStatusReportsUnknownAndRestrictedCapabilities() throws Exception
	{
		PluginCapabilityManifest manifest = PluginCapabilityManifest.builder(
				"restricted", "Restricted", "1.0.0", PluginArtifactSource.MICROBOT_HUB)
			.capabilities(Arrays.asList("game_state.read", "credentials.access", "future.power"))
			.build();
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "restricted")
			.entryClasses("restricted.Plugin")
			.signature("test")
			.capabilityManifest(manifest)
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))));

		PluginRuntimeArtifactStatus status = runtime.discoverStatus().getArtifacts().get(0);

		assertEquals(PluginCapabilityState.UNKNOWN, status.getCapabilityState());
		assertEquals("block", status.getCapabilityPolicyAction());
		assertEquals("capabilities_unknown", status.getCapabilityReasonCode());
		assertEquals(Arrays.asList("game_state.read", "credentials.access", "future.power"), status.getCapabilities());
		assertEquals(Collections.singletonList("credentials.access"), status.getRestrictedCapabilities());
		assertTrue(status.getErrors().contains("Plugin declares capabilities this client does not recognize."));
	}

	@Test
	public void discoveryStatusReportsRuneLiteHubProvenance() throws Exception
	{
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.RUNELITE_HUB, "runelite")
			.entryClasses("runelite.Plugin")
			.capabilityManifest(capabilities(PluginArtifactSource.RUNELITE_HUB, "runelite"))
			.build();
		PluginRuntime runtime = new PluginRuntime(Collections.singletonList(
			new StaticRepository(PluginArtifactSource.RUNELITE_HUB, Collections.singletonList(artifact))));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();
		PluginRuntimeArtifactStatus status = result.getArtifacts().get(0);

		assertFalse(result.hasErrors());
		assertTrue(status.isLoadable());
		assertEquals(PluginArtifactSignatureClassification.TRUSTED_RUNELITE_HUB, status.getSignatureClassification());
		assertEquals("allow", status.getSignaturePolicyAction());
		assertEquals("trusted_runelite_hub", status.getSignatureReasonCode());
		assertEquals("Loaded through RuneLite Hub trust path.", status.getSignatureReason());
		assertEquals(Collections.emptyList(), status.getErrors());
	}

	@Test
	public void discoveryStatusAllowsTrustedMicrobotSignatures() throws Exception
	{
		File jar = temporaryFolder.newFile("trusted.jar");
		writeFile(jar, "trusted");
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "trusted")
			.artifactFile(jar)
			.signature("signature")
			.entryClasses("trusted.Plugin")
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "trusted"))
			.build();
		PluginRuntime runtime = new PluginRuntime(
			Collections.singletonList(new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))),
			new PluginArtifactValidator(version -> true),
			new PluginArtifactVerifier((signedArtifact, artifactFile) -> PluginArtifactSignatureVerification.TRUSTED));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();
		PluginRuntimeArtifactStatus status = result.getArtifacts().get(0);

		assertFalse(result.hasErrors());
		assertTrue(status.isLoadable());
		assertEquals(PluginArtifactSignatureClassification.TRUSTED_MICROBOT, status.getSignatureClassification());
		assertEquals("allow", status.getSignaturePolicyAction());
		assertEquals("trusted_microbot", status.getSignatureReasonCode());
		assertEquals("Verified Microbot signature.", status.getSignatureReason());
		assertEquals(Collections.emptyList(), status.getErrors());
	}

	@Test
	public void discoveryStatusBlocksUnknownSignerByDefault() throws Exception
	{
		File jar = temporaryFolder.newFile("unknown-signer.jar");
		writeFile(jar, "unknown");
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.MICROBOT_HUB, "unknown-signer")
			.artifactFile(jar)
			.signature("signature")
			.entryClasses("unknown.Plugin")
			.capabilityManifest(capabilities(PluginArtifactSource.MICROBOT_HUB, "unknown-signer"))
			.build();
		PluginRuntime runtime = new PluginRuntime(
			Collections.singletonList(new StaticRepository(PluginArtifactSource.MICROBOT_HUB, Collections.singletonList(artifact))),
			new PluginArtifactValidator(version -> true),
			new PluginArtifactVerifier((signedArtifact, artifactFile) -> PluginArtifactSignatureVerification.UNKNOWN_SIGNER));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();
		PluginRuntimeArtifactStatus status = result.getArtifacts().get(0);

		assertTrue(result.hasErrors());
		assertFalse(status.isLoadable());
		assertEquals(PluginArtifactSignatureClassification.UNKNOWN_SIGNER, status.getSignatureClassification());
		assertEquals("block", status.getSignaturePolicyAction());
		assertEquals("unknown_signer", status.getSignatureReasonCode());
		assertEquals("Plugin was signed by an untrusted signer.", status.getSignatureReason());
		assertTrue(status.getErrors().contains("Plugin was signed by an untrusted signer."));
	}

	@Test
	public void discoveryStatusAllowsLocalInvalidSignatureWithDeveloperOverride() throws Exception
	{
		File jar = temporaryFolder.newFile("local-invalid.jar");
		writeFile(jar, "local-invalid");
		PluginArtifact artifact = PluginArtifact.builder(PluginArtifactSource.LOCAL_DIRECTORY, "local-invalid")
			.artifactFile(jar)
			.signature("signature")
			.entryClasses("local.Plugin")
			.capabilityManifest(capabilities(PluginArtifactSource.LOCAL_DIRECTORY, "local-invalid"))
			.build();
		PluginRuntime runtime = new PluginRuntime(
			Collections.singletonList(new StaticRepository(PluginArtifactSource.LOCAL_DIRECTORY, Collections.singletonList(artifact))),
			new PluginArtifactValidator(version -> true),
			new PluginArtifactVerifier((signedArtifact, artifactFile) -> PluginArtifactSignatureVerification.INVALID, true));

		PluginRuntimeDiscoveryResult result = runtime.discoverStatus();
		PluginRuntimeArtifactStatus status = result.getArtifacts().get(0);

		assertFalse(result.hasErrors());
		assertTrue(status.isLoadable());
		assertEquals(PluginArtifactSignatureClassification.INVALID_SIGNATURE, status.getSignatureClassification());
		assertEquals("warn", status.getSignaturePolicyAction());
		assertEquals("dev_override", status.getSignatureReasonCode());
		assertEquals("Loaded by explicit local developer override.", status.getSignatureReason());
		assertEquals(Collections.singletonList("Loaded by explicit local developer override."), status.getWarnings());
		assertEquals(Collections.emptyList(), status.getErrors());
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

	private static PluginCapabilityManifest capabilities(PluginArtifactSource source, String id)
	{
		return PluginCapabilityManifest.builder(id, id, "1.0.0", source)
			.capabilities(Collections.singletonList("game_state.read"))
			.build();
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
