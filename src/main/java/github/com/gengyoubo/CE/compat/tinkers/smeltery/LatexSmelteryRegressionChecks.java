package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.ltxprogrammer.changed.init.ChangedFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.smeltery.block.component.SearedBlock;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;

import java.util.List;
import java.util.function.BiConsumer;

/** Actual native multiblock, capability, fuel, GUI-data and loot checks. */
public final class LatexSmelteryRegressionChecks {
    private static BiConsumer<Boolean, String> verify;
    private LatexSmelteryRegressionChecks() {}
    private static void check(boolean value, String message) { verify.accept(value, "Smeltery: " + message); }
    private static Block block(String name) { return ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(name)); }
    private record Furnace(HeatingStructureBlockEntity controller, BlockPos core, BlockPos tank, BlockPos spare, Block brick) {}
    private static Furnace furnace(ServerLevel level, BlockPos min, boolean foundry) {
        Block brick = block(foundry ? "tconstruct:scorched_bricks" : "tconstruct:seared_bricks");
        level.getChunkAt(min);
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(2, 2, 2))) {
            boolean inside = pos.equals(min.offset(1, 1, 1));
            level.setBlockAndUpdate(pos, inside || pos.getY() == min.getY() + 2 ? Blocks.AIR.defaultBlockState() : brick.defaultBlockState());
        }
        BlockPos controllerPos = min.offset(1, 1, 0), tank = min.offset(2, 1, 1), core = min.offset(0, 1, 1), spare = min.offset(1, 1, 2);
        level.setBlockAndUpdate(tank, LatexSmelteryCompat.TANK.get().defaultBlockState());
        level.setBlockAndUpdate(controllerPos, block(foundry ? "tconstruct:foundry_controller" : "tconstruct:smeltery_controller")
                .defaultBlockState().setValue(ControllerBlock.FACING, Direction.NORTH));
        var controller = (HeatingStructureBlockEntity) level.getBlockEntity(controllerPos);
        reform(level, controller);
        check(controller.getStructure() != null, "Native " + (foundry ? "foundry" : "smeltery") + " forms with the latex tank");
        check(controller.getStructure().getTanks().contains(tank), "New tank appears in native fuel-tank list");
        return new Furnace(controller, core, tank, spare, brick);
    }
    private static void reform(ServerLevel level, HeatingStructureBlockEntity controller) {
        controller.updateStructure();
        HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controller.getBlockPos(), controller.getBlockState(), controller);
    }
    private static TankBlockEntity tank(ServerLevel level, BlockPos pos) { return (TankBlockEntity) level.getBlockEntity(pos); }
    public static void run(ServerLevel level, BiConsumer<Boolean, String> checks) {
        verify = checks;
        for (String name : new String[]{"latex_combustion_core", "latex_fuel_tank"})
            check(level.getRecipeManager().byKey(ResourceLocation.parse("changede:smeltery/" + name)).isPresent(), "Crafting recipe loads: " + name);
        storage(level);
        for (boolean foundry : new boolean[]{false, true}) combustion(level, furnace(level, new BlockPos(foundry ? 72 : 48, 96, 48), foundry));
    }
    private static void storage(ServerLevel level) {
        BlockPos pos = new BlockPos(40, 96, 40);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, LatexSmelteryCompat.TANK.get().defaultBlockState());
        TankBlockEntity entity = tank(level, pos);
        var tank = entity.getTank();
        var dark = new FluidStack(ChangedFluids.DARK_LATEX.get(), 9000);
        var white = new FluidStack(ChangedFluids.WHITE_LATEX.get(), 1000);
        check(tank.getCapacity() == 8000, "Placed tank has exactly 8 B capacity");
        check(tank.fill(dark, IFluidHandler.FluidAction.SIMULATE) == 8000 && tank.isEmpty(), "Simulation does not mutate the tank");
        check(tank.fill(dark, IFluidHandler.FluidAction.EXECUTE) == 8000, "Dark latex fill clamps at 8 B");
        check(tank.fill(white, IFluidHandler.FluidAction.EXECUTE) == 0, "Dark and white latex cannot mix");
        check(tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "Water is rejected");
        check(tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "Lava is rejected");
        check(entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(), "Tank exposes pipe-compatible fluid capability");
        var loaded = new LatexFuelTankBlockEntity(pos, entity.getBlockState()); loaded.load(entity.saveWithFullMetadata());
        check(loaded.getTank().getFluidAmount() == 8000 && loaded.getTank().getFluid().isFluidEqual(dark), "NBT reload preserves full tank contents");
        var drops = Block.getDrops(entity.getBlockState(), level, pos, entity);
        check(drops.size() == 1 && drops.get(0).is(LatexSmelteryCompat.TANK_ITEM.get()), "Native block loot drops the latex tank");
        ItemStack dropped = drops.get(0);
        var itemTank = dropped.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
        check(itemTank.getFluidInTank(0).getAmount() == 8000, "Breaking preserves 8 B in the dropped item");
        check(itemTank.getTankCapacity(0) == 8000 && !itemTank.isFluidValid(0, new FluidStack(Fluids.LAVA, 1)), "Item capability has 8 B capacity and rejects other fluids");
        BlockPos placed = pos.offset(0, 0, 2);
        level.setBlockAndUpdate(placed, LatexSmelteryCompat.TANK.get().defaultBlockState());
        LatexSmelteryCompat.TANK.get().setPlacedBy(level, placed, level.getBlockState(placed), null, dropped);
        check(tank(level, placed).getTank().getFluidAmount() == 8000, "Replacing the dropped tank restores its fluid");
        itemTank.drain(8000, IFluidHandler.FluidAction.EXECUTE);
        check(itemTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "Empty item tank refuses water");
        check(itemTank.fill(white, IFluidHandler.FluidAction.EXECUTE) == 1000, "Empty item tank accepts white latex");
        tank.drain(8000, IFluidHandler.FluidAction.EXECUTE);
        check(tank.fill(white, IFluidHandler.FluidAction.EXECUTE) == 1000, "Drained block tank can switch latex type");
    }
    private static void combustion(ServerLevel level, Furnace furnace) {
        var controller = furnace.controller(); var fuel = controller.getFuelModule();
        var access = (LatexFuelAccess) fuel;
        var tank = tank(level, furnace.tank()).getTank();
        tank.fill(new FluidStack(ChangedFluids.DARK_LATEX.get(), 8000), IFluidHandler.FluidAction.EXECUTE);
        check(!access.changede$coreReady(), "No core is initially present");
        check(fuel.findFuel(false) == 0 && fuel.findFuel(true) == 0 && tank.getFluidAmount() == 8000, "Latex cannot burn without the core");
        var info = fuel.getFuelInfo();
        check(info.getTemperature() == 0 && info.getTotalAmount() == 8000 && info.getCapacity() == 8000, "Blocked fuel remains visible at 8 B with no usable temperature");
        var tooltip = LatexSmelteryRules.tooltip(fuel, info, List.of(Component.literal("fluid"), Component.literal("invalid")));
        check(tooltip.get(1).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && text.getKey().equals("gui.changede.smeltery.requires_core"), "Controller tooltip explains the missing core");
        level.setBlockAndUpdate(furnace.core(), LatexSmelteryCompat.CORE.get().defaultBlockState()); reform(level, controller);
        check(controller.getStructure() != null && controller.getStructure().contains(furnace.core()), "Core belongs to the formed native furnace wall");
        check(access.changede$coreReady(), "Connected core enables latex fuel");
        check(fuel.findFuel(false) == 1000 && tank.getFluidAmount() == 8000, "Fuel preview has lava temperature and consumes nothing");
        check(fuel.findFuel(true) == 1000 && tank.getFluidAmount() == 7950 && fuel.getFuel() == 100 && fuel.getRate() == 10,
                "One burn matches lava: 50 mB, duration 100, rate 10, temperature 1000");
        check(fuel.getCount() == 9 && fuel.get(7) == 1 && fuel.get(8) == 1, "Menu data synchronizes core and latex-batch status");
        CompoundTag saved = fuel.writeToTag(new CompoundTag());
        var reloadedFuel = new MultitankFuelModule(controller, () -> controller.getStructure().getTanks()); reloadedFuel.readFromTag(saved);
        check(((LatexFuelAccess) reloadedFuel).changede$latexBatch() && reloadedFuel.getFuel() == 100, "Burned fuel type and remaining time persist on reload");
        level.setBlockAndUpdate(furnace.core(), furnace.brick().defaultBlockState());
        check(!fuel.hasFuel() && fuel.getFuel() == 0, "Removing the core immediately stops the current latex batch");
        check(!reloadedFuel.hasFuel(), "Reloaded latex batch also requires the core");
        check(fuel.findFuel(true) == 0 && tank.getFluidAmount() == 7950, "Removed core prevents further latex consumption");
        level.setBlockAndUpdate(furnace.spare(), block("tconstruct:seared_fuel_tank").defaultBlockState());
        // Foundry accepts scorched native tanks instead of seared ones.
        if (controller instanceof slimeknights.tconstruct.smeltery.block.entity.controller.FoundryBlockEntity)
            level.setBlockAndUpdate(furnace.spare(), block("tconstruct:scorched_fuel_tank").defaultBlockState());
        reform(level, controller);
        tank(level, furnace.spare()).getTank().fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE);
        check(fuel.findFuel(true) == 1000 && fuel.getFuel() == 100 && !access.changede$latexBatch(), "Lava works without a core and gains no discarded latex time");
        tank.drain(8000, IFluidHandler.FluidAction.EXECUTE);
        info = fuel.getFuelInfo();
        check(info.getFluid().getFluid() == Fluids.LAVA && info.getCapacity() == 4000, "Empty latex tank does not inflate the lava capacity display");
        level.setBlockAndUpdate(furnace.core(), LatexSmelteryCompat.CORE.get().defaultBlockState()); reform(level, controller);
        tank.fill(new FluidStack(ChangedFluids.WHITE_LATEX.get(), 8000), IFluidHandler.FluidAction.EXECUTE);
        tank(level, furnace.spare()).getTank().drain(4000, IFluidHandler.FluidAction.EXECUTE);
        check(fuel.findFuel(true) == 1000 && tank.getFluidAmount() == 7950, "White latex uses the same lava fuel parameters");
        info = fuel.getFuelInfo();
        check(info.getTotalAmount() == 7950 && info.getCapacity() == 12000 && info.getTemperature() == 1000, "Display includes empty native tanks capable of holding latex");
        var rows = LatexSmelteryRules.tooltip(fuel, info, List.of(Component.literal("fluid"), Component.literal("temperature")));
        check(rows.stream().anyMatch(row -> row.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && text.getKey().equals("gui.changede.smeltery.latex_amounts")), "Tooltip includes separate black and white latex totals");
        var inventory = controller.getMeltingInventory();
        inventory.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        for (int tick = 0; tick < 300; tick++) HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controller.getBlockPos(), controller.getBlockState(), controller);
        check(inventory.getStackInSlot(0).isEmpty() && !controller.getTank().getFluidInTank(0).isEmpty(), "Native furnace actually melts iron using latex fuel");
    }
}
