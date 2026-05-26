/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.Collections;
import java.util.List;

public final class PluginArtifactValidationResult
{
	private static final PluginArtifactValidationResult VALID = new PluginArtifactValidationResult(
		Collections.emptyList(),
		Collections.emptyList(),
		null,
		null,
		null,
		null);

	private final List<String> errors;
	private final List<String> warnings;
	private final PluginArtifactSignatureClassification signatureClassification;
	private final String signaturePolicyAction;
	private final String signatureReasonCode;
	private final String signatureReason;

	private PluginArtifactValidationResult(
		List<String> errors,
		List<String> warnings,
		PluginArtifactSignatureClassification signatureClassification,
		String signaturePolicyAction,
		String signatureReasonCode,
		String signatureReason)
	{
		this.errors = Collections.unmodifiableList(errors);
		this.warnings = Collections.unmodifiableList(warnings);
		this.signatureClassification = signatureClassification;
		this.signaturePolicyAction = signaturePolicyAction;
		this.signatureReasonCode = signatureReasonCode;
		this.signatureReason = signatureReason;
	}

	public static PluginArtifactValidationResult valid()
	{
		return VALID;
	}

	public static PluginArtifactValidationResult invalid(List<String> errors)
	{
		return new PluginArtifactValidationResult(errors, Collections.emptyList(), null, null, null, null);
	}

	public static PluginArtifactValidationResult signature(
		List<String> errors,
		List<String> warnings,
		PluginArtifactSignatureClassification signatureClassification,
		String signaturePolicyAction,
		String signatureReasonCode,
		String signatureReason)
	{
		return new PluginArtifactValidationResult(
			errors,
			warnings,
			signatureClassification,
			signaturePolicyAction,
			signatureReasonCode,
			signatureReason);
	}

	public boolean isValid()
	{
		return errors.isEmpty();
	}

	public List<String> getErrors()
	{
		return errors;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}

	public PluginArtifactSignatureClassification getSignatureClassification()
	{
		return signatureClassification;
	}

	public String getSignaturePolicyAction()
	{
		return signaturePolicyAction;
	}

	public String getSignatureReasonCode()
	{
		return signatureReasonCode;
	}

	public String getSignatureReason()
	{
		return signatureReason;
	}
}
