package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.Locale;
import java.util.Map;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.tile.interfaces.ISustainedData;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public final class FissionThermoelectricBlockEntity extends TileEntityFissionReactorCasing implements ISustainedData {
    public static final String DATA = "CEThermoelectric";
    public enum Status { UNFORMED, IDLE, GENERATING, POWER_LIMITED, SATURATED, WARNING, FUSED }
    private final ThermoelectricEnergy energy = new ThermoelectricEnergy(
            () -> FissionConfig.THERMO_CAPACITY.get(), () -> FissionConfig.THERMO_OUTPUT.get(),
            () -> level == null ? 0 : level.getGameTime(), this::markForSave);
    private LazyOptional<IEnergyStorage> forgeEnergy = LazyOptional.of(() -> energy);
    private LazyOptional<IStrictEnergyHandler> strictEnergy = LazyOptional.of(() -> energy);
    private boolean fused;
    private Status status = Status.UNFORMED;
    private double lastHeat, lastElectricity, lastPassive, lastEnvironment;

    public FissionThermoelectricBlockEntity(IBlockProvider provider, BlockPos pos, BlockState state) { super(provider, pos, state); }
    public ThermoelectricEnergy energy() { return energy; }
    public boolean isFused() { return fused; }
    public Status status() { return fused ? Status.FUSED : status; }
    public double lastHeat() { return lastHeat; }
    public double lastElectricity() { return lastElectricity; }
    public double lastPassive() { return lastPassive; }
    public double lastEnvironment() { return lastEnvironment; }

    public void fuse() {
        if (!fused) { fused = true; record(ThermoelectricModel.Transfer.ZERO, Status.FUSED); markForSave(); updateVisual(); }
    }
    public void record(ThermoelectricModel.Transfer transfer, Status state) {
        lastHeat = transfer.heat(); lastElectricity = transfer.electricity();
        lastPassive = transfer.passiveHeat(); lastEnvironment = transfer.environmentalHeat(); status = state;
    }
    public boolean repair(Player player) {
        var reactor = getMultiblock();
        if (!fused) return false;
        if (!reactor.isFormed() || reactor.isActive() || reactor.heatCapacitor.getTemperature() >= ThermoelectricModel.START_TEMPERATURE) {
            player.displayClientMessage(Component.translatable("message.changede.thermo.repair_unsafe"), true);
            return false;
        }
        fused = false; status = Status.IDLE; markForSave(); updateVisual();
        player.displayClientMessage(Component.translatable("message.changede.thermo.repaired"), true);
        return true;
    }
    public void describe(Player player) {
        summary().forEach(player::sendSystemMessage);
    }
    public java.util.List<Component> summary() {
        var lines = new java.util.ArrayList<Component>();
        var reactor = getMultiblock();
        lines.add(Component.translatable("message.changede.thermo.status", Component.translatable("state.changede.thermo." + status().name().toLowerCase(Locale.ROOT))));
        lines.add(Component.translatable("message.changede.thermo.rates", number(lastHeat), number(ThermoelectricEnergy.toFE(lastElectricity)), number(lastPassive)));
        lines.add(Component.translatable("message.changede.thermo.energy", number(energy.storedJoules()), number(FissionConfig.THERMO_CAPACITY.get())));
        if (reactor.isFormed()) {
            var thermal = ((FissionUpgradeAccess) reactor).changede$fissionUpgrades().thermal();
            lines.add(Component.translatable("message.changede.thermo.modules", thermal.intactCount(), thermal.fusedCount()));
        }
        return lines;
    }
    public static String number(double value) { return String.format(Locale.ROOT, "%.2f", value); }

    @Override protected boolean onUpdateServer(FissionReactorMultiblockData reactor) {
        boolean packet = super.onUpdateServer(reactor);
        if (!reactor.isFormed()) record(ThermoelectricModel.Transfer.ZERO, fused ? Status.FUSED : Status.UNFORMED);
        pushEnergy(reactor);
        updateVisual();
        return packet;
    }
    private void pushEnergy(FissionReactorMultiblockData reactor) {
        if (energy.storedJoules() <= 0 || level == null || isRemoved()) return;
        for (Direction side : Direction.values()) {
            BlockPos adjacent = worldPosition.relative(side);
            if (!level.hasChunkAt(adjacent) || (reactor.isFormed() && !reactor.isPositionOutsideBounds(adjacent))) continue;
            var neighbor = level.getBlockEntity(adjacent);
            if (neighbor == null) continue;
            var strict = neighbor.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY, side.getOpposite()).resolve();
            if (strict.isPresent()) {
                var available = energy.extractEnergy(0, energy.getEnergy(0), Action.SIMULATE);
                if (available.isZero()) break;
                var accepted = available.subtract(strict.get().insertEnergy(available, Action.SIMULATE));
                if (!accepted.isZero()) {
                    var sent = energy.extractEnergy(0, accepted, Action.EXECUTE);
                    var rejected = strict.get().insertEnergy(sent, Action.EXECUTE);
                    energy.refundOutput(Math.min(sent.doubleValue(), rejected.doubleValue()));
                }
            } else if (EnergyUnit.FORGE_ENERGY.isEnabled()) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).ifPresent(target -> {
                    int available = energy.extractEnergy(Integer.MAX_VALUE, true);
                    int accepted = target.receiveEnergy(available, true);
                    if (accepted > 0) {
                        int sent = energy.extractEnergy(accepted, false);
                        int received = target.receiveEnergy(sent, false);
                        energy.refundOutput(ThermoelectricEnergy.fromFE(sent - Math.max(0, Math.min(sent, received))));
                    }
                });
            }
        }
    }
    private void updateVisual() {
        if (level == null || level.isClientSide || isRemoved()) return;
        BlockState current = getBlockState();
        if (!(current.getBlock() instanceof FissionThermoelectricBlock)) return;
        boolean lit = !fused && lastHeat > 0;
        if (current.getValue(FissionThermoelectricBlock.FUSED) != fused || current.getValue(FissionThermoelectricBlock.LIT) != lit)
            level.setBlock(worldPosition, current.setValue(FissionThermoelectricBlock.FUSED, fused).setValue(FissionThermoelectricBlock.LIT, lit), 2);
    }
    @Override public void writeSustainedData(CompoundTag dataMap) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Fused", fused); energy.save(tag);
        dataMap.put(DATA, tag);
    }
    @Override public void readSustainedData(CompoundTag dataMap) {
        CompoundTag tag = dataMap.getCompound(DATA);
        fused = tag.getBoolean("Fused"); energy.load(tag);
        record(ThermoelectricModel.Transfer.ZERO, fused ? Status.FUSED : Status.UNFORMED);
    }
    @Override public Map<String, String> getTileDataRemap() { return Map.of(DATA, DATA); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY && EnergyUnit.FORGE_ENERGY.isEnabled()) return forgeEnergy.cast();
        if (cap == mekanism.common.capabilities.Capabilities.STRICT_ENERGY) return strictEnergy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { forgeEnergy.invalidate(); strictEnergy.invalidate(); super.invalidateCaps(); }
    @Override public void reviveCaps() {
        super.reviveCaps(); forgeEnergy = LazyOptional.of(() -> energy); strictEnergy = LazyOptional.of(() -> energy);
    }
}
