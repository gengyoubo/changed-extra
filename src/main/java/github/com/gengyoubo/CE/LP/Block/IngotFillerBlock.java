package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.IngotFillerBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.LP.world.Menu.IngotFillerMenu;
import github.com.gengyoubo.CE.LP.recipe.IngotFillingDiagnostics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.network.NetworkHooks;

@SuppressWarnings("deprecation")
public final class IngotFillerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public IngotFillerBlock(Properties properties) {
        super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new IngotFillerBlockEntity(pos,state); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState state,Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state,Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return !level.isClientSide && type==CELPBlockEntity.INGOT_FILLER.get() ? (world,pos,block,entity)->((IngotFillerBlockEntity)entity).tick() : null;
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(FluidUtil.interactWithFluidHandler(player,hand,level,pos,hit.getDirection()))return InteractionResult.CONSUME;
        if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,new SimpleMenuProvider(
                (id,inventory,p)->new IngotFillerMenu(id,inventory,pos),Component.translatable("block.changede.ingot_filler")),
                buffer->{buffer.writeBlockPos(pos);buffer.writeUtf(IngotFillingDiagnostics.getDisplay());});
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof IngotFillerBlockEntity machine) {
            for(int slot=0;slot<2;slot++)Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),machine.getItemHandler().getStackInSlot(slot));
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
