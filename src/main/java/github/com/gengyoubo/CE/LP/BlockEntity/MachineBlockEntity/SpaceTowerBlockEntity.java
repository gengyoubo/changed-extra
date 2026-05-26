package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SpaceTowerBlockEntity extends GeneratingKineticBlockEntity implements ILatexEnergyHandler {
    public static final int LP_CAPACITY = 50_000;
    public static final int DEFAULT_CE_RPM = 8;
    public static final int DEFAULT_CE_SU = 256;
    public static final int MIN_CE_RPM = 2;
    public static final int MAX_CE_RPM = 256;
    public static final int MIN_CE_SU = 1;
    public static final int MAX_CE_SU = 16_384;
    public static final int BASE_CE_COST_PER_MINUTE = 15;
    public static final int CE_STORAGE_MINUTES = 5;
    private static final int TICKS_PER_MINUTE = 20 * 60;

    private final Map<SpaceTowerEnergyType, IOType> modes = new EnumMap<>(SpaceTowerEnergyType.class);
    private int lpEnergy;
    private double jouleBuffer;
    private double ceStoredLp;
    private double ceOutputDebt;
    private double ceInputAccumulator;
    private int ceRpm = DEFAULT_CE_RPM;
    private int ceSu = DEFAULT_CE_SU;
    private boolean ceOutputPowered;
    private boolean queuedKineticRefresh;

    public SpaceTowerBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.SPACE_TOWER_BLOCK_ENTITY.get(), pos, state);
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            modes.put(type, type == SpaceTowerEnergyType.LP ? IOType.OUTPUT : IOType.INPUT);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public IOType getMode(SpaceTowerEnergyType type) {
        return modes.getOrDefault(type, IOType.INPUT);
    }

    public void toggleMode(SpaceTowerEnergyType type) {
        setMode(type, getMode(type) == IOType.INPUT ? IOType.OUTPUT : IOType.INPUT);
    }

    public void setMode(SpaceTowerEnergyType type, IOType mode) {
        modes.put(type, mode);
        refreshKinetics();
        updateCeOutputPoweredState();
        sync();
    }

    public int getCeRpm() {
        return ceRpm;
    }

    public int getCeSu() {
        return ceSu;
    }

    public void adjustCeRpm(int delta) {
        setCeRpm(ceRpm + delta);
    }

    public void adjustCeSu(int delta) {
        setCeSu(ceSu + delta);
    }

    public void setCeRpm(int rpm) {
        ceRpm = Mth.clamp(roundToStep(rpm, 2), MIN_CE_RPM, MAX_CE_RPM);
        clampCeStorage();
        refreshKinetics();
        sync();
    }

    public void setCeSu(int su) {
        ceSu = Mth.clamp(su, MIN_CE_SU, MAX_CE_SU);
        clampCeStorage();
        refreshKinetics();
        sync();
    }

    public int getCeCostPerMinute() {
        return getCeCostPerMinute(ceRpm, ceSu);
    }

    public int getMaxCeStoredLp() {
        return getCeCostPerMinute() * CE_STORAGE_MINUTES;
    }

    public int getCeStoredLpDisplay() {
        return (int)Math.floor(ceStoredLp);
    }

    public int getJouleBufferDisplay() {
        return (int)Math.floor(jouleBuffer);
    }

    public int receiveEnergyAsType(SpaceTowerEnergyType type, double amount) {
        if (amount <= 0.0D || getMode(type) != IOType.INPUT) {
            return 0;
        }

        if (type == SpaceTowerEnergyType.CE) {
            boolean wasPowered = canGenerateCeOutput();
            double before = ceStoredLp;
            ceStoredLp = Math.min(getMaxCeStoredLp(), ceStoredLp + amount);
            updateCeOutputPoweredState(wasPowered);
            sync();
            return (int)Math.floor(ceStoredLp - before);
        }

        jouleBuffer += amount * type.joulesPerUnit();
        int lp = (int)Math.floor(jouleBuffer / SpaceTowerEnergyType.LP.joulesPerUnit());
        int received = receiveLpIgnoringMode(lp);
        jouleBuffer -= received * SpaceTowerEnergyType.LP.joulesPerUnit();
        if (received > 0) {
            sync();
        }
        return received;
    }

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

        double requestedJoules = requestedAmount * type.joulesPerUnit();
        int requestedLp = (int)Math.ceil(requestedJoules / SpaceTowerEnergyType.LP.joulesPerUnit());
        int extractedLp = extractLpIgnoringMode(requestedLp);
        if (extractedLp > 0) {
            sync();
        }
        return extractedLp * SpaceTowerEnergyType.LP.joulesPerUnit() / type.joulesPerUnit();
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
        return LP_CAPACITY;
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
        int actualRpm = Math.max(MIN_CE_RPM, Math.round(Math.abs(getSpeed())));
        int actualStressUnits = getActualInputStressUnits(actualRpm);
        double lpPerTick = getCeCostPerMinute(actualRpm, actualStressUnits) / (double)TICKS_PER_MINUTE;
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

    private static int getCeCostPerMinute(int rpm, int stressUnits) {
        int rpmExtra = Math.max(0, (roundToStep(rpm, 2) - DEFAULT_CE_RPM) / 2);
        int suExtra = Math.max(0, (stressUnits - DEFAULT_CE_SU + 3) / 4);
        return BASE_CE_COST_PER_MINUTE + rpmExtra + suExtra;
    }

    private void pushEnergy() {
        if (level == null || level.isClientSide || getMode(SpaceTowerEnergyType.LP) != IOType.OUTPUT) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));

            if (neighbor instanceof ILatexEnergyHandler handler) {
                int extracted = this.extractEnergy(100, dir);
                int received = handler.receiveEnergy(extracted, dir.getOpposite());

                if (received < extracted) {
                    this.receiveLpIgnoringMode(extracted - received);
                }
            }
        }
    }

    private int receiveLpIgnoringMode(int amount) {
        boolean wasPowered = canGenerateCeOutput();
        int accepted = Math.min(LP_CAPACITY - lpEnergy, Math.max(0, amount));
        lpEnergy += accepted;
        if (accepted > 0) {
            setChanged();
            updateCeOutputPoweredState(wasPowered);
        }
        return accepted;
    }

    private int extractLpIgnoringMode(int amount) {
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
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static int roundToStep(int value, int step) {
        return Math.round(value / (float)step) * step;
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

        CompoundTag modesTag = new CompoundTag();
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            modesTag.putString(type.name(), getMode(type).name());
        }
        tag.put("Modes", modesTag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        lpEnergy = Mth.clamp(tag.getInt("LpEnergy"), 0, LP_CAPACITY);
        jouleBuffer = tag.getDouble("JouleBuffer");
        ceStoredLp = tag.getDouble("CeStoredLp");
        ceOutputDebt = tag.getDouble("CeOutputDebt");
        ceInputAccumulator = tag.getDouble("CeInputAccumulator");
        ceRpm = tag.contains("CeRpm") ? tag.getInt("CeRpm") : DEFAULT_CE_RPM;
        ceSu = tag.contains("CeSu") ? tag.getInt("CeSu") : DEFAULT_CE_SU;
        ceOutputPowered = false;

        if (tag.contains("Modes")) {
            CompoundTag modesTag = tag.getCompound("Modes");
            for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
                if (modesTag.contains(type.name())) {
                    try {
                        modes.put(type, IOType.valueOf(modesTag.getString(type.name())));
                    } catch (IllegalArgumentException ignored) {
                        modes.put(type, type == SpaceTowerEnergyType.LP ? IOType.OUTPUT : IOType.INPUT);
                    }
                }
            }
        }

        ceRpm = Mth.clamp(roundToStep(ceRpm, 2), MIN_CE_RPM, MAX_CE_RPM);
        ceSu = Mth.clamp(ceSu, MIN_CE_SU, MAX_CE_SU);
        clampCeStorage();
        if (!clientPacket) {
            queueKineticRefresh();
        }
    }
}
