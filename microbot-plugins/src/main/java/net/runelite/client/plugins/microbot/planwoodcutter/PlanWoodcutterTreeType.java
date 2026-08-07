package net.runelite.client.plugins.microbot.planwoodcutter;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

import java.util.Arrays;

@Getter
public enum PlanWoodcutterTreeType
{
    NORMAL("Normal", 1, ItemID.LOGS, 1,
            "Tree", "Evergreen", "Dead tree", "Dying tree", "Jungle tree", "Light tree"),
    ACHEY("Achey", 1, ItemID.ACHEY_TREE_LOGS, -1, "Achey tree"),
    OAK("Oak", 15, ItemID.OAK_LOGS, 15, "Oak", "Oak tree"),
    WILLOW("Willow", 30, ItemID.WILLOW_LOGS, 30, "Willow", "Willow tree"),
    TEAK("Teak", 35, ItemID.TEAK_LOGS, -1, "Teak", "Teak tree"),
    JATOBA("Jatoba", 40, ItemID.JATOBA_LOGS, -1, "Jatoba tree"),
    JUNIPER("Juniper", 42, ItemID.JUNIPER_LOGS, -1, "Mature juniper tree"),
    MAPLE("Maple", 45, ItemID.MAPLE_LOGS, 45, "Maple", "Maple tree"),
    MAHOGANY("Mahogany", 50, ItemID.MAHOGANY_LOGS, -1, "Mahogany", "Mahogany tree"),
    ARCTIC_PINE("Arctic pine", 54, ItemID.ARCTIC_PINE_LOG, -1,
            "Arctic pine", "Arctic pine tree"),
    YEW("Yew", 60, ItemID.YEW_LOGS, 60, "Yew", "Yew tree"),
    BLISTERWOOD("Blisterwood", 62, ItemID.BLISTERWOOD_LOGS, -1, "Blisterwood tree"),
    CAMPHOR("Camphor", 66, ItemID.CAMPHOR_LOGS, -1, "Camphor tree"),
    MAGIC("Magic", 75, ItemID.MAGIC_LOGS, 75, "Magic", "Magic tree"),
    IRONWOOD("Ironwood", 80, ItemID.IRONWOOD_LOGS, -1, "Ironwood tree"),
    REDWOOD("Redwood", 90, ItemID.REDWOOD_LOGS, 90, "Redwood", "Redwood tree"),
    ROSEWOOD("Rosewood", 92, ItemID.ROSEWOOD_LOGS, -1, "Rosewood tree"),
    CUSTOM("Custom", 1, -1, -1);

    private final String displayName;
    private final int woodcuttingLevel;
    private final int logItemId;
    private final int arrowShaftLevel;
    private final String[] objectNames;

    PlanWoodcutterTreeType(
            String displayName,
            int woodcuttingLevel,
            int logItemId,
            int arrowShaftLevel,
            String... objectNames)
    {
        this.displayName = displayName;
        this.woodcuttingLevel = woodcuttingLevel;
        this.logItemId = logItemId;
        this.arrowShaftLevel = arrowShaftLevel;
        this.objectNames = objectNames;
    }

    public boolean supportsArrowShafts()
    {
        return arrowShaftLevel >= 0 && logItemId >= 0;
    }

    public boolean hasValidTargetName(String customTreeName)
    {
        return this != CUSTOM || customTreeName != null && !customTreeName.trim().isEmpty();
    }

    boolean matches(String objectName, String customTreeName)
    {
        if (objectName == null)
        {
            return false;
        }
        if (this == CUSTOM)
        {
            return customTreeName != null && objectName.equalsIgnoreCase(customTreeName.trim());
        }
        return Arrays.stream(objectNames).anyMatch(objectName::equalsIgnoreCase);
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
