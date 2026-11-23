package cn.blockforge.aatu_ut.enhancement;

import cn.blockforge.aatu_exup.AttributeEnhancement;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EnhancementTooltip {
    private static final String HEADER = "tooltip.aatu_exup.enhancement_total";
    private static final String ATTRIBUTE = "tooltip.aatu_exup.attribute_total";

    private EnhancementTooltip() {}

    public static void append(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.has(DataComponents.CUSTOM_DATA)) return;
        CompoundTag addon = EnhancementData.root(stack);
        CompoundTag bonuses = addon.getCompound("bonuses");
        boolean showOptionalBase = !ModList.get().isLoaded("forbidden_knowledge")
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("forbidden_knowledge_enhancements");
        if (bonuses.isEmpty() && !showOptionalBase) return;
        CompoundTag flags = addon.getCompound("breakthrough");

        Map<StatType, Double> base = EnhancementData.base(stack);
        List<Component> tooltip = event.getToolTip();
        for (StatType stat : StatType.values()) {
            double raw = bonuses.getDouble(stat.key);
            if (!Double.isFinite(raw) || stat.attribute() == null) continue;
            double original = base.getOrDefault(stat, 0d);
            double transferred = EnhancementData.effectiveExtra(bonuses, flags, stat, original);
            boolean fallbackBase = showOptionalBase && !stat.core() && stat.supports(stack);
            if (transferred <= 0 && (!fallbackBase || original <= 0)) continue;

            int header = headerIndex(tooltip);
            if (header < 0) {
                tooltip.add(Component.translatable(HEADER).withStyle(ChatFormatting.GOLD));
                header = tooltip.size() - 1;
            }

            String nameKey = nameKey(stat);
            Component line = Component.translatable(ATTRIBUTE, formatValue(stat, original + transferred),
                    Component.translatable(nameKey))
                    .withStyle(stat.core() ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE);
            int index = header + 1;
            while (index < tooltip.size() && isTranslation(tooltip.get(index), ATTRIBUTE)) {
                if (isAttribute(tooltip.get(index), nameKey)) break;
                index++;
            }
            if (index < tooltip.size() && isAttribute(tooltip.get(index), nameKey)) tooltip.set(index, line);
            else tooltip.add(index, line);
        }
    }

    private static int headerIndex(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (isTranslation(tooltip.get(i), HEADER)) return i;
        }
        return -1;
    }

    private static boolean isTranslation(Component component, String key) {
        return component.getContents() instanceof TranslatableContents contents && key.equals(contents.getKey());
    }

    private static boolean isAttribute(Component line, String nameKey) {
        if (!(line.getContents() instanceof TranslatableContents contents) || !ATTRIBUTE.equals(contents.getKey())) return false;
        Object[] args = contents.getArgs();
        return args.length > 1 && args[1] instanceof Component name && isTranslation(name, nameKey);
    }

    private static String nameKey(StatType stat) {
        if (stat.core()) return AttributeEnhancement.attributeNameKey(stat.key);
        if (!ModList.get().isLoaded("forbidden_knowledge")) return "stat.aatu_ut." + stat.name().toLowerCase(Locale.ROOT);
        return "tooltip.forbidden_knowledge.attribute." + switch (stat) {
            case FIRE -> "fire";
            case ARCANE -> "arcane";
            case FROST -> "frost";
            case RANGED -> "ranged_damage";
            case HEALING -> "healing";
            case SPELL_PENETRATION -> "spell_penetration";
            default -> throw new IllegalArgumentException("Not a forbidden attribute: " + stat);
        };
    }

    private static String formatValue(StatType stat, double value) {
        if (stat.core()) return AttributeEnhancement.formatValue(stat.key, value);
        if (stat == StatType.SPELL_PENETRATION) return String.format(Locale.ROOT, "%.1f%%", value * 100d);
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
