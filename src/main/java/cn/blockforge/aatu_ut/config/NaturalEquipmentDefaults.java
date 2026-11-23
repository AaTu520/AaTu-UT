package cn.blockforge.aatu_ut.config;

import cn.blockforge.champions_level.api.RewardProgression;
import java.util.ArrayList;
import java.util.List;

final class NaturalEquipmentDefaults {
    private NaturalEquipmentDefaults() {}

    static List<UtConfig.NaturalEquipmentTier> create() {
        List<UtConfig.NaturalEquipmentTier> tiers = new ArrayList<>(List.of(
                tier(1, 150, 1, 10),
                tier(151, 300, 1, 25),
                tier(301, 500, 1, 35)));
        for (RewardProgression.Tier source : RewardProgression.highTiers()) {
            tiers.add(tier(source.minimumLevel(), source.maximumLevel(),
                    source.attack().minimum(), source.attack().maximum()));
        }
        return tiers;
    }

    private static UtConfig.NaturalEquipmentTier tier(int minimum, int maximum, int weaponMinimum, int weaponMaximum) {
        UtConfig.NaturalEquipmentTier tier = new UtConfig.NaturalEquipmentTier();
        tier.minimumLevel = minimum;
        tier.maximumLevel = maximum;
        tier.weaponBonus = new UtConfig.Range(weaponMinimum, weaponMaximum);
        return tier;
    }
}
