package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import github.com.gengyoubo.CE.LP.compat.SpaceTowerForgeEnergyPusher;
import github.com.gengyoubo.CE.LP.compat.SpaceTowerForgeEnergyStorage;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CreateSpaceTowerBlockEntity extends GeneratingKineticBlockEntity implements ILatexEnergyHandler, SpaceTowerAccess {
    private static final int TICKS_PER_MINUTE = 20 * 60;

    private final Map<SpaceTowerEnergyType, IOType> modes = new EnumMap<>(SpaceTowerEnergyType.class);
    private int lpEnergy;
    private double jouleBuffer;
    private double ceStoredLp;
    private double ceOutputDebt;
    private double ceInputAccumulator;
    private int ceRpm = SpaceTowerBlockEntity.DEFAULT_CE_RPM;
    private int ceSu = SpaceTowerBlockEntity.DEFAULT_CE_SU;
    private boolean ceOutputPowered;
    private boolean queuedKineticRefresh;
    private final SpaceTowerForgeEnergyStorage forgeEnergy = new SpaceTowerForgeEnergyStorage(this);
    private final LazyOptional<IEnergyStorage> forgeEnergyCapability = LazyOptional.of(() -> forgeEnergy);

    public CreateSpaceTowerBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.SPACE_TOWER_BLOCK_ENTITY.get(), pos, state);
        SpaceTowerCommon.initializeDefaultModes(modes);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public IOType getMode(SpaceTowerEnergyType type) {
        return modes.getOrDefault(type, IOType.INPUT);
    }

    @Override
    public void toggleMode(SpaceTowerEnergyType type) {
        setMode(type, getMode(type) == IOType.INPUT ? IOType.OUTPUT : IOType.INPUT);
    }

    @Override
    public void setMode(SpaceTowerEnergyType type, IOType mode) {
        modes.put(type, mode);
        notifyNeighbors();
        refreshKinetics();
        updateCeOutputPoweredState();
        sync();
    }

    @Override
    public int getCeRpm() {
        return ceRpm;
    }

    @Override
    public int getCeSu() {
        return ceSu;
    }

    @Override
    public void adjustCeRpm(int delta) {
        setCeRpm(ceRpm + delta);
    }

    @Override
    public void adjustCeSu(int delta) {
        setCeSu(ceSu + delta);
    }

    @Override
    public void setCeRpm(int rpm) {
        int oldCost = getCeCostPerMinute();
        ceRpm = Mth.clamp(SpaceTowerCommon.roundToStep(rpm), SpaceTowerBlockEntity.MIN_CE_RPM, SpaceTowerBlockEntity.MAX_CE_RPM);
        rescaleCeStorage(oldCost);
        refreshKinetics();
        sync();
    }

    @Override
    public void setCeSu(int su) {
        int oldCost = getCeCostPerMinute();
        ceSu = Mth.clamp(su, SpaceTowerBlockEntity.MIN_CE_SU, SpaceTowerBlockEntity.MAX_CE_SU);
        rescaleCeStorage(oldCost);
        refreshKinetics();
        sync();
    }

    @Override
    public int getCeCostPerMinute() {
        return SpaceTowerCommon.getCeCostPerMinute(ceRpm, ceSu);
    }

    @Override
    public int getMaxCeStoredLp() {
        return getCeCostPerMinute() * SpaceTowerBlockEntity.CE_STORAGE_MINUTES;
    }

    @Override
    public int getCeStoredLpDisplay() {
        return (int)Math.floor(ceStoredLp);
    }

    @Override
    public int getJouleBufferDisplay() {
        return (int)Math.floor(jouleBuffer);
    }

    @Override
    public void receiveEnergyAsType(SpaceTowerEnergyType type, double amount) {
        if (amount <= 0.0D || getMode(type) != IOType.INPUT) {
            return;
        }

        if (type == SpaceTowerEnergyType.CE) {
            boolean wasPowered = canGenerateCeOutput();
            double before = ceStoredLp;
            ceStoredLp = Math.min(getMaxCeStoredLp(), ceStoredLp + amount);
            updateCeOutputPoweredState(wasPowered);
            sync();
            return;
        }

        jouleBuffer = SpaceTowerCommon.receiveAsLpBuffer(jouleBuffer, type, amount, this::receiveLpIgnoringMode, this::sync);
    }

    @Override
    public double extractEnergyAsType(SpaceTowerEnergyType type, double requestedAmount) {
        if (requestedAmount <= 0.0D || getMode(type) != IOType.OUTPUT) {
            return 0.0D;
        }

        if (type == SpaceTowerEnergyType.CE) {
            boolean wasPowered = canGenerateCeOutput();
            double extracted = Math.min(ceStoredLp, requestedAmount);
            ceStoredLp -= extracted;
            if (extracted > 0.0D) {
                updateCeOutputPoweredState(wasPowered);
                sync();
            }
            return extracted;
        }

        return SpaceTowerCommon.extractFromLp(type, requestedAmount, this::extractLpIgnoringMode, this::sync);
    }

    @Override
    public void refundEnergyAsType(SpaceTowerEnergyType type, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        if (type == SpaceTowerEnergyType.CE) {
            boolean wasPowered = canGenerateCeOutput();
            ceStoredLp = Math.min(getMaxCeStoredLp(), ceStoredLp + amount);
            updateCeOutputPoweredState(wasPowered);
            sync();
            return;
        }

        double joules = amount * type.joulesPerUnit();
        int lp = (int)Math.floor(joules / SpaceTowerEnergyType.LP.joulesPerUnit());
        if (receiveLpIgnoringMode(lp) > 0) {
            sync();
        }
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        if (getMode(SpaceTowerEnergyType.LP) != IOType.INPUT) {
            return 0;
        }
        int received = receiveLpIgnoringMode(amount);
        if (received > 0) {
            sync();
        }
        return received;
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        if (getMode(SpaceTowerEnergyType.LP) != IOType.OUTPUT) {
            return 0;
        }
        int extracted = extractLpIgnoringMode(amount);
        if (extracted > 0) {
            sync();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return lpEnergy;
    }

    @Override
    public int getMaxEnergyStored() {
        return SpaceTowerBlockEntity.LP_CAPACITY;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return forgeEnergyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        forgeEnergyCapability.invalidate();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!canGenerateCeOutput()) {
            return 0.0F;
        }
        return ceRpm;
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (!canGenerateCeOutput()) {
            return 0.0F;
        }
        return stressUnitsToImpact(ceSu, getCurrentKineticSpeedOrConfigured());
    }

    @Override
    public float calculateStressApplied() {
        if (getMode(SpaceTowerEnergyType.CE) != IOType.INPUT) {
            return 0.0F;
        }
        float speed = Math.abs(getSpeed()) > 0.0F ? Math.abs(getSpeed()) : ceRpm;
        return stressUnitsToImpact(ceSu, speed);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level != null && !level.isClientSide && getMode(SpaceTowerEnergyType.CE) == IOType.OUTPUT) {
            updateStressCapacityIfNetworkPresent();
        }
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            super.tick();
            return;
        }

        super.tick();

        if (queuedKineticRefresh) {
            queuedKineticRefresh = false;
            forceKineticRefresh();
        }

        updateCeOutputPoweredState();

        if (getMode(SpaceTowerEnergyType.CE) == IOType.OUTPUT && Math.abs(getSpeed()) > 0.0F) {
            consumeForCeOutput();
        } else if (getMode(SpaceTowerEnergyType.CE) == IOType.INPUT && Math.abs(getSpeed()) > 0.0F && !isOverStressed()) {
            storeIncomingCe();
        }

        pushEnergy();
        SpaceTowerForgeEnergyPusher.pull(level, worldPosition, this);
        SpaceTowerForgeEnergyPusher.push(level, worldPosition, this);
    }

    private boolean canPayCeOutput() {
        return ceStoredLp > 0.0D || lpEnergy > 0;
    }

    private boolean canGenerateCeOutput() {
        return getMode(SpaceTowerEnergyType.CE) == IOType.OUTPUT && canPayCeOutput();
    }

    private void consumeForCeOutput() {
        boolean wasPowered = canGenerateCeOutput();
        double costPerTick = getCeCostPerMinute() / (double)TICKS_PER_MINUTE;
        double paidByCeStorage = Math.min(ceStoredLp, costPerTick);
        ceStoredLp -= paidByCeStorage;
        ceOutputDebt += costPerTick - paidByCeStorage;

        if (ceOutputDebt >= 1.0D) {
            int lpNeeded = (int)Math.floor(ceOutputDebt);
            int extracted = extractLpIgnoringMode(lpNeeded);
            ceOutputDebt -= extracted;
            if (extracted < lpNeeded) {
                ceOutputDebt = 0.0D;
                updateCeOutputPoweredState();
            }
        }

        updateCeOutputPoweredState(wasPowered);
        setChanged();
    }

    private void storeIncomingCe() {
        int actualRpm = Math.max(SpaceTowerBlockEntity.MIN_CE_RPM, Math.round(Math.abs(getSpeed())));
        int actualStressUnits = getActualInputStressUnits(actualRpm);
        double lpPerTick = SpaceTowerCommon.getCeCostPerMinute(actualRpm, actualStressUnits) / (double)TICKS_PER_MINUTE;
        ceInputAccumulator += lpPerTick;
        if (ceInputAccumulator >= 1.0D) {
            int wholeLp = (int)Math.floor(ceInputAccumulator);
            int acceptedLp = receiveLpIgnoringMode(wholeLp);
            ceInputAccumulator -= acceptedLp;

            if (acceptedLp < wholeLp) {
                double overflowLp = wholeLp - acceptedLp;
                double acceptedCe = Math.min(getMaxCeStoredLp() - ceStoredLp, overflowLp);
                ceStoredLp += acceptedCe;
                ceInputAccumulator -= acceptedCe;
            }
            sync();
        }
    }

    private static float stressUnitsToImpact(float stressUnits, float speed) {
        return stressUnits / Math.max(1.0F, Math.abs(speed));
    }

    private float getCurrentKineticSpeedOrConfigured() {
        float theoreticalSpeed = Math.abs(getTheoreticalSpeed());
        if (theoreticalSpeed > 0.0F) {
            return theoreticalSpeed;
        }

        float generatedSpeed = Math.abs(getGeneratedSpeed());
        if (generatedSpeed > 0.0F) {
            return generatedSpeed;
        }

        return ceRpm;
    }

    private int getActualInputStressUnits(int actualRpm) {
        if (Math.abs(getSpeed()) <= 0.0F) {
            return 0;
        }
        if (!hasNetwork()) {
            return ceSu;
        }

        KineticNetwork kineticNetwork = getOrCreateNetwork();
        if (kineticNetwork == null) {
            return ceSu;
        }
        float ownStress = Math.max(0.0F, kineticNetwork.getActualStressOf(this));
        float otherStress = Math.max(0.0F, kineticNetwork.calculateStress() - ownStress);
        float availableStress = kineticNetwork.calculateCapacity() - otherStress;
        if (!Float.isFinite(availableStress) || availableStress <= 0.0F) {
            return ceSu;
        }
        return Math.max(ceSu, Math.round(availableStress));
    }

    private void pushEnergy() {
        SpaceTowerCommon.pushLpOutput(level, worldPosition, this, this::getMode, this::receiveLpIgnoringMode);
    }

    protected int receiveLpIgnoringMode(int amount) {
        boolean wasPowered = canGenerateCeOutput();
        int accepted = Math.min(SpaceTowerBlockEntity.LP_CAPACITY - lpEnergy, Math.max(0, amount));
        lpEnergy += accepted;
        if (accepted > 0) {
            setChanged();
            updateCeOutputPoweredState(wasPowered);
        }
        return accepted;
    }

    protected int extractLpIgnoringMode(int amount) {
        boolean wasPowered = canGenerateCeOutput();
        int extracted = Math.min(lpEnergy, Math.max(0, amount));
        lpEnergy -= extracted;
        if (extracted > 0) {
            setChanged();
            updateCeOutputPoweredState(wasPowered);
        }
        return extracted;
    }

    private void clampCeStorage() {
        ceStoredLp = Math.min(ceStoredLp, getMaxCeStoredLp());
    }

    private void rescaleCeStorage(int oldCostPerMinute) {
        int newCostPerMinute = getCeCostPerMinute();
        if (oldCostPerMinute > 0 && newCostPerMinute > 0) {
            ceStoredLp = ceStoredLp * newCostPerMinute / oldCostPerMinute;
        }
        clampCeStorage();
    }

    private void refreshKinetics() {
        if (level != null && !level.isClientSide) {
            updateGeneratedRotation();
            updateStressCapacityIfNetworkPresent();
            networkDirty = true;
        }
    }

    private void updateCeOutputPoweredState() {
        updateCeOutputPoweredState(ceOutputPowered);
    }

    private void updateCeOutputPoweredState(boolean wasPowered) {
        boolean powered = canGenerateCeOutput();
        ceOutputPowered = powered;
        if (powered != wasPowered) {
            refreshKinetics();
        }
    }

    private void queueKineticRefresh() {
        queuedKineticRefresh = true;
        ceOutputPowered = false;
    }

    private void forceKineticRefresh() {
        boolean powered = canGenerateCeOutput();
        ceOutputPowered = powered;
        if (powered && !hasSource()) {
            setSpeed(0.0F);
        }
        updateGeneratedRotation();
        updateStressCapacityIfNetworkPresent();
        networkDirty = true;
        sendData();
    }

    private void updateStressCapacityIfNetworkPresent() {
        if (level == null || level.isClientSide || !hasNetwork()) {
            return;
        }

        KineticNetwork kineticNetwork = getOrCreateNetwork();
        if (kineticNetwork != null) {
            kineticNetwork.updateCapacityFor(this, calculateAddedStressCapacity());
        }
    }

    private void sync() {
        SpaceTowerCommon.sync(this);
    }

    private void notifyNeighbors() {
        SpaceTowerCommon.notifyNeighbors(this);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("LpEnergy", lpEnergy);
        tag.putDouble("JouleBuffer", jouleBuffer);
        tag.putDouble("CeStoredLp", ceStoredLp);
        tag.putDouble("CeOutputDebt", ceOutputDebt);
        tag.putDouble("CeInputAccumulator", ceInputAccumulator);
        tag.putInt("CeRpm", ceRpm);
        tag.putInt("CeSu", ceSu);
        tag.putBoolean("CeOutputPowered", ceOutputPowered);

        SpaceTowerCommon.saveModes(tag, this::getMode);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        lpEnergy = Mth.clamp(tag.getInt("LpEnergy"), 0, SpaceTowerBlockEntity.LP_CAPACITY);
        jouleBuffer = tag.getDouble("JouleBuffer");
        ceStoredLp = tag.getDouble("CeStoredLp");
        ceOutputDebt = tag.getDouble("CeOutputDebt");
        ceInputAccumulator = tag.getDouble("CeInputAccumulator");
        ceRpm = tag.contains("CeRpm") ? tag.getInt("CeRpm") : SpaceTowerBlockEntity.DEFAULT_CE_RPM;
        ceSu = tag.contains("CeSu") ? tag.getInt("CeSu") : SpaceTowerBlockEntity.DEFAULT_CE_SU;
        ceOutputPowered = false;

        SpaceTowerCommon.loadModes(tag, modes);

        ceRpm = Mth.clamp(SpaceTowerCommon.roundToStep(ceRpm), SpaceTowerBlockEntity.MIN_CE_RPM, SpaceTowerBlockEntity.MAX_CE_RPM);
        ceSu = Mth.clamp(ceSu, SpaceTowerBlockEntity.MIN_CE_SU, SpaceTowerBlockEntity.MAX_CE_SU);
        clampCeStorage();
        if (!clientPacket) {
            queueKineticRefresh();
        }
    }
}
