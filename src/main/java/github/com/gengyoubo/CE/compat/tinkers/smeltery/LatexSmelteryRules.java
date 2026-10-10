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
    public static boolean isLatex(Fluid fluid) { return fluid == ChangedFluids.DARK_LATEX.get() || fluid == ChangedFluids.WHITE_LATEX.get(); }
    public static boolean coreAt(HeatingStructureBlockEntity parent, BlockPos pos) {
        var level = parent.getLevel(); var structure = parent.getStructure();
        if (level == null || structure == null || pos == null || !structure.contains(pos) || !level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        // Replacing an existing valid wall does not reassign native servant flags.
        // Membership in the formed controller's structure is the authoritative check.
        return state.is(LatexSmelteryCompat.CORE.get()) && parent.getBlockState().getValue(ControllerBlock.IN_STRUCTURE);
    }
    public static BlockPos findCore(HeatingStructureBlockEntity parent) {
        var structure = parent.getStructure();
        if (structure == null) return null;
        for (BlockPos pos : BlockPos.betweenClosed(structure.getMinPos(), structure.getMaxPos()))
            if (coreAt(parent, pos)) return pos.immutable();
        return null;
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
        int dark = 0, white = 0;
        for (IFluidHandler tank : tanks) {
            relevant |= tank instanceof slimeknights.tconstruct.library.fluid.FluidTankBase<?> && tank.getTankCapacity(0) == LatexSmelteryCompat.CAPACITY;
            var fluid = tank.getFluidInTank(0);
            if (fluid.getFluid() == ChangedFluids.DARK_LATEX.get()) dark += fluid.getAmount();
            if (fluid.getFluid() == ChangedFluids.WHITE_LATEX.get()) white += fluid.getAmount();
        }
        if (!relevant && dark == 0 && white == 0) return original;
        List<Component> result = new ArrayList<>(original);
        if (isLatex(info.getFluid().getFluid()) && !access.changede$coreReady() && result.size() > 1)
            result.set(1, Component.translatable("gui.changede.smeltery.requires_core").withStyle(ChatFormatting.RED));
        if (!info.isEmpty()) result.add(Component.translatable("gui.changede.smeltery.fuel_capacity", info.getTotalAmount(), info.getCapacity()).withStyle(ChatFormatting.GRAY));
        result.add(Component.translatable("gui.changede.smeltery.latex_amounts", dark, white).withStyle(ChatFormatting.GRAY));
        if (!isLatex(info.getFluid().getFluid()) || access.changede$coreReady() || result.size() <= 1)
            result.add(Component.translatable(access.changede$coreReady() ? "gui.changede.smeltery.core_ready" : "gui.changede.smeltery.requires_core")
                    .withStyle(access.changede$coreReady() ? ChatFormatting.GREEN : ChatFormatting.RED));
        return result;
    }
}
