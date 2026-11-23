package cn.blockforge.aatu_ut.enhancement;

import java.util.List;
import java.util.UUID;

import cn.blockforge.aatu_exup.AttributeEnhancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Tracks one recipient's enhanced equipment across both phases of a boss death. */
public final class EquipmentDropLimiter {
    private static final String ROOT = "aatu_ut_equipment_drop_limits";
    private static final TagKey<Item> SWORD_ENCHANTABLE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "enchantable/sword"));
    private static final TagKey<Item> BOW_ENCHANTABLE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "enchantable/bow"));
    private static final TagKey<Item> CROSSBOW_ENCHANTABLE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "enchantable/crossbow"));

    public enum Kind { WEAPON, STAFF, RANGED, ARMOR }

    private final CompoundTag claimed;

    private EquipmentDropLimiter(CompoundTag claimed) {
        this.claimed = claimed;
    }

    public static EquipmentDropLimiter forRecipient(Mob boss, UUID recipient) {
        CompoundTag data = boss.getPersistentData();
        if (!data.contains(ROOT, Tag.TAG_COMPOUND)) data.put(ROOT, new CompoundTag());
        CompoundTag recipients = data.getCompound(ROOT);
        String key = recipient == null ? "shared" : recipient.toString();
        if (!recipients.contains(key, Tag.TAG_COMPOUND)) recipients.put(key, new CompoundTag());
        return new EquipmentDropLimiter(recipients.getCompound(key));
    }

    public static Kind kind(ItemStack stack) {
        if (stack.isEmpty() || EnhancementData.isHat(stack)) return null;
        if (stack.getItem() instanceof ArmorItem) return Kind.ARMOR;
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) return Kind.RANGED;
        if (EnhancementData.isStaff(stack)) return Kind.STAFF;
        if (stack.is(BOW_ENCHANTABLE) || stack.is(CROSSBOW_ENCHANTABLE)) return Kind.RANGED;
        return AttributeEnhancement.isWeapon(stack) || stack.is(SWORD_ENCHANTABLE) ? Kind.WEAPON : null;
    }

    public static boolean hasEnhancement(ItemStack stack) {
        return !EnhancementData.total(stack).isEmpty();
    }

    public static boolean containsEnhancedEquipment(List<ItemStack> drops) {
        return drops.stream().anyMatch(stack -> kind(stack) != null && hasEnhancement(stack));
    }

    public void observeExisting(List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            Kind kind = kind(stack);
            if (kind != null && hasEnhancement(stack)) claim(kind);
        }
    }

    public void reconcile(List<ItemStack> drops) {
        for (Kind kind : Kind.values()) claimed.remove(kind.name());
        observeExisting(drops);
    }

    public boolean available(Kind kind) {
        return kind == null || !claimed.getBoolean(kind.name());
    }

    public boolean hasAny() {
        for (Kind kind : Kind.values()) if (!available(kind)) return true;
        return false;
    }

    public void claim(Kind kind) {
        if (kind != null) claimed.putBoolean(kind.name(), true);
    }

    /** Already-created ordinary drops remain untouched; hats do not consume a category. */
    public boolean acceptGenerated(ItemStack stack) {
        Kind kind = kind(stack);
        if (kind == null || !hasEnhancement(stack)) return true;
        if (!available(kind)) return false;
        claim(kind);
        return true;
    }
}
