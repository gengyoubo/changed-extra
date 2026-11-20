package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyHolder;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyRules;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergySync;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyStorage;
import github.com.gengyoubo.CE.LP.energy.WorkbenchRecipeProgress;
import net.foxyas.changedaddon.block.entity.UnifuserBlockEntity;
import net.foxyas.changedaddon.recipe.UnifuserRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UnifuserBlockEntity.class, remap = false)
public abstract class UnifuserEnergyMixin implements WorkbenchEnergyHolder, ILatexEnergyHandler {
    @Unique private WorkbenchEnergyStorage changed_extra$energy;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void changed_extra$initEnergy(CallbackInfo ci) {
        changed_extra$ensureEnergy();
    }

    @Unique
    private WorkbenchEnergyStorage changed_extra$ensureEnergy() {
        if (changed_extra$energy == null) {
            changed_extra$energy = new WorkbenchEnergyStorage(WorkbenchEnergyRules.capacityFor(this), this::changed_extra$markEnergyChanged);
        }
        return changed_extra$energy;
    }

    @Inject(method = {"load", "m_142466_"}, at = @At("TAIL"))
    private void changed_extra$loadEnergy(CompoundTag tag, CallbackInfo ci) {
        changed_extra$ensureEnergy().setEnergyStored(tag.getInt(WorkbenchEnergyRules.NBT_KEY));
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("TAIL"))
    private void changed_extra$saveEnergy(CompoundTag tag, CallbackInfo ci) {
        tag.putInt(WorkbenchEnergyRules.NBT_KEY, changed_extra$ensureEnergy().getEnergyStored());
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void changed_extra$requireEnergy(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (!level.isClientSide && blockEntity instanceof UnifuserBlockEntity unifuser && changed_extra$willProgress(level, blockEntity, unifuser)) {
            if (!WorkbenchEnergyRules.consume(unifuser, false)) {
                ci.cancel();
            }
        }
    }

    @Override
    public WorkbenchEnergyStorage changed_extra$getWorkbenchEnergy() {
        return changed_extra$ensureEnergy();
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        return changed_extra$ensureEnergy().receiveEnergy(Math.max(0, amount), false);
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return changed_extra$ensureEnergy().getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return changed_extra$ensureEnergy().getMaxEnergyStored();
    }

    @Unique
    private static boolean changed_extra$willProgress(Level level, BlockEntity blockEntity, UnifuserBlockEntity unifuser) {
        WorkbenchRecipeProgress.Context context = WorkbenchRecipeProgress.context(
                level,
                blockEntity,
                unifuser.tickCount,
                unifuser.startRecipe
        );
        if (context == null) {
            return false;
        }

        IItemHandlerModifiable handler = context.handler();
        ItemStack inputA = handler.getStackInSlot(0);
        ItemStack inputB = handler.getStackInSlot(1);
        ItemStack inputC = handler.getStackInSlot(2);
        if (inputA.isEmpty() && inputB.isEmpty() && inputC.isEmpty()) {
            return false;
        }

        UnifuserRecipe recipe = UnifuserBlockEntity.findRecipe(context.level(), inputA, inputB, inputC);
        return recipe != null && WorkbenchRecipeProgress.canAcceptResult(
                handler.getStackInSlot(3),
                recipe.getResultItem(level.registryAccess())
        );
    }

    @Unique
    private void changed_extra$markEnergyChanged() {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        WorkbenchEnergySync.markChangedAndSync(blockEntity, changed_extra$ensureEnergy());
    }
}
