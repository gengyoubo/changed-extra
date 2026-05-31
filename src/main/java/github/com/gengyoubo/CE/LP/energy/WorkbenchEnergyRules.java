package github.com.gengyoubo.CE.LP.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class WorkbenchEnergyRules {
    public static final int NORMAL_CAPACITY = 30000;
    public static final int NORMAL_COST = 30;
    public static final int ADVANCED_CAPACITY = 100000;
    public static final int ADVANCED_COST = 100;
    public static final String NBT_KEY = "ChangedEWorkbenchEnergy";

    private WorkbenchEnergyRules() {
    }

    public static int capacityFor(Object owner) {
        return isAdvanced(owner) ? ADVANCED_CAPACITY : NORMAL_CAPACITY;
    }

    public static int costFor(Object owner) {
        return isAdvanced(owner) ? ADVANCED_COST : NORMAL_COST;
    }

    public static boolean consume(Object owner, boolean simulate) {
        if (owner instanceof WorkbenchEnergyHolder holder) {
            return holder.changede$getWorkbenchEnergy().consumeEnergy(costFor(owner), simulate);
        }
        return false;
    }

    public static boolean canConsume(Object owner) {
        return consume(owner, true);
    }

    public static boolean consumeInfuserCraft(Level level, BlockPos pos, boolean simulate) {
        if (level == null || level.isClientSide) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WorkbenchEnergyHolder holder) {
            return holder.changede$getWorkbenchEnergy().consumeEnergy(NORMAL_COST, simulate);
        }
        return false;
    }

    private static boolean isAdvanced(Object owner) {
        String name = owner.getClass().getName();
        return name.endsWith("AdvancedUnifuserBlockEntity") || name.endsWith("AdvancedCatalyzerBlockEntity");
    }
}
