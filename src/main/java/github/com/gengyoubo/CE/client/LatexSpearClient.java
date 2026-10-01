package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.SpearJabPacket;
import github.com.gengyoubo.CE.init.CEItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class LatexSpearClient {
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (!event.isAttack() || player == null || !player.getMainHandItem().is(CEItem.LATEX_SPEAR.get())) return;
        event.setCanceled(true);
        boolean ready = !player.isUsingItem() && !player.getCooldowns().isOnCooldown(CEItem.LATEX_SPEAR.get());
        event.setSwingHand(ready);
        if (ready) {
            CENetwork.INSTANCE.sendToServer(new SpearJabPacket());
            player.resetAttackStrengthTicker();
            player.getCooldowns().addCooldown(CEItem.LATEX_SPEAR.get(), github.com.gengyoubo.CE.items.SpearCombatRules.JAB_COOLDOWN);
        }
    }
    @Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Models {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(CEItem.LATEX_SPEAR.get(),
                    ResourceLocation.fromNamespaceAndPath("changede", "charging"),
                    (stack, level, entity, seed) -> {
                        // MC-302895: the exact stack must be active, rather than either hand containing a spear.
                        if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) return 0;
                        int ticks = stack.getUseDuration() - entity.getUseItemRemainingTicks()
                                - github.com.gengyoubo.CE.items.SpearCombatRules.WARMUP;
                        if (ticks > github.com.gengyoubo.CE.items.SpearCombatRules.DISENGAGED) return 3;
                        return ticks > github.com.gengyoubo.CE.items.SpearCombatRules.TIRED ? 2 : 1;
                    }));
        }
    }
}
