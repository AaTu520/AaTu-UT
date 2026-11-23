package cn.blockforge.aatu_ut.enhancement;

import cn.blockforge.aatu_ut.UtMod;
import cn.blockforge.aatu_ut.config.UtConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import java.util.*;

public final class BookData {
    private static final String SUCCESS_CHANCE = "unbreakable_success_chance";
    private BookData() {}
    public static boolean isBook(ItemStack stack) { return stack.is(UtMod.ATTRIBUTE_BOOK.get()); }
    public static boolean unbreakable(ItemStack stack) { return isBook(stack) && EnhancementData.root(stack).getBoolean("unbreakable_book"); }
    public static ItemStack create(Map<StatType, Double> attributes) {
        ItemStack book = new ItemStack(UtMod.ATTRIBUTE_BOOK.get());
        EnhancementData.edit(book, tag -> {
            CompoundTag data = new CompoundTag();
            attributes.forEach((stat, value) -> { if (Double.isFinite(value) && value > 0) data.putDouble(stat.key, value); });
            tag.put("book_attributes", data);
        });
        return book;
    }
    public static ItemStack unbreakableBook() {
        return unbreakableBook(1.0);
    }
    public static ItemStack unbreakableBook(double successChance) {
        requireValidChance(successChance);
        ItemStack book = new ItemStack(UtMod.ATTRIBUTE_BOOK.get());
        EnhancementData.edit(book, tag -> {
            tag.putBoolean("unbreakable_book", true);
            tag.putDouble(SUCCESS_CHANCE, successChance);
        });
        book.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        return book;
    }
    public static OptionalDouble successChance(ItemStack book) {
        if (!unbreakable(book)) return OptionalDouble.empty();
        CompoundTag data = EnhancementData.root(book);
        if (!data.contains(SUCCESS_CHANCE, Tag.TAG_ANY_NUMERIC)) return OptionalDouble.empty();
        double value = data.getDouble(SUCCESS_CHANCE);
        return Double.isFinite(value) && value >= 0 && value <= 1 ? OptionalDouble.of(value) : OptionalDouble.empty();
    }
    public static void initializeSuccessChance(ItemStack book) {
        if (!unbreakable(book)) return;
        if (!book.has(DataComponents.UNBREAKABLE)) book.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        if (successChance(book).isPresent()) return;
        // Older books had no saved chance; bind it once on the server so later config changes cannot alter them.
        double chance = UtConfig.get().books.unbreakableSuccessChance;
        requireValidChance(chance);
        EnhancementData.edit(book, tag -> tag.putDouble(SUCCESS_CHANCE, chance));
    }
    private static void requireValidChance(double chance) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
            throw new IllegalArgumentException("Unbreakable book success chance must be between 0 and 1");
        }
    }
    public static Map<StatType, Double> attributes(ItemStack book) {
        Map<StatType, Double> output = new EnumMap<>(StatType.class);
        CompoundTag values = EnhancementData.root(book).getCompound("book_attributes");
        for (StatType stat : StatType.values()) {
            double value = values.getDouble(stat.key);
            if (Double.isFinite(value) && value > 0 && value <= 10000000) output.put(stat, value);
        }
        return output;
    }
}
