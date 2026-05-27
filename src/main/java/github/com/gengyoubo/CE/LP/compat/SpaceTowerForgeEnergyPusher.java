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
        SpaceTowerEnergyType type = SpaceTowerForgeEnergyStorage.getExtractType(tower);
        if (level == null || level.isClientSide || type == null) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) {
                continue;
            }

            neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(storage -> pushTo(tower, storage, type));
        }
    }

    public static void pull(Level level, BlockPos pos, SpaceTowerAccess tower) {
        SpaceTowerEnergyType type = SpaceTowerForgeEnergyStorage.getReceiveType(tower);
        if (level == null || level.isClientSide || type == null) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) {
                continue;
            }

            neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(storage -> pullFrom(tower, storage, type));
        }
    }

    private static void pushTo(SpaceTowerAccess tower, IEnergyStorage storage, SpaceTowerEnergyType type) {
        if (!storage.canReceive()) {
            return;
        }

        int accepted = storage.receiveEnergy(TRANSFER_PER_TICK, true);
        if (accepted <= 0) {
            return;
        }

        double extracted = tower.extractEnergyAsType(type, accepted);
        if (extracted <= 0.0D) {
            return;
        }

        int sent = storage.receiveEnergy((int)Math.floor(extracted), false);
        if (sent < extracted) {
            tower.refundEnergyAsType(type, extracted - sent);
        }
    }

    private static void pullFrom(SpaceTowerAccess tower, IEnergyStorage storage, SpaceTowerEnergyType type) {
        if (!storage.canExtract() || tower.getEnergyStored() >= tower.getMaxEnergyStored()) {
            return;
        }

        int extractable = storage.extractEnergy(TRANSFER_PER_TICK, true);
        if (extractable <= 0) {
            return;
        }

        int extracted = storage.extractEnergy(extractable, false);
        if (extracted <= 0) {
            return;
        }

        tower.receiveEnergyAsType(type, extracted);
    }
}
