package github.com.gengyoubo.CE.LP.energy;

import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.WorkbenchEnergyPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public final class WorkbenchEnergySync {
    private static final double SYNC_DISTANCE_SQR = 64.0D * 64.0D;

    private WorkbenchEnergySync() {
    }

    public static void sync(BlockEntity blockEntity, WorkbenchEnergyStorage energy) {
        sync(blockEntity, energy.getEnergyStored(), energy.getMaxEnergyStored());
    }

    public static void sync(BlockEntity blockEntity, int stored, int max) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        WorkbenchEnergyPacket packet = new WorkbenchEnergyPacket(level.dimension().location(), pos, stored, max);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= SYNC_DISTANCE_SQR) {
                CENetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    public static void markChangedAndSync(BlockEntity blockEntity, WorkbenchEnergyStorage energy) {
        blockEntity.setChanged();
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, 3);
        sync(blockEntity, energy);
    }
}
