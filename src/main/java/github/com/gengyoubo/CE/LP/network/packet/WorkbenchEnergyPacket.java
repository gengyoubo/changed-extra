package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.LP.client.WorkbenchEnergyClientCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WorkbenchEnergyPacket {
    private final ResourceLocation dimension;
    private final BlockPos pos;
    private final int stored;
    private final int max;

    public WorkbenchEnergyPacket(ResourceLocation dimension, BlockPos pos, int stored, int max) {
        this.dimension = dimension;
        this.pos = pos;
        this.stored = stored;
        this.max = max;
    }

    public static void encode(WorkbenchEnergyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.stored);
        buffer.writeVarInt(packet.max);
    }

    public static WorkbenchEnergyPacket decode(FriendlyByteBuf buffer) {
        return new WorkbenchEnergyPacket(
                buffer.readResourceLocation(),
                buffer.readBlockPos(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(WorkbenchEnergyPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                WorkbenchEnergyClientCache.update(packet.dimension, packet.pos, packet.stored, packet.max)
        ));
        context.setPacketHandled(true);
    }
}
