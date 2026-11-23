package cn.blockforge.aatu_ut.mixin;

import cn.blockforge.aatu_ut.enhancement.EnhancementData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "cn.blockforge.forbidden_knowledge.ForbiddenAttributeData", remap = false)
public abstract class ForbiddenCapacityMixin {
    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static double aatuUt$remaining(double value, ItemStack stack, String type, double requested) { return EnhancementData.coreCapacity(stack, type, value); }
}
