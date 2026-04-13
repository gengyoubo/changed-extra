package github.com.gengyoubo.LP.network;

import github.com.gengyoubo.LP.network.packet.CycleGeneratorRedstoneModePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class CENetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("changede", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(
                packetId++,
                CycleGeneratorRedstoneModePacket.class,
                CycleGeneratorRedstoneModePacket::encode,
                CycleGeneratorRedstoneModePacket::decode,
                CycleGeneratorRedstoneModePacket::handle
        );
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}
