package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyHolder;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyRules;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergySync;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyStorage;
import net.ltxprogrammer.changed.block.entity.PurifierBlockEntity;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PurifierBlockEntity.class, remap = false)
public abstract class PurifierEnergyMixin extends BaseContainerBlockEntity implements WorkbenchEnergyHolder, ILatexEnergyHandler {
    @Unique private WorkbenchEnergyStorage changed_extra$energy;

    protected PurifierEnergyMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void changed_extra$initEnergy(CallbackInfo ci) {
        changed_extra$ensureEnergy();
    }

    @Unique
    private WorkbenchEnergyStorage changed_extra$ensureEnergy() {
        if (changed_extra$energy == null) {
            changed_extra$energy = new WorkbenchEnergyStorage(WorkbenchEnergyRules.NORMAL_CAPACITY, this::changed_extra$markEnergyChanged);
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
    private static void changed_extra$requireEnergy(Level level, BlockPos pos, BlockState state, PurifierBlockEntity purifier, CallbackInfo ci) {
        if (!level.isClientSide && changed_extra$isWorking(level, purifier)) {
            if (!WorkbenchEnergyRules.consume(purifier, false)) {
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
    private static boolean changed_extra$isWorking(Level level, PurifierBlockEntity purifier) {
        ItemStack stack = purifier.items.get(0);
        if (stack.isEmpty()) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        if (stack.is(ChangedItems.LATEX_SYRINGE.get()) && tag != null) {
            return !tag.getBoolean("safe");
        }

        return PurifierBlockEntity.isConversionRecipe(level.getRecipeManager(), stack);
    }

    @Unique
    private void changed_extra$markEnergyChanged() {
        WorkbenchEnergySync.markChangedAndSync(this, changed_extra$ensureEnergy());
    }
}
