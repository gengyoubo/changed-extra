package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.ILatexTypedEnergyHandler;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.TypedLatexEnergyStorage;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
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

public class LatexEnergyConverterBlockEntity extends BlockEntity implements ILatexEnergyHandler, ILatexTypedEnergyHandler {
    public static final int LP_CAPACITY = 100_000;
    public static final int OUTPUT_CAPACITY = 100_000;
    public static final int TANK_CAPACITY = 4_000;
    private static final int LP_PER_CYCLE = 1_000;
    private static final int FLUID_PER_CYCLE = 1_000;
    private static final int OUTPUT_PER_CYCLE = 200;

    private final LatexEnergyType outputType;
    private final Fluid requiredFluid;
    private int lp;
    private final TypedLatexEnergyStorage output;
    private final FluidTank tank;
    private LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluidCapability;

    public LatexEnergyConverterBlockEntity(BlockPos pos, BlockState state, boolean white) {
        super(white ? CELPBlockEntity.WHITE_LATEX_POWER_CONVERTER.get() : CELPBlockEntity.DARK_LATEX_POWER_CONVERTER.get(), pos, state);
        outputType = white ? LatexEnergyType.WLP : LatexEnergyType.DLP;
        requiredFluid = white ? ChangedFluids.WHITE_LATEX.get() : ChangedFluids.DARK_LATEX.get();
        output = new TypedLatexEnergyStorage(outputType, OUTPUT_CAPACITY);
        tank = new FluidTank(TANK_CAPACITY) {
            @Override public boolean isFluidValid(FluidStack stack) { return stack.getFluid() == requiredFluid; }
            @Override protected void onContentsChanged() { setChanged(); }
        };
        fluidCapability = LazyOptional.of(() -> tank);
    }

    public void tick() {
        if (level == null || level.isClientSide || lp < LP_PER_CYCLE || tank.getFluidAmount() < FLUID_PER_CYCLE
                || output.getCapacity() - output.getEnergyStored() < OUTPUT_PER_CYCLE) return;
        if (tank.drain(FLUID_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE).getAmount() != FLUID_PER_CYCLE) return;
        lp -= LP_PER_CYCLE;
        output.receive(OUTPUT_PER_CYCLE);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public LatexEnergyType getOutputType() { return outputType; }
    public int getOutputStored() { return output.getEnergyStored(); }
    public int getOutputCapacity() { return output.getCapacity(); }
    public int getFluidStored() { return tank.getFluidAmount(); }
    public int getFluidCapacity() { return tank.getCapacity(); }
    public Fluid getRequiredFluid() { return requiredFluid; }

    @Override public int receiveEnergy(int amount, Direction from) {
        int received = Math.min(Math.max(amount, 0), LP_CAPACITY - lp);
        lp += received;
        if (received > 0) setChanged();
        return received;
    }
    @Override public int extractEnergy(int amount, Direction from) { return 0; }
    @Override public LatexEnergyType getEnergyType() { return outputType; }
    @Override public int receiveTypedEnergy(LatexEnergyType type, int amount) { return 0; }
    @Override public int extractTypedEnergy(LatexEnergyType type, int amount) {
        if (type != outputType) return 0;
        int extracted = output.extract(amount);
        if (extracted > 0) setChanged();
        return extracted;
    }
    @Override public int getTypedEnergyStored() { return output.getEnergyStored(); }
    @Override public int getTypedEnergyCapacity() { return output.getCapacity(); }
    @Override public int getEnergyStored() { return lp; }
    @Override public int getMaxEnergyStored() { return LP_CAPACITY; }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("LP", lp);
        tag.putInt("TypedEnergy", output.getEnergyStored());
        tag.put("LatexTank", tank.writeToNBT(new CompoundTag()));
    }
    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        lp = Math.min(Math.max(tag.getInt("LP"), 0), LP_CAPACITY);
        output.setEnergy(tag.getInt("TypedEnergy"));
        tank.readFromNBT(tag.getCompound("LatexTank"));
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
}
