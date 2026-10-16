package github.com.gengyoubo.CE.compat.mekanism.fission;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.items.contents.CellConfig;
import mekanism.api.Action;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.registries.MekanismGases;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/** Isolated so the non-AE server can run the same reactor regression checks. */
final class FissionAe2Checks {
    private static FissionMeInterface throughputPort;
    private static FissionReactorMultiblockData throughputReactor;
    private static IStorageProvider throughputProvider;
    private static double initialEnergy;
    private static double idleCost;
    private static int throughputTicks;
    private static final int WATER_PER_TICK = 38_400_000;

    static void prepare(ServerLevel level, BlockPos port) {
        level.setBlockAndUpdate(port.west(), AEBlocks.ENERGY_CELL.block().defaultBlockState());
        var energyCell = (EnergyCellBlockEntity) level.getBlockEntity(port.west());
        energyCell.injectAEPower(8_000, Actionable.MODULATE);
    }

    static void verify(ServerLevel level, FissionReactorMultiblockData reactor, BlockPos pos) {
        var port = (FissionMeInterface) level.getBlockEntity(pos);
        var node = port.getMainNode();
        FissionRegressionChecks.check(node.isReady() && node.isActive(), "Real ME node has power and channel");
        var grid = node.getGrid();
        var cell = StorageCells.getCellInventory(AEItems.FLUID_CELL_64K.stack(), null);
        var water = AEFluidKey.of(Fluids.WATER);
        var lava = AEFluidKey.of(Fluids.LAVA);
        var source = IActionSource.ofMachine(port);
        long offered = 30_000_000;
        long waterStored = cell.insert(water, offered, Actionable.MODULATE, source);
        long lavaStored = cell.insert(lava, 10_000, Actionable.MODULATE, source);
        FissionRegressionChecks.check(waterStored > reactor.fluidCoolantTank.getCapacity(), "Real fluid cell stores more than reactor capacity");
        IStorageProvider provider = mounts -> mounts.mount(cell);
        grid.getStorageService().addGlobalStorageProvider(provider);
        reactor.setActive(false);
        reactor.fluidCoolantTank.setEmpty();
        int capacity = reactor.fluidCoolantTank.getCapacity();
        double energyBefore = grid.getEnergyService().extractAEPower(1.0E9, Actionable.SIMULATE, PowerMultiplier.ONE);
        FissionRegressionChecks.check(energyBefore > 0 && energyBefore < capacity / 125.0,
                "Finite ordinary energy cell cannot afford the old per-volume extraction charge");
        port.supplyWater(reactor);
        FissionRegressionChecks.check(reactor.fluidCoolantTank.getFluidAmount() == capacity, "ME fills entire tank in one call without pipe/export-bus rate cap");
        FissionRegressionChecks.check(grid.getEnergyService().extractAEPower(1.0E9, Actionable.SIMULATE, PowerMultiplier.ONE) == energyBefore,
                "Water extraction adds no per-volume AE energy charge");
        double oldSetting = FissionConfig.ME_IDLE_POWER.get();
        double oldIdle = grid.getEnergyService().getIdlePowerUsage();
        try {
            FissionConfig.ME_IDLE_POWER.set(oldSetting + 8);
            port.supplyWater(reactor);
            FissionRegressionChecks.check(Math.abs(grid.getEnergyService().getIdlePowerUsage() - oldIdle - 8) < 1.0E-7,
                    "Existing ME interface applies changed fixed power configuration");
        } finally {
            FissionConfig.ME_IDLE_POWER.set(oldSetting);
            port.supplyWater(reactor);
        }
        FissionRegressionChecks.check(cell.extract(water, Long.MAX_VALUE, Actionable.SIMULATE, source) == waterStored - capacity, "ME source and reactor water balance exactly");
        FissionRegressionChecks.check(cell.extract(lava, Long.MAX_VALUE, Actionable.SIMULATE, source) == lavaStored, "Only water is extracted");
        port.supplyWater(reactor);
        FissionRegressionChecks.check(cell.extract(water, Long.MAX_VALUE, Actionable.SIMULATE, source) == waterStored - capacity, "Full reactor makes no extra extraction");
        reactor.fluidCoolantTank.setEmpty();
        reactor.gasCoolantTank.setStack(MekanismGases.SODIUM.getStack(1000));
        port.supplyWater(reactor);
        FissionRegressionChecks.check(reactor.fluidCoolantTank.isEmpty(), "ME water cannot mix into sodium cooling");
        reactor.gasCoolantTank.setEmpty();
        grid.getStorageService().removeGlobalStorageProvider(provider);
        port.supplyWater(reactor);
        FissionRegressionChecks.check(reactor.fluidCoolantTank.isEmpty(), "Unavailable ME water cannot be created");
        // Tick injection must supply before SCRAM: return the cell and begin with an empty tank.
        grid.getStorageService().addGlobalStorageProvider(provider);
        reactor.heatCapacitor.setHeat(300 * reactor.heatCapacitor.getHeatCapacity());
        reactor.fuelTank.setStack(MekanismGases.FISSILE_FUEL.getStack(100));
        reactor.rateLimit = 0.01;
        reactor.setActive(true);
        reactor.tick(level);
        FissionRegressionChecks.check(reactor.isActive() && reactor.lastBurnRate > 0 && reactor.fluidCoolantTank.getFluidAmount() > 0,
                "Tick pulls ME water before dry-start SCRAM and fuel burn");
        reactor.setActive(false);
        long scarce = cell.extract(water, Long.MAX_VALUE, Actionable.SIMULATE, source);
        reactor.fluidCoolantTank.setEmpty();
        port.supplyWater(reactor);
        FissionRegressionChecks.check(reactor.fluidCoolantTank.getFluidAmount() == Math.min(scarce, capacity),
                "Scarce ME water transfers exactly the available amount");
        long remaining = cell.extract(water, Long.MAX_VALUE, Actionable.SIMULATE, source);
        FissionRegressionChecks.check(remaining + reactor.fluidCoolantTank.getFluidAmount() == scarce, "Scarce source remains conserved");
        grid.getStorageService().removeGlobalStorageProvider(provider);
        var tag = port.saveWithFullMetadata();
        FissionRegressionChecks.check(tag.contains("FissionMeNode"), "ME node identity saved with block entity");
        // A detached receiver isolates transport throughput from fuel, boiling and SCRAM limits.
        // The real formed reactor above still verifies the normal tick integration.
        throughputPort = port;
        throughputReactor = new FissionReactorMultiblockData(port);
        throughputReactor.setShape(new VoxelCuboid(pos, pos.offset(17, 17, 17)));
        throughputReactor.setFormedForce(true);
        var creativeStack = AEItems.FLUID_CELL_CREATIVE.stack();
        CellConfig.create(creativeStack).setStack(0, new GenericStack(water, 0));
        var creativeWater = StorageCells.getCellInventory(creativeStack, null);
        throughputProvider = mounts -> mounts.mount(creativeWater);
        grid.getStorageService().addGlobalStorageProvider(throughputProvider);
        port.supplyWater(throughputReactor);
        FissionRegressionChecks.check(throughputReactor.fluidCoolantTank.getFluidAmount() == 583_200_000,
                "Finite AE energy fills a maximum-size receiver from a real creative fluid cell");
        initialEnergy = grid.getEnergyService().extractAEPower(1.0E9, Actionable.SIMULATE, PowerMultiplier.ONE);
        idleCost = grid.getEnergyService().getIdlePowerUsage() * PowerMultiplier.CONFIG.multiplier;
        throughputTicks = 0;
    }

    /** Called once per real server tick after the other reactor regression checks. */
    static boolean tick() {
        var port = throughputPort;
        var node = port.getMainNode();
        var grid = node.getGrid();
        ++throughputTicks;
        if (throughputTicks <= 200) {
            throughputReactor.fluidCoolantTank.shrinkStack(WATER_PER_TICK, Action.EXECUTE);
            port.supplyWater(throughputReactor);
            FissionRegressionChecks.check(throughputReactor.fluidCoolantTank.getFluidAmount() == 583_200_000,
                    "Sustained 38,400,000 mB/t with finite AE energy, tick " + throughputTicks);
            if (throughputTicks == 200) {
                double energyAfter = grid.getEnergyService().extractAEPower(1.0E9, Actionable.SIMULATE, PowerMultiplier.ONE);
                double spent = initialEnergy - energyAfter;
                FissionRegressionChecks.check(Math.abs(spent - 200 * idleCost) <= 2 * idleCost + 1.0E-7,
                        "200 real ticks pay only fixed network idle power while moving 7,680,000,000 mB");
                grid.getEnergyService().extractAEPower(1.0E9, Actionable.MODULATE, PowerMultiplier.ONE);
                throughputReactor.fluidCoolantTank.setEmpty();
            }
            return false;
        }
        if (throughputTicks < 205) return false;
        FissionRegressionChecks.check(!node.isActive(), "Empty ordinary energy cell leaves ME interface inactive");
        port.supplyWater(throughputReactor);
        FissionRegressionChecks.check(throughputReactor.fluidCoolantTank.isEmpty(), "Unpowered interface cannot draw even creative water");
        grid.getStorageService().removeGlobalStorageProvider(throughputProvider);
        var host = port.getCapability(appeng.capabilities.Capabilities.IN_WORLD_GRID_NODE_HOST, null);
        FissionRegressionChecks.check(host.isPresent(), "ME grid host capability exposed");
        port.setRemoved();
        port.supplyWater(throughputReactor);
        FissionRegressionChecks.check(!node.isReady() && throughputReactor.fluidCoolantTank.isEmpty(), "Removed interface disconnects and cannot extract water");
        FissionRegressionChecks.check(!host.isPresent(), "Removed interface invalidates grid host capability");
        return true;
    }
    private FissionAe2Checks() {}
}
