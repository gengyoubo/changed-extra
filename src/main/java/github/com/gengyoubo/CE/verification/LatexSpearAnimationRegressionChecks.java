package github.com.gengyoubo.CE.verification;

import github.com.gengyoubo.CE.LP.network.packet.SpearJabAnimationPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;

import java.util.UUID;

/** Opt-in client smoke check, then exit. Does not open a world or run in normal games. */
@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class LatexSpearAnimationRegressionChecks {
    private static boolean finished;

    @SubscribeEvent public static void verify(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("changede.verifySpearAnimations")) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof TitleScreen)) return;
        finished = true;
        try {
            // Force every rendering target through the real Forge/Mixin class loader.
            Class.forName("net.minecraft.client.renderer.ItemInHandRenderer");
            Class.forName("net.minecraft.client.model.HumanoidModel");
            Class.forName("net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator");
            var packet = new SpearJabAnimationPacket(1234, UUID.randomUUID(),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                SpearJabAnimationPacket.encode(packet, buffer);
                if (!packet.equals(SpearJabAnimationPacket.decode(buffer)) || buffer.isReadable()) {
                    throw new AssertionError("Spear animation packet round trip failed");
                }
            } finally { buffer.release(); }
            LogManager.getLogger("changede-spear-animation-check").info("ALL SPEAR ANIMATION CLIENT CHECKS PASSED");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Spear rendering target missing", exception);
        } finally { minecraft.stop(); }
    }
}
