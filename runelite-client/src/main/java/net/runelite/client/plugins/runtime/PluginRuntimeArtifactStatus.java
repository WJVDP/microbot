/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PluginRuntimeArtifactStatus implements PluginArtifactStatusProjection
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

	@Override
	public Map<String, Object> toBridgeV1ArtifactDto()
	{
		Map<String, Object> dto = new LinkedHashMap<>();
		dto.put("id", artifact.getId());
		dto.put("displayName", artifact.getDisplayName());
		dto.put("version", artifact.getVersion());
		dto.put("source", artifact.getSource().name());
		dto.put("metadataSource", artifact.getMetadataSource().name());
		dto.put("entryClasses", artifact.getEntryClasses());
		dto.put("minClientVersion", artifact.getMinClientVersion());
		dto.put("checksumSha256", artifact.getChecksumSha256());
		dto.put("signature", artifact.getSignature());
		dto.put("installed", artifact.getArtifactFile() != null);
		dto.put("loadable", isLoadable());
		dto.put("warnings", warnings);
		dto.put("pluginApiVersion", pluginApiVersion);
		dto.put("clientPluginApiVersion", clientPluginApiVersion);
		dto.put("compatibilityPolicyAction", compatibilityPolicyAction);
		dto.put("compatibilityReasonCode", compatibilityReasonCode);
		dto.put("compatibilityReason", compatibilityReason);
		dto.put("signatureClassification", signatureClassification == null ? null : signatureClassification.name());
		dto.put("signaturePolicyAction", signaturePolicyAction);
		dto.put("signatureReasonCode", signatureReasonCode);
		dto.put("signatureReason", signatureReason);
		dto.put("capability_state", capabilityState.name().toLowerCase(Locale.ROOT));
		dto.put("capabilities", capabilities);
		dto.put("restricted_capabilities", restrictedCapabilities);
		dto.put("capability_policy_action", capabilityPolicyAction);
		dto.put("capability_reason", capabilityReasonCode);
		dto.put("capability_reason_message", capabilityReason);
		dto.put("errors", errors);
		return dto;
	}
}
