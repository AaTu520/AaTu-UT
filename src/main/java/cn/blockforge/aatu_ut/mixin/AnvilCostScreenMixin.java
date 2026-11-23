package cn.blockforge.aatu_ut.mixin;

import cn.blockforge.aatu_ut.enhancement.BookData;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(AnvilScreen.class)
public abstract class AnvilCostScreenMixin {
    // Only our recipe bypasses the visual 40-level warning; its actual server cost remains 80.
    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 40))
    private int aatuUt$warningThreshold(int threshold) {
        AnvilMenu menu = ((AnvilScreen) (Object) this).getMenu();
        return BookData.isBook(menu.getSlot(1).getItem()) ? Integer.MAX_VALUE : threshold;
    }
}
