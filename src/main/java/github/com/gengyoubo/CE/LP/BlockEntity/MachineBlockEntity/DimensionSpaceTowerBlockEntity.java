package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.ILatexTypedEnergyHandler;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
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

public final class DimensionSpaceTowerBlockEntity extends BlockEntity
        implements ILatexEnergyHandler, ILatexTypedEnergyHandler, DimensionTowerChannels.Endpoint {
    public static final int CAPACITY = 50_000;
    private final int[] energy = new int[LatexEnergyType.values().length];
    private LatexEnergyType energyType = LatexEnergyType.LP;
    private IOType mode = IOType.INPUT;
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS_ON;
    private int channel;
    private boolean enabled;
    private boolean anchored;
    private boolean registered;
    private int budgetTick = Integer.MIN_VALUE;
    private int receivedThisTick;
    private int extractedThisTick;

    private record Address(ResourceKey<Level> dimension, BlockPos pos) { }

    public DimensionSpaceTowerBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.DIMENSION_SPACE_TOWER.get(), pos, state);
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
        resetBudgets();
        for (Direction side : Direction.values()) {
            BlockPos pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos)) continue;
            BlockEntity neighbor = level.getBlockEntity(pos);
            if (neighbor instanceof DimensionSpaceTowerBlockEntity) continue;
            if (mode == IOType.INPUT) pull(neighbor, side);
            if (mode == IOType.OUTPUT) push(neighbor, side);
        }
    }

    private void resetBudgets() {
        int tick = level instanceof ServerLevel serverLevel ? serverLevel.getServer().getTickCount() : 0;
        if (budgetTick != tick) {
            budgetTick = tick;
            receivedThisTick = 0;
            extractedThisTick = 0;
        }
    }

    private void pull(BlockEntity neighbor, Direction side) {
        int request = Math.min(CAPACITY - stored(), DimensionTowerChannels.TRANSFER_PER_TICK - receivedThisTick);
        if (request <= 0) return;
        int extracted = 0;
        if (energyType == LatexEnergyType.LP && neighbor instanceof ILatexEnergyHandler handler) {
            extracted = handler.extractEnergy(request, side.getOpposite());
        } else if (neighbor instanceof ILatexTypedEnergyHandler handler && handler.getEnergyType() == energyType) {
            extracted = handler.extractTypedEnergy(energyType, request, side.getOpposite());
        }
        receiveTypedEnergy(energyType, Math.max(0, Math.min(request, extracted)));
    }

    private void push(BlockEntity neighbor, Direction side) {
        int offer = Math.min(stored(), DimensionTowerChannels.TRANSFER_PER_TICK - extractedThisTick);
        if (offer <= 0) return;
        int received = 0;
        if (energyType == LatexEnergyType.LP && neighbor instanceof ILatexEnergyHandler handler) {
            received = handler.receiveEnergy(offer, side.getOpposite());
        } else if (neighbor instanceof ILatexTypedEnergyHandler handler && handler.getEnergyType() == energyType) {
            received = handler.receiveTypedEnergy(energyType, offer, side.getOpposite());
        }
        extractTypedEnergy(energyType, Math.max(0, Math.min(offer, received)));
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
                DimensionTowerManager.network(serverLevel.getServer()).register(this);
                registered = true;
            }
        } else {
            disconnect();
            releaseAnchor();
        }
    }

    private void disconnect() {
        if (level instanceof ServerLevel serverLevel) DimensionTowerManager.unregister(serverLevel.getServer(), this);
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
                ? DimensionTowerManager.peerCount(serverLevel.getServer(), this) : 0;
    }

    public void setChannel(int value) {
        if (value < 0 || value > DimensionTowerChannels.MAX_CHANNEL) return;
        channel = value;
        configurationChanged();
    }
    public void toggleEnabled() { enabled = !enabled; configurationChanged(); }
    public void cycleEnergyType() {
        energyType = LatexEnergyType.values()[(energyType.ordinal() + 1) % LatexEnergyType.values().length];
        configurationChanged();
    }
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

    @Override public Object address() { return new Address(level.dimension(), worldPosition); }
    @Override public boolean active() {
        return anchored && registered && isEnabled() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this;
    }
    @Override public int channel() { return channel; }
    @Override public LatexEnergyType energyType() { return energyType; }
    @Override public IOType mode() { return mode; }
    @Override public int stored() { return energy[energyType.ordinal()]; }
    @Override public int capacity() { return CAPACITY; }
    @Override public void moveEnergy(int delta) {
        energy[energyType.ordinal()] = Math.max(0, Math.min(CAPACITY, stored() + delta));
        setChanged();
    }

    @Override public LatexEnergyType getEnergyType() { return energyType; }
    @Override public int receiveTypedEnergy(LatexEnergyType type, int amount) {
        if (type != energyType || mode != IOType.INPUT || !active()) return 0;
        resetBudgets();
        int accepted = Math.min(Math.max(0, amount), Math.min(CAPACITY - stored(),
                DimensionTowerChannels.TRANSFER_PER_TICK - receivedThisTick));
        if (accepted > 0) { moveEnergy(accepted); receivedThisTick += accepted; }
        return accepted;
    }
    @Override public int extractTypedEnergy(LatexEnergyType type, int amount) {
        if (type != energyType || mode != IOType.OUTPUT || !active()) return 0;
        resetBudgets();
        int extracted = Math.min(Math.max(0, amount), Math.min(stored(),
                DimensionTowerChannels.TRANSFER_PER_TICK - extractedThisTick));
        if (extracted > 0) { moveEnergy(-extracted); extractedThisTick += extracted; }
        return extracted;
    }
    @Override public int getTypedEnergyStored() { return stored(); }
    @Override public int getTypedEnergyCapacity() { return CAPACITY; }
    @Override public int receiveEnergy(int amount, Direction from) { return receiveTypedEnergy(LatexEnergyType.LP, amount); }
    @Override public int extractEnergy(int amount, Direction from) { return extractTypedEnergy(LatexEnergyType.LP, amount); }
    @Override public int getEnergyStored() { return energy[LatexEnergyType.LP.ordinal()]; }
    @Override public int getMaxEnergyStored() { return energyType == LatexEnergyType.LP ? CAPACITY : 0; }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Channel", channel);
        tag.putBoolean("Enabled", enabled);
        tag.putString("EnergyType", energyType.name());
        tag.putString("Mode", mode.name());
        tag.putString("RedstoneMode", redstoneMode.name());
        for (LatexEnergyType type : LatexEnergyType.values()) tag.putInt(type.name(), energy[type.ordinal()]);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        channel = Math.max(0, Math.min(DimensionTowerChannels.MAX_CHANNEL, tag.getInt("Channel")));
        enabled = tag.getBoolean("Enabled");
        energyType = readEnum(LatexEnergyType.class, tag.getString("EnergyType"), LatexEnergyType.LP);
        mode = readEnum(IOType.class, tag.getString("Mode"), IOType.INPUT);
        redstoneMode = readEnum(RedstoneMode.class, tag.getString("RedstoneMode"), RedstoneMode.ALWAYS_ON);
        for (LatexEnergyType type : LatexEnergyType.values()) energy[type.ordinal()] = Math.max(0, Math.min(CAPACITY, tag.getInt(type.name())));
    }

    private static <T extends Enum<T>> T readEnum(Class<T> type, String value, T fallback) {
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException exception) { return fallback; }
    }
}
