/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.Collections;
import java.util.List;

public final class PluginRuntimeArtifactStatus
{
	private final PluginArtifact artifact;
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
	private final PluginCapabilityState capabilityState;
	private final List<String> capabilities;
	private final List<String> restrictedCapabilities;
	private final String capabilityPolicyAction;
	private final String capabilityReasonCode;
	private final String capabilityReason;

	PluginRuntimeArtifactStatus(PluginArtifact artifact, List<String> errors)
	{
		this(artifact, errors, Collections.emptyList(), null, null, null, null, null, null, null, null, null,
			PluginCapabilityState.MISSING, Collections.emptyList(), Collections.emptyList(), null, null, null);
	}

	PluginRuntimeArtifactStatus(
		PluginArtifact artifact,
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
		String compatibilityReason,
		PluginCapabilityState capabilityState,
		List<String> capabilities,
		List<String> restrictedCapabilities,
		String capabilityPolicyAction,
		String capabilityReasonCode,
		String capabilityReason)
	{
		this.artifact = artifact;
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
		this.capabilityState = capabilityState;
		this.capabilities = Collections.unmodifiableList(capabilities);
		this.restrictedCapabilities = Collections.unmodifiableList(restrictedCapabilities);
		this.capabilityPolicyAction = capabilityPolicyAction;
		this.capabilityReasonCode = capabilityReasonCode;
		this.capabilityReason = capabilityReason;
	}

	public PluginArtifact getArtifact()
	{
		return artifact;
	}

	public boolean isLoadable()
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

	public PluginCapabilityState getCapabilityState()
	{
		return capabilityState;
	}

	public List<String> getCapabilities()
	{
		return capabilities;
	}

	public List<String> getRestrictedCapabilities()
	{
		return restrictedCapabilities;
	}

	public String getCapabilityPolicyAction()
	{
		return capabilityPolicyAction;
	}

	public String getCapabilityReasonCode()
	{
		return capabilityReasonCode;
	}

	public String getCapabilityReason()
	{
		return capabilityReason;
	}
}
