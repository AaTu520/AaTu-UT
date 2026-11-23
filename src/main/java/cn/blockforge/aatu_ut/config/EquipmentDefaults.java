package cn.blockforge.aatu_ut.config;

import cn.blockforge.champions_level.api.RewardProgression;
import java.util.*;

final class EquipmentDefaults {
    private EquipmentDefaults() {}

    static List<UtConfig.EquipmentTier> create() {
        // A successful higher-tier hat must exclude the fallback roll later in the same group.
        List<UtConfig.EquipmentTier> tiers = new ArrayList<>(List.of(
                hat(100, 150, 0.05, 5, 3),
                hat(151, 300, 0.05, 10, 10),
                hat(151, 300, 0.35, 5, 3),
                weapon(151, 300, 25),
                hat(301, 500, 0.15, 10, 10),
                weapon(301, 500, 35)));
        for (RewardProgression.Tier tier : RewardProgression.highTiers()) {
            tiers.add(hat(tier.minimumLevel(), tier.maximumLevel(), tier.hatChance(),
                    tier.armor().minimum(), tier.armor().maximum(), tier.toughness().minimum(), tier.toughness().maximum()));
            tiers.add(weapon(tier.minimumLevel(), tier.maximumLevel(), tier.weaponChance(), tier.attack().minimum(), tier.attack().maximum()));
        }
        return tiers;
    }

    private static UtConfig.EquipmentTier hat(int minimumLevel, int maximumLevel, double chance, int armor, int toughness) {
        return hat(minimumLevel, maximumLevel, chance, 1, armor, 1, toughness);
    }

    private static UtConfig.EquipmentTier hat(int minimumLevel, int maximumLevel, double chance,
                                             int minimumArmor, int maximumArmor, int minimumToughness, int maximumToughness) {
        UtConfig.EquipmentEntry entry = new UtConfig.EquipmentEntry("minecraft:glass", "armor", minimumArmor, maximumArmor);
        entry.attributes.put("armor_toughness", new UtConfig.Range(minimumToughness, maximumToughness));
        return tier(minimumLevel, maximumLevel, chance, "hat", List.of(entry));
    }

    private static UtConfig.EquipmentTier weapon(int minimumLevel, int maximumLevel, int attackMaximum) {
        return weapon(minimumLevel, maximumLevel, 0.05, 1, attackMaximum);
    }

    private static UtConfig.EquipmentTier weapon(int minimumLevel, int maximumLevel, double chance, int minimumAttack, int maximumAttack) {
        List<UtConfig.EquipmentEntry> pool = new ArrayList<>();
        pool.add(new UtConfig.EquipmentEntry("minecraft:diamond_sword", "attack_damage", minimumAttack, maximumAttack));
        for (String school : List.of("arcane", "fire", "frost")) {
            UtConfig.EquipmentEntry staff = new UtConfig.EquipmentEntry("wizards:staff_" + school, "spell_power:" + school, minimumAttack, maximumAttack);
            staff.optional = true;
            pool.add(staff);
        }
        return tier(minimumLevel, maximumLevel, chance, "weapon", pool);
    }

    private static UtConfig.EquipmentTier tier(int minimumLevel, int maximumLevel, double chance,
                                              String exclusiveGroup, List<UtConfig.EquipmentEntry> pool) {
        UtConfig.EquipmentTier tier = new UtConfig.EquipmentTier();
        tier.minimumLevel = minimumLevel;
        tier.maximumLevel = maximumLevel;
        tier.chance = chance;
        tier.exclusiveGroup = exclusiveGroup;
        tier.pool = new ArrayList<>(pool);
        return tier;
    }
}
