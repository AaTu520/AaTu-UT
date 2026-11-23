package cn.blockforge.aatu_ut.enhancement;

import cn.blockforge.aatu_exup.AttributeData;
import cn.blockforge.aatu_ut.config.UtConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class EquipmentDropFactory {
    private EquipmentDropFactory() {
    }

    public static ItemStack create(UtConfig.EquipmentEntry entry, RandomSource random, RegistryAccess registries) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.item)));
        stack = HatDropFactory.prepare(stack, random, registries);
        for (var attribute : entry.attributes.entrySet()) {
            StatType stat = StatType.find(attribute.getKey());
            if (stat == null || !stat.supports(stack)) continue;
            double min = attribute.getValue().minimum, max = attribute.getValue().maximum;
            EnhancementData.addBase(stack, stat, min + random.nextDouble() * (max - min));
        }
        int current = AttributeData.getSoul(stack);
        if (entry.soul >= current) AttributeData.addSoul(stack, entry.soul - current);
        else AttributeData.consumeSoul(stack, current - entry.soul);
        return stack;
    }
}
