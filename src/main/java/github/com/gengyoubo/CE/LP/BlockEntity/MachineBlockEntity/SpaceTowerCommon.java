package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

final class SpaceTowerCommon {
    private SpaceTowerCommon() {
    }

    static void initializeDefaultModes(Map<SpaceTowerEnergyType, IOType> modes) {
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            modes.put(type, defaultMode(type));
        }
    }

    static IOType defaultMode(SpaceTowerEnergyType type) {
        return type == SpaceTowerEnergyType.LP ? IOType.OUTPUT : IOType.INPUT;
    }

    static void pushLpOutput(Level level, BlockPos pos, ILatexEnergyHandler source,
                             Function<SpaceTowerEnergyType, IOType> modeGetter, IntConsumer refundLp) {
        if (level == null || level.isClientSide || modeGetter.apply(SpaceTowerEnergyType.LP) != IOType.OUTPUT) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor instanceof ILatexEnergyHandler handler) {
                int extracted = source.extractEnergy(source.getEnergyStored(), dir);
                int received = handler.receiveEnergy(extracted, dir.getOpposite());
                if (received < extracted) {
                    refundLp.accept(extracted - received);
                }
            }
        }
    }

    static int getCeCostPerMinute(int rpm, int stressUnits) {
        int rpmExtra = Math.max(0, (roundToStep(rpm) - SpaceTowerBlockEntity.DEFAULT_CE_RPM) / 2);
        int suExtra = Math.max(0, (stressUnits - SpaceTowerBlockEntity.DEFAULT_CE_SU + 3) / 4);
        return SpaceTowerBlockEntity.BASE_CE_COST_PER_MINUTE + rpmExtra + suExtra;
    }

    static int roundToStep(int value) {
        return Math.round(value / (float) 2) * 2;
    }

    static void sync(BlockEntity blockEntity) {
        blockEntity.setChanged();
        Level level = blockEntity.getLevel();
        if (level != null) {
            level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        }
    }

    static void notifyNeighbors(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(blockEntity.getBlockPos(), blockEntity.getBlockState().getBlock());
        }
    }

    static void saveModes(CompoundTag tag, Function<SpaceTowerEnergyType, IOType> modeGetter) {
        CompoundTag modesTag = new CompoundTag();
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            modesTag.putString(type.name(), modeGetter.apply(type).name());
        }
        tag.put("Modes", modesTag);
    }

    static void loadModes(CompoundTag tag, Map<SpaceTowerEnergyType, IOType> modes) {
        if (!tag.contains("Modes")) {
            return;
        }

        CompoundTag modesTag = tag.getCompound("Modes");
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            if (modesTag.contains(type.name())) {
                try {
                    modes.put(type, IOType.valueOf(modesTag.getString(type.name())));
                } catch (IllegalArgumentException ignored) {
                    modes.put(type, defaultMode(type));
                }
            }
        }
    }
}
