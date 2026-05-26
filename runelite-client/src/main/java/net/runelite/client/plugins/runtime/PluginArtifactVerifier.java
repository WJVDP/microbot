/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.runelite.client.plugins.health.StartupTimingRegistry;

public class PluginArtifactVerifier
{
	public static final String MISSING_ARTIFACT_FILE_ERROR = "Plugin artifact file is missing";
	public static final String CHECKSUM_MISMATCH_ERROR_PREFIX = "Plugin artifact checksum mismatch: expected ";
	public static final String CHECKSUM_READ_ERROR_PREFIX = "Plugin artifact checksum could not be read: ";
	public static final String SIGNATURE_INVALID_ERROR = "Plugin artifact signature is invalid";
	public static final String SIGNATURE_READ_ERROR_PREFIX = "Plugin artifact signature could not be verified: ";
	public static final String MALFORMED_MANIFEST_ERROR = "Plugin artifact manifest is malformed";

	private final PluginArtifactSignatureVerifier signatureVerifier;

	public PluginArtifactVerifier()
	{
		this(PluginArtifactSignatureVerifier.ACCEPT_UNSIGNED);
	}

	public PluginArtifactVerifier(PluginArtifactSignatureVerifier signatureVerifier)
	{
		this.signatureVerifier = signatureVerifier;
	}

	public PluginArtifactValidationResult verify(PluginArtifact artifact)
	{
		long start = System.nanoTime();
		List<String> errors = new ArrayList<>();
		try
		{
			if (artifact.hasMalformedManifest())
			{
				errors.add(MALFORMED_MANIFEST_ERROR);
			}

			File artifactFile = artifact.getArtifactFile();
			if (artifact.getChecksumSha256() != null && (artifactFile == null || !artifactFile.isFile()))
			{
				errors.add(MISSING_ARTIFACT_FILE_ERROR);
				return PluginArtifactValidationResult.invalid(errors);
			}

			if (artifactFile != null && !artifactFile.isFile())
			{
				errors.add(MISSING_ARTIFACT_FILE_ERROR);
				return PluginArtifactValidationResult.invalid(errors);
			}

			if (artifactFile != null && artifact.getChecksumSha256() != null)
			{
				verifyChecksum(artifact, artifactFile, errors);
			}

			if (artifactFile != null && artifact.getSignature() != null)
			{
				verifySignature(artifact, artifactFile, errors);
			}

			return errors.isEmpty()
				? PluginArtifactValidationResult.valid()
				: PluginArtifactValidationResult.invalid(errors);
		}
		finally
		{
			StartupTimingRegistry.getDefault().record("plugin.jar-verification", artifact.getId(), System.nanoTime() - start);
		}
	}

	private static void verifyChecksum(PluginArtifact artifact, File artifactFile, List<String> errors)
	{
		try
		{
			String actual = sha256(artifactFile);
			String expected = artifact.getChecksumSha256().toLowerCase(Locale.ROOT);
			if (!actual.equals(expected))
			{
				errors.add(CHECKSUM_MISMATCH_ERROR_PREFIX + expected + ", actual " + actual);
			}
		}
		catch (IOException ex)
		{
			errors.add(CHECKSUM_READ_ERROR_PREFIX + ex.getMessage());
		}
	}

	private void verifySignature(PluginArtifact artifact, File artifactFile, List<String> errors)
	{
		try
		{
			if (!signatureVerifier.verify(artifact, artifactFile))
			{
				errors.add(SIGNATURE_INVALID_ERROR);
			}
		}
		catch (IOException ex)
		{
			errors.add(SIGNATURE_READ_ERROR_PREFIX + ex.getMessage());
		}
	}

	private static String sha256(File file) throws IOException
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream inputStream = new DigestInputStream(Files.newInputStream(file.toPath()), digest))
			{
				byte[] buffer = new byte[8192];
				while (inputStream.read(buffer) != -1)
				{
					// DigestInputStream updates the digest as bytes are read.
				}
			}
			return toHex(digest.digest());
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	private static String toHex(byte[] bytes)
	{
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte value : bytes)
		{
			builder.append(String.format("%02x", value));
		}
		return builder.toString();
	}
}
