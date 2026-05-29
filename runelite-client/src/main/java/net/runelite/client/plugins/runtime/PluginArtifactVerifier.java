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
	public static final String UNSIGNED_BLOCKED_REASON = "Unsigned plugin is not allowed from this source.";
	public static final String UNSIGNED_LOCAL_REASON = "Unsigned local plugin. Allowed for development.";
	public static final String TRUSTED_RUNELITE_HUB_REASON = "Loaded through RuneLite Hub trust path.";
	public static final String TRUSTED_MICROBOT_REASON = "Verified Microbot signature.";
	public static final String UNKNOWN_SIGNER_REASON = "Plugin was signed by an untrusted signer.";
	public static final String INVALID_SIGNATURE_REASON = "Plugin signature did not match the artifact.";
	public static final String MALFORMED_SIGNATURE_REASON = "Plugin signature metadata could not be read.";
	public static final String DEV_OVERRIDE_REASON = "Loaded by explicit local developer override.";

	private final PluginArtifactSignatureVerifier signatureVerifier;
	private final boolean allowLocalDeveloperSignatureOverride;

	public PluginArtifactVerifier()
	{
		this(PluginArtifactSignatureVerifier.ACCEPT_UNSIGNED);
	}

	public PluginArtifactVerifier(PluginArtifactSignatureVerifier signatureVerifier)
	{
		this(signatureVerifier, false);
	}

	public PluginArtifactVerifier(PluginArtifactSignatureVerifier signatureVerifier, boolean allowLocalDeveloperSignatureOverride)
	{
		this.signatureVerifier = signatureVerifier;
		this.allowLocalDeveloperSignatureOverride = allowLocalDeveloperSignatureOverride;
	}

	public PluginArtifactValidationResult verify(PluginArtifact artifact)
	{
		long start = System.nanoTime();
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		PluginArtifactSignatureClassification signatureClassification = null;
		String signaturePolicyAction = null;
		String signatureReasonCode = null;
		String signatureReason = null;
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
			}

			if (artifactFile != null && !artifactFile.isFile())
			{
				errors.add(MISSING_ARTIFACT_FILE_ERROR);
			}

			if (artifactFile != null && artifact.getChecksumSha256() != null)
			{
				verifyChecksum(artifact, artifactFile, errors);
			}

			if (artifactFile != null && artifact.getSignature() != null)
			{
				SignatureStatus signatureStatus = verifySignature(artifact, artifactFile, errors);
				signatureClassification = signatureStatus.classification;
				signaturePolicyAction = signatureStatus.policyAction;
				signatureReasonCode = signatureStatus.reasonCode;
				signatureReason = signatureStatus.reason;
				if (signatureStatus.warning)
				{
					warnings.add(signatureReason);
				}
			}
			else if (artifact.getSignature() == null && artifact.getSource() == PluginArtifactSource.MICROBOT_HUB)
			{
				signatureClassification = PluginArtifactSignatureClassification.UNSIGNED_BLOCKED;
				signaturePolicyAction = "block";
				signatureReasonCode = "unsigned_blocked";
				signatureReason = UNSIGNED_BLOCKED_REASON;
				errors.add(signatureReason);
			}
			else if (artifact.getSignature() == null && artifact.getSource() == PluginArtifactSource.LOCAL_DIRECTORY)
			{
				signatureClassification = PluginArtifactSignatureClassification.UNSIGNED_LOCAL;
				signaturePolicyAction = "warn";
				signatureReasonCode = "unsigned_local";
				signatureReason = UNSIGNED_LOCAL_REASON;
				warnings.add(signatureReason);
			}
			else if (artifact.getSource() == PluginArtifactSource.RUNELITE_HUB)
			{
				signatureClassification = PluginArtifactSignatureClassification.TRUSTED_RUNELITE_HUB;
				signaturePolicyAction = "allow";
				signatureReasonCode = "trusted_runelite_hub";
				signatureReason = TRUSTED_RUNELITE_HUB_REASON;
			}

			if (signatureClassification != null)
			{
				return PluginArtifactValidationResult.signature(
					errors,
					warnings,
					signatureClassification,
					signaturePolicyAction,
					signatureReasonCode,
					signatureReason);
			}

			return errors.isEmpty() && warnings.isEmpty()
				? PluginArtifactValidationResult.valid()
				: PluginArtifactValidationResult.signature(errors, warnings, null, null, null, null);
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

	private SignatureStatus verifySignature(PluginArtifact artifact, File artifactFile, List<String> errors)
	{
		try
		{
			PluginArtifactSignatureVerification verification = signatureVerifier.verify(artifact, artifactFile);
			if (verification == PluginArtifactSignatureVerification.TRUSTED)
			{
				if (artifact.getSource() == PluginArtifactSource.RUNELITE_HUB)
				{
					return SignatureStatus.allow(
						PluginArtifactSignatureClassification.TRUSTED_RUNELITE_HUB,
						"trusted_runelite_hub",
						TRUSTED_RUNELITE_HUB_REASON);
				}
				return SignatureStatus.allow(
					PluginArtifactSignatureClassification.TRUSTED_MICROBOT,
					"trusted_microbot",
					TRUSTED_MICROBOT_REASON);
			}
			SignatureStatus blocked = blockedSignatureStatus(artifact, verification);
			if (blocked.warning)
			{
				return blocked;
			}
			errors.add(blocked.reason);
			return blocked;
		}
		catch (IOException ex)
		{
			errors.add(SIGNATURE_READ_ERROR_PREFIX + ex.getMessage());
			return SignatureStatus.block(
				PluginArtifactSignatureClassification.INVALID_SIGNATURE,
				"invalid_signature",
				INVALID_SIGNATURE_REASON);
		}
	}

	private SignatureStatus blockedSignatureStatus(PluginArtifact artifact, PluginArtifactSignatureVerification verification)
	{
		if (allowLocalDeveloperSignatureOverride && artifact.getSource() == PluginArtifactSource.LOCAL_DIRECTORY)
		{
			return SignatureStatus.warn(blockedSignatureClassification(verification), "dev_override", DEV_OVERRIDE_REASON);
		}
		return SignatureStatus.block(blockedSignatureClassification(verification), blockedReasonCode(verification), blockedReason(verification));
	}

	private static PluginArtifactSignatureClassification blockedSignatureClassification(PluginArtifactSignatureVerification verification)
	{
		if (verification == PluginArtifactSignatureVerification.UNKNOWN_SIGNER)
		{
			return PluginArtifactSignatureClassification.UNKNOWN_SIGNER;
		}
		if (verification == PluginArtifactSignatureVerification.MALFORMED)
		{
			return PluginArtifactSignatureClassification.MALFORMED_SIGNATURE;
		}
		return PluginArtifactSignatureClassification.INVALID_SIGNATURE;
	}

	private static String blockedReasonCode(PluginArtifactSignatureVerification verification)
	{
		if (verification == PluginArtifactSignatureVerification.UNKNOWN_SIGNER)
		{
			return "unknown_signer";
		}
		if (verification == PluginArtifactSignatureVerification.MALFORMED)
		{
			return "malformed_signature";
		}
		return "invalid_signature";
	}

	private static String blockedReason(PluginArtifactSignatureVerification verification)
	{
		if (verification == PluginArtifactSignatureVerification.UNKNOWN_SIGNER)
		{
			return UNKNOWN_SIGNER_REASON;
		}
		if (verification == PluginArtifactSignatureVerification.MALFORMED)
		{
			return MALFORMED_SIGNATURE_REASON;
		}
		return INVALID_SIGNATURE_REASON;
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

	private static final class SignatureStatus
	{
		private final PluginArtifactSignatureClassification classification;
		private final String policyAction;
		private final String reasonCode;
		private final String reason;
		private final boolean warning;

		private SignatureStatus(
			PluginArtifactSignatureClassification classification,
			String policyAction,
			String reasonCode,
			String reason,
			boolean warning)
		{
			this.classification = classification;
			this.policyAction = policyAction;
			this.reasonCode = reasonCode;
			this.reason = reason;
			this.warning = warning;
		}

		private static SignatureStatus allow(
			PluginArtifactSignatureClassification classification,
			String reasonCode,
			String reason)
		{
			return new SignatureStatus(classification, "allow", reasonCode, reason, false);
		}

		private static SignatureStatus block(
			PluginArtifactSignatureClassification classification,
			String reasonCode,
			String reason)
		{
			return new SignatureStatus(classification, "block", reasonCode, reason, false);
		}

		private static SignatureStatus warn(
			PluginArtifactSignatureClassification classification,
			String reasonCode,
			String reason)
		{
			return new SignatureStatus(classification, "warn", reasonCode, reason, true);
		}
	}
}
