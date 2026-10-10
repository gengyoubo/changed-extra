package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;

public final class LatexFuelTankBlock extends SearedTankBlock {
    public LatexFuelTankBlock() {
        super(LatexSmelteryCompat.properties().noOcclusion().lightLevel(LIGHT_GETTER), LatexSmelteryCompat.CAPACITY);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LatexFuelTankBlockEntity(pos, state); }
}
