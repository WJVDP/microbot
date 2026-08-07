package net.runelite.client.plugins.microbot.planwoodcutter;

import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;

import java.util.Map;
import java.util.Set;

final class PlanWoodcutterData
{
    private static final Map<Integer, Integer> AXE_LEVELS = Map.ofEntries(
            axe(ItemID.BRONZE_AXE, 1),
            axe(ItemID.BRONZE_AXE_2H, 1),
            axe(ItemID.IRON_AXE, 1),
            axe(ItemID.IRON_AXE_2H, 1),
            axe(ItemID.STEEL_AXE, 6),
            axe(ItemID.STEEL_AXE_2H, 6),
            axe(ItemID.BLACK_AXE, 11),
            axe(ItemID.BLACK_AXE_2H, 11),
            axe(ItemID.MITHRIL_AXE, 21),
            axe(ItemID.MITHRIL_AXE_2H, 21),
            axe(ItemID.ADAMANT_AXE, 31),
            axe(ItemID.ADAMANT_AXE_2H, 31),
            axe(ItemID.RUNE_AXE, 41),
            axe(ItemID.RUNE_AXE_2H, 41),
            axe(ItemID.TRAIL_GILDED_AXE, 41),
            axe(ItemID.DRAGON_AXE, 61),
            axe(ItemID.DRAGON_AXE_2H, 61),
            axe(ItemID.INFERNAL_AXE, 61),
            axe(ItemID.INFERNAL_AXE_EMPTY, 61),
            axe(ItemID.TRAILBLAZER_AXE, 61),
            axe(ItemID.TRAILBLAZER_AXE_EMPTY, 61),
            axe(ItemID.TRAILBLAZER_AXE_NO_INFERNAL, 61),
            axe(ItemID.TRAILBLAZER_RELOADED_AXE, 61),
            axe(ItemID.TRAILBLAZER_RELOADED_AXE_EMPTY, 61),
            axe(ItemID.TRAILBLAZER_RELOADED_AXE_NO_INFERNAL, 61),
            axe(ItemID.LEAGUE_TRAILBLAZER_AXE, 61),
            axe(ItemID._3A_AXE, 61),
            axe(ItemID._3A_AXE_2H, 61),
            axe(ItemID.CRYSTAL_AXE, 71),
            axe(ItemID.CRYSTAL_AXE_2H, 71)
    );

    private static final Set<Integer> LOG_IDS = Set.of(
            ItemID.LOGS,
            ItemID.OAK_LOGS,
            ItemID.WILLOW_LOGS,
            ItemID.TEAK_LOGS,
            ItemID.MAPLE_LOGS,
            ItemID.MAHOGANY_LOGS,
            ItemID.YEW_LOGS,
            ItemID.MAGIC_LOGS,
            ItemID.REDWOOD_LOGS,
            ItemID.ACHEY_TREE_LOGS,
            ItemID.JATOBA_LOGS,
            ItemID.ARCTIC_PINE_LOG,
            ItemID.JUNIPER_LOGS,
            ItemID.BLISTERWOOD_LOGS,
            ItemID.BREW_SCRAPEY_LOGS,
            ItemID.CAMPHOR_LOGS,
            ItemID.IRONWOOD_LOGS,
            ItemID.ROSEWOOD_LOGS
    );

    private PlanWoodcutterData()
    {
    }

    static boolean hasUsableAxe(int woodcuttingLevel)
    {
        return Rs2Inventory.contains(item -> isUsableAxe(item, woodcuttingLevel))
                || Rs2Equipment.isWearing(item -> isUsableAxe(item, woodcuttingLevel));
    }

    static boolean isUsableAxe(Rs2ItemModel item, int woodcuttingLevel)
    {
        return item != null && minimumAxeLevel(item.getId()) <= woodcuttingLevel;
    }

    static int minimumAxeLevel(int itemId)
    {
        return AXE_LEVELS.getOrDefault(itemId, Integer.MAX_VALUE);
    }

    static boolean isLog(Rs2ItemModel item)
    {
        return item != null && !item.isNoted() && LOG_IDS.contains(item.getId());
    }

    static boolean isLogId(int itemId)
    {
        return LOG_IDS.contains(itemId);
    }

    private static Map.Entry<Integer, Integer> axe(int itemId, int level)
    {
        return Map.entry(itemId, level);
    }
}
