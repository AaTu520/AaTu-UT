package cn.blockforge.aatu_ut.item;

import cn.blockforge.aatu_ut.enhancement.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.math.BigDecimal;
import java.util.*;

public final class AttributeBookItem extends Item {
    private static final String DIVIDER = "-----------------------------------";
    private static final int LEGACY_BOOK_CHECK_INTERVAL = 20;
    public AttributeBookItem(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) {
        boolean unbreakable = BookData.unbreakable(stack);
        return Component.translatable(unbreakable ? "item.aatu_ut.unbreakable_book" : "item.aatu_ut.attribute_book")
                .withStyle(unbreakable ? ChatFormatting.BLUE : ChatFormatting.AQUA);
    }
    @Override public boolean isFoil(ItemStack stack) { return BookData.unbreakable(stack); }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!level.isClientSide && holder.tickCount % LEGACY_BOOK_CHECK_INTERVAL == 0) {
            BookData.initializeSuccessChance(stack);
        }
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean unbreakable = BookData.unbreakable(stack);
        tooltip.add(Component.literal(DIVIDER).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(unbreakable ? "tooltip.aatu_ut.unbreakable_flavor" : "tooltip.aatu_ut.attribute_flavor")
                .withStyle(unbreakable ? ChatFormatting.BLUE : ChatFormatting.AQUA));
        tooltip.add(Component.translatable(unbreakable ? "tooltip.aatu_ut.unbreakable_use" : "tooltip.aatu_ut.book_use")
                .withStyle(ChatFormatting.WHITE));
        if (!unbreakable) {
            tooltip.add(Component.translatable("tooltip.aatu_ut.attribute_breakthrough").withStyle(ChatFormatting.WHITE));
        }
        tooltip.add(Component.literal(DIVIDER).withStyle(ChatFormatting.DARK_GRAY));
        if (unbreakable) {
            tooltip.add(Component.translatable("tooltip.aatu_ut.unbreakable").withStyle(ChatFormatting.BLUE));
            OptionalDouble chance = BookData.successChance(stack);
            Component chanceLine = chance.isPresent()
                    ? Component.translatable("tooltip.aatu_ut.unbreakable_chance", formatPercent(chance.getAsDouble()))
                    : Component.translatable("tooltip.aatu_ut.unbreakable_chance_pending");
            tooltip.add(chanceLine.copy().withStyle(ChatFormatting.WHITE));
            return;
        }
        BookData.attributes(stack).forEach((stat, value) -> tooltip.add(Component.translatable("stat.aatu_ut." + stat.name().toLowerCase(Locale.ROOT))
                .append(" +" + String.format(Locale.ROOT, "%.2f", value)).withStyle(ChatFormatting.AQUA)));
    }

    private static String formatPercent(double chance) {
        return BigDecimal.valueOf(chance).movePointRight(2).stripTrailingZeros().toPlainString();
    }
}
