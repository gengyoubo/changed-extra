package github.com.gengyoubo.CE.compat.jer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class JerOreClientState {
    private static final List<OreGenerationData> pending = new ArrayList<>();
    private static List<OreGenerationData> snapshot = List.of();
    private JerOreClientState() {}

    public static List<OreGenerationData> snapshot() { return snapshot; }

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        pending.clear();
        snapshot = List.of();
        if (ModList.get().isLoaded("jeresources") && JerOreNetwork.CHANNEL.isRemotePresent(event.getConnection())) {
            JerOreNetwork.CHANNEL.sendToServer(new JerOreNetwork.Request());
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        pending.clear();
        snapshot = List.of();
        if (ModList.get().isLoaded("jeresources")) JerWorldGenCompat.clear();
    }

    public static void receive(JerOreNetwork.Batch packet) {
        if (!ModList.get().isLoaded("jeresources")) return;
        if (packet.first()) pending.clear();
        if (pending.size() + packet.entries().size() > 16384) throw new IllegalArgumentException("Too many ore charts");
        pending.addAll(packet.entries());
        if (packet.last()) {
            snapshot = List.copyOf(pending);
            pending.clear();
            JerWorldGenCompat.refresh();
        }
    }
}
