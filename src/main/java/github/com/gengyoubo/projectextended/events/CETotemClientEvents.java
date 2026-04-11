package github.com.gengyoubo.projectextended.events;

import github.com.gengyoubo.projectextended.PTotemOfUndying;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CETotemClientEvents {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                return 0xFFFFFF;
            }
            if (tintIndex != 1) {
                return 0xFFFFFF;
            }
            Minecraft minecraft = Minecraft.getInstance();
            long time = minecraft.level != null ? minecraft.level.getGameTime() : System.currentTimeMillis() / 50L;

            if ((time % 4) < 2) {
                float hue = (time % 10L) / 10.0F;
                return Mth.hsvToRgb(hue, 1.0F, 1.0F);
            } else {
                return 0xFFFFFF;
            }
        }, PTotemOfUndying.MATTER_TOTEM_OF_UNDYING_TRUE.get());
    }
}
