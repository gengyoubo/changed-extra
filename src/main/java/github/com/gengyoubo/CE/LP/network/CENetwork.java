package github.com.gengyoubo.CE.LP.network;

import github.com.gengyoubo.CE.LP.network.packet.CycleGeneratorRedstoneModePacket;
import github.com.gengyoubo.CE.LP.network.packet.LatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.LP.network.packet.MaidWorkSwitchPacket;
import github.com.gengyoubo.CE.LP.network.packet.RequestLatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.LP.network.packet.RequestWorkbenchEnergyPacket;
import github.com.gengyoubo.CE.LP.network.packet.SpaceTowerConfigPacket;
import github.com.gengyoubo.CE.LP.network.packet.WorkbenchEnergyPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class CENetwork {
    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("changede", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(packetId++, github.com.gengyoubo.CE.skill.SkillTreePacket.FlightState.class,
                github.com.gengyoubo.CE.skill.SkillTreePacket.FlightState::encode,
                github.com.gengyoubo.CE.skill.SkillTreePacket.FlightState::decode,
                github.com.gengyoubo.CE.skill.SkillTreePacket.FlightState::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(packetId++, github.com.gengyoubo.CE.skill.SkillTreePacket.Request.class,
                github.com.gengyoubo.CE.skill.SkillTreePacket.Request::encode,
                github.com.gengyoubo.CE.skill.SkillTreePacket.Request::decode,
                github.com.gengyoubo.CE.skill.SkillTreePacket.Request::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(packetId++, github.com.gengyoubo.CE.skill.SkillTreePacket.Snapshot.class,
                github.com.gengyoubo.CE.skill.SkillTreePacket.Snapshot::encode,
                github.com.gengyoubo.CE.skill.SkillTreePacket.Snapshot::decode,
                github.com.gengyoubo.CE.skill.SkillTreePacket.Snapshot::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
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
        INSTANCE.registerMessage(
                packetId++,
                RequestWorkbenchEnergyPacket.class,
                RequestWorkbenchEnergyPacket::encode,
                RequestWorkbenchEnergyPacket::decode,
                RequestWorkbenchEnergyPacket::handle
        );
        INSTANCE.registerMessage(
                packetId++,
                WorkbenchEnergyPacket.class,
                WorkbenchEnergyPacket::encode,
                WorkbenchEnergyPacket::decode,
                WorkbenchEnergyPacket::handle
        );
        INSTANCE.registerMessage(
                packetId++,
                MaidWorkSwitchPacket.class,
                MaidWorkSwitchPacket::encode,
                MaidWorkSwitchPacket::decode,
                MaidWorkSwitchPacket::handle
        );
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}
