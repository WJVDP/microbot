package net.runelite.client.plugins.microbot.planwoodcutter;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        assertTrue(PlanWoodcutterTreeType.ARCTIC_PINE.matches("Arctic pine tree", ""));
        assertTrue(PlanWoodcutterTreeType.CUSTOM.matches("Cursed tree", " Cursed tree "));
        assertFalse(PlanWoodcutterTreeType.CUSTOM.matches("Tree", "Cursed tree"));
    }

    @Test
    public void selectsEachSupportedWoodcuttingActionFromTheObject()
    {
        String[] woodcuttingActions = {"Chop down", "Chop", "Cut down", "Cut", "Chop-down"};
        for (String action : woodcuttingActions)
        {
            assertEquals(action, PlanWoodcutterScript.findWoodcuttingAction(
                    new String[]{"Inspect", action, null}));
        }

        assertEquals("Cut", PlanWoodcutterScript.findWoodcuttingAction(
                new String[]{"Inspect", "Cut", "Chop down"}));
        assertNull(PlanWoodcutterScript.findWoodcuttingAction(
                new String[]{"Inspect", "Search"}));
        assertNull(PlanWoodcutterScript.findWoodcuttingAction(null));
    }
}
