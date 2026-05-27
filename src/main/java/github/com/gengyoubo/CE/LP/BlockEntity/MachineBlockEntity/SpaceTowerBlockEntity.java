package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

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
import java.util.Map;

public class SpaceTowerBlockEntity extends BlockEntity implements ILatexEnergyHandler, SpaceTowerAccess {
    public static final int LP_CAPACITY = 50_000;
    public static final int DEFAULT_CE_RPM = 8;
    public static final int DEFAULT_CE_SU = 256;
    public static final int MIN_CE_RPM = 2;
    public static final int MAX_CE_RPM = 256;
    public static final int MIN_CE_SU = 1;
    public static final int MAX_CE_SU = 16_384;
    public static final int BASE_CE_COST_PER_MINUTE = 15;
    public static final int CE_STORAGE_MINUTES = 5;

    private final Map<SpaceTowerEnergyType, IOType> modes = new EnumMap<>(SpaceTowerEnergyType.class);
    private int lpEnergy;
    private double jouleBuffer;
    private double ceStoredLp;
    private int ceRpm = DEFAULT_CE_RPM;
    private int ceSu = DEFAULT_CE_SU;
    private final SpaceTowerForgeEnergyStorage forgeEnergy = new SpaceTowerForgeEnergyStorage(this);
    private final LazyOptional<IEnergyStorage> forgeEnergyCapability = LazyOptional.of(() -> forgeEnergy);

    public SpaceTowerBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.SPACE_TOWER_BLOCK_ENTITY.get(), pos, state);
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            modes.put(type, type == SpaceTowerEnergyType.LP ? IOType.OUTPUT : IOType.INPUT);
        }
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        pushEnergy();
        SpaceTowerForgeEnergyPusher.push(level, worldPosition, this);
    }

    public IOType getMode(SpaceTowerEnergyType type) {
        return modes.getOrDefault(type, IOType.INPUT);
    }

    public void toggleMode(SpaceTowerEnergyType type) {
        setMode(type, getMode(type) == IOType.INPUT ? IOType.OUTPUT : IOType.INPUT);
    }

    public void setMode(SpaceTowerEnergyType type, IOType mode) {
        modes.put(type, mode);
        notifyNeighbors();
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
        int oldCost = getCeCostPerMinute();
        ceRpm = Mth.clamp(roundToStep(rpm, 2), MIN_CE_RPM, MAX_CE_RPM);
        rescaleCeStorage(oldCost);
        sync();
    }

    public void setCeSu(int su) {
        int oldCost = getCeCostPerMinute();
        ceSu = Mth.clamp(su, MIN_CE_SU, MAX_CE_SU);
        rescaleCeStorage(oldCost);
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

    @Override
    public int receiveEnergyAsType(SpaceTowerEnergyType type, double amount) {
        if (amount <= 0.0D || getMode(type) != IOType.INPUT) {
            return 0;
        }

        if (type == SpaceTowerEnergyType.CE) {
            double before = ceStoredLp;
            ceStoredLp = Math.min(getMaxCeStoredLp(), ceStoredLp + amount);
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

    @Override
    public double extractEnergyAsType(SpaceTowerEnergyType type, double requestedAmount) {
        if (requestedAmount <= 0.0D || getMode(type) != IOType.OUTPUT) {
            return 0.0D;
        }

        if (type == SpaceTowerEnergyType.CE) {
            double extracted = Math.min(ceStoredLp, requestedAmount);
            ceStoredLp -= extracted;
            if (extracted > 0.0D) {
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
    public void refundEnergyAsType(SpaceTowerEnergyType type, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        if (type == SpaceTowerEnergyType.CE) {
            ceStoredLp = Math.min(getMaxCeStoredLp(), ceStoredLp + amount);
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
        return LP_CAPACITY;
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

    protected int receiveLpIgnoringMode(int amount) {
        int accepted = Math.min(LP_CAPACITY - lpEnergy, Math.max(0, amount));
        lpEnergy += accepted;
        if (accepted > 0) {
            setChanged();
        }
        return accepted;
    }

    protected int extractLpIgnoringMode(int amount) {
        int extracted = Math.min(lpEnergy, Math.max(0, amount));
        lpEnergy -= extracted;
        if (extracted > 0) {
            setChanged();
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

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void notifyNeighbors() {
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    private static int roundToStep(int value, int step) {
        return Math.round(value / (float)step) * step;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("LpEnergy", lpEnergy);
        tag.putDouble("JouleBuffer", jouleBuffer);
        tag.putDouble("CeStoredLp", ceStoredLp);
        tag.putInt("CeRpm", ceRpm);
        tag.putInt("CeSu", ceSu);

        CompoundTag modesTag = new CompoundTag();
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            modesTag.putString(type.name(), getMode(type).name());
        }
        tag.put("Modes", modesTag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        lpEnergy = Mth.clamp(tag.getInt("LpEnergy"), 0, LP_CAPACITY);
        jouleBuffer = tag.getDouble("JouleBuffer");
        ceStoredLp = tag.getDouble("CeStoredLp");
        ceRpm = tag.contains("CeRpm") ? tag.getInt("CeRpm") : DEFAULT_CE_RPM;
        ceSu = tag.contains("CeSu") ? tag.getInt("CeSu") : DEFAULT_CE_SU;

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
    }
}
