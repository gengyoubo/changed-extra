package github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity;

import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.ltxprogrammer.changed.init.ChangedFluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicLatexFluidGeneratorBlockEntity extends GeneratorBlockEntity {
    private static final int ENERGY_CAPACITY = 10_000;
    private static final int TANK_CAPACITY = 4_000;
    private static final int FLUID_PER_CYCLE = 250;
    private static final int LP_PER_CYCLE = 200;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) { return isLatex(stack.getFluid()); }
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> tank);

    public BasicLatexFluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.BASIC_LATEX_FLUID_GENERATOR_BLOCK_ENTITY.get(), pos, state, ENERGY_CAPACITY);
    }

    private static boolean isLatex(Fluid fluid) {
        return fluid == ChangedFluids.DARK_LATEX.get() || fluid == ChangedFluids.WHITE_LATEX.get();
    }

    @Override protected int generate(ItemStack fuel) { return 0; }

    @Override protected void tryGenerate() {
        if (getMaxEnergyStored() - getEnergyStored() < LP_PER_CYCLE || tank.getFluidAmount() < FLUID_PER_CYCLE) return;
        FluidStack consumed = tank.drain(FLUID_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
        if (consumed.getAmount() != FLUID_PER_CYCLE) return;
        receiveEnergy(LP_PER_CYCLE, null);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("LatexFluidTank", tank.writeToNBT(new CompoundTag()));
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("LatexFluidTank"));
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
        fluidCapability = LazyOptional.of(() -> tank);
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(capability, side);
    }

    public FluidStack getStoredFluid() { return tank.getFluid().copy(); }
}
