package cn.blockforge.aatu_ut.mixin;

import cn.blockforge.aatu_exup.AttributeData;
import cn.blockforge.aatu_ut.enhancement.EnhancementData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = AttributeData.class, remap = false)
public abstract class EnhancementCapacityMixin {
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static double aatuUt$remaining(double value, ItemStack stack, String type, double requested) {
        if (EnhancementData.isHat(stack)) EnhancementData.edit(stack, tag -> tag.putBoolean("hat", true));
        return EnhancementData.coreCapacity(stack, type, value);
    }
}
