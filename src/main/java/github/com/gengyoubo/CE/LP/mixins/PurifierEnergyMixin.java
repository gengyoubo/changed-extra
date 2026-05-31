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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PurifierBlockEntity.class, remap = false)
public abstract class PurifierEnergyMixin extends BaseContainerBlockEntity implements WorkbenchEnergyHolder, ILatexEnergyHandler {
    @Unique private WorkbenchEnergyStorage changede$energy;
    @Unique private LazyOptional<IEnergyStorage> changede$energyCapability = LazyOptional.empty();

    protected PurifierEnergyMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void changede$initEnergy(CallbackInfo ci) {
        changede$ensureEnergy();
    }

    @Unique
    private WorkbenchEnergyStorage changede$ensureEnergy() {
        if (changede$energy == null) {
            changede$energy = new WorkbenchEnergyStorage(WorkbenchEnergyRules.NORMAL_CAPACITY, this::changede$markEnergyChanged);
            changede$energyCapability = LazyOptional.of(() -> changede$energy);
        }
        return changede$energy;
    }

    @Inject(method = {"load", "m_142466_"}, at = @At("TAIL"))
    private void changede$loadEnergy(CompoundTag tag, CallbackInfo ci) {
        changede$ensureEnergy().setEnergyStored(tag.getInt(WorkbenchEnergyRules.NBT_KEY));
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("TAIL"))
    private void changede$saveEnergy(CompoundTag tag, CallbackInfo ci) {
        tag.putInt(WorkbenchEnergyRules.NBT_KEY, changede$ensureEnergy().getEnergyStored());
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void changede$requireEnergy(Level level, BlockPos pos, BlockState state, PurifierBlockEntity purifier, CallbackInfo ci) {
        if (!level.isClientSide && changede$isWorking(level, purifier)) {
            if (!WorkbenchEnergyRules.consume(purifier, false)) {
                ci.cancel();
            }
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            changede$ensureEnergy();
            return changede$energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        changede$energyCapability.invalidate();
    }

    @Override
    public WorkbenchEnergyStorage changede$getWorkbenchEnergy() {
        return changede$ensureEnergy();
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        return changede$ensureEnergy().receiveEnergy(Math.max(0, amount), false);
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return changede$ensureEnergy().getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return changede$ensureEnergy().getMaxEnergyStored();
    }

    @Unique
    private static boolean changede$isWorking(Level level, PurifierBlockEntity purifier) {
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
    private void changede$markEnergyChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            WorkbenchEnergySync.sync(this, changede$ensureEnergy());
        }
    }
}
