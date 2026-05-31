package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyHolder;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyRules;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergySync;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyStorage;
import net.foxyas.changedaddon.block.entity.CatalyzerBlockEntity;
import net.foxyas.changedaddon.recipe.CatalyzerRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CatalyzerBlockEntity.class, remap = false)
public abstract class CatalyzerEnergyMixin implements WorkbenchEnergyHolder, ILatexEnergyHandler {
    @Unique private WorkbenchEnergyStorage changede$energy;
    @Unique private LazyOptional<IEnergyStorage> changede$energyCapability = LazyOptional.empty();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void changede$initEnergy(CallbackInfo ci) {
        changede$ensureEnergy();
    }

    @Unique
    private WorkbenchEnergyStorage changede$ensureEnergy() {
        if (changede$energy == null) {
            changede$energy = new WorkbenchEnergyStorage(WorkbenchEnergyRules.capacityFor(this), this::changede$markEnergyChanged);
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

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
    private <T> void changede$getEnergyCapability(Capability<T> cap, Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (cap == ForgeCapabilities.ENERGY) {
            changede$ensureEnergy();
            cir.setReturnValue(changede$energyCapability.cast());
        }
    }

    @Inject(method = {"setRemoved", "m_7651_"}, at = @At("TAIL"))
    private void changede$invalidateEnergy(CallbackInfo ci) {
        changede$energyCapability.invalidate();
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void changede$requireEnergy(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity instanceof CatalyzerBlockEntity catalyzer && changede$willProgress(level, blockEntity, catalyzer)) {
            if (!WorkbenchEnergyRules.consume(catalyzer, false)) {
                ci.cancel();
            }
        }
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
    private static boolean changede$willProgress(Level level, BlockEntity blockEntity, CatalyzerBlockEntity catalyzer) {
        if (!(level instanceof ServerLevel serverLevel) || catalyzer.tickCount < 5 || !catalyzer.startRecipe) {
            return false;
        }

        IItemHandlerModifiable handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .filter(IItemHandlerModifiable.class::isInstance)
                .map(IItemHandlerModifiable.class::cast)
                .orElse(null);
        if (handler == null) {
            return false;
        }

        ItemStack input = handler.getStackInSlot(0).copy();
        if (input.isEmpty()) {
            return false;
        }

        CatalyzerRecipe recipe = changede$findRecipe(serverLevel, input);
        return recipe != null && changede$canAcceptResult(handler.getStackInSlot(1), recipe.getResultItem(level.registryAccess()));
    }

    @Unique
    private static boolean changede$canAcceptResult(ItemStack output, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), result.getMaxStackSize());
    }

    @Invoker("findRecipe")
    private static CatalyzerRecipe changede$findRecipe(ServerLevel level, ItemStack input) {
        throw new AssertionError();
    }

    @Unique
    private void changede$markEnergyChanged() {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        blockEntity.setChanged();
        if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            BlockState state = blockEntity.getBlockState();
            blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, 3);
            WorkbenchEnergySync.sync(blockEntity, changede$ensureEnergy());
        }
    }
}
