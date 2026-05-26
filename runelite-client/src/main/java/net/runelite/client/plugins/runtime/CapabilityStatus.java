/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 */
package net.runelite.client.plugins.runtime;

import java.util.List;

final class CapabilityStatus
{
	final PluginCapabilityState state;
	final List<String> capabilities;
	final List<String> restrictedCapabilities;
	final String policyAction;
	final String reasonCode;
	final String reason;
	final boolean warning;

	CapabilityStatus(
		PluginCapabilityState state,
		List<String> capabilities,
		List<String> restrictedCapabilities,
		String policyAction,
		String reasonCode,
		String reason,
		boolean warning)
	{
		this.state = state;
		this.capabilities = capabilities;
		this.restrictedCapabilities = restrictedCapabilities;
		this.policyAction = policyAction;
		this.reasonCode = reasonCode;
		this.reason = reason;
		this.warning = warning;
	}
}
