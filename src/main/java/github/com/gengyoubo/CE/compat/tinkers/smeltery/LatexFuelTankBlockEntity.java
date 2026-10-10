package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

public final class LatexFuelTankBlockEntity extends TankBlockEntity {
    public LatexFuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(LatexSmelteryCompat.TANK_ENTITY.get(), pos, state, LatexSmelteryCompat.TANK.get());
        tank.setValidator(stack -> LatexSmelteryRules.isLatex(stack.getFluid()));
    }
    @Override public void updateTank(CompoundTag nbt) {
        super.updateTank(nbt);
        if (!tank.isEmpty() && !LatexSmelteryRules.isLatex(tank.getFluid().getFluid())) tank.setFluid(FluidStack.EMPTY);
        else if (tank.getFluidAmount() > LatexSmelteryCompat.CAPACITY) tank.getFluid().setAmount(LatexSmelteryCompat.CAPACITY);
    }
}
