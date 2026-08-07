package net.runelite.client.plugins.microbot.planwoodcutter;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlanWoodcutterDataTest
{
    @Test
    public void recognizesAxeTiersAndRejectsBattleaxes()
    {
        assertEquals(1, PlanWoodcutterData.minimumAxeLevel(ItemID.BRONZE_AXE));
        assertEquals(61, PlanWoodcutterData.minimumAxeLevel(ItemID.DRAGON_AXE_2H));
        assertEquals(71, PlanWoodcutterData.minimumAxeLevel(ItemID.CRYSTAL_AXE));
        assertEquals(Integer.MAX_VALUE,
                PlanWoodcutterData.minimumAxeLevel(ItemID.DRAGON_BATTLEAXE));
    }

    @Test
    public void recognizesAllConfiguredTreeOutputsAsLogs()
    {
        for (PlanWoodcutterTreeType treeType : PlanWoodcutterTreeType.values())
        {
            if (treeType.getLogItemId() >= 0)
            {
                assertTrue(treeType + " output should be droppable",
                        PlanWoodcutterData.isLogId(treeType.getLogItemId()));
            }
        }
        assertFalse(PlanWoodcutterData.isLogId(ItemID.DRAGON_AXE));
    }

    @Test
    public void matchesAliasesAndCustomNamesCaseInsensitively()
    {
        assertTrue(PlanWoodcutterTreeType.OAK.matches("Oak tree", ""));
        assertTrue(PlanWoodcutterTreeType.MAGIC.matches("magic TREE", ""));
        assertTrue(PlanWoodcutterTreeType.CUSTOM.matches("Cursed tree", " Cursed tree "));
        assertFalse(PlanWoodcutterTreeType.CUSTOM.matches("Tree", "Cursed tree"));
    }
}
