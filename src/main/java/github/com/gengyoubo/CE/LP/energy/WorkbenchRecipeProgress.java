package github.com.gengyoubo.CE.LP.energy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;

public final class WorkbenchRecipeProgress {
    private WorkbenchRecipeProgress() {
    }

    public static Context context(Level level, BlockEntity blockEntity, int tickCount, boolean startRecipe) {
        if (!(level instanceof ServerLevel serverLevel) || tickCount < 5 || !startRecipe) {
            return null;
        }

        IItemHandlerModifiable handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .filter(IItemHandlerModifiable.class::isInstance)
                .map(IItemHandlerModifiable.class::cast)
                .orElse(null);
        return handler == null ? null : new Context(serverLevel, handler);
    }

    public static boolean canAcceptResult(ItemStack output, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), result.getMaxStackSize());
    }

    public record Context(ServerLevel level, IItemHandlerModifiable handler) {
    }
}
