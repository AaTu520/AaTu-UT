package cn.blockforge.aatu_ut.enhancement;

import cn.blockforge.champions_level.ChampionsLevel;
import cn.blockforge.champions_level.api.BossLootEvent;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

public final class GuardianDropsCompat {
    private static final Method GUARDIAN_DROPS = findGuardianDrops();

    private GuardianDropsCompat() {}

    public static List<ItemStack> get(BossLootEvent event) {
        if (GUARDIAN_DROPS == null) return List.of();
        try {
            Object result = GUARDIAN_DROPS.invoke(event);
            if (result instanceof List<?> drops) {
                return drops.stream().filter(ItemStack.class::isInstance).map(ItemStack.class::cast).toList();
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            ChampionsLevel.LOGGER.error("Could not read guardian drops; leaving them unchanged", exception);
        }
        return List.of();
    }

    private static Method findGuardianDrops() {
        try {
            return BossLootEvent.class.getMethod("guardianDrops");
        } catch (ReflectiveOperationException | SecurityException | LinkageError exception) {
            ChampionsLevel.LOGGER.warn("Guardian armor enhancement requires the matching Champions Level update");
            return null;
        }
    }
}
