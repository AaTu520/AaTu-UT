package cn.blockforge.aatu_ut.mixin;

import cn.blockforge.aatu_ut.enhancement.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AnvilTransactionMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void aatuUt$takeBookResult(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        if (!((Object) this instanceof AnvilMenu menu) || !BookData.isBook(menu.getSlot(1).getItem())) return;
        if (type == ClickType.PICKUP_ALL) { ci.cancel(); return; }
        if (slot != 2) return;
        if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) BookService.take(menu, player, type == ClickType.QUICK_MOVE);
        ci.cancel();
    }
}
