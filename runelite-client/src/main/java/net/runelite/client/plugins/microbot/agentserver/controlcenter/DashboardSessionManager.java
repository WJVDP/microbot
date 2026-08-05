package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/** In-memory one-time bootstrap and short-lived browser session storage. */
public final class DashboardSessionManager
{
	public static final long BOOTSTRAP_TTL_MS = 30_000L;
	public static final long SESSION_TTL_MS = 15 * 60_000L;

	private final SecureRandom random;
	private final LongSupplier clock;
	private final AtomicReference<ExpiringToken> pendingOpen = new AtomicReference<>();
	private final Map<String, Long> browserBootstraps = new ConcurrentHashMap<>();
	private final Map<String, Session> sessions = new ConcurrentHashMap<>();

	public DashboardSessionManager()
	{
		this(new SecureRandom(), System::currentTimeMillis);
	}

	DashboardSessionManager(SecureRandom random, LongSupplier clock)
	{
		this.random = random;
		this.clock = clock;
	}

	/** Creates a nonce that can be claimed by exactly one subsequent dashboard navigation. */
	public void issueBootstrap()
	{
		long now = clock.getAsLong();
		pendingOpen.set(new ExpiringToken(randomToken(), now + BOOTSTRAP_TTL_MS));
		sweep(now);
	}

	/** Moves the pending open nonce into an HttpOnly bootstrap cookie. */
	public String claimBootstrapForBrowser()
	{
		long now = clock.getAsLong();
		ExpiringToken token = pendingOpen.getAndSet(null);
		if (token == null || token.expiresAt <= now)
		{
			return null;
		}
		browserBootstraps.put(token.value, token.expiresAt);
		return token.value;
	}

	/** Consumes a bootstrap cookie and creates a new short-lived session. */
	public SessionGrant exchangeBootstrap(String bootstrapToken)
	{
		long now = clock.getAsLong();
		if (bootstrapToken == null)
		{
			return null;
		}
		Long expiresAt = browserBootstraps.remove(bootstrapToken);
		if (expiresAt == null || expiresAt <= now)
		{
			return null;
		}
		String sessionId = randomToken();
		String csrfToken = randomToken();
		long sessionExpiresAt = now + SESSION_TTL_MS;
		sessions.put(sessionId, new Session(csrfToken, sessionExpiresAt));
		sweep(now);
		return new SessionGrant(sessionId, csrfToken, sessionExpiresAt);
	}

	public SessionGrant resume(String sessionId)
	{
		if (sessionId == null)
		{
			return null;
		}
		long now = clock.getAsLong();
		Session session = sessions.get(sessionId);
		if (session == null || session.expiresAt <= now)
		{
			sessions.remove(sessionId);
			return null;
		}
		return new SessionGrant(sessionId, session.csrfToken, session.expiresAt);
	}

	public boolean authenticate(String sessionId)
	{
		return resume(sessionId) != null;
	}

	public boolean authenticateMutation(String sessionId, String csrfToken)
	{
		SessionGrant grant = resume(sessionId);
		return grant != null && constantTimeEquals(grant.csrfToken, csrfToken);
	}

	public boolean hasLiveSession()
	{
		long now = clock.getAsLong();
		sweep(now);
		return !sessions.isEmpty() || pendingOpen.get() != null || !browserBootstraps.isEmpty();
	}

	public void clear()
	{
		pendingOpen.set(null);
		browserBootstraps.clear();
		sessions.clear();
	}

	private void sweep(long now)
	{
		ExpiringToken pending = pendingOpen.get();
		if (pending != null && pending.expiresAt <= now)
		{
			pendingOpen.compareAndSet(pending, null);
		}
		browserBootstraps.entrySet().removeIf(entry -> entry.getValue() <= now);
		sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
	}

	private String randomToken()
	{
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static boolean constantTimeEquals(String expected, String provided)
	{
		if (expected == null || provided == null)
		{
			return false;
		}
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
			provided.getBytes(StandardCharsets.UTF_8));
	}

	private static final class ExpiringToken
	{
		private final String value;
		private final long expiresAt;

		private ExpiringToken(String value, long expiresAt)
		{
			this.value = value;
			this.expiresAt = expiresAt;
		}
	}

	private static final class Session
	{
		private final String csrfToken;
		private final long expiresAt;

		private Session(String csrfToken, long expiresAt)
		{
			this.csrfToken = csrfToken;
			this.expiresAt = expiresAt;
		}
	}

	public static final class SessionGrant
	{
		private final String sessionId;
		private final String csrfToken;
		private final long expiresAt;

		private SessionGrant(String sessionId, String csrfToken, long expiresAt)
		{
			this.sessionId = sessionId;
			this.csrfToken = csrfToken;
			this.expiresAt = expiresAt;
		}

		public String getSessionId()
		{
			return sessionId;
		}

		public String getCsrfToken()
		{
			return csrfToken;
		}

		public long getExpiresAt()
		{
			return expiresAt;
		}
	}
}
