package github.com.gengyoubo.CE.LP.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Server-confirmed animation, including misses. Dimension/UUID protect against reused entity IDs. */
public record SpearJabAnimationPacket(int entityId, UUID playerId, ResourceLocation dimension) {
    public static void encode(SpearJabAnimationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeUUID(packet.playerId);
        buffer.writeResourceLocation(packet.dimension);
    }
    public static SpearJabAnimationPacket decode(FriendlyByteBuf buffer) {
        return new SpearJabAnimationPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readResourceLocation());
    }
    public static void handle(SpearJabAnimationPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                github.com.gengyoubo.CE.client.LatexSpearAnimations.receiveJab(packet.entityId, packet.playerId, packet.dimension)));
        context.setPacketHandled(true);
    }
}
