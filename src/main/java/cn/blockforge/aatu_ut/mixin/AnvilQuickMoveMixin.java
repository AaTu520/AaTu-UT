package cn.blockforge.aatu_ut.mixin;

import cn.blockforge.aatu_ut.enhancement.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCombinerMenu.class)
public abstract class AnvilQuickMoveMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void aatuUt$shiftBookResult(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof AnvilMenu menu && index == 2 && BookData.isBook(menu.getSlot(1).getItem())) cir.setReturnValue(BookService.take(menu, player, true));
    }
}
