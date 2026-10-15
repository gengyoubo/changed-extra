package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.ArrayList;
import java.util.List;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.world.level.Level;

/** One settlement per formed reactor, using a common pre-cooling temperature for every module. */
public final class FissionThermalState {
    private List<FissionThermoelectricBlockEntity> modules = List.of();
    private long lastTick = Long.MIN_VALUE;
    private double previousTemperature, risingKelvinPerSecond;

    public void setModules(List<FissionThermoelectricBlockEntity> modules) { this.modules = List.copyOf(modules); }
    public int intactCount() { return (int) modules.stream().filter(module -> !module.isRemoved() && !module.isFused()).count(); }
    public int fusedCount() { return (int) modules.stream().filter(module -> !module.isRemoved() && module.isFused()).count(); }

    public void beforeCoolant(FissionReactorMultiblockData reactor, Level world, double ambient) {
        if (!reactor.isFormed() || world.isClientSide || lastTick == world.getGameTime()) return;
        List<FissionThermoelectricBlockEntity> intact = modules.stream()
                .filter(module -> !module.isRemoved() && module.getMultiblock() == reactor && !module.isFused()).toList();
        if (intact.isEmpty()) return; // No changes whatsoever to native heat timing without an intact module.

        // Mek defers handleHeat. Settle fuel/incoming heat before both thermal extraction and native boiling.
        reactor.heatCapacitor.update();
        double temperature = reactor.heatCapacitor.getTemperature();
        double stored = reactor.heatCapacitor.getHeat();
        if (!Double.isFinite(temperature) || !Double.isFinite(stored)) return;
        long tick = world.getGameTime();
        if (lastTick != Long.MIN_VALUE && tick == lastTick + 1) {
            double instant = (temperature - previousTemperature) * 20;
            risingKelvinPerSecond += (instant - risingKelvinPerSecond) * (1 - Math.exp(-1.0 / 20));
        } else risingKelvinPerSecond = 0;
        lastTick = tick; previousTemperature = temperature;

        if (ThermoelectricModel.mustFuse(temperature, reactor.reactorDamage)) {
            intact.forEach(FissionThermoelectricBlockEntity::fuse);
            return;
        }
        double maximum = FissionConfig.THERMO_MAX_HEAT.get();
        double request = ThermoelectricModel.request(temperature, risingKelvinPerSecond, maximum);
        double efficiency = ThermoelectricModel.efficiency(temperature, ambient, FissionConfig.THERMO_EFFICIENCY.get());
        var plans = new ArrayList<ThermoelectricModel.Transfer>(intact.size());
        double requested = 0;
        for (var module : intact) {
            var plan = ThermoelectricModel.plan(request, efficiency, module.energy().neededJoules(), FissionConfig.THERMO_PASSIVE.get());
            plans.add(plan); requested += plan.heat();
        }
        double available = Math.max(0, stored - reactor.heatCapacitor.getHeatCapacity() * Math.max(ambient, ThermoelectricModel.START_TEMPERATURE));
        double scale = ThermoelectricModel.allocationScale(available, requested);
        double removed = 0;
        for (int index = 0; index < intact.size(); index++) {
            var module = intact.get(index);
            var plan = plans.get(index).scale(scale);
            double generated = module.energy().generate(plan.electricity());
            // Account only energy really accepted, including any tiny floating-point clamp at the buffer boundary.
            double converting = efficiency > 0 ? generated / efficiency : 0;
            double heat = Math.min(plan.heat(), converting + plan.passiveHeat());
            var actual = new ThermoelectricModel.Transfer(heat, generated, plan.passiveHeat(), Math.max(0, heat - generated));
            FissionThermoelectricBlockEntity.Status status;
            if (temperature >= ThermoelectricModel.WARNING_TEMPERATURE) status = FissionThermoelectricBlockEntity.Status.WARNING;
            else if (heat <= 0) status = FissionThermoelectricBlockEntity.Status.IDLE;
            else if (generated + 1.0E-7 < request * scale * efficiency) status = FissionThermoelectricBlockEntity.Status.POWER_LIMITED;
            else if (request >= maximum && maximum > 0) status = FissionThermoelectricBlockEntity.Status.SATURATED;
            else status = FissionThermoelectricBlockEntity.Status.GENERATING;
            module.record(actual, status);
            removed += heat;
        }
        if (removed > 0) {
            reactor.heatCapacitor.setHeat(stored - Math.min(removed, available));
            reactor.markDirty();
        }
    }

    public void checkFuses(FissionReactorMultiblockData reactor) {
        if (ThermoelectricModel.mustFuse(reactor.heatCapacitor.getTemperature(), reactor.reactorDamage))
            modules.forEach(FissionThermoelectricBlockEntity::fuse);
    }
    public void meltdown(FissionReactorMultiblockData reactor, Level world) {
        for (var pos : reactor.locations) {
            if (world.hasChunkAt(pos) && world.getBlockEntity(pos) instanceof FissionThermoelectricBlockEntity module) module.fuse();
        }
    }
}
