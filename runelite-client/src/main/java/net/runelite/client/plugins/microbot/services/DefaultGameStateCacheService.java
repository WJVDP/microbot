package net.runelite.client.plugins.microbot.services;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.annotations.Varp;
import net.runelite.client.plugins.microbot.api.boat.Rs2BoatCache;
import net.runelite.client.plugins.microbot.api.npc.Rs2NpcCache;
import net.runelite.client.plugins.microbot.api.player.Rs2PlayerCache;
import net.runelite.client.plugins.microbot.api.playerstate.Rs2PlayerStateCache;
import net.runelite.client.plugins.microbot.api.tileitem.Rs2TileItemCache;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.util.security.LoginManager;

@Singleton
public class DefaultGameStateCacheService implements GameStateCacheService
{
	private final Rs2PlayerStateCache playerStateCache;
	private final Rs2NpcCache npcCache;
	private final Rs2PlayerCache playerCache;
	private final Rs2TileItemCache tileItemCache;
	private final Rs2TileObjectCache tileObjectCache;
	private final Rs2BoatCache boatCache;

	@Inject
	DefaultGameStateCacheService(
		Rs2PlayerStateCache playerStateCache,
		Rs2NpcCache npcCache,
		Rs2PlayerCache playerCache,
		Rs2TileItemCache tileItemCache,
		Rs2TileObjectCache tileObjectCache,
		Rs2BoatCache boatCache)
	{
		this.playerStateCache = playerStateCache;
		this.npcCache = npcCache;
		this.playerCache = playerCache;
		this.tileItemCache = tileItemCache;
		this.tileObjectCache = tileObjectCache;
		this.boatCache = boatCache;
	}

	@Override
	public boolean isLoggedIn()
	{
		return LoginManager.isLoggedIn();
	}

	@Override
	public int getVarbitValue(@Varbit int varbit)
	{
		return playerStateCache.getVarbitValue(varbit);
	}

	@Override
	public int getVarpValue(@Varp int varpId)
	{
		return playerStateCache.getVarpValue(varpId);
	}

	@Override
	public Rs2PlayerStateCache getPlayerStateCache()
	{
		return playerStateCache;
	}

	@Override
	public Rs2NpcCache getNpcCache()
	{
		return npcCache;
	}

	@Override
	public Rs2PlayerCache getPlayerCache()
	{
		return playerCache;
	}

	@Override
	public Rs2TileItemCache getTileItemCache()
	{
		return tileItemCache;
	}

	@Override
	public Rs2TileObjectCache getTileObjectCache()
	{
		return tileObjectCache;
	}

	@Override
	public Rs2BoatCache getBoatCache()
	{
		return boatCache;
	}
}
