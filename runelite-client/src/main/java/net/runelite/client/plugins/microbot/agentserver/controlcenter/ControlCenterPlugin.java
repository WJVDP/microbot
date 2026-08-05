package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicit opt-in marker for automation plugins that may be controlled from the
 * local dashboard. The id is part of the dashboard API and must remain stable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ControlCenterPlugin
{
	String id();

	/**
	 * Optional logger namespaces to capture. The plugin package is used when this
	 * is empty.
	 */
	String[] loggerPrefixes() default {};
}
