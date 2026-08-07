package net.runelite.client.plugins.microbot.planfiremaker;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Getter
public enum PlanFiremakerLogType
{
    NORMAL("Logs", ItemID.LOGS, 1, 400),
    OAK("Oak logs", ItemID.OAK_LOGS, 15, 600),
    WILLOW("Willow logs", ItemID.WILLOW_LOGS, 30, 900),
    TEAK("Teak logs", ItemID.TEAK_LOGS, 35, 1_050),
    JATOBA("Jatoba logs", ItemID.JATOBA_LOGS, 40, 1_200),
    ARCTIC_PINE("Arctic pine logs", ItemID.ARCTIC_PINE_LOG, 42, 1_250),
    MAPLE("Maple logs", ItemID.MAPLE_LOGS, 45, 1_350),
    MAHOGANY("Mahogany logs", ItemID.MAHOGANY_LOGS, 50, 1_575),
    YEW("Yew logs", ItemID.YEW_LOGS, 60, 2_025),
    BLISTERWOOD("Blisterwood logs", ItemID.BLISTERWOOD_LOGS, 62, 960),
    CAMPHOR("Camphor logs", ItemID.CAMPHOR_LOGS, 66, 1_800),
    MAGIC("Magic logs", ItemID.MAGIC_LOGS, 75, 3_038),
    IRONWOOD("Ironwood logs", ItemID.IRONWOOD_LOGS, 80, 2_205),
    REDWOOD("Redwood logs", ItemID.REDWOOD_LOGS, 90, 3_500),
    ROSEWOOD("Rosewood logs", ItemID.ROSEWOOD_LOGS, 92, 2_680);

    private final String displayName;
    private final int itemId;
    private final int requiredLevel;
    private final int xpTenths;

    PlanFiremakerLogType(String displayName, int itemId, int requiredLevel, int xpTenths)
    {
        this.displayName = displayName;
        this.itemId = itemId;
        this.requiredLevel = requiredLevel;
        this.xpTenths = xpTenths;
    }

    static Optional<PlanFiremakerLogType> bestUnlocked(
            int level,
            PlanFiremakerLogType maximum,
            java.util.Map<PlanFiremakerLogType, Integer> quantities)
    {
        return Arrays.stream(values())
                .filter(type -> type.ordinal() <= maximum.ordinal())
                .filter(type -> type.requiredLevel <= level)
                .filter(type -> quantities.getOrDefault(type, 0) > 0)
                .max(Comparator.comparingInt(PlanFiremakerLogType::getXpTenths)
                        .thenComparingInt(PlanFiremakerLogType::getRequiredLevel));
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
