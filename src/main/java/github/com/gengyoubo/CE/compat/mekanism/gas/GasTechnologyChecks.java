package github.com.gengyoubo.CE.compat.mekanism.gas;

import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.E.TypedEnergyPipeBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Coord4D;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.radiation.IRadiationManager;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismGases;
import mekanism.common.tile.TileEntityRadioactiveWasteBarrel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;

/** Real pipes, real dual-energy ports and a real waste barrel in an opt-in disposable world. */
public final class GasTechnologyChecks {
    private static final BlockPos BASE = new BlockPos(64, 120, 64);
    private static MinecraftServer server;
    private static int ticks, checks;
    private static boolean done;
    private static long decay;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++; github.com.gengyoubo.CE.changede.LOGGER.info("GAS PASS: {}", message);
    }
    public static void started(ServerStartedEvent event) {
        server = event.getServer();
        try {
            check(server.getPlayerCount() == 0, "No players in isolated test world");
            var level = server.overworld(); level.setChunkForced(4, 4, true); level.setChunkForced(3, 4, true); level.setChunkForced(5, 4, true);
            level.setChunkForced(4, 3, true);
            decay = MekanismConfig.general.radioactiveWasteBarrelDecayAmount.get();
            MekanismConfig.general.radioactiveWasteBarrelDecayAmount.set(0L);
            // Clear only this fixture's exact positions; repeated checks must not inherit the previous run's inventory.
            for (BlockPos pos : new BlockPos[]{BASE, BASE.east(), BASE.west(), BASE.south(), BASE.south().above(),
                    BASE.north(), BASE.north(2), BASE.north(3), BASE.east(8), BASE.east(12), BASE.east(14)})
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(BASE, GasCompat.REVERSER.getBlock().defaultBlockState());
            level.setBlockAndUpdate(BASE.east(), CELPBlock.WLP_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(BASE.west(), CELPBlock.DLP_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(BASE.south(), GasCompat.GAS_PIPE.getBlock().defaultBlockState());
            level.setBlockAndUpdate(BASE.south().above(), MekanismBlocks.RADIOACTIVE_WASTE_BARREL.getBlock().defaultBlockState());
            var barrel = (TileEntityRadioactiveWasteBarrel) level.getBlockEntity(BASE.south().above());
            barrel.getGasTank().setStack(MekanismGases.SPENT_NUCLEAR_WASTE.getStack(100)); barrel.setActive(true);
            level.setBlockAndUpdate(BASE.north(), GasCompat.GAS_PIPE.getBlock().defaultBlockState());
            level.setBlockAndUpdate(BASE.north(2), MekanismBlocks.BASIC_PRESSURIZED_TUBE.getBlock().defaultBlockState());
            level.setBlockAndUpdate(BASE.north(3), MekanismBlocks.BASIC_CHEMICAL_TANK.getBlock().defaultBlockState());
            level.setBlockAndUpdate(BASE.east(12), MekanismBlocks.CHEMICAL_INFUSER.getBlock().defaultBlockState());
            var infuser = (mekanism.common.tile.machine.TileEntityChemicalInfuser) level.getBlockEntity(BASE.east(12));
            infuser.leftTank.setStack(GasCompat.BASIC_GASES.get("white_latex_gas").getStack(10));
            infuser.rightTank.setStack(GasCompat.BASIC_GASES.get("wolf_gas").getStack(10));
            infuser.getEnergyContainer().setEnergy(infuser.getEnergyContainer().getMaxEnergy());
            level.setBlockAndUpdate(BASE.east(14), MekanismBlocks.ROTARY_CONDENSENTRATOR.getBlock().defaultBlockState());
            var rotary = (mekanism.common.tile.machine.TileEntityRotaryCondensentrator) level.getBlockEntity(BASE.east(14));
            rotary.mode = true;
            rotary.fluidTank.setStack(new net.minecraftforge.fluids.FluidStack(net.ltxprogrammer.changed.init.ChangedFluids.WOLF_GAS.get(), 10));
            rotary.getEnergyContainers(null).get(0).setEnergy(rotary.getEnergyContainers(null).get(0).getMaxEnergy());
        } catch (Throwable error) { finish(error); }
    }
    private static ReverserBlockEntity machine(ServerLevel level) { return (ReverserBlockEntity) level.getBlockEntity(BASE); }
    public static void tick(TickEvent.ServerTickEvent event) {
        if (server == null || done || event.phase != TickEvent.Phase.END) return;
        ticks++;
        if (ticks <= 80) return;
        int phase = ticks - 80;
        try {
            var level = server.overworld(); var machine = machine(level);
            if (phase == 20) {
                recipes();
                var barrel = (TileEntityRadioactiveWasteBarrel) level.getBlockEntity(BASE.south().above());
                var pipe = level.getBlockEntity(BASE.south()).getCapability(Capabilities.GAS_HANDLER, null).resolve().orElseThrow();
                check(machine.inputTank.getStored() >= 10, "Waste barrel and CE gas pipe fill radioactive input (input=" + machine.inputTank.getStored()
                        + ", barrel=" + barrel.getGasTank().getStored() + ", pipe=" + pipe.getChemicalInTank(0).getAmount() + ", facing=" + machine.getDirection()
                        + ", rear=" + machine.getCapability(Capabilities.GAS_HANDLER, Direction.SOUTH).isPresent() + ", accepts=" + machine.acceptsGas(MekanismGases.SPENT_NUCLEAR_WASTE.get()) + ")");
                // Native networks retain a buffer; isolate the following exact machine accounting from transport timing.
                level.removeBlock(BASE.south(), false);
                machine.inputTank.setStack(MekanismGases.SPENT_NUCLEAR_WASTE.getStack(100));
                var infuser = (mekanism.common.tile.machine.TileEntityChemicalInfuser) level.getBlockEntity(BASE.east(12));
                check(infuser.leftTank.isEmpty() && infuser.rightTank.isEmpty() && infuser.centerTank.getStored() == 20, "Real chemical infuser synthesizes wolf gas without increasing total volume");
                var rotary = (mekanism.common.tile.machine.TileEntityRotaryCondensentrator) level.getBlockEntity(BASE.east(14));
                check(rotary.fluidTank.isEmpty() && rotary.gasTank.getStored() == 10, "Real rotary machine converts Changed transfur fluid into CE gas");
                rotary.nextMode(); rotary.getEnergyContainers(null).get(0).setEnergy(rotary.getEnergyContainers(null).get(0).getMaxEnergy());
                check(machine.progress() == 0, "No progress without dual power");
                check(!machine.getCapability(ForgeCapabilities.ENERGY, null).isPresent(), "FE cannot substitute for typed power");
                check(machine.getCapability(Capabilities.GAS_HANDLER, Direction.SOUTH).isPresent(), "Rear exposes gas input");
                check(!machine.getCapability(Capabilities.GAS_HANDLER, Direction.EAST).isPresent(), "Typed power port does not expose gas");
                check(machine.receiveTypedEnergy(LatexEnergyType.DLP, 50, Direction.EAST) == 0, "WLP face rejects DLP");
                check(machine.receiveTypedEnergy(LatexEnergyType.LP, 50, null) == 0, "LP rejected");
                ((TypedEnergyPipeBlockEntity) level.getBlockEntity(BASE.east())).receiveTypedEnergy(LatexEnergyType.WLP, 1000);
            }
            if (phase == 40) {
                var rotary = (mekanism.common.tile.machine.TileEntityRotaryCondensentrator) level.getBlockEntity(BASE.east(14));
                check(rotary.gasTank.isEmpty() && rotary.fluidTank.getFluidAmount() == 10, "Real rotary reverse conversion restores original Changed fluid exactly");
                check(machine.getTypedEnergyStored(LatexEnergyType.WLP) == 1000 && machine.progress() == 0, "Missing DLP consumes no WLP from a real pipe");
                level.removeBlock(BASE.north(), false);
                machine.outputTank.setStack(MekanismGases.FISSILE_FUEL.getStack(ReverserBlockEntity.TANK_CAPACITY));
                machine.receiveTypedEnergy(LatexEnergyType.WLP, 1000, null);
                machine.receiveTypedEnergy(LatexEnergyType.DLP, 1000, null);
                ((TypedEnergyPipeBlockEntity) level.getBlockEntity(BASE.west())).receiveTypedEnergy(LatexEnergyType.DLP, 1000);
            }
            if (phase == 60) {
                check(machine.progress() == 0 && machine.status() == ReverserBlockEntity.Status.OUTPUT_FULL, "Full output pauses processing");
                check(machine.getTypedEnergyStored(LatexEnergyType.WLP) == 2000 && machine.getTypedEnergyStored(LatexEnergyType.DLP) == 2000, "Full output consumes neither power type; both real pipes deliver independently");
                machine.outputTank.setEmpty();
                level.setBlockAndUpdate(BASE.north(), GasCompat.GAS_PIPE.getBlock().defaultBlockState());
            }
            if (phase == 285) {
                var target = level.getBlockEntity(BASE.north(3)).getCapability(Capabilities.GAS_HANDLER, null).resolve().orElseThrow();
                check(target.getChemicalInTank(0).isTypeEqual(MekanismGases.FISSILE_FUEL.getStack(1)) && target.getChemicalInTank(0).getAmount() == 1,
                        "Reversed fuel passes through CE and native Mek tubes into a native tank");
                check(machine.inputTank.getStored() == 90, "One complete batch consumes exactly ten waste");
                check(machine.getTypedEnergyStored(LatexEnergyType.WLP) == 0 && machine.getTypedEnergyStored(LatexEnergyType.DLP) == 0, "10 seconds consumes exactly 2000 WLP and 2000 DLP");
                int progress = machine.progress(); machine.process(); machine.process();
                check(machine.progress() == progress, "Same world tick cannot process twice");
                BlockPos resume = BASE.east(8);
                level.setBlockAndUpdate(resume, GasCompat.REVERSER.getBlock().defaultBlockState());
                var worker = (ReverserBlockEntity) level.getBlockEntity(resume);
                worker.inputTank.setStack(MekanismGases.SPENT_NUCLEAR_WASTE.getStack(10));
                worker.receiveTypedEnergy(LatexEnergyType.WLP, 2000, null); worker.receiveTypedEnergy(LatexEnergyType.DLP, 2000, null);
            }
            if (phase == 335) {
                BlockPos resume = BASE.east(8);
                var worker = (ReverserBlockEntity) level.getBlockEntity(resume);
                check(worker.progress() == 50, "Independent work order advances exactly 50 ticks");
                var loaded = (ReverserBlockEntity) worker.getType().create(resume, worker.getBlockState()); loaded.load(worker.saveWithFullMetadata());
                level.setBlockEntity(loaded);
                check(loaded.progress() == 50 && loaded.getTypedEnergyStored(LatexEnergyType.WLP) == 1500, "Chunk-style block entity replacement restores progress and charged energy");
            }
            if (phase == 510) {
                var worker = (ReverserBlockEntity) level.getBlockEntity(BASE.east(8));
                check(worker.inputTank.isEmpty() && worker.outputTank.getStored() == 1, "Reloaded work order completes exactly one 10:1 conversion");
                check(worker.getTypedEnergyStored(LatexEnergyType.WLP) == 0 && worker.getTypedEnergyStored(LatexEnergyType.DLP) == 0, "Reload neither duplicates nor forgives energy costs");
                persistence(level, machine);
                finish(null);
            }
        } catch (Throwable error) { finish(error); }
    }
    private static void recipes() {
        var manager = server.getRecipeManager();
        for (var entry : GasCompat.BASIC_GASES.entrySet()) {
            check(MekanismAPI.gasRegistry().containsKey(ResourceLocation.parse("changede:" + entry.getKey())), "Basic gas registered: " + entry.getKey());
            var recipe = manager.byKey(ResourceLocation.parse("changede:gas/rotary/" + entry.getKey()));
            check(recipe.isPresent() && recipe.get() instanceof mekanism.api.recipes.RotaryRecipe rotary && rotary.hasFluidToGas() && rotary.hasGasToFluid(), "Bidirectional Changed fluid conversion: " + entry.getKey());
        }
        for (String base : new String[]{"white_latex_gas", "dark_latex_gas"}) for (String species : new String[]{"skunk_gas", "tiger_gas", "wolf_gas"})
            check(manager.byKey(ResourceLocation.parse("changede:gas/infusing/" + species + "_from_" + base)).isPresent(), "Gas synthesis recipe: " + base + "/" + species);
        var recipe = (ReversingRecipe) manager.byKey(ResourceLocation.parse("changede:reversing/spent_nuclear_waste_to_fissile_fuel")).orElseThrow();
        check(recipe.ticks() == 200 && recipe.whiteRate() == 200 && recipe.darkRate() == 200, "Approved reverse defaults loaded from JSON");
        check(!recipe.getInput().testType(MekanismGases.NUCLEAR_WASTE.get()), "Ordinary nuclear waste cannot bypass upstream processing");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            recipe.getSerializer().toNetwork(buffer, recipe);
            var restored = recipe.getSerializer().fromNetwork(recipe.getId(), buffer);
            check(recipe.signature().equals(restored.signature()), "Network preserves gas quantities, time and both energy costs");
        } finally { buffer.release(); }
        for (String id : new String[]{"reverser", "basic_gas_pipe"}) check(manager.byKey(ResourceLocation.parse("changede:gas/" + id)).isPresent(), "Machine crafting recipe: " + id);
    }
    private static void persistence(ServerLevel level, ReverserBlockEntity source) {
        source.receiveTypedEnergy(LatexEnergyType.WLP, 90_000, null); source.receiveTypedEnergy(LatexEnergyType.DLP, 80_000, null);
        var saved = source.saveWithFullMetadata();
        var restored = (ReverserBlockEntity) source.getType().create(BASE, source.getBlockState()); restored.load(saved);
        check(restored.getTypedEnergyStored(LatexEnergyType.WLP) == 90_000 && restored.getTypedEnergyStored(LatexEnergyType.DLP) == 80_000, "NBT preserves independent typed buffers beyond short range");
        check(restored.inputTank.getStored() == source.inputTank.getStored(), "NBT preserves radioactive gas");
        var menu = new ReverserMenu(1, FakePlayerFactory.getMinecraft(level).getInventory(), BASE);
        check(menu.value(0) == 90_000 && menu.value(1) == 80_000, "Menu splits 32-bit energy values correctly");
        source.outputTank.setStack(MekanismGases.FISSILE_FUEL.getStack(5));
        var drops = Block.getDrops(source.getBlockState(), level, BASE, source);
        check(drops.size() == 1 && drops.get(0).is(GasCompat.REVERSER.asItem()), "Native block drops own machine");
        var droppedTile = (ReverserBlockEntity) source.getType().create(BASE, source.getBlockState());
        var data = mekanism.common.util.ItemDataUtils.getDataMapIfPresent(drops.get(0));
        droppedTile.readSustainedData(data);
        mekanism.api.DataHandlerUtils.readContainers(droppedTile.getGasTanks(null), data.getList(mekanism.api.NBTConstants.GAS_TANKS, net.minecraft.nbt.Tag.TAG_COMPOUND));
        check(droppedTile.inputTank.isEmpty(), "Radioactive waste is removed from survival drops by native Mek rules");
        check(droppedTile.outputTank.getStored() == 5, "Nonradioactive fuel is preserved in survival drops");
        check(droppedTile.getTypedEnergyStored(LatexEnergyType.WLP) == 90_000, "Survival drop preserves stored WLP");
        double before = IRadiationManager.INSTANCE.getRadiationLevel(new Coord4D(BASE, level));
        level.removeBlock(BASE, false);
        check(IRadiationManager.INSTANCE.getRadiationLevel(new Coord4D(BASE, level)) > before, "Actual dismantling releases stored waste radiation");
    }
    private static void finish(Throwable error) {
        done = true;
        MekanismConfig.general.radioactiveWasteBarrelDecayAmount.set(decay);
        try { Files.writeString(Path.of("gas-checks.txt"), error == null ? "PASS: " + checks + " assertions\n" : "FAIL: " + error + "\n"); }
        catch (Exception ignored) {}
        if (error != null) github.com.gengyoubo.CE.changede.LOGGER.error("GAS TECHNOLOGY CHECKS FAILED", error);
        server.execute(() -> server.halt(false));
    }
    private GasTechnologyChecks() {}
}
