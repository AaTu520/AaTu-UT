package cn.blockforge.aatu_ut.mixin;

import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface CombinerAccess {
    @Accessor("access") ContainerLevelAccess aatuUt$access();
}
