package cn.blockforge.aatu_ut.client;

import cn.blockforge.aatu_ut.UtMod;
import cn.blockforge.aatu_ut.enhancement.BookData;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = UtMod.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UtClient {
    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(UtMod.DISMANTLING_MENU.get(), DismantlingScreen::new);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                UtMod.ATTRIBUTE_BOOK.get(),
                UtMod.id("unbreakable"),
                (stack, level, entity, seed) -> BookData.unbreakable(stack) ? 1.0F : 0.0F));
    }
}
