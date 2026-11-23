package cn.blockforge.aatu_ut.enhancement;

import java.util.List;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

final class HatDropFactory {
    private static final List<Item> GLASS_COLORS = List.of(
            Items.LIGHT_BLUE_STAINED_GLASS, Items.GLASS, Items.PINK_STAINED_GLASS,
            Items.LIME_STAINED_GLASS, Items.YELLOW_STAINED_GLASS);

    private HatDropFactory() {}

    static ItemStack prepare(ItemStack stack, RandomSource random, RegistryAccess registries) {
        if (!EnhancementData.isHatDropCandidate(stack)) return stack;

        // Existing glass reward entries keep their level, weight and stats while gaining the color pool.
        if (stack.is(Items.GLASS)) {
            stack = stack.transmuteCopy(GLASS_COLORS.get(random.nextInt(GLASS_COLORS.size())));
        }
        EnhancementData.edit(stack, tag -> tag.putBoolean("hat", true));
        if (GLASS_COLORS.contains(stack.getItem())) addHelmetEnchantments(stack, registries);
        return stack;
    }

    private static void addHelmetEnchantments(ItemStack stack, RegistryAccess registries) {
        var registry = registries.registryOrThrow(Registries.ENCHANTMENT);
        var enchantments = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        registry.getHolder(Enchantments.PROTECTION).ifPresent(holder -> enchantments.upgrade(holder, 5));
        registry.getHolder(Enchantments.RESPIRATION).ifPresent(holder -> enchantments.upgrade(holder, 3));
        registry.getHolder(Enchantments.AQUA_AFFINITY).ifPresent(holder -> enchantments.upgrade(holder, 1));
        registry.getHolder(Enchantments.THORNS).ifPresent(holder -> enchantments.upgrade(holder, 3));
        // Non-equipment may lack this component, so ItemStack.enchant alone can silently do nothing.
        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
    }
}
