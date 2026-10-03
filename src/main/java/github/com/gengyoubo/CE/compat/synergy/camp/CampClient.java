package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class CampClient {
    static void initialize(IEventBus bus) { bus.addListener(CampClient::setup); MinecraftForge.EVENT_BUS.register(CampClient.class); }
    @SubscribeEvent public static void render(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity().getPersistentData().getBoolean("changede_camp_hidden")) event.setCanceled(true);
    }
    private static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> MenuScreens.register(LatexCampfireCompat.MENU.get(), LatexCampfireScreen::new)); }
    static void snapshot(CampNetwork.Snapshot packet) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof LatexCampfireMenu menu && menu.containerId == packet.menu() && packet.view() != null) {
            menu.view = packet.view();
            if (Minecraft.getInstance().screen instanceof LatexCampfireScreen screen) screen.refresh();
        }
    }
    static void visibility(CampNetwork.Visibility packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var entity = level.getEntity(packet.entity());
        if (entity != null && entity.getUUID().equals(packet.id())) entity.getPersistentData().putBoolean("changede_camp_hidden", packet.hidden());
    }
}
