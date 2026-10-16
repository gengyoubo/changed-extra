package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.registries.MekanismGases;
import mekanism.generators.common.content.fission.FissionReactorCache;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import mekanism.generators.common.registries.GeneratorsBlocks;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;

/** Opt-in checks using a real formed reactor in the disposable build/fission-server world. */
public final class FissionRegressionChecks {
    private static final BlockPos BASE = new BlockPos(64, 120, 64);
    private static final EnumMap<FissionUpgrade, List<BlockPos>> POSITIONS = new EnumMap<>(FissionUpgrade.class);
    private static MinecraftServer server;
    private static FissionReactorMultiblockData verifiedReactor;
    private static int ticks, assertions;
    private static boolean done;

    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        assertions++;
        github.com.gengyoubo.CE.changede.LOGGER.info("FISSION PASS: {}", message);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0E-7, message + " (" + actual + ", expected " + expected + ")");
    }

    public static void started(ServerStartedEvent event) {
        server = event.getServer();
        try {
            check(server.getPlayerCount() == 0, "No players in disposable test world");
            var level = server.overworld();
            level.setChunkForced(4, 4, true);
            level.setChunkForced(3, 4, true);
            for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) {
                boolean wall = x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4;
                level.setBlockAndUpdate(BASE.offset(x, y, z), wall ? GeneratorsBlocks.FISSION_REACTOR_CASING.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(BASE.offset(2, 1, 2), GeneratorsBlocks.FISSION_FUEL_ASSEMBLY.getBlock().defaultBlockState());
            level.setBlockAndUpdate(BASE.offset(2, 2, 2), GeneratorsBlocks.CONTROL_ROD_ASSEMBLY.getBlock().defaultBlockState());
            int slot = 0;
            for (var type : FissionUpgrade.values()) {
                if (!FissionCompat.UPGRADES.containsKey(type)) continue;
                int count = type == FissionUpgrade.SCRAM || type == FissionUpgrade.ME_PORT ? 1 : 2;
                for (int n = 0; n < count; n++) {
                    BlockPos pos = type == FissionUpgrade.ME_PORT ? BASE.offset(0, 2, 2) : BASE.offset(1 + slot % 3, 1 + slot / 3, 0);
                    level.setBlockAndUpdate(pos, FissionCompat.UPGRADES.get(type).getBlock().defaultBlockState());
                    POSITIONS.computeIfAbsent(type, unused -> new ArrayList<>()).add(pos);
                    if (type != FissionUpgrade.ME_PORT) slot++;
                }
            }
            if (ModList.get().isLoaded("ae2")) FissionAe2Checks.prepare(level, POSITIONS.get(FissionUpgrade.ME_PORT).get(0));
        } catch (Throwable error) { finish(error); }
    }

    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null || done) return;
        ++ticks;
        if (ticks > 100) {
            try {
                if (FissionAe2Checks.tick()) complete(server.overworld());
            } catch (Throwable error) { finish(error); }
            return;
        }
        if (ticks == 99) {
            try {
                var level = server.overworld();
                var casing = (TileEntityFissionReactorCasing) level.getBlockEntity(BASE);
                FissionThermoelectricChecks.prepareNativeTick(level, casing.getMultiblock(), POSITIONS.get(FissionUpgrade.THERMOELECTRIC));
            } catch (Throwable error) { finish(error); }
            return;
        }
        if (ticks != 100) return;
        try {
            var level = server.overworld();
            var casing = (TileEntityFissionReactorCasing) level.getBlockEntity(BASE);
            var formed = casing.getMultiblock();
            verifiedReactor = formed;
            check(formed.isFormed(), "Native validator forms reactor with all upgrade casings");
            check(formed instanceof FissionUpgradeAccess, "Generator multiblock has upgrade Mixin applied");
            var state = ((FissionUpgradeAccess) formed).changede$fissionUpgrades();
            state.beforeTick(formed, level);
            for (FissionUpgrade type : FissionUpgrade.values())
                check(state.count(type) == POSITIONS.getOrDefault(type, List.of()).size(), "Counts installed " + type);
            recipesAndDrops(level);
            FissionThermoelectricChecks.verify(level, formed, POSITIONS.get(FissionUpgrade.THERMOELECTRIC));
            fuelAndCooling(level);
            scram(level);
            persistence(level);
            if (ModList.get().isLoaded("ae2")) {
                FissionAe2Checks.verify(level, formed, POSITIONS.get(FissionUpgrade.ME_PORT).get(0));
                return; // Continue the finite-power throughput check over actual server ticks.
            }
            else {
                check(!net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(net.minecraft.resources.ResourceLocation.parse("changede:fission_me_interface")),
                        "ME port absent without AE2; other upgrades still work");
                check(server.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("changede:fission/fission_me_interface")).isEmpty(),
                        "ME recipe absent without AE2");
                check(!server.getPackRepository().getSelectedIds().contains("changede:fission_me"), "ME loot pack absent without AE2");
            }
            complete(level);
        } catch (Throwable error) { finish(error); }
    }

    private static void complete(ServerLevel level) {
        var casing = (TileEntityFissionReactorCasing) level.getBlockEntity(BASE);
        // Keep the instance whose upgrades were verified; native revalidation may replace it
        // during the sustained ME check, especially after the interface removal check.
        var formed = verifiedReactor;
        var state = ((FissionUpgradeAccess) formed).changede$fissionUpgrades();
        // Removal must invalidate native structure and remove the corresponding bonuses on reformation.
        BlockPos removed = POSITIONS.get(FissionUpgrade.COOLING).get(0);
        level.setBlockAndUpdate(removed, GeneratorsBlocks.FISSION_REACTOR_CASING.getBlock().defaultBlockState());
        state.beforeTick(formed, level);
        check(state.count(FissionUpgrade.COOLING) == 1, "Removing one cooling block removes exactly one bonus");
        check(!formed.isFormed() || casing.getStructure().getMultiblockData() == null, "Native structure invalidates when upgrade is replaced");
        finish(null);
    }

    private static void recipesAndDrops(ServerLevel level) {
        for (var entry : FissionCompat.UPGRADES.entrySet()) {
            String id = entry.getKey().id;
            check(server.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("changede:fission/" + id)).isPresent(), "Recipe loaded: " + id);
            var pos = POSITIONS.get(entry.getKey()).get(0);
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos));
            check(drops.size() == 1 && drops.get(0).is(entry.getValue().asItem()), "Survival drop is upgrade item: " + id);
            check(entry.getValue().getBlock().defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), "Pickaxe tag: " + id);
            check(level.getBlockEntity(pos).getType().isValid(level.getBlockState(pos)), "Own block entity type accepts upgrade state: " + id);
        }
    }

    private static FissionReactorMultiblockData sample(ServerLevel level, int cooling, int x, int y, boolean scram) {
        var tile = (TileEntityFissionReactorCasing) level.getBlockEntity(BASE);
        var reactor = new FissionReactorMultiblockData(tile);
        reactor.setShape(new VoxelCuboid(BASE, BASE.offset(4, 4, 4)));
        reactor.setAssemblies(1);
        reactor.surfaceArea = 6;
        // Same shell size/heat capacity for every comparison; selected real blocks belong to this sample only.
        reactor.locations.add(BASE);
        for (int n = 0; n < cooling; n++) reactor.locations.add(POSITIONS.get(FissionUpgrade.COOLING).get(n));
        for (int n = 0; n < x; n++) reactor.locations.add(POSITIONS.get(FissionUpgrade.PROCESSOR_X).get(n));
        for (int n = 0; n < y; n++) reactor.locations.add(POSITIONS.get(FissionUpgrade.PROCESSOR_Y).get(n));
        if (scram) reactor.locations.add(POSITIONS.get(FissionUpgrade.SCRAM).get(0));
        reactor.setFormedForce(true);
        reactor.onCreated(level);
        reactor.heatCapacitor.setHeatCapacity(100_000, false);
        reactor.heatCapacitor.setHeat(300 * reactor.heatCapacitor.getHeatCapacity());
        reactor.rateLimit = 1;
        reactor.fuelTank.setStack(MekanismGases.FISSILE_FUEL.getStack(1000));
        reactor.fluidCoolantTank.setStack(new FluidStack(Fluids.WATER, reactor.fluidCoolantTank.getCapacity()));
        ((FissionUpgradeAccess) reactor).changede$fissionUpgrades().beforeTick(reactor, level);
        return reactor;
    }

    private static double fuel(FissionReactorMultiblockData reactor) { return reactor.fuelTank.getStored() + reactor.burnRemaining; }
    private static double waste(FissionReactorMultiblockData reactor) { return reactor.wasteTank.getStored() + reactor.partialWaste; }

    private static void fuelAndCooling(ServerLevel level) {
        double returnSetting = FissionConfig.RETURN_CHANCE.get();
        try {
            FissionConfig.RETURN_CHANCE.set(0.0);
            for (int count = 0; count <= 2; count++) {
                var r = sample(level, 0, count, 0, false);
                r.setActive(true);
                r.tick(level);
                near(fuel(r), 1000 - 1 / (1 + 0.01 * count), "Stacked X fuel cost for " + count + " blocks");
                near(waste(r), 1 + 0.01 * count, "Stacked X waste output for " + count + " blocks");
                near(r.lastBurnRate, 1 + 0.01 * count, "Stacked X heat/output rate for " + count + " blocks");
            }
            var nativeCool = sample(level, 0, 0, 0, false);
            nativeCool.heatCapacitor.setHeat(500 * nativeCool.heatCapacitor.getHeatCapacity());
            nativeCool.tick(level);
            long steam = nativeCool.lastBoilRate;
            int nativeWater = nativeCool.fluidCoolantTank.getCapacity() - nativeCool.fluidCoolantTank.getFluidAmount();
            check(steam > 100 && nativeWater == steam, "Native water/steam baseline");
            for (int count = 1; count <= 2; count++) {
                var r = sample(level, count, 0, 0, false);
                r.heatCapacitor.setHeat(500 * r.heatCapacitor.getHeatCapacity());
                r.tick(level);
                near(r.lastBoilRate, steam, "Cooling keeps steam unchanged for " + count + " blocks");
                near(r.heatCapacitor.getTemperature(), nativeCool.heatCapacitor.getTemperature(), "Cooling keeps heat removal unchanged");
                near(r.fluidCoolantTank.getCapacity() - r.fluidCoolantTank.getFluidAmount(), Math.ceil(steam / (1 + 0.01 * count)), "Stacked cooling water cost");
            }
            var noRefund = sample(level, 0, 1, 2, false);
            noRefund.setActive(true); noRefund.tick(level);
            near(fuel(noRefund), 1000 - 1 / 1.01, "Zero chance does not refund");
            FissionConfig.RETURN_CHANCE.set(1.0);
            var refund = sample(level, 0, 1, 2, false);
            refund.setActive(true); refund.tick(level);
            near(fuel(refund), 1000, "100% chance refunds only consumed fuel");
            near(waste(refund), 1.01, "Refund preserves X waste output");
            near(((FissionUpgradeAccess) refund).changede$fissionUpgrades().returnChance(), 1, "Stacked return probability caps at one");
            refund.fuelTank.setStack(MekanismGases.FISSILE_FUEL.getStack(refund.fuelTank.getCapacity()));
            refund.burnRemaining = 0.5;
            refund.tick(level);
            near(fuel(refund), refund.fuelTank.getCapacity() + 0.5, "Refund preserves fractional balance at a full tank");
            FissionConfig.RETURN_CHANCE.set(0.01);
            var two = sample(level, 0, 0, 2, false);
            near(((FissionUpgradeAccess) two).changede$fissionUpgrades().returnChance(), 0.02, "Two Y blocks provide two percentage points");
            var empty = sample(level, 0, 1, 1, false);
            empty.fuelTank.setEmpty(); empty.setActive(true); empty.tick(level);
            near(fuel(empty), 0, "Empty reactor cannot mint returned fuel");
            near(waste(empty), 0, "Empty reactor cannot mint waste");
        } finally { FissionConfig.RETURN_CHANCE.set(returnSetting); }
    }

    private static void scram(ServerLevel level) {
        var r = sample(level, 1, 1, 0, true);
        var state = ((FissionUpgradeAccess) r).changede$fissionUpgrades();
        double demand = state.predictedWaterPerTick(r);
        check(demand > 0, "Predicts water demand on cold first activation");
        r.fluidCoolantTank.setStack(new FluidStack(Fluids.WATER, (int) Math.ceil(demand * 200)));
        r.setActive(true); state.beforeTick(r, level);
        check(r.isActive(), "Exactly ten seconds does not trip");
        r.fluidCoolantTank.setStack(new FluidStack(Fluids.WATER, (int) Math.ceil(demand * 200) - 1));
        r.tick(level);
        check(!r.isActive() && r.lastBurnRate == 0, "Below ten seconds trips before burning");
        near(fuel(r), 1000, "SCRAM consumes no fuel");
        r.fluidCoolantTank.setStack(new FluidStack(Fluids.WATER, r.fluidCoolantTank.getCapacity()));
        r.tick(level);
        check(!r.isActive(), "Refill does not auto-restart reactor");
        r.fluidCoolantTank.setEmpty(); r.setActive(true); r.tick(level);
        check(!r.isActive(), "Dry start is rejected");
        r.gasCoolantTank.setStack(MekanismGases.SODIUM.getStack(1000));
        r.setActive(true); state.beforeTick(r, level);
        check(r.isActive(), "Water SCRAM does not interrupt a sodium-cooled reactor");
    }

    private static void persistence(ServerLevel level) {
        var r = sample(level, 0, 1, 0, false);
        r.rateLimit = 0.01; r.setActive(true); r.tick(level);
        var cache = new FissionReactorCache(); cache.sync(r);
        var tag = new CompoundTag(); cache.save(tag);
        var loadedCache = new FissionReactorCache(); loadedCache.load(tag);
        var loaded = sample(level, 0, 1, 0, false); loadedCache.apply(loaded);
        near(fuel(loaded), fuel(r), "Cache save/load preserves fractional upgraded fuel");
        near(waste(loaded), waste(r), "Cache save/load preserves fractional upgraded waste");
        check(loaded.isActive(), "Native active state persists");
        loaded.fuelTank.setEmpty(); loaded.burnRemaining = 0.001;
        loaded.tick(level);
        check(fuel(loaded) < 1.0E-10 && waste(loaded) > waste(r), "Sub-mB fuel can finish without duplication or loss");
    }

    private static void finish(Throwable error) {
        done = true;
        try {
            String report = error == null ? "PASS: " + assertions + " assertions\n" : "FAIL: " + error + "\n";
            Files.writeString(Path.of("fission-checks.txt"), report);
            if (error == null) github.com.gengyoubo.CE.changede.LOGGER.info("FISSION CHECKS PASSED: {}", assertions);
            else github.com.gengyoubo.CE.changede.LOGGER.error("FISSION CHECKS FAILED", error);
        } catch (Exception ignored) { }
        server.execute(() -> server.halt(false));
    }
}
