package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.math.FloatingLong;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.HeatUtils;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;

/** Opt-in real-server checks, invoked by the existing disposable reactor fixture. */
final class FissionThermoelectricChecks {
    static void prepareNativeTick(ServerLevel level, FissionReactorMultiblockData reactor, List<BlockPos> positions) {
        check(reactor.isFormed(), "native fixture formed before running thermal tick");
        reactor.setActive(false);
        reactor.reactorDamage = 0;
        reactor.heatCapacitor.setHeat(1100 * reactor.heatCapacitor.getHeatCapacity());
        reactor.fluidCoolantTank.setStack(new FluidStack(Fluids.WATER, reactor.fluidCoolantTank.getCapacity()));
        for (BlockPos pos : positions) ((FissionThermoelectricBlockEntity) level.getBlockEntity(pos)).energy().loadJoules(0);
    }
    private static void check(boolean value, String message) { FissionRegressionChecks.check(value, "Thermo: " + message); }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0E-6, message + " (" + actual + ", expected " + expected + ")");
    }
    static void verify(ServerLevel level, FissionReactorMultiblockData reactor, List<BlockPos> positions) {
        energy();
        var modules = positions.stream().map(pos -> (FissionThermoelectricBlockEntity) level.getBlockEntity(pos)).toList();
        check(modules.size() == 2 && modules.stream().allMatch(m -> m.getMultiblock() == reactor), "modules share the actual formed reactor");
        for (var module : modules) for (Direction side : Direction.values()) {
            check(module.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, side).resolve().orElseThrow() == module.energy(), "FE faces share storage");
            check(module.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY, side).resolve().orElseThrow() == module.energy(), "Mek faces share storage");
        }
        double capacity = reactor.heatCapacitor.getHeatCapacity();
        near(modules.stream().mapToDouble(FissionThermoelectricBlockEntity::lastHeat).sum(), 200_000, "native reactor tick executes thermal extraction before boiling");
        double remainingTemperature = 1100 - 200_000 / capacity;
        double steamHeat = reactor.getBoilEfficiency() * (remainingTemperature - HeatUtils.BASE_BOIL_TEMP) * capacity * .5;
        var state = ((FissionUpgradeAccess) reactor).changede$fissionUpgrades();
        long expectedSteam = (long) Math.min(steamHeat * HeatUtils.getSteamEnergyEfficiency() / HeatUtils.getWaterThermalEnthalpy(),
                Math.floor(reactor.fluidCoolantTank.getCapacity() / state.waterUsage()));
        near(reactor.lastBoilRate, expectedSteam, "native boiling uses remaining heat, without double credit");
        double boiling = reactor.lastBoilRate * HeatUtils.getWaterThermalEnthalpy() / HeatUtils.getSteamEnergyEfficiency();
        near(reactor.heatCapacitor.getHeat(), 1100 * capacity - 200_000 - boiling - reactor.lastEnvironmentLoss * capacity,
                "real tick conserves thermal extraction, steam heat and native environmental loss");
        reactor.setActive(false);
        reactor.reactorDamage = 0;
        reset(modules);
        reactor.heatCapacitor.setHeat(1100 * capacity);
        settle(reactor, level, modules);
        near(1100 * capacity - reactor.heatCapacitor.getHeat(), 200_000, "two modules remove exactly two rated heat budgets");
        near(modules.get(0).lastHeat(), modules.get(1).lastHeat(), "identical modules receive equal heat");
        for (var module : modules) {
            near(module.lastHeat(), module.lastElectricity() + module.lastEnvironment(), "heat equals electricity plus environmental rejection");
            check(module.lastElectricity() > 0 && module.lastElectricity() < module.lastHeat(), "Carnot-adjusted conversion never mints energy");
        }
        check(!reactor.isActive() && modules.get(0).lastHeat() > 0, "stopped reactor still cools residual heat");
        for (var module : modules) module.energy().loadJoules(FissionConfig.THERMO_CAPACITY.get());
        reactor.heatCapacitor.setHeat(1100 * capacity);
        settle(reactor, level, modules);
        near(1100 * capacity - reactor.heatCapacitor.getHeat(), 50_000, "full buffers retain 25 percent passive cooling per module");
        near(modules.get(0).lastElectricity(), 0, "full buffer generates no electricity");
        reset(modules);
        reactor.heatCapacitor.setHeat(449 * capacity);
        settle(reactor, level, modules);
        near(reactor.heatCapacitor.getHeat(), 449 * capacity, "cold reactor is not cooled below startup threshold");
        reactor.heatCapacitor.handleHeat(651 * capacity);
        settle(reactor, level, modules);
        near(reactor.heatCapacitor.getHeat(), 1100 * capacity - 200_000, "queued fuel/incoming heat settles before extraction");
        reset(modules);
        double oldMaximum = FissionConfig.THERMO_MAX_HEAT.get();
        try {
            FissionConfig.THERMO_MAX_HEAT.set(1.0E9);
            reactor.heatCapacitor.setHeatCapacity(1, false);
            reactor.heatCapacitor.setHeat(450.01);
            settle(reactor, level, modules);
            near(reactor.heatCapacitor.getTemperature(), 450, "shared heat budget preserves temperature floor under actual saturation");
            near(modules.get(0).lastHeat(), modules.get(1).lastHeat(), "scarce heat allocation is fair");
        } finally {
            FissionConfig.THERMO_MAX_HEAT.set(oldMaximum);
            reactor.heatCapacitor.setHeatCapacity(capacity, false);
        }
        reset(modules);
        reactor.heatCapacitor.setHeat(1800 * capacity);
        settle(reactor, level, modules);
        check(modules.stream().allMatch(FissionThermoelectricBlockEntity::isFused), "all modules fuse from same hot snapshot");
        near(reactor.heatCapacitor.getHeat(), 1800 * capacity, "fusion happens before any protective extraction");
        var module = modules.get(0);
        module.energy().loadJoules(12345.6789123);
        check(module.energy().extractEnergy(1, true) == 1, "fused module can still export stored electricity");
        var saved = module.saveWithFullMetadata();
        var reloaded = (FissionThermoelectricBlockEntity) module.getType().create(module.getBlockPos(), module.getBlockState());
        reloaded.load(saved);
        check(reloaded.isFused(), "block entity save/load preserves fuse");
        near(reloaded.energy().storedJoules(), module.energy().storedJoules(), "block entity save/load preserves fractional Joules");
        var drops = Block.getDrops(module.getBlockState(), level, module.getBlockPos(), module);
        check(drops.size() == 1 && ItemDataUtils.getCompound(drops.get(0), FissionThermoelectricBlockEntity.DATA).getBoolean("Fused"), "actual survival loot preserves fuse");
        near(ItemDataUtils.getCompound(drops.get(0), FissionThermoelectricBlockEntity.DATA).getDouble("Joules"), 12345.6789123, "actual survival loot preserves fractional energy");
        BlockPos placed = new BlockPos(60, 120, 64);
        var block = FissionCompat.UPGRADES.get(FissionUpgrade.THERMOELECTRIC).getBlock();
        level.setBlockAndUpdate(placed, block.defaultBlockState());
        var player = FakePlayerFactory.getMinecraft(level);
        block.setPlacedBy(level, placed, level.getBlockState(placed), player, drops.get(0));
        var replaced = (FissionThermoelectricBlockEntity) level.getBlockEntity(placed);
        check(replaced.isFused(), "placing the dropped module does not repair it");
        near(replaced.energy().storedJoules(), 12345.6789123, "placement restores stored energy");
        check(!replaced.repair(player), "unformed module cannot be repaired");
        level.removeBlock(placed, false);
        check(!module.repair(player), "hot module cannot be repaired");
        reactor.heatCapacitor.setHeat(300 * capacity);
        reactor.setActive(true);
        check(!module.repair(player), "running reactor cannot be repaired");
        reactor.setActive(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FissionCompat.HEAT_EXCHANGE_CORE.get(), 2));
        block.use(module.getBlockState(), level, module.getBlockPos(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(module.getBlockPos()), Direction.NORTH, module.getBlockPos(), false));
        check(!module.isFused() && player.getMainHandItem().getCount() == 1, "safe right-click repair consumes exactly one core");
        near(module.energy().storedJoules(), 12345.6789123, "repair retains energy");
        check(modules.get(1).repair(player), "second module repairs independently");
        reactor.reactorDamage = 100;
        reactor.heatCapacitor.setHeat(1200 * capacity);
        settle(reactor, level, modules);
        check(modules.stream().allMatch(FissionThermoelectricBlockEntity::isFused), "100 percent damage at 1200 K also fuses all modules");
        reactor.reactorDamage = 0;
        reactor.heatCapacitor.setHeat(300 * capacity);
        for (var tile : modules) check(tile.repair(player), "cold repair restores casing without unforming reactor");
        check(reactor.isFormed(), "visual state changes preserve native structure");
        check(module.summary().size() == 4, "Jade and inspection expose status, rates, energy and module counts");
        state.thermal().setModules(List.of());
        reactor.meltdownHappened(level);
        check(modules.stream().allMatch(FissionThermoelectricBlockEntity::isFused), "actual meltdown callback finds modules without cached discovery");
        reactor.reactorDamage = 0;
        state.beforeTick(reactor, level);
        for (var tile : modules) tile.repair(player);
        reset(modules);
        // The following ME test starts cold: do not feed it this deliberately hot fixture's prior steam demand.
        reactor.lastBoilRate = 0;
    }
    private static void reset(List<FissionThermoelectricBlockEntity> modules) {
        modules.forEach(module -> module.energy().loadJoules(0));
    }
    private static void settle(FissionReactorMultiblockData reactor, ServerLevel level, List<FissionThermoelectricBlockEntity> modules) {
        var thermal = new FissionThermalState();
        thermal.setModules(modules);
        thermal.beforeCoolant(reactor, level, 300);
    }
    private static void energy() {
        long[] tick = {100};
        var storage = new ThermoelectricEnergy(() -> 2_500_000, () -> 16_000, () -> tick[0], () -> {});
        storage.loadJoules(ThermoelectricEnergy.fromFE(100_000) + .1234567);
        double initial = storage.storedJoules();
        check(storage.receiveEnergy(1000, false) == 0 && !storage.canReceive(), "FE insertion refused");
        near(storage.insertEnergy(0, FloatingLong.create(100), Action.EXECUTE).doubleValue(), 100, "Mek insertion refused");
        check(storage.extractEnergy(10_000, true) == 10_000, "FE simulation reports availability");
        near(storage.storedJoules(), initial, "simulation preserves exact fractional storage");
        check(storage.extractEnergy(10_000, false) == 10_000, "first side exports 10000 FE");
        double strict = storage.extractEnergy(0, FloatingLong.create(1_000_000), Action.EXECUTE).doubleValue();
        near(strict, ThermoelectricEnergy.fromFE(6000), "strict and FE handlers share one output cap");
        check(storage.extractEnergy(1, false) == 0, "another side cannot bypass exhausted tick budget");
        var saved = new CompoundTag(); storage.save(saved);
        var restored = new ThermoelectricEnergy(() -> 2_500_000, () -> 16_000, () -> tick[0], () -> {});
        restored.load(saved);
        near(restored.storedJoules(), storage.storedJoules(), "storage persistence retains fractional balance");
        check(restored.extractEnergy(1, false) == 0, "same-tick reload cannot reset output cap");
        tick[0]++;
        check(restored.extractEnergy(16000, false) == 16000, "next tick renews output budget");
        var ratio = MekanismConfig.general.forgeConversionRate.get();
        try {
            MekanismConfig.general.forgeConversionRate.set(FloatingLong.create(5));
            tick[0]++;
            double before = restored.storedJoules();
            check(restored.extractEnergy(1000, false) == 1000, "exports FE with server conversion setting");
            near(before - restored.storedJoules(), 5000, "conversion uses 5 J per FE when configured");
        } finally { MekanismConfig.general.forgeConversionRate.set(ratio); }
        storage.loadJoules(10_000_000);
        near(storage.generate(100), 0, "lowered capacity blocks generation without deleting existing balance");
        tick[0]++;
        double before = storage.storedJoules();
        int exported = storage.extractEnergy(1000, false);
        storage.refundOutput(ThermoelectricEnergy.fromFE(exported));
        near(storage.storedJoules(), before, "rejected export refunds energy even above reduced capacity");
    }
    private FissionThermoelectricChecks() {}
}
