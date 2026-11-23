package cn.blockforge.aatu_ut.enhancement;

import cn.blockforge.aatu_exup.AaTu_EXUp_ModConfig;
import cn.blockforge.aatu_exup.AttributeEnhancement;
import cn.blockforge.aatu_ut.config.UtConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.*;

public enum StatType {
    ATTACK("attack_damage", 233, true), ATTACK_SPEED("attack_speed", 1.2, false), PENETRATION("penetration", 1, false),
    ARMOR("armor", 233, true), TOUGHNESS("armor_toughness", 233, true), HEALTH("armor_health", 20, false), SPEED("armor_speed", 0.05, false),
    FIRE("spell_power:fire", 233, true), ARCANE("spell_power:arcane", 233, true), FROST("spell_power:frost", 233, true),
    RANGED("ranged_weapon:damage", 233, true), HEALING("spell_power:healing", 60, false), SPELL_PENETRATION("forbidden_knowledge:spell_penetration", 1, false);
    public final String key;
    public final double factoryLimit;
    public final boolean breakthrough;
    StatType(String key, double factoryLimit, boolean breakthrough) { this.key = key; this.factoryLimit = factoryLimit; this.breakthrough = breakthrough; }
    public static StatType find(String key) { for (StatType stat : values()) if (stat.key.equals(key)) return stat; return null; }
    public boolean core() { return !key.contains(":"); }
    public double originalLimit() {
        double value = core() ? AaTu_EXUp_ModConfig.attributeLimit(key) : AaTu_EXUp_ModConfig.forbiddenKnowledgeAttributeLimit(key);
        return Double.isFinite(value) ? Math.max(0, value) : factoryLimit;
    }
    public double cap(boolean broken) { return breakthrough && broken ? factoryLimit * 2 : originalLimit(); }
    public Holder<Attribute> attribute() {
        if (core()) return AttributeEnhancement.attributeFor(key);
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(key)).orElse(null);
    }
    public boolean supports(ItemStack stack) {
        boolean armor = stack.getItem() instanceof ArmorItem || EnhancementData.isHat(stack);
        if (this == ARMOR || this == TOUGHNESS || this == HEALTH || this == SPEED) return armor;
        if (armor) return false;
        if (core()) return AttributeEnhancement.isWeapon(stack);
        if (this == RANGED) return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
        if (attribute() == null) return false;
        return EnhancementData.supportsForbidden(stack, key);
    }
}
