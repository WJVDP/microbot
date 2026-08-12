package net.runelite.client.plugins.microbot.questhelper.automation;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-local exclusive ownership for quest interactions.
 *
 * <p>A controller may acquire repeatedly, but only the current owner may release.
 * The lease deliberately contains no game logic so ownership can be unit tested.</p>
 */
public final class QuestAutomationLease
{
    public enum Owner
    {
        LEGACY_QUEST_SCRIPT,
        PLAN_QUESTING
    }

    private static final AtomicReference<Owner> OWNER = new AtomicReference<>();

    private QuestAutomationLease()
    {
    }

    public static boolean acquire(Owner requested)
    {
        if (requested == null)
        {
            throw new IllegalArgumentException("requested owner is required");
        }

        Owner current = OWNER.get();
        return current == requested || OWNER.compareAndSet(null, requested);
    }

    public static boolean isOwnedBy(Owner expected)
    {
        return OWNER.get() == expected;
    }

    public static Owner currentOwner()
    {
        return OWNER.get();
    }

    public static void release(Owner releasing)
    {
        if (releasing != null)
        {
            OWNER.compareAndSet(releasing, null);
        }
    }

    static void clearForTests()
    {
        OWNER.set(null);
    }
}
