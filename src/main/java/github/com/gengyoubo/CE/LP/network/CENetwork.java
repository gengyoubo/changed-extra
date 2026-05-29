package github.com.gengyoubo.CE.LP.network;

import github.com.gengyoubo.CE.LP.network.packet.CycleGeneratorRedstoneModePacket;
import github.com.gengyoubo.CE.LP.network.packet.LatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.LP.network.packet.RequestLatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.LP.network.packet.SpaceTowerConfigPacket;
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
        INSTANCE.registerMessage(
                packetId++,
                SpaceTowerConfigPacket.class,
                SpaceTowerConfigPacket::encode,
                SpaceTowerConfigPacket::decode,
                SpaceTowerConfigPacket::handle
        );
        INSTANCE.registerMessage(
                packetId++,
                RequestLatexPaintingPortalPreviewPacket.class,
                RequestLatexPaintingPortalPreviewPacket::encode,
                RequestLatexPaintingPortalPreviewPacket::decode,
                RequestLatexPaintingPortalPreviewPacket::handle
        );
        INSTANCE.registerMessage(
                packetId++,
                LatexPaintingPortalPreviewPacket.class,
                LatexPaintingPortalPreviewPacket::encode,
                LatexPaintingPortalPreviewPacket::decode,
                LatexPaintingPortalPreviewPacket::handle
        );
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}
