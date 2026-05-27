package github.com.gengyoubo.CE.LP.compat;

import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.SpaceTowerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public final class SpaceTowerForgeEnergyPusher {
    private static final int TRANSFER_PER_TICK = 1_000;

    private SpaceTowerForgeEnergyPusher() {
    }

    public static void push(Level level, BlockPos pos, SpaceTowerAccess tower) {
        if (level == null || level.isClientSide || tower.getMode(SpaceTowerEnergyType.J) != IOType.OUTPUT) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) {
                continue;
            }

            neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(storage -> pushTo(tower, storage));
        }
    }

    private static void pushTo(SpaceTowerAccess tower, IEnergyStorage storage) {
        if (!storage.canReceive()) {
            return;
        }

        int accepted = storage.receiveEnergy(TRANSFER_PER_TICK, true);
        if (accepted <= 0) {
            return;
        }

        double extracted = tower.extractEnergyAsType(SpaceTowerEnergyType.J, accepted);
        if (extracted <= 0.0D) {
            return;
        }

        int sent = storage.receiveEnergy((int)Math.floor(extracted), false);
        if (sent < extracted) {
            tower.refundEnergyAsType(SpaceTowerEnergyType.J, extracted - sent);
        }
    }
}
