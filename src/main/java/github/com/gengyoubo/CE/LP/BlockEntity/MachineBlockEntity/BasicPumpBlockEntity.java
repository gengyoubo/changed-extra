package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicPumpBlockEntity extends BlockEntity {
    private static final int TANK_CAPACITY = 16_000;
    private static final int PUMP_INTERVAL_TICKS = 20;
    private static final int AMOUNT_PER_PUMP = 1_000;
    private static final ResourceLocation LATEX_SPACE = ResourceLocation.parse("changede:latex_space");
    private static final ResourceLocation DARK_LATEX = ResourceLocation.parse("changed:dark_latex");
    private static final ResourceLocation WHITE_LATEX = ResourceLocation.parse("changed:white_latex");
    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) { return isSupportedFluid(stack); }
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> tank);
    private int pumpTimer;

    public BasicPumpBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.BASIC_PUMP_BLOCK_ENTITY.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide || ++pumpTimer < PUMP_INTERVAL_TICKS) return;
        pumpTimer = 0;
        FluidState source = level.getFluidState(worldPosition.below());
        if (!source.isSource() || !isAllowedSource(source)) return;
        FluidStack extracted = new FluidStack(source.getType(), AMOUNT_PER_PUMP);
        tank.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
    }

    private boolean isAllowedSource(FluidState fluidState) {
        if (fluidState.is(FluidTags.WATER)) {
            return isInfiniteWaterSource(worldPosition.below());
        }

        ResourceLocation dimension = level.dimension().location();
        if (dimension.equals(LATEX_SPACE)) {
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluidState.getType());
            return DARK_LATEX.equals(fluidId) || WHITE_LATEX.equals(fluidId);
        }
        return false;
    }

    private boolean isInfiniteWaterSource(BlockPos sourcePos) {
        for (int xOffset = -1; xOffset <= 0; xOffset++) {
            for (int zOffset = -1; zOffset <= 0; zOffset++) {
                boolean squareIsWater = true;
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        BlockPos pos = sourcePos.offset(xOffset + dx, 0, zOffset + dz);
                        if (!level.getFluidState(pos).is(FluidTags.WATER)
                                || !level.getFluidState(pos).isSource()) {
                            squareIsWater = false;
                        }
                    }
                }
                if (squareIsWater) return true;
            }
        }
        return false;
    }
    @SuppressWarnings("deprecation")
    private static boolean isSupportedFluid(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        return fluid.is(FluidTags.WATER) || DARK_LATEX.equals(id) || WHITE_LATEX.equals(id);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("PumpTank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("PumpTimer", pumpTimer);
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("PumpTank"));
        pumpTimer = tag.getInt("PumpTimer");
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
