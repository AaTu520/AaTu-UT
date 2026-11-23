package cn.blockforge.aatu_ut.enhancement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.blockforge.aatu_ut.config.UtConfig;
import cn.blockforge.champions_level.api.BossLootEvent;
import cn.blockforge.champions_level.api.LateBossDropsEvent;
import cn.blockforge.champions_level.boss.BossService;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EquipmentPityRewards {
    private static final String SHARED = "equipment";
    private static final boolean REWARD_CALLBACKS_AVAILABLE = rewardCallbacksAvailable();

    private EquipmentPityRewards() {
    }

    public static void schedule(BossLootEvent event, EquipmentDropLimiter limiter, UtConfig config) {
        if (!REWARD_CALLBACKS_AVAILABLE) return;
        event.whenPrepared(() -> limiter.reconcile(event.drops()));
        if (!BossService.arenaBoss(event.boss()) || event.recipient() == null
                || config.equipmentRewards.equipmentPityKills <= 0) return;
        event.whenPrepared(() -> prepare(event, limiter, config));
    }

    private static void prepare(BossLootEvent event, EquipmentDropLimiter limiter, UtConfig config) {
        MinecraftServer server = event.boss().getServer();
        UUID recipient = event.recipient();
        if (server == null || recipient == null) return;
        EquipmentPityState state = EquipmentPityState.get(server);
        int threshold = config.equipmentRewards.equipmentPityKills;
        int missed = state.misses(recipient, SHARED);
        if (!limiter.hasAny() && missed >= threshold - 1) {
            ItemStack guaranteed = guarantee(event.level(), config, event.boss().getRandom(), limiter, server.registryAccess());
            if (!guaranteed.isEmpty()) event.drops().add(guaranteed);
            else cn.blockforge.champions_level.ChampionsLevel.LOGGER.warn(
                    "Could not create guaranteed enhanced equipment for {} at level {}; keeping the pity counter", recipient, event.level());
        }

        int next = limiter.hasAny() ? 0 : Math.min(threshold - 1, Math.min(missed, threshold - 1) + 1);
        event.whenCommitted(() -> state.setMisses(recipient, SHARED, next));
    }

    public static void scheduleLate(LateBossDropsEvent event, EquipmentDropLimiter limiter, UtConfig config) {
        if (!REWARD_CALLBACKS_AVAILABLE || !BossService.arenaBoss(event.boss()) || event.recipient() == null
                || config.equipmentRewards.equipmentPityKills <= 0) return;
        event.whenPrepared(() -> {
            limiter.observeExisting(event.drops());
            MinecraftServer server = event.boss().getServer();
            if (!EquipmentDropLimiter.containsEnhancedEquipment(event.drops()) || server == null) return;
            EquipmentPityState state = EquipmentPityState.get(server);
            event.whenCommitted(() -> state.setMisses(event.recipient(), SHARED, 0));
        });
    }

    private static boolean rewardCallbacksAvailable() {
        try {
            BossLootEvent.class.getMethod("whenPrepared", Runnable.class);
            BossLootEvent.class.getMethod("whenCommitted", Runnable.class);
            LateBossDropsEvent.class.getMethod("whenPrepared", Runnable.class);
            LateBossDropsEvent.class.getMethod("whenCommitted", Runnable.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            cn.blockforge.champions_level.ChampionsLevel.LOGGER.warn(
                    "Enhanced-equipment pity is unavailable: Champions Level must be updated with AaTu UT");
            return false;
        }
    }

    private static ItemStack guarantee(int level, UtConfig config, RandomSource random, EquipmentDropLimiter limiter,
                                       RegistryAccess registries) {
        List<UtConfig.EquipmentEntry> candidates = new ArrayList<>();
        for (UtConfig.EquipmentTier tier : config.equipmentDrops) {
            if (level < tier.minimumLevel || level > tier.maximumLevel || tier.chance <= 0) continue;
            for (UtConfig.EquipmentEntry entry : tier.pool) {
                ResourceLocation id = ResourceLocation.tryParse(entry.item);
                if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                EquipmentDropLimiter.Kind kind = EquipmentDropLimiter.kind(stack);
                if (kind == null || !limiter.available(kind) || !hasUsableAttribute(stack, entry)) continue;
                candidates.add(entry);
            }
        }

        while (!candidates.isEmpty()) {
            double weight = candidates.stream().mapToDouble(entry -> entry.weight).sum();
            if (weight <= 0) break;
            double roll = random.nextDouble() * weight;
            int selected = candidates.size() - 1;
            for (int index = 0; index < candidates.size(); index++) {
                roll -= candidates.get(index).weight;
                if (roll < 0) { selected = index; break; }
            }
            ItemStack stack = EquipmentDropFactory.create(candidates.remove(selected), random, registries);
            if (EquipmentDropLimiter.hasEnhancement(stack) && limiter.acceptGenerated(stack)) return stack;
        }
        return fallbackSword(level, config, random, limiter);
    }

    private static boolean hasUsableAttribute(ItemStack stack, UtConfig.EquipmentEntry entry) {
        for (var attribute : entry.attributes.entrySet()) {
            StatType stat = StatType.find(attribute.getKey());
            if (stat != null && stat.attribute() != null && stat.supports(stack)
                    && stat.originalLimit() > 0 && attribute.getValue().maximum > 0) return true;
        }
        return false;
    }

    private static ItemStack fallbackSword(int level, UtConfig config, RandomSource random,
                                           EquipmentDropLimiter limiter) {
        if (!limiter.available(EquipmentDropLimiter.Kind.WEAPON)) return ItemStack.EMPTY;
        double limit = StatType.ATTACK.originalLimit();
        if (limit <= 0) return ItemStack.EMPTY;
        UtConfig.Range bonus = config.naturalEquipment.tiers.stream()
                .filter(tier -> level >= tier.minimumLevel && level <= tier.maximumLevel)
                .map(tier -> tier.weaponBonus).findFirst().orElse(new UtConfig.Range(1, 1));
        if (bonus.maximum <= 0) return ItemStack.EMPTY;
        double amount = bonus.minimum + random.nextDouble() * (bonus.maximum - bonus.minimum);
        if (amount <= 0) amount = Math.min(1, bonus.maximum);
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        EnhancementData.addBase(stack, StatType.ATTACK, Math.min(limit, amount));
        return EquipmentDropLimiter.hasEnhancement(stack) && limiter.acceptGenerated(stack) ? stack : ItemStack.EMPTY;
    }
}
