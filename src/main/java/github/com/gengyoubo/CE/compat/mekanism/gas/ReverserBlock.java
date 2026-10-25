package github.com.gengyoubo.CE.compat.mekanism.gas;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.network.chat.Component;

/** Mekanism's BlockTile handles the wrench, security and opening the GUI. */
public final class ReverserBlock extends BlockTile<ReverserBlockEntity, BlockTypeTile<ReverserBlockEntity>> {
    public ReverserBlock() {
        super(GasCompat.REVERSER_TYPE, properties -> properties);
        registerDefaultState(defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH));
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.BlockGetter level,
            java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.changede.reverser"));
        tooltip.add(Component.translatable("tooltip.changede.reverser.ports"));
    }
}
