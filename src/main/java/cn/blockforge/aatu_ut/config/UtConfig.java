package cn.blockforge.aatu_ut.config;

import cn.blockforge.champions_level.config.ConfigStore;
import cn.blockforge.aatu_ut.enhancement.StatType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import java.util.*;
import static cn.blockforge.champions_level.config.ConfigStore.*;

public final class UtConfig {
    public static final ConfigStore<UtConfig> STORE = new ConfigStore<>("aatu_ut", UtConfig.class, UtConfig::new, UtConfig::validate,
            List.of("extraction", "books", "hatBlocks", "equipmentDrops", "naturalEquipment", "equipmentRewards"), ignored -> {});
    public Extraction extraction = new Extraction();
    public Books books = new Books();
    public List<String> hatBlocks = new ArrayList<>(List.of("minecraft:light_blue_stained_glass", "minecraft:glass",
            "minecraft:pink_stained_glass", "minecraft:lime_stained_glass", "minecraft:yellow_stained_glass"));
    public List<EquipmentTier> equipmentDrops = EquipmentDefaults.create();
    public NaturalEquipment naturalEquipment = new NaturalEquipment();
    public EquipmentRewards equipmentRewards = new EquipmentRewards();
    public static UtConfig get() { return STORE.get(); }
    public static final class Extraction {
        public double ratio = 0.85;
        public List<String> blockedMods = new ArrayList<>();
    }
    public static final class Books {
        public int ordinaryCostLevels = 15;
        public int breakthroughCostLevels = 80;
        public double breakthroughEfficiency = 0.3;
        public int unbreakableMinimumLevel = 100;
        public double unbreakableDropChance = 0.01;
        public int minimumBookSuccessPercent = 1;
        public int maximumBookSuccessPercent = 30;
        // Retained for books made before their success chance was stored on each copy.
        public double unbreakableSuccessChance = 0.25;
    }
    public static final class EquipmentTier {
        public int minimumLevel = 1;
        public int maximumLevel = 10000;
        public double chance = 0.2;
        public String exclusiveGroup = "";
        public List<EquipmentEntry> pool = new ArrayList<>(List.of(
                new EquipmentEntry("minecraft:iron_sword", "attack_damage", 1, 5),
                new EquipmentEntry("minecraft:iron_chestplate", "armor", 1, 3),
                new EquipmentEntry("minecraft:glass", "armor", 1, 3)));
    }
    public static final class Range {
        public double minimum = 1;
        public double maximum = 3;
        public Range() {}
        public Range(double minimum, double maximum) { this.minimum = minimum; this.maximum = maximum; }
    }
    public static final class NaturalEquipment {
        public boolean enabled = true;
        public List<NaturalEquipmentTier> tiers = NaturalEquipmentDefaults.create();
    }
    public static final class EquipmentRewards {
        public int equipmentPityKills = 15;
    }
    public static final class NaturalEquipmentTier {
        public int minimumLevel = 1;
        public int maximumLevel = 150;
        public Range weaponBonus = new Range(1, 10);
    }
    public static final class EquipmentEntry {
        public String item = "minecraft:iron_sword";
        public double weight = 1;
        public int soul = 3;
        public boolean optional;
        public Map<String, Range> attributes = new LinkedHashMap<>();
        public EquipmentEntry() { attributes.put("attack_damage", new Range(1, 5)); }
        public EquipmentEntry(String item, String stat, double min, double max) { this.item = item; attributes.put(stat, new Range(min, max)); }
    }
    private static void validate(UtConfig c) {
        require(c != null && c.extraction != null && c.books != null && c.hatBlocks != null
                && c.equipmentDrops != null && c.naturalEquipment != null && c.equipmentRewards != null, "缺少配置分组");
        range(c.equipmentRewards.equipmentPityKills, 0, 10000, "强化装备保底次数");
        range(c.extraction.ratio, 0, 1, "提取比例");
        require(c.extraction.blockedMods != null && c.extraction.blockedMods.size() <= 1024, "模组黑名单无效");
        c.extraction.blockedMods.forEach(id -> require(id != null && id.matches("[a-z0-9_.-]{1,100}"), "模组ID无效"));
        range(c.books.ordinaryCostLevels, 1, 10000, "普通经验费用");
        range(c.books.breakthroughCostLevels, 1, 10000, "突破经验费用");
        range(c.books.breakthroughEfficiency, 0.001, 1, "突破效果比例");
        range(c.books.unbreakableMinimumLevel, 1, 10000, "无法破坏最低等级");
        range(c.books.unbreakableDropChance, 0, 1, "无法破坏书掉率");
        range(c.books.minimumBookSuccessPercent, 1, 30, "无法破坏书最低成功率");
        range(c.books.maximumBookSuccessPercent, c.books.minimumBookSuccessPercent, 30, "无法破坏书最高成功率");
        range(c.books.unbreakableSuccessChance, 0, 1, "无法破坏成功率");
        require(c.hatBlocks.size() <= 2048, "帽子列表过长");
        c.hatBlocks.forEach(id -> require(ResourceLocation.tryParse(id) != null
                && BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)) instanceof BlockItem, "帽子方块ID无效：" + id));
        validateNaturalEquipment(c.naturalEquipment);
        require(c.equipmentDrops.size() <= 128, "装备档位过多");
        for (EquipmentTier tier : c.equipmentDrops) {
            require(tier != null && tier.minimumLevel >= 1 && tier.maximumLevel >= tier.minimumLevel && tier.maximumLevel <= 10000, "装备等级区间无效");
            require(tier.exclusiveGroup != null && tier.exclusiveGroup.matches("[a-z0-9_.-]{0,100}"), "装备互斥组无效");
            range(tier.chance, 0, 1, "装备掉率");
            require(tier.pool != null && tier.pool.size() <= 256, "装备池无效");
            for (EquipmentEntry e : tier.pool) {
                require(e != null && e.item != null && ResourceLocation.tryParse(e.item) != null, "装备ID无效");
                range(e.weight, 0.001, 100000, "装备权重"); range(e.soul, 0, 99, "注灵数");
                if (e.optional && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(e.item))) continue;
                net.minecraft.world.item.ItemStack target = new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.item)));
                require(!target.isEmpty(), "装备不存在或为空气：" + e.item);
                require(e.attributes != null && e.attributes.size() <= StatType.values().length, "装备属性无效");
                for (var entry : e.attributes.entrySet()) {
                    StatType stat = StatType.find(entry.getKey());
                    require(stat != null && entry.getValue() != null, "属性ID无效：" + entry.getKey());
                    boolean armorType = stat == StatType.ARMOR || stat == StatType.TOUGHNESS || stat == StatType.HEALTH || stat == StatType.SPEED;
                    require(target.getItem() instanceof BlockItem ? armorType : stat.supports(target), "装备不支持此属性：" + e.item + " / " + entry.getKey());
                    require(stat.attribute() != null, "此属性的前置模组未安装：" + entry.getKey());
                    range(entry.getValue().minimum, 0, stat.originalLimit(), "属性最小值");
                    range(entry.getValue().maximum, entry.getValue().minimum, stat.originalLimit(), "属性最大值");
                }
            }
        }
    }
    private static void validateNaturalEquipment(NaturalEquipment equipment) {
        require(equipment.tiers != null && equipment.tiers.size() <= 128, "原有装备等级档位无效");
        List<NaturalEquipmentTier> ordered = new ArrayList<>(equipment.tiers);
        require(ordered.stream().allMatch(Objects::nonNull), "原有装备等级档位无效");
        ordered.sort(Comparator.comparingInt(tier -> tier.minimumLevel));
        int previousMaximum = 0;
        for (NaturalEquipmentTier tier : ordered) {
            require(tier.minimumLevel > previousMaximum && tier.maximumLevel >= tier.minimumLevel
                    && tier.maximumLevel <= 10000, "原有装备等级区间无效或重叠");
            validateNaturalRange(tier.weaponBonus, "武器强化");
            previousMaximum = tier.maximumLevel;
        }
    }
    private static void validateNaturalRange(Range value, String name) {
        require(value != null, name + "范围无效");
        range(value.minimum, 0, 10000, name + "最小值");
        range(value.maximum, value.minimum, 10000, name + "最大值");
    }
}
