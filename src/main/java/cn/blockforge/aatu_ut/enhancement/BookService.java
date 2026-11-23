package cn.blockforge.aatu_ut.enhancement;

import cn.blockforge.aatu_exup.AttributeData;
import cn.blockforge.aatu_ut.config.UtConfig;
import cn.blockforge.aatu_ut.mixin.CombinerAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import java.util.*;

public final class BookService {
    public record Plan(ItemStack result, int cost, boolean unbreakable) {}
    private BookService() {}
    public static boolean canDismantle(ItemStack source) {
        return source.getCount() == 1 && AttributeData.getSoul(source) == AttributeData.MAX_SOUL
                && !UtConfig.get().extraction.blockedMods.contains(BuiltInRegistries.ITEM.getKey(source.getItem()).getNamespace())
                && EnhancementData.total(source).values().stream().anyMatch(v -> v * UtConfig.get().extraction.ratio > 0);
    }
    public static ItemStack dismantle(ItemStack source) {
        if (!canDismantle(source)) return ItemStack.EMPTY;
        Map<StatType, Double> attributes = EnhancementData.total(source);
        attributes.replaceAll((stat, value) -> value * UtConfig.get().extraction.ratio);
        return BookData.create(attributes);
    }
    public static Plan plan(ItemStack source, ItemStack book) {
        if (source.isEmpty() || source.getCount() != 1 || !BookData.isBook(book)) return null;
        UtConfig.Books c = UtConfig.get().books;
        ItemStack result = source.copy();
        if (BookData.unbreakable(book)) {
            if (!source.isDamageableItem() || source.has(DataComponents.UNBREAKABLE)) return null;
            result.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
            return new Plan(result, c.ordinaryCostLevels, true);
        }
        Map<StatType, Double> totals = EnhancementData.total(source);
        boolean any = false, breakthrough = false;
        for (var entry : BookData.attributes(book).entrySet()) {
            StatType stat = entry.getKey();
            if (!stat.supports(source) || stat.attribute() == null) continue;
            double current = totals.getOrDefault(stat, 0d);
            double bookValue = entry.getValue();
            double ordinary = Math.min(bookValue, Math.max(0, stat.originalLimit() - current));
            double overflow = stat.breakthrough
                    ? Math.min((bookValue - ordinary) * c.breakthroughEfficiency,
                            Math.max(0, stat.cap(true) - current - ordinary))
                    : 0;
            double addition = ordinary + overflow;
            if (addition <= 0) continue;
            boolean broken = overflow > 0;
            EnhancementData.addTransferred(result, stat, addition, broken);
            any = true; breakthrough |= broken;
        }
        return any ? new Plan(result, breakthrough ? c.breakthroughCostLevels : c.ordinaryCostLevels, false) : null;
    }
    public static void updateAnvil(AnvilUpdateEvent event) {
        if (!BookData.isBook(event.getRight())) return;
        Plan plan = plan(event.getLeft(), event.getRight());
        if (plan == null) { event.setCanceled(true); return; }
        event.setOutput(plan.result); event.setCost(plan.cost); event.setMaterialCost(1);
    }

    /** This owns only our book's result slot, including shift-click; vanilla recipes keep vanilla handling. */
    public static ItemStack take(AnvilMenu menu, Player player, boolean quickMove) {
        if (player.level().isClientSide) return ItemStack.EMPTY;
        ItemStack source = menu.getSlot(0).getItem(), book = menu.getSlot(1).getItem();
        BookData.initializeSuccessChance(book);
        Plan plan = plan(source, book);
        if (plan == null || !menu.stillValid(player) || (!player.hasInfiniteMaterials() && player.experienceLevel < plan.cost)) return ItemStack.EMPTY;
        if (menu.getSlot(2).getItem().isEmpty()) return ItemStack.EMPTY;
        ItemStack preview = plan.result;
        ItemStack cursor = menu.getCarried();
        if (quickMove) {
            boolean space = player.getInventory().items.stream().anyMatch(stack -> stack.isEmpty()
                    || (ItemStack.isSameItemSameComponents(stack, preview) && stack.getCount() < stack.getMaxStackSize()));
            // A failed unbreakable roll returns the source item, so reserve a genuinely empty slot.
            if (plan.unbreakable) space = player.getInventory().items.stream().anyMatch(ItemStack::isEmpty);
            if (!space) return ItemStack.EMPTY;
        } else if (!cursor.isEmpty() && (plan.unbreakable || !ItemStack.isSameItemSameComponents(cursor, preview)
                || cursor.getCount() >= cursor.getMaxStackSize())) return ItemStack.EMPTY;
        boolean succeeded = !plan.unbreakable || player.getRandom().nextDouble() < BookData.successChance(book).orElseThrow();
        ItemStack result = succeeded ? preview.copy() : source.copy();
        if (quickMove) {
            ItemStack insertion = result.copy();
            if (!player.getInventory().add(insertion)) return ItemStack.EMPTY;
        } else if (cursor.isEmpty()) menu.setCarried(result.copy());
        else cursor.grow(1);
        if (!player.hasInfiniteMaterials()) player.giveExperienceLevels(-plan.cost);
        ItemStack leftConsumed = source.copy(), rightConsumed = book.copy();
        menu.getSlot(0).set(ItemStack.EMPTY);
        book.shrink(1); menu.getSlot(1).set(book);
        menu.getSlot(2).set(ItemStack.EMPTY);
        menu.setMaximumCost(0);
        float breakChance = CommonHooks.onAnvilRepair(player, result, leftConsumed, rightConsumed);
        ((CombinerAccess) menu).aatuUt$access().execute((level, pos) -> {
            var state = level.getBlockState(pos);
            if (!player.hasInfiniteMaterials() && state.is(BlockTags.ANVIL) && player.getRandom().nextFloat() < breakChance) {
                var damaged = AnvilBlock.damage(state);
                if (damaged == null) { level.removeBlock(pos, false); level.levelEvent(1029, pos, 0); }
                else { level.setBlock(pos, damaged, 2); level.levelEvent(1030, pos, 0); }
            } else level.levelEvent(1030, pos, 0);
        });
        menu.broadcastChanges();
        if (plan.unbreakable) player.displayClientMessage(Component.literal(succeeded ? "无法破坏提升成功" : "提升失败，装备已保留"), true);
        return result;
    }
}
