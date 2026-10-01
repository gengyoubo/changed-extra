package github.com.gengyoubo.CE.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class LatexSkillScreenEvents {
    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        github.com.gengyoubo.CE.skill.LatexSkills.applyClientFlight(null, 0);
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
        int panelWidth = screen instanceof CreativeModeInventoryScreen ? 195 : 176;
        int panelHeight = screen instanceof CreativeModeInventoryScreen ? 136 : 166;
        event.addListener(Button.builder(Component.translatable("screen.changede.skills.button"), button ->
                Minecraft.getInstance().setScreen(new LatexSkillScreen(screen)))
                .bounds(Math.max(4, (screen.width - panelWidth) / 2 - 40), (screen.height - panelHeight) / 2 + 34, 34, 20).build());
    }
}
