package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Coord4D;
import mekanism.api.chemical.gas.attribute.GasAttributes;
import mekanism.api.radiation.IRadiationManager;
import mekanism.common.registries.MekanismGases;
import mekanism.common.util.HeatUtils;
import mekanism.generators.common.config.MekanismGeneratorsConfig;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Counts upgrades belonging to this formed reactor; unrelated and unloaded blocks never contribute. */
public final class FissionUpgradeState {
    private final List<BlockPos> positions = new ArrayList<>();
    private final EnumMap<FissionUpgrade, Integer> installed = new EnumMap<>(FissionUpgrade.class);
    private boolean discovered;

    public void beforeTick(FissionReactorMultiblockData reactor, Level level) {
        if (!discovered) {
            for (BlockPos pos : reactor.locations) {
                if (level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof FissionUpgradeBlock)
                    positions.add(pos.immutable());
            }
            discovered = true;
        }
        installed.clear();
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockState(pos).getBlock() instanceof FissionUpgradeBlock block) {
                installed.merge(block.upgrade, 1, Integer::sum);
                if (level.getBlockEntity(pos) instanceof ReactorWaterSource source) source.supplyWater(reactor);
            }
        }
        // Supply comes first; trip before burning, including the very first tick after activation.
        if (has(FissionUpgrade.SCRAM) && reactor.isActive() && reactor.gasCoolantTank.isEmpty()) {
            double demand = predictedWaterPerTick(reactor);
            if (demand > 0 && reactor.fluidCoolantTank.getFluidAmount() / demand < 200.0)
                reactor.setActive(false);
        }
    }

    public int count(FissionUpgrade upgrade) { return installed.getOrDefault(upgrade, 0); }
    public boolean has(FissionUpgrade upgrade) { return count(upgrade) > 0; }
    public double outputMultiplier() { return 1 + count(FissionUpgrade.PROCESSOR_X) * FissionConfig.OUTPUT_BONUS.get(); }
    public double fuelUsage() { return 1 / (1 + count(FissionUpgrade.PROCESSOR_X) * FissionConfig.FUEL_EFFICIENCY.get()); }
    public double returnChance() { return Math.min(1, count(FissionUpgrade.PROCESSOR_Y) * FissionConfig.RETURN_CHANCE.get()); }

    public double predictedWaterPerTick(FissionReactorMultiblockData reactor) {
        double burn = Math.min(reactor.rateLimit, reactor.getMaxBurnRate());
        double output = outputMultiplier();
        double fuelHeat = burn * output * MekanismGeneratorsConfig.generators.energyPerFissionFuel.get().doubleValue();
        double currentHeat = Math.max(0, reactor.getBoilEfficiency()
                * (reactor.heatCapacitor.getTemperature() - HeatUtils.BASE_BOIL_TEMP)
                * reactor.heatCapacitor.getHeatCapacity() * 0.5);
        double steam = Math.max(reactor.lastBoilRate,
                Math.max(fuelHeat, currentHeat) * HeatUtils.getSteamEnergyEfficiency() / HeatUtils.getWaterThermalEnthalpy());
        return steam <= 0 ? 0 : Math.ceil(steam * waterUsage());
    }

    public double waterUsage() { return 1 / (1 + count(FissionUpgrade.COOLING) * FissionConfig.COOLING_EFFICIENCY.get()); }

    /** Water path only. Sodium and unupgraded reactors use the native method unchanged. */
    public void cool(FissionReactorMultiblockData reactor) {
        double heat = reactor.getBoilEfficiency() * (reactor.heatCapacitor.getTemperature() - HeatUtils.BASE_BOIL_TEMP)
                * reactor.heatCapacitor.getHeatCapacity() * 0.5;
        double steamDemand = Math.max(0, heat * HeatUtils.getSteamEnergyEfficiency() / HeatUtils.getWaterThermalEnthalpy());
        long steam = (long) Math.min(steamDemand, Math.floor(reactor.fluidCoolantTank.getFluidAmount() / waterUsage()));
        reactor.lastBoilRate = steam;
        if (steam <= 0) return;
        int used = (int) Math.min(reactor.fluidCoolantTank.getFluidAmount(), Math.ceil(steam * waterUsage()));
        reactor.fluidCoolantTank.shrinkStack(used, Action.EXECUTE);
        reactor.heatedCoolantTank.insert(MekanismGases.STEAM.getStack(steam), Action.EXECUTE, AutomationType.INTERNAL);
        reactor.heatCapacitor.handleHeat(-steam * HeatUtils.getWaterThermalEnthalpy() / HeatUtils.getSteamEnergyEfficiency());
    }

    /** Uses native fractional fuel/waste fields so cache merge, save and restart retain all balances. */
    public void burn(FissionReactorMultiblockData reactor, Level level) {
        double usage = fuelUsage();
        double output = outputMultiplier();
        double available = reactor.fuelTank.getStored() + reactor.burnRemaining;
        double baseBurn = Math.min(Math.min(reactor.rateLimit, reactor.getMaxBurnRate()), available / usage);
        double consumed = baseBurn * usage;
        double remaining = Math.max(0, available - consumed);
        if (consumed > 0 && has(FissionUpgrade.PROCESSOR_Y) && level.random.nextDouble() < returnChance())
            remaining += consumed;
        // Never create fuel beyond the tank capacity; only refund what this tick actually consumed.
        remaining = Math.min(remaining, reactor.fuelTank.getCapacity());
        reactor.fuelTank.setStack(MekanismGases.FISSILE_FUEL.getStack((long) remaining));
        reactor.burnRemaining = remaining % 1;
        double effectiveBurn = baseBurn * output;
        reactor.lastBurnRate = effectiveBurn;
        reactor.heatCapacitor.handleHeat(effectiveBurn * MekanismGeneratorsConfig.generators.energyPerFissionFuel.get().doubleValue());
        reactor.partialWaste += effectiveBurn;
        long waste = (long) reactor.partialWaste;
        reactor.partialWaste %= 1;
        if (waste > 0) {
            var excess = reactor.wasteTank.insert(MekanismGases.NUCLEAR_WASTE.getStack(waste), Action.EXECUTE, AutomationType.INTERNAL);
            if (!excess.isEmpty() && IRadiationManager.INSTANCE.isRadiationEnabled()) {
                excess.ifAttributePresent(GasAttributes.Radiation.class, radiation -> IRadiationManager.INSTANCE.radiate(
                        new Coord4D(reactor.getBounds().getCenter(), level), excess.getAmount() * radiation.getRadioactivity()));
            }
        }
        reactor.markDirty();
    }
}
