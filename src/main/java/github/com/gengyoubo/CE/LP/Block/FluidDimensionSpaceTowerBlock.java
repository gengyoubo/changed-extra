package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.FluidDimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.LP.world.Menu.FluidDimensionSpaceTowerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
@SuppressWarnings("deprecation")
public final class FluidDimensionSpaceTowerBlock extends SpaceTowerBlock {
    public FluidDimensionSpaceTowerBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidDimensionSpaceTowerBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, CELPBlockEntity.FLUID_DIMENSION_SPACE_TOWER.get(),
                (world, pos, blockState, tower) -> tower.tick());
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, accessPlayer) -> new FluidDimensionSpaceTowerMenu(id, inventory, pos),
                    Component.translatable("block.changede.fluid_dimension_space_tower")), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                          net.minecraft.world.level.block.Block block, BlockPos neighbor, boolean moving) {
        if (level.getBlockEntity(pos) instanceof FluidDimensionSpaceTowerBlockEntity tower) tower.refreshNetwork();
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof FluidDimensionSpaceTowerBlockEntity tower) tower.shutdown();
        super.onRemove(state, level, pos, replacement, moving);
    }
}
