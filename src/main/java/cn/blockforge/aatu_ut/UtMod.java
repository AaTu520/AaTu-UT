package cn.blockforge.aatu_ut;

import cn.blockforge.aatu_ut.block.DismantlingBlock;
import cn.blockforge.aatu_ut.config.UtConfig;
import cn.blockforge.aatu_ut.enhancement.*;
import cn.blockforge.aatu_ut.item.AttributeBookItem;
import cn.blockforge.aatu_ut.menu.DismantlingMenu;
import cn.blockforge.champions_level.api.BossLootEvent;
import cn.blockforge.champions_level.api.LateBossDropsEvent;
import cn.blockforge.champions_level.network.LevelNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.*;
import java.util.*;

@Mod(UtMod.ID)
public final class UtMod {
    public static final String ID = "aatu_ut";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredBlock<DismantlingBlock> TABLE = BLOCKS.register("dismantling_table", () -> new DismantlingBlock(BlockBehaviour.Properties.of().strength(3.5f)));
    public static final DeferredItem<BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem(TABLE);
    public static final DeferredItem<AttributeBookItem> ATTRIBUTE_BOOK = ITEMS.register("attribute_book", () -> new AttributeBookItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<MenuType<?>, MenuType<DismantlingMenu>> DISMANTLING_MENU = MENUS.register("dismantling", () -> IMenuTypeExtension.create(DismantlingMenu::new));
    static {
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.literal("AaTu UT")).icon(TABLE_ITEM::toStack)
                .displayItems((params, output) -> {
                    output.accept(TABLE_ITEM);
                    for (StatType stat : StatType.values()) {
                        double maximum = Math.max(stat.originalLimit(), stat.cap(true));
                        if (maximum > 0) output.accept(BookData.create(Map.of(stat, maximum)));
                    }
                    output.accept(BookData.unbreakableBook());
                }).build());
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    public UtMod(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); MENUS.register(bus); TABS.register(bus);
        UtConfig.get();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EnhancementData::modifiers);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EnhancementTooltip::append);
        NeoForge.EVENT_BUS.addListener(BookService::updateAnvil);
        NeoForge.EVENT_BUS.addListener(UtMod::drops);
        NeoForge.EVENT_BUS.addListener(UtMod::lateDrops);
        NeoForge.EVENT_BUS.addListener(UtMod::commands);
    }
    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("aatuut").requires(source -> source.hasPermission(2))
                .then(Commands.literal("config").executes(context -> { LevelNetwork.openConfig(context.getSource().getPlayerOrException(), ID); return 1; })));
    }
    private static void drops(BossLootEvent event) {
        var random = event.boss().getRandom();
        UtConfig c = UtConfig.get();
        EquipmentDropLimiter limiter = EquipmentDropLimiter.forRecipient(event.boss(), event.recipient());
        limiter.observeExisting(event.drops());
        NaturalEquipmentDrops.enhance(event.boss(), event.level(), event.originalBossDrops(), c, limiter);
        NaturalEquipmentDrops.enhanceGuardianArmor(event.boss(), event.level(), GuardianDropsCompat.get(event), c, limiter);
        if (event.level() >= c.books.unbreakableMinimumLevel && random.nextDouble() < c.books.unbreakableDropChance) {
            int minimum = c.books.minimumBookSuccessPercent;
            int maximum = c.books.maximumBookSuccessPercent;
            double successChance = (minimum + random.nextInt(maximum - minimum + 1)) / 100.0;
            event.drops().add(BookData.unbreakableBook(successChance));
        }
        Set<String> awardedGroups = new HashSet<>();
        for (UtConfig.EquipmentTier tier : c.equipmentDrops) {
            if (event.level() < tier.minimumLevel || event.level() > tier.maximumLevel) continue;
            if (!tier.exclusiveGroup.isEmpty() && awardedGroups.contains(tier.exclusiveGroup)) continue;
            List<UtConfig.EquipmentEntry> available = tier.pool.stream()
                    .filter(e -> BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(e.item))).toList();
            if (available.isEmpty() || random.nextDouble() >= tier.chance) continue;
            double roll = random.nextDouble() * available.stream().mapToDouble(e -> e.weight).sum();
            for (UtConfig.EquipmentEntry entry : available) {
                roll -= entry.weight;
                if (roll > 0) continue;
                ItemStack stack = EquipmentDropFactory.create(entry, random, event.boss().registryAccess());
                if (limiter.acceptGenerated(stack)) {
                    event.drops().add(stack);
                }
                if (!tier.exclusiveGroup.isEmpty()) awardedGroups.add(tier.exclusiveGroup);
                break;
            }
        }
        EquipmentPityRewards.schedule(event, limiter, c);
    }

    private static void lateDrops(LateBossDropsEvent event) {
        EquipmentDropLimiter limiter = EquipmentDropLimiter.forRecipient(event.boss(), event.recipient());
        limiter.observeExisting(event.drops());
        NaturalEquipmentDrops.enhance(event.boss(), event.level(), event.drops(), UtConfig.get(), limiter);
        EquipmentPityRewards.scheduleLate(event, limiter, UtConfig.get());
    }
}
