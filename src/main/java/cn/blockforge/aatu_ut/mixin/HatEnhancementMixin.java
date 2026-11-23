package cn.blockforge.aatu_ut.mixin;

import cn.blockforge.aatu_exup.AttributeEnhancement;
import cn.blockforge.aatu_ut.enhancement.EnhancementData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = AttributeEnhancement.class, remap = false)
public abstract class HatEnhancementMixin {
    @Inject(method = "isArmor", at = @At("HEAD"), cancellable = true)
    private static void aatuUt$hatAsArmor(ItemStack stack, CallbackInfoReturnable<Boolean> cir) { if (EnhancementData.isHat(stack)) cir.setReturnValue(true); }
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private static void aatuUt$headSlot(ItemAttributeModifierEvent event, CallbackInfo ci) {
        if (EnhancementData.isHat(event.getItemStack())) { EnhancementData.hatBaseModifiers(event); ci.cancel(); }
    }
}
