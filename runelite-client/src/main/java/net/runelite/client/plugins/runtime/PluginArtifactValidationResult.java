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
		null,
		null,
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
	private final Integer pluginApiVersion;
	private final Integer clientPluginApiVersion;
	private final String compatibilityPolicyAction;
	private final String compatibilityReasonCode;
	private final String compatibilityReason;

	private PluginArtifactValidationResult(
		List<String> errors,
		List<String> warnings,
		PluginArtifactSignatureClassification signatureClassification,
		String signaturePolicyAction,
		String signatureReasonCode,
		String signatureReason,
		Integer pluginApiVersion,
		Integer clientPluginApiVersion,
		String compatibilityPolicyAction,
		String compatibilityReasonCode,
		String compatibilityReason)
	{
		this.errors = Collections.unmodifiableList(errors);
		this.warnings = Collections.unmodifiableList(warnings);
		this.signatureClassification = signatureClassification;
		this.signaturePolicyAction = signaturePolicyAction;
		this.signatureReasonCode = signatureReasonCode;
		this.signatureReason = signatureReason;
		this.pluginApiVersion = pluginApiVersion;
		this.clientPluginApiVersion = clientPluginApiVersion;
		this.compatibilityPolicyAction = compatibilityPolicyAction;
		this.compatibilityReasonCode = compatibilityReasonCode;
		this.compatibilityReason = compatibilityReason;
	}

	public static PluginArtifactValidationResult valid()
	{
		return VALID;
	}

	public static PluginArtifactValidationResult invalid(List<String> errors)
	{
		return new PluginArtifactValidationResult(errors, Collections.emptyList(), null, null, null, null, null, null, null, null, null);
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
			signatureReason,
			null,
			null,
			null,
			null,
			null);
	}

	public static PluginArtifactValidationResult compatibility(
		List<String> errors,
		List<String> warnings,
		Integer pluginApiVersion,
		Integer clientPluginApiVersion,
		String compatibilityPolicyAction,
		String compatibilityReasonCode,
		String compatibilityReason)
	{
		return new PluginArtifactValidationResult(
			errors,
			warnings,
			null,
			null,
			null,
			null,
			pluginApiVersion,
			clientPluginApiVersion,
			compatibilityPolicyAction,
			compatibilityReasonCode,
			compatibilityReason);
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

	public Integer getPluginApiVersion()
	{
		return pluginApiVersion;
	}

	public Integer getClientPluginApiVersion()
	{
		return clientPluginApiVersion;
	}

	public String getCompatibilityPolicyAction()
	{
		return compatibilityPolicyAction;
	}

	public String getCompatibilityReasonCode()
	{
		return compatibilityReasonCode;
	}

	public String getCompatibilityReason()
	{
		return compatibilityReason;
	}
}
