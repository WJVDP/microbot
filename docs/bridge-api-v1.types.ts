export type BridgeVersion = "1";

export interface BridgeStatus {
  bridgeVersion: BridgeVersion;
  serverTime: string;
  runeliteVersion: string;
  microbotVersion: string;
  pluginManagerAvailable: boolean;
  pluginCount: number;
}

export interface BridgePlugin {
  id: string;
  displayName: string;
  className: string;
  enabled: boolean;
  active: boolean;
  hidden: boolean;
  external: boolean;
  description: string;
}

export interface BridgePluginList {
  count: number;
  plugins: BridgePlugin[];
}

export interface BridgePluginCommandResponse extends BridgePlugin {
  changed: boolean;
}

export interface BridgeConfigRange {
  min: number;
  max: number;
}

export interface BridgeConfigSection {
  key: string;
  name: string;
  description: string;
  position: number;
  closedByDefault: boolean;
}

export interface BridgeConfigItem {
  key: string;
  name: string;
  description: string;
  type: string;
  position: number;
  hidden: boolean;
  secret: boolean;
  section: string;
  warning: string;
  range?: BridgeConfigRange;
}

export interface BridgePluginConfigSchema {
  group: string;
  sections: BridgeConfigSection[];
  items: BridgeConfigItem[];
}

export interface BridgePluginConfigValues {
  group: string;
  values: Record<string, string | null>;
}

export interface BridgePluginConfigWriteResponse extends BridgePluginConfigValues {
  success: boolean;
  changed: string[];
}

export interface BridgePluginConfigWriteRequest {
  key?: string;
  value?: unknown;
  values?: Record<string, unknown>;
}

export interface BridgePluginArtifact {
  id: string;
  displayName: string | null;
  version: string | null;
  source: string;
  metadataSource: string;
  entryClasses: string[];
  minClientVersion: string | null;
  checksumSha256: string | null;
  signature: string | null;
  installed: boolean;
  loadable: boolean;
  warnings: string[];
  signatureClassification: "TRUSTED_MICROBOT" | "TRUSTED_RUNELITE_HUB" | "UNSIGNED_LOCAL" | "UNKNOWN_SIGNER" | "INVALID_SIGNATURE" | "MALFORMED_SIGNATURE" | "UNSIGNED_BLOCKED" | null;
  signaturePolicyAction: "allow" | "warn" | "block" | null;
  signatureReasonCode: "trusted_microbot" | "trusted_runelite_hub" | "unsigned_local" | "unsigned_blocked" | "unknown_signer" | "invalid_signature" | "malformed_signature" | "dev_override" | null;
  signatureReason: string | null;
  capability_state: "normal" | "missing" | "unknown" | "restricted";
  capabilities: string[];
  restricted_capabilities: string[];
  capability_policy_action: "allow" | "warn" | "block";
  capability_reason: "capabilities_ok" | "capabilities_missing" | "capabilities_unknown" | "capabilities_restricted" | "capabilities_local_warning" | "capabilities_blocked_for_source";
  capability_reason_message: string;
  errors: string[];
}

export interface BridgePluginArtifacts {
  count: number;
  hasErrors: boolean;
  artifacts: BridgePluginArtifact[];
}

export type BridgeArtifactAction = "install" | "update" | "remove";

export interface BridgeArtifactCommandRequest {
  version?: string;
}

export interface BridgeArtifactCommandResponse {
  commandId: string;
  action: BridgeArtifactAction;
  targetType: "pluginArtifact";
  id: string;
  accepted: boolean;
  status: "queued";
  version?: string | null;
}

export interface BridgeEvent {
  id: string;
  time: string;
  type: "plugin.install" | "plugin.update" | "plugin.remove" | "plugin.state" | "plugin.config" | string;
  level: "debug" | "info" | "warn" | "error" | string;
  source: "bridge-v1" | string;
  pluginId: string | null;
  action: string;
  status: string;
  message: string;
}

export interface BridgeEvents {
  count: number;
  events: BridgeEvent[];
}

export interface BridgeRuntimeHealth {
  serverTime: string;
  pluginManagerAvailable: boolean;
  configManagerAvailable: boolean;
  pluginCount: number;
  pluginHealth: BridgePluginHealthStatus;
  startupTiming: BridgeStartupTimingStatus;
  artifactStatusAvailable: boolean;
  artifactCount?: number;
  artifactErrors?: boolean;
  artifactError?: string;
}

export interface BridgePluginHealth {
  pluginId: string;
  exceptionCount: number;
  slowCallCount: number;
  totalCallCount: number;
  totalDurationMs: number;
  maxDurationMs: number;
  lastOperation: string | null;
  lastFailure: string | null;
  lastFailureStackTrace: string | null;
  lastFailureTime: string | null;
  disabledOrBlockedReason: string | null;
}

export interface BridgePluginHealthStatus {
  slowCallThresholdMs: number;
  count: number;
  plugins: BridgePluginHealth[];
}

export interface BridgeStartupTiming {
  time: string;
  stage: string;
  detail: string | null;
  durationMs: number;
  durationNanos: number;
}

export interface BridgeStartupTimingStatus {
  count: number;
  timings: BridgeStartupTiming[];
}

export interface BridgeError {
  error: string;
}
