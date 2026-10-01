package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicPumpBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicPumpBlock extends BaseEntityBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BasicPumpBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(2.0F).requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.MODEL; }
    @Override public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) { return new BasicPumpBlockEntity(pos, state); }
    @Override public @NotNull BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override @SuppressWarnings("deprecation") public @NotNull BlockState rotate(@NotNull BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override @SuppressWarnings("deprecation") public @NotNull BlockState mirror(@NotNull BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @SuppressWarnings("deprecation")
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            github.com.gengyoubo.CE.LP.world.Menu.MachineStatusMenu.open(serverPlayer, pos);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (type != CELPBlockEntity.BASIC_PUMP_BLOCK_ENTITY.get()) return null;
        return (tickLevel, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof BasicPumpBlockEntity pump) pump.tick();
        };
    }
}
