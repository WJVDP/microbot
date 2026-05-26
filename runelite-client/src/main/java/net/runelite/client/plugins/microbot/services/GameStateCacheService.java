package net.runelite.client.plugins.microbot.services;

import net.runelite.api.annotations.Varbit;
import net.runelite.api.annotations.Varp;
import net.runelite.client.plugins.microbot.api.boat.Rs2BoatCache;
import net.runelite.client.plugins.microbot.api.npc.Rs2NpcCache;
import net.runelite.client.plugins.microbot.api.player.Rs2PlayerCache;
import net.runelite.client.plugins.microbot.api.playerstate.Rs2PlayerStateCache;
import net.runelite.client.plugins.microbot.api.tileitem.Rs2TileItemCache;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;

public interface GameStateCacheService
{
	boolean isLoggedIn();

	int getVarbitValue(@Varbit int varbit);

	int getVarpValue(@Varp int varpId);

	Rs2PlayerStateCache getPlayerStateCache();

	Rs2NpcCache getNpcCache();

	Rs2PlayerCache getPlayerCache();

	Rs2TileItemCache getTileItemCache();

	Rs2TileObjectCache getTileObjectCache();

	Rs2BoatCache getBoatCache();
}
