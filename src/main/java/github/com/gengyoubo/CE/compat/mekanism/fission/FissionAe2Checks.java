package github.com.gengyoubo.CE.compat.mekanism.fission;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import mekanism.common.registries.MekanismGases;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/** Isolated so the non-AE server can run the same reactor regression checks. */
final class FissionAe2Checks {
    static void prepare(ServerLevel level, BlockPos port) {
        level.setBlockAndUpdate(port.west(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
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
        port.supplyWater(reactor);
        FissionRegressionChecks.check(reactor.fluidCoolantTank.getFluidAmount() == capacity, "ME fills entire tank in one call without pipe/export-bus rate cap");
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
        var tag = port.saveWithFullMetadata();
        FissionRegressionChecks.check(tag.contains("FissionMeNode"), "ME node identity saved with block entity");
        var host = port.getCapability(appeng.capabilities.Capabilities.IN_WORLD_GRID_NODE_HOST, null);
        FissionRegressionChecks.check(host.isPresent(), "ME grid host capability exposed");
        reactor.fluidCoolantTank.setEmpty();
        port.setRemoved();
        port.supplyWater(reactor);
        FissionRegressionChecks.check(!node.isReady() && reactor.fluidCoolantTank.isEmpty(), "Removed interface disconnects and cannot extract water");
        FissionRegressionChecks.check(!host.isPresent(), "Removed interface invalidates grid host capability");
    }
    private FissionAe2Checks() {}
}
