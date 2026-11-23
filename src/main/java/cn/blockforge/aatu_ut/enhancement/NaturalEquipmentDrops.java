package cn.blockforge.aatu_ut.enhancement;

import cn.blockforge.aatu_ut.config.UtConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class NaturalEquipmentDrops {
    private static final String ARMOR_DROP_GROUP = "hat";
    private NaturalEquipmentDrops() {}

    public static void enhance(Mob boss, int level, List<ItemStack> drops, UtConfig config,
                               EquipmentDropLimiter limiter) {
        if (!config.naturalEquipment.enabled) return;
        UtConfig.NaturalEquipmentTier tier = config.naturalEquipment.tiers.stream()
                .filter(entry -> level >= entry.minimumLevel && level <= entry.maximumLevel)
                .findFirst().orElse(null);
        RandomSource random = boss.getRandom();
        for (ItemStack stack : drops) {
            EquipmentDropLimiter.Kind kind = EquipmentDropLimiter.kind(stack);
            if (kind != null && !limiter.available(kind)) continue;
            boolean enhanced = false;
            if (stack.getItem() instanceof ArmorItem) {
                enhanced = enhanceArmor(stack, level, config, random);
            } else if (tier == null) {
                continue;
            } else if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
                enhanced = add(stack, StatType.RANGED, tier.weaponBonus, random);
            } else if (EnhancementData.isStaff(stack)) {
                List<StatType> available = List.of(StatType.FIRE, StatType.ARCANE, StatType.FROST, StatType.HEALING).stream()
                        .filter(stat -> stat.attribute() != null && stat.supports(stack)).toList();
                if (!available.isEmpty()) enhanced = add(stack, available.get(random.nextInt(available.size())), tier.weaponBonus, random);
            } else if (StatType.ATTACK.supports(stack)) {
                enhanced = add(stack, StatType.ATTACK, tier.weaponBonus, random);
            }
            if (enhanced) limiter.claim(kind);
        }
    }

    public static void enhanceGuardianArmor(Mob boss, int level, List<ItemStack> drops, UtConfig config,
                                            EquipmentDropLimiter limiter) {
        if (!config.naturalEquipment.enabled || !limiter.available(EquipmentDropLimiter.Kind.ARMOR)) return;
        RandomSource random = boss.getRandom();
        for (ItemStack stack : drops) {
            if (!(stack.getItem() instanceof ArmorItem)) continue;
            if (enhanceArmor(stack, level, config, random)) {
                limiter.claim(EquipmentDropLimiter.Kind.ARMOR);
                return;
            }
        }
    }

    private static boolean enhanceArmor(ItemStack stack, int level, UtConfig config, RandomSource random) {
        // Use the configured hat order so high-tier success excludes its fallback.
        for (UtConfig.EquipmentTier tier : config.equipmentDrops) {
            if (!ARMOR_DROP_GROUP.equals(tier.exclusiveGroup) || level < tier.minimumLevel || level > tier.maximumLevel) continue;
            List<UtConfig.EquipmentEntry> available = tier.pool.stream()
                    .filter(entry -> BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(entry.item))).toList();
            if (available.isEmpty() || random.nextDouble() >= tier.chance) continue;
            double roll = random.nextDouble() * available.stream().mapToDouble(entry -> entry.weight).sum();
            for (UtConfig.EquipmentEntry entry : available) {
                roll -= entry.weight;
                if (roll > 0) continue;
                boolean enhanced = false;
                for (var attribute : entry.attributes.entrySet()) {
                    StatType stat = StatType.find(attribute.getKey());
                    if (stat != null && stat.supports(stack)) enhanced |= add(stack, stat, attribute.getValue(), random);
                }
                return enhanced;
            }
        }
        return false;
    }

    private static boolean add(ItemStack stack, StatType stat, UtConfig.Range range, RandomSource random) {
        if (stat.attribute() == null || !stat.supports(stack)) return false;
        double current = EnhancementData.total(stack).getOrDefault(stat, 0d);
        double remaining = Math.max(0, stat.originalLimit() - current);
        double amount = Math.min(remaining, range.minimum + random.nextDouble() * (range.maximum - range.minimum));
        if (amount <= 0) return false;
        EnhancementData.addBase(stack, stat, amount);
        return true;
    }
}
