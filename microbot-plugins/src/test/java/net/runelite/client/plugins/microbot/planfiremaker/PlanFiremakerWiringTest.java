package net.runelite.client.plugins.microbot.planfiremaker;

import org.junit.Test;

import javax.inject.Singleton;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlanFiremakerWiringTest
{
    @Test
    public void sharesOneScriptInstanceBetweenPluginAndOverlay()
    {
        assertNotNull(PlanFiremakerScript.class.getAnnotation(Singleton.class));
    }

    @Test
    public void invalidatedBankEpochMakesForecastUnknownAgain()
    {
        assertTrue(PlanFiremakerScript.hasVerifiedBankSnapshot(1));
        assertFalse(PlanFiremakerScript.hasVerifiedBankSnapshot(0));
    }
}
