package cn.blockforge.aatu_ut.menu;

import cn.blockforge.aatu_ut.UtMod;
import cn.blockforge.aatu_ut.enhancement.BookService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class DismantlingMenu extends AbstractContainerMenu {
    private final SimpleContainer input = new SimpleContainer(1);
    private final SimpleContainer output = new SimpleContainer(1);
    private final Player owner;
    private final BlockPos pos;
    public DismantlingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos()); }
    public DismantlingMenu(int id, Inventory inventory, BlockPos pos) {
        super(UtMod.DISMANTLING_MENU.get(), id);
        owner = inventory.player; this.pos = pos.immutable();
        input.addListener(container -> update());
        addSlot(new Slot(input, 0, 44, 34) { @Override public int getMaxStackSize() { return 1; } });
        addSlot(new Slot(output, 0, 116, 34) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) {
                if (player.level().isClientSide) return !output.getItem(0).isEmpty();
                ItemStack current = BookService.dismantle(input.getItem(0));
                return !current.isEmpty() && ItemStack.isSameItemSameComponents(current, output.getItem(0));
            }
            @Override public void onTake(Player player, ItemStack stack) {
                input.removeItem(0, 1); output.setItem(0, ItemStack.EMPTY); super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }
    private void update() {
        if (!owner.level().isClientSide) { output.setItem(0, BookService.dismantle(input.getItem(0))); broadcastChanges(); }
    }
    @Override public boolean stillValid(Player player) {
        return player.canInteractWithBlock(pos, 4) && player.level().getBlockState(pos).is(UtMod.TABLE.get());
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) { clearContainer(player, input); output.setItem(0, ItemStack.EMPTY); }
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index == 1) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index == 0) {
            if (!moveItemStackTo(stack, 2, slots.size(), false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
