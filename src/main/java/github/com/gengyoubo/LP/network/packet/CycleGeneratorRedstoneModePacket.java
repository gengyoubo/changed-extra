package github.com.gengyoubo.LP.network.packet;

import github.com.gengyoubo.LP.BlockEntity.GeneratorBlockEntity.GeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CycleGeneratorRedstoneModePacket {
    private final BlockPos pos;

    public CycleGeneratorRedstoneModePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(CycleGeneratorRedstoneModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static CycleGeneratorRedstoneModePacket decode(FriendlyByteBuf buffer) {
        return new CycleGeneratorRedstoneModePacket(buffer.readBlockPos());
    }

    public static void handle(CycleGeneratorRedstoneModePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);
            if (blockEntity instanceof GeneratorBlockEntity generatorBlockEntity) {
                generatorBlockEntity.cycleRedstoneMode();
            }
        });
        context.setPacketHandled(true);
    }
}
