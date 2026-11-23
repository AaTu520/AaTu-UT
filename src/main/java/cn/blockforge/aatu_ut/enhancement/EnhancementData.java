package cn.blockforge.aatu_ut.enhancement;

import cn.blockforge.aatu_exup.AttributeData;
import cn.blockforge.aatu_exup.WeaponAttributeSlots;
import cn.blockforge.aatu_ut.UtMod;
import cn.blockforge.aatu_ut.config.UtConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import java.util.*;

public final class EnhancementData {
    private static final String ROOT = "aatu_ut";
    private static final Class<?> STAFF = staffClass();
    private static final java.lang.reflect.Method FORBIDDEN_SUPPORT = forbiddenSupport();
    private static final boolean HAS_WEAPON_SLOT_RESOLVER = hasWeaponSlotResolver();
    private static volatile long hatsRevision = -1;
    private static volatile Set<String> configuredHats = Set.of();
    private EnhancementData() {}
    public static CompoundTag root(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT); }
    public static void edit(ItemStack stack, java.util.function.Consumer<CompoundTag> editor) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag tag = root.getCompound(ROOT); editor.accept(tag); root.put(ROOT, tag);
        });
    }
    private static Class<?> staffClass() {
        try { return Class.forName("net.spell_engine.api.item.weapon.StaffItem", false, EnhancementData.class.getClassLoader()); }
        catch (ClassNotFoundException ignored) { return null; }
    }
    public static boolean isStaff(ItemStack stack) { return STAFF != null && STAFF.isInstance(stack.getItem()); }
    private static java.lang.reflect.Method forbiddenSupport() {
        try { return Class.forName("cn.blockforge.forbidden_knowledge.ForbiddenEnhancement").getMethod("supportsType", ItemStack.class, String.class); }
        catch (ReflectiveOperationException ignored) { return null; }
    }
    private static boolean hasWeaponSlotResolver() {
        try {
            Class<?> resolver = Class.forName("cn.blockforge.aatu_exup.WeaponAttributeSlots", false, EnhancementData.class.getClassLoader());
            resolver.getMethod("resolve", ItemAttributeModifierEvent.class, Holder.class, EquipmentSlotGroup.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) { return false; }
    }
    public static boolean supportsForbidden(ItemStack stack, String key) {
        if (FORBIDDEN_SUPPORT == null) return isStaff(stack);
        try { return (Boolean) FORBIDDEN_SUPPORT.invoke(null, stack, key); }
        catch (ReflectiveOperationException ignored) { return false; }
    }
    public static boolean isHat(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem)) return false;
        if (stack.has(DataComponents.CUSTOM_DATA) && root(stack).getBoolean("hat")) return true;
        // Glass must be awarded as a hat; other configured blocks keep their existing eligibility.
        return !stack.is(Tags.Items.GLASS_BLOCKS) && isHatDropCandidate(stack);
    }
    public static boolean isHatDropCandidate(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem)) return false;
        if (hatsRevision != UtConfig.STORE.revision()) {
            Set<String> ids = new HashSet<>(UtConfig.get().hatBlocks);
            for (var tier : UtConfig.get().equipmentDrops) for (var entry : tier.pool) {
                if (BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(entry.item)) instanceof BlockItem) ids.add(entry.item);
            }
            configuredHats = Set.copyOf(ids); hatsRevision = UtConfig.STORE.revision();
        }
        return configuredHats.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    public static Map<StatType, Double> base(ItemStack stack) {
        Map<StatType, Double> values = new EnumMap<>(StatType.class);
        for (var entry : AttributeData.getEnhancements(stack)) {
            StatType stat = StatType.find(entry.type()); if (stat != null) values.merge(stat, entry.value(), Double::sum);
        }
        CompoundTag source = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("forbidden_knowledge_enhancements");
        for (Tag raw : source.getList("attributes", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw; StatType stat = StatType.find(entry.getString("type")); double value = entry.getDouble("value");
            if (stat != null && !stat.core() && Double.isFinite(value) && value > 0) values.merge(stat, Math.min(value, stat.originalLimit()), (a, b) -> Math.min(stat.originalLimit(), a + b));
        }
        return values;
    }
    public static double storedExtra(ItemStack stack, StatType stat) {
        double value = root(stack).getCompound("bonuses").getDouble(stat.key);
        return Double.isFinite(value) ? Math.max(0, value) : 0;
    }
    public static double effectiveExtra(ItemStack stack, StatType stat, double base) {
        CompoundTag addon = root(stack);
        return effectiveExtra(addon.getCompound("bonuses"), addon.getCompound("breakthrough"), stat, base);
    }
    static double effectiveExtra(CompoundTag bonuses, CompoundTag flags, StatType stat, double base) {
        double value = bonuses.getDouble(stat.key);
        if (!Double.isFinite(value) || value <= 0) return 0;
        return Math.min(value, Math.max(0, stat.cap(flags.getBoolean(stat.key)) - base));
    }
    public static Map<StatType, Double> total(ItemStack stack) {
        Map<StatType, Double> values = base(stack);
        for (StatType stat : StatType.values()) {
            double existing = values.getOrDefault(stat, 0d);
            double total = existing + effectiveExtra(stack, stat, existing);
            if (total > 0) values.put(stat, total);
        }
        return values;
    }
    public static void addTransferred(ItemStack stack, StatType stat, double value, boolean broken) {
        edit(stack, tag -> {
            CompoundTag bonuses = tag.getCompound("bonuses"); bonuses.putDouble(stat.key, storedExtra(stack, stat) + value); tag.put("bonuses", bonuses);
            if (broken) { CompoundTag flags = tag.getCompound("breakthrough"); flags.putBoolean(stat.key, true); tag.put("breakthrough", flags); }
            if (isHat(stack)) tag.putBoolean("hat", true);
        });
    }
    public static double coreCapacity(ItemStack stack, String key, double requested) {
        StatType stat = StatType.find(key);
        if (stat == null || requested <= 0) return requested;
        double current = total(stack).getOrDefault(stat, 0d);
        return Math.min(requested, Math.max(0, stat.originalLimit() - current));
    }
    public static void addBase(ItemStack stack, StatType stat, double value) {
        if (stat.core()) { AttributeData.add(stack, stat.key, value); return; }
        if (stat.attribute() == null) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag data = tag.getCompound("forbidden_knowledge_enhancements");
            ListTag entries = data.getList("attributes", Tag.TAG_COMPOUND);
            double amount = Math.min(value, stat.originalLimit());
            for (Tag raw : entries) {
                CompoundTag entry = (CompoundTag) raw;
                if (entry.getString("type").equals(stat.key)) { entry.putDouble("value", Math.min(stat.originalLimit(), entry.getDouble("value") + amount)); data.put("attributes", entries); tag.put("forbidden_knowledge_enhancements", data); return; }
            }
            CompoundTag entry = new CompoundTag(); entry.putString("type", stat.key); entry.putDouble("value", amount); entries.add(entry);
            data.put("attributes", entries); tag.put("forbidden_knowledge_enhancements", data);
        });
    }
    public static void modifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.has(DataComponents.CUSTOM_DATA)) return;
        CompoundTag addon = root(stack), bonuses = addon.getCompound("bonuses"), flags = addon.getCompound("breakthrough");
        boolean fallback = !net.neoforged.fml.ModList.get().isLoaded("forbidden_knowledge");
        if (bonuses.isEmpty() && (!fallback || !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("forbidden_knowledge_enhancements"))) return;
        Map<StatType, Double> base = base(stack);
        if (fallback) {
            for (var entry : base.entrySet()) {
                StatType stat = entry.getKey();
                if (!stat.core() && stat.attribute() != null && stat.supports(stack)) {
                    String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().replace(':', '_');
                    event.addModifier(stat.attribute(), new AttributeModifier(UtMod.id("base_" + item + "_" + stat.name().toLowerCase(Locale.ROOT)),
                            entry.getValue(), AttributeModifier.Operation.ADD_VALUE), statSlot(event, EquipmentSlotGroup.HAND, stat));
                }
            }
        }
        EquipmentSlotGroup slot = modifierSlot(stack);
        for (StatType stat : StatType.values()) {
            double amount = effectiveExtra(bonuses, flags, stat, base.getOrDefault(stat, 0d));
            if (amount <= 0 || stat.attribute() == null) continue;
            String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().replace(':', '_');
            event.addModifier(stat.attribute(), new AttributeModifier(UtMod.id("transferred_" + item + "_" + stat.name().toLowerCase(Locale.ROOT)), amount,
                    AttributeModifier.Operation.ADD_VALUE), statSlot(event, slot, stat));
        }
    }
    private static EquipmentSlotGroup modifierSlot(ItemStack stack) {
        if (isHat(stack)) return EquipmentSlotGroup.HEAD;
        if (stack.getItem() instanceof ArmorItem armor) {
            EquipmentSlot slot = stack.getEquipmentSlot();
            return EquipmentSlotGroup.bySlot(slot != null ? slot : armor.getEquipmentSlot());
        }
        return EquipmentSlotGroup.HAND;
    }
    private static EquipmentSlotGroup statSlot(ItemAttributeModifierEvent event, EquipmentSlotGroup defaultSlot, StatType stat) {
        if (defaultSlot != EquipmentSlotGroup.HAND) return defaultSlot;
        EquipmentSlotGroup fallback = stat == StatType.PENETRATION || stat == StatType.SPELL_PENETRATION
                ? EquipmentSlotGroup.MAINHAND : EquipmentSlotGroup.HAND;
        return HAS_WEAPON_SLOT_RESOLVER ? WeaponAttributeSlots.resolve(event, stat.attribute(), fallback) : fallback;
    }
    public static void hatBaseModifiers(ItemAttributeModifierEvent event) {
        for (var entry : base(event.getItemStack()).entrySet()) {
            StatType stat = entry.getKey();
            if (stat.core() && stat.attribute() != null) event.addModifier(stat.attribute(), new AttributeModifier(UtMod.id("hat_" + stat.name().toLowerCase(Locale.ROOT)), entry.getValue(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD);
        }
    }
}
