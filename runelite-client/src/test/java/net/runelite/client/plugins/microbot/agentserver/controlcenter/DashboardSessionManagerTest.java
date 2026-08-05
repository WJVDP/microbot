package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class DashboardSessionManagerTest
{
	@Test
	public void bootstrapIsSingleUseAndCreatesExpiringCsrfSession()
	{
		AtomicLong now = new AtomicLong(1_000L);
		DashboardSessionManager manager = new DashboardSessionManager(new SecureRandom(), now::get);

		manager.issueBootstrap();
		String bootstrap = manager.claimBootstrapForBrowser();
		assertNotNull(bootstrap);
		assertNull(manager.claimBootstrapForBrowser());

		DashboardSessionManager.SessionGrant session = manager.exchangeBootstrap(bootstrap);
		assertNotNull(session);
		assertNull(manager.exchangeBootstrap(bootstrap));
		assertTrue(manager.authenticate(session.getSessionId()));
		assertTrue(manager.authenticateMutation(session.getSessionId(), session.getCsrfToken()));
		assertFalse(manager.authenticateMutation(session.getSessionId(), "wrong"));

		now.set(1_000L + DashboardSessionManager.SESSION_TTL_MS + 1);
		assertFalse(manager.authenticate(session.getSessionId()));
		assertFalse(manager.hasLiveSession());
	}

	@Test
	public void expiredBootstrapCannotBeClaimed()
	{
		AtomicLong now = new AtomicLong(10L);
		DashboardSessionManager manager = new DashboardSessionManager(new SecureRandom(), now::get);
		manager.issueBootstrap();
		now.addAndGet(DashboardSessionManager.BOOTSTRAP_TTL_MS + 1);
		assertNull(manager.claimBootstrapForBrowser());
	}
}
