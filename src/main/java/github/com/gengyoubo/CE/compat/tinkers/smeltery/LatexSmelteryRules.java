package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.ltxprogrammer.changed.init.ChangedFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;

import java.util.ArrayList;
import java.util.List;

public final class LatexSmelteryRules {
    private LatexSmelteryRules() {}
    public static boolean isLatex(Fluid fluid) {
        return fluid == ChangedFluids.DARK_LATEX.get() || fluid == ChangedFluids.WHITE_LATEX.get() || MoltenLatexFluids.isMolten(fluid);
    }
    public static boolean coreAt(HeatingStructureBlockEntity parent, BlockPos pos) {
        var level = parent.getLevel(); var structure = parent.getStructure();
        if (level == null || structure == null || pos == null || !structure.contains(pos) || !level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        // Replacing an existing valid wall does not reassign native servant flags.
        // Membership in the formed controller's structure is the authoritative check.
        return (state.is(LatexSmelteryCompat.CORE.get()) || state.is(LatexSmelteryCompat.DYNAMIC_CORE.get()))
                && parent.getBlockState().getValue(ControllerBlock.IN_STRUCTURE);
    }
    public static BlockPos findCore(HeatingStructureBlockEntity parent) {
        var structure = parent.getStructure();
        if (structure == null) return null;
        for (BlockPos pos : BlockPos.betweenClosed(structure.getMinPos(), structure.getMaxPos()))
            if (coreAt(parent, pos)) return pos.immutable();
        return null;
    }
    public static boolean dynamicCoreReady(HeatingStructureBlockEntity parent) {
        var structure = parent.getStructure();
        if (structure == null) return false;
        for (BlockPos pos : BlockPos.betweenClosed(structure.getMinPos(), structure.getMaxPos()))
            if (coreAt(parent, pos) && parent.getLevel().getBlockState(pos).is(LatexSmelteryCompat.DYNAMIC_CORE.get())) return true;
        return false;
    }

    /** Select the coolest available fuel which can finish at least one pending recipe. */
    public static BlockPos selectFuel(MultitankFuelModule module, HeatingStructureBlockEntity parent) {
        record Candidate(BlockPos pos, int temperature) {}
        List<Candidate> candidates = new ArrayList<>();
        for (BlockPos pos : parent.getStructure().getTanks()) {
            if (!parent.getLevel().hasChunkAt(pos)) continue;
            var entity = parent.getLevel().getBlockEntity(pos);
            if (entity == null) continue;
            var tank = entity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
            if (tank == null || tank.getTanks() == 0) continue;
            var fluid = tank.getFluidInTank(0);
            var recipe = MeltingFuelLookup.findFuel(fluid.getFluid());
            if (recipe == null || temperature(module, fluid.getFluid()) == 0 || fluid.getAmount() < recipe.getAmount(fluid.getFluid())) continue;
            var required = new FluidStack(fluid, recipe.getAmount(fluid.getFluid()));
            if (tank.drain(required, IFluidHandler.FluidAction.SIMULATE).getAmount() != required.getAmount()) continue;
            candidates.add(new Candidate(pos, recipe.getTemperature()));
        }
        candidates.sort(java.util.Comparator.comparingInt(Candidate::temperature));
        if (candidates.isEmpty()) return null;
        var inventory = parent.getMeltingInventory();
        for (Candidate candidate : candidates) {
            if (inventory.canHeat(candidate.temperature())) return candidate.pos();
        }
        // Alloy-only work must also be able to request hotter fuel. Item work is processed first.
        if (parent instanceof slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity) {
            var alloyTank = new slimeknights.tconstruct.smeltery.block.entity.module.alloying.SmelteryAlloyTank(parent.getTank());
            var recipes = parent.getLevel().getRecipeManager().getRecipesFor(
                    slimeknights.tconstruct.library.recipe.TinkerRecipeTypes.ALLOYING.get(), alloyTank, parent.getLevel());
            for (Candidate candidate : candidates) {
                alloyTank.setTemperature(candidate.temperature());
                if (recipes.stream().anyMatch(recipe -> recipe.canPerform(alloyTank))) return candidate.pos();
            }
        }
        // Native entity melting has no item recipe temperature; use the least expensive temperature.
        return candidates.get(0).pos();
    }
    private static List<IFluidHandler> tanks(FuelModule module) {
        if (!(module instanceof LatexFuelAccess access) || !(access.changede$parent() instanceof HeatingStructureBlockEntity parent)
                || parent.getLevel() == null || parent.getStructure() == null) return List.of();
        List<IFluidHandler> tanks = new ArrayList<>();
        for (BlockPos pos : parent.getStructure().getTanks()) {
            if (!parent.getLevel().hasChunkAt(pos)) continue;
            var entity = parent.getLevel().getBlockEntity(pos);
            if (entity != null) entity.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(tanks::add);
        }
        return tanks;
    }
    public static int temperature(FuelModule module, Fluid fluid) {
        if (isLatex(fluid) && !((LatexFuelAccess) module).changede$coreReady()) return 0;
        var recipe = MeltingFuelLookup.findFuel(fluid);
        return recipe == null ? 0 : recipe.getTemperature();
    }
    /** Empty tanks count only when they can actually hold the displayed fuel. */
    public static FuelModule.FuelInfo fuelInfo(MultitankFuelModule module) {
        List<IFluidHandler> tanks = tanks(module);
        FluidStack selected = module.getLastFluid();
        if (selected.isEmpty() || temperature(module, selected.getFluid()) == 0) {
            selected = FluidStack.EMPTY;
            for (IFluidHandler tank : tanks) {
                var fluid = tank.getFluidInTank(0);
                if (!fluid.isEmpty() && temperature(module, fluid.getFluid()) > 0) { selected = fluid; break; }
            }
            if (selected.isEmpty()) for (IFluidHandler tank : tanks) {
                if (!tank.getFluidInTank(0).isEmpty()) { selected = tank.getFluidInTank(0); break; }
            }
        }
        if (selected.isEmpty()) return FuelModule.FuelInfo.EMPTY;
        int amount = 0, capacity = 0;
        for (IFluidHandler tank : tanks) {
            var fluid = tank.getFluidInTank(0);
            if (fluid.isEmpty() ? tank.isFluidValid(0, selected) : fluid.isFluidEqual(selected)) {
                amount += fluid.getAmount(); capacity += tank.getTankCapacity(0);
            }
        }
        return FuelModule.FuelInfo.of(new FluidStack(selected, amount), capacity, temperature(module, selected.getFluid()));
    }
    public static List<Component> tooltip(FuelModule module, FuelModule.FuelInfo info, List<Component> original) {
        if (!(module instanceof LatexFuelAccess access) || !(access.changede$parent() instanceof HeatingStructureBlockEntity)) return original;
        List<IFluidHandler> tanks = tanks(module);
        boolean relevant = isLatex(info.getFluid().getFluid());
        int dark = 0, white = 0, moltenDark = 0, moltenWhite = 0;
        for (IFluidHandler tank : tanks) {
            relevant |= tank instanceof slimeknights.tconstruct.library.fluid.FluidTankBase<?> && tank.getTankCapacity(0) == LatexSmelteryCompat.CAPACITY;
            var fluid = tank.getFluidInTank(0);
            if (fluid.getFluid() == ChangedFluids.DARK_LATEX.get()) dark += fluid.getAmount();
            if (fluid.getFluid() == ChangedFluids.WHITE_LATEX.get()) white += fluid.getAmount();
            if (fluid.getFluid() == MoltenLatexFluids.DARK.source.get()) moltenDark += fluid.getAmount();
            if (fluid.getFluid() == MoltenLatexFluids.WHITE.source.get()) moltenWhite += fluid.getAmount();
        }
        if (!relevant && dark == 0 && white == 0 && moltenDark == 0 && moltenWhite == 0) return original;
        List<Component> result = new ArrayList<>(original);
        if (isLatex(info.getFluid().getFluid()) && !access.changede$coreReady() && result.size() > 1)
            result.set(1, Component.translatable("gui.changede.smeltery.requires_core").withStyle(ChatFormatting.RED));
        if (!info.isEmpty()) result.add(Component.translatable("gui.changede.smeltery.fuel_capacity", info.getTotalAmount(), info.getCapacity()).withStyle(ChatFormatting.GRAY));
        result.add(Component.translatable("gui.changede.smeltery.latex_amounts", dark, white).withStyle(ChatFormatting.GRAY));
        if (moltenDark > 0 || moltenWhite > 0)
            result.add(Component.translatable("gui.changede.smeltery.molten_latex_amounts", moltenDark, moltenWhite).withStyle(ChatFormatting.GRAY));
        if (!isLatex(info.getFluid().getFluid()) || access.changede$coreReady() || result.size() <= 1)
            result.add(Component.translatable(access.changede$coreReady() ? "gui.changede.smeltery.core_ready" : "gui.changede.smeltery.requires_core")
                    .withStyle(access.changede$coreReady() ? ChatFormatting.GREEN : ChatFormatting.RED));
        return result;
    }
}
