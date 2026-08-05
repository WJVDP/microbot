package net.runelite.client.plugins.microbot.agentserver.controlcenter;

/**
 * Supplies an already thread-safe, immutable extension to standard dashboard
 * status. Implementations must not read live RuneLite entities from this call.
 */
@FunctionalInterface
public interface ControlCenterStatusProvider
{
	ControlCenterStatusSnapshot snapshot();
}
