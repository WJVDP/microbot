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
  artifactStatusAvailable: boolean;
  artifactCount?: number;
  artifactErrors?: boolean;
  artifactError?: string;
}

export interface BridgeError {
  error: string;
}
