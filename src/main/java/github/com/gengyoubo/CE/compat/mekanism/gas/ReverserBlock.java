package github.com.gengyoubo.CE.compat.mekanism.gas;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkHooks;

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
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ReverserBlockEntity tile) {
            if (tile.tryWrench(state, player, hand, hit) != mekanism.common.tile.base.WrenchResult.PASS) return InteractionResult.SUCCESS;
            NetworkHooks.openScreen(server, new SimpleMenuProvider((id, inventory, openingPlayer) -> new ReverserMenu(id, inventory, pos),
                    Component.translatable("block.changede.reverser")), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
