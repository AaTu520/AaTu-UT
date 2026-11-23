package cn.blockforge.aatu_ut.enhancement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Stores independent counters in the world so they survive disconnects and restarts. */
public final class EquipmentPityState extends SavedData {
    private final Map<UUID, Map<String, Integer>> misses = new HashMap<>();

    public static EquipmentPityState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EquipmentPityState::new, EquipmentPityState::load, null), "aatu_ut_equipment_pity");
    }

    private static EquipmentPityState load(CompoundTag tag, HolderLookup.Provider registries) {
        EquipmentPityState state = new EquipmentPityState();
        for (Tag row : tag.getList("players", Tag.TAG_COMPOUND)) {
            CompoundTag player = (CompoundTag) row;
            if (!player.hasUUID("uuid")) continue;
            CompoundTag counters = player.getCompound("counters");
            Map<String, Integer> values = new HashMap<>();
            for (String key : counters.getAllKeys()) {
                int value = counters.getInt(key);
                if (value > 0) values.put(key, value);
            }
            if (!values.isEmpty()) state.misses.put(player.getUUID("uuid"), values);
        }
        return state;
    }

    public int misses(UUID player, String category) {
        return misses.getOrDefault(player, Map.of()).getOrDefault(category, 0);
    }

    public void setMisses(UUID player, String category, int count) {
        int next = Math.max(0, count);
        if (misses(player, category) == next) return;
        if (next == 0) {
            Map<String, Integer> values = misses.get(player);
            if (values != null) {
                values.remove(category);
                if (values.isEmpty()) misses.remove(player);
            }
        } else {
            misses.computeIfAbsent(player, ignored -> new HashMap<>()).put(category, next);
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (var entry : misses.entrySet()) {
            CompoundTag counters = new CompoundTag();
            entry.getValue().forEach(counters::putInt);
            CompoundTag player = new CompoundTag();
            player.putUUID("uuid", entry.getKey());
            player.put("counters", counters);
            players.add(player);
        }
        tag.put("players", players);
        return tag;
    }
}
