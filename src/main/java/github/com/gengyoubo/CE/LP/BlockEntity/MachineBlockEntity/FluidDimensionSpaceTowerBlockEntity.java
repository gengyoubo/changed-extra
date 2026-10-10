package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.BlockEntity.RedstoneMode;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerChannels;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerChunks;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerManager;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.changede;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
@SuppressWarnings("deprecation")

public final class FluidDimensionSpaceTowerBlockEntity extends BlockEntity {
    public static final int CAPACITY = 50_000;
    private final FluidTank tank =
            new FluidTank(CAPACITY) {
                @Override protected void onContentsChanged() { setChanged(); }
            };
    private LazyOptional<IFluidHandler> fluidCapability =
            LazyOptional.of(() -> new Port());
    private IOType mode = IOType.INPUT;
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS_ON;
    private int channel;
    private boolean enabled;
    private boolean anchored;
    private boolean registered;

    private record Address(ResourceKey<Level> dimension, BlockPos pos) { }

    public FluidDimensionSpaceTowerBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.FLUID_DIMENSION_SPACE_TOWER.get(), pos, state);
    }

    public boolean isEnabled() {
        if (!(level instanceof ServerLevel) || isRemoved() || !enabled || channel <= 0
                || channel > DimensionTowerChannels.MAX_CHANNEL || mode == IOType.NONE) return false;
        return switch (redstoneMode) {
            case ALWAYS_ON -> true;
            case ALWAYS_OFF -> false;
            case ON_WITH_REDSTONE -> level.hasNeighborSignal(worldPosition);
            case OFF_WITH_REDSTONE -> !level.hasNeighborSignal(worldPosition);
        };
    }

    public void tick() {
        refreshNetwork();
        if (!active()) return;
        for (Direction side : Direction.values()) {
            BlockPos pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos)) continue;
            BlockEntity neighbor = level.getBlockEntity(pos);
            if (neighbor instanceof FluidDimensionSpaceTowerBlockEntity) continue;
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite())
                    .ifPresent(handler -> {
                        if (mode == IOType.INPUT) FluidUtil.tryFluidTransfer(tank, handler, CAPACITY - tank.getFluidAmount(), true);
                        if (mode == IOType.OUTPUT) FluidUtil.tryFluidTransfer(handler, tank, tank.getFluidAmount(), true);
                    });
        }
    }

    public void refreshNetwork() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (isEnabled()) {
            if (!anchored) {
                anchored = true;
                for (var chunk : DimensionTowerChunks.around(worldPosition.getX(), worldPosition.getZ())) {
                    ForgeChunkManager.forceChunk(serverLevel, changede.MODID, worldPosition, chunk.x(), chunk.z(), true, true);
                }
            }
            if (!registered) {
                DimensionTowerManager.fluidNetwork(serverLevel.getServer()).register(this);
                registered = true;
            }
        } else {
            disconnect();
            releaseAnchor();
        }
    }

    private void disconnect() {
        if (level instanceof ServerLevel serverLevel) DimensionTowerManager.unregisterFluid(serverLevel.getServer(), this);
        registered = false;
    }

    private void releaseAnchor() {
        if (level instanceof ServerLevel serverLevel && anchored) {
            anchored = false;
            for (var chunk : DimensionTowerChunks.around(worldPosition.getX(), worldPosition.getZ())) {
                ForgeChunkManager.forceChunk(serverLevel, changede.MODID, worldPosition, chunk.x(), chunk.z(), false, true);
            }
        }
    }

    /** Actual block removal, distinct from unloading a world during server shutdown. */
    public void shutdown() {
        disconnect();
        releaseAnchor();
    }

    @Override public void setRemoved() {
        disconnect();
        super.setRemoved();
    }

    @Override public void onChunkUnloaded() {
        disconnect();
        // Keep the saved ticket during shutdown so Forge can restore it on next startup.
        anchored = false;
        super.onChunkUnloaded();
    }

    public int getChannel() { return channel; }
    public boolean isSwitchedOn() { return enabled; }
    public IOType getMode() { return mode; }
    public RedstoneMode getRedstoneMode() { return redstoneMode; }
    public int getLoadedChunkCount() {
        return anchored ? DimensionTowerChunks.around(worldPosition.getX(), worldPosition.getZ()).size() : 0;
    }
    public int getPeerCount() {
        return active() && level instanceof ServerLevel serverLevel
                ? DimensionTowerManager.fluidPeerCount(serverLevel.getServer(), this) : 0;
    }

    public void setChannel(int value) {
        if (value < 0 || value > DimensionTowerChannels.MAX_CHANNEL) return;
        channel = value;
        configurationChanged();
    }
    public void toggleEnabled() { enabled = !enabled; configurationChanged(); }
    public void cycleMode() {
        mode = IOType.values()[(mode.ordinal() + 1) % IOType.values().length];
        configurationChanged();
    }
    public void cycleRedstoneMode() {
        redstoneMode = RedstoneMode.values()[(redstoneMode.ordinal() + 1) % RedstoneMode.values().length];
        configurationChanged();
    }
    private void configurationChanged() {
        refreshNetwork();
        SpaceTowerCommon.notifyNeighbors(this);
        SpaceTowerCommon.sync(this);
    }

    public Object address() { return new Address(level.dimension(), worldPosition); }
    public boolean active() {
        return anchored && registered && isEnabled() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this;
    }

    public FluidStack getFluid() { return tank.getFluid().copy(); }
    public int networkFill(FluidStack fluid, IFluidHandler.FluidAction action) {
        return active() && mode == IOType.OUTPUT ? tank.fill(fluid, action) : 0;
    }
    public void networkDrain(int amount) {
        if (active() && mode == IOType.INPUT) tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
    }

    private final class Port implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int index) { return index == 0 ? getFluid() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int index) { return index == 0 ? CAPACITY : 0; }
        @Override public boolean isFluidValid(int index, FluidStack stack) { return index == 0 && !stack.isEmpty(); }
        @Override public int fill(FluidStack stack, FluidAction action) {
            return active() && mode == IOType.INPUT ? tank.fill(stack, action) : 0;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return active() && mode == IOType.OUTPUT ? tank.drain(stack, action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            return active() && mode == IOType.OUTPUT ? tank.drain(amount, action) : FluidStack.EMPTY;
        }
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); fluidCapability.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        fluidCapability = LazyOptional.of(() -> new Port());
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Channel", channel);
        tag.putBoolean("Enabled", enabled);
        tag.putString("Mode", mode.name());
        tag.putString("RedstoneMode", redstoneMode.name());
        tag.put("Fluid", tank.writeToNBT(new CompoundTag()));
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        channel = Math.max(0, Math.min(DimensionTowerChannels.MAX_CHANNEL, tag.getInt("Channel")));
        enabled = tag.getBoolean("Enabled");
        mode = readEnum(IOType.class, tag.getString("Mode"), IOType.INPUT);
        redstoneMode = readEnum(RedstoneMode.class, tag.getString("RedstoneMode"), RedstoneMode.ALWAYS_ON);
        tank.readFromNBT(tag.getCompound("Fluid"));
        if (tank.getFluidAmount() > CAPACITY) tank.getFluid().setAmount(CAPACITY);
    }

    private static <T extends Enum<T>> T readEnum(Class<T> type, String value, T fallback) {
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException exception) { return fallback; }
    }
}
