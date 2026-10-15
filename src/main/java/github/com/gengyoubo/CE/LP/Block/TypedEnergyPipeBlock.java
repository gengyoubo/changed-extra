package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.ILatexTypedEnergyHandler;
import github.com.gengyoubo.CE.LP.ILatexMultiEnergyHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.E.TypedEnergyPipeBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Reuses the six-port pipe shape and wrench menu, with strictly separate energy channels. */
public final class TypedEnergyPipeBlock extends BasicEnergyPipeBlock {
    private final LatexEnergyType energyType;
    public TypedEnergyPipeBlock(BlockBehaviour.Properties properties,LatexEnergyType type) {
        super(properties);energyType=type;
    }
    @Override protected BlockEntityType<?> getPipeBlockEntityType() {
        return energyType==LatexEnergyType.WLP ? CELPBlockEntity.WLP_PIPE.get() : CELPBlockEntity.DLP_PIPE.get();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return new TypedEnergyPipeBlockEntity(pos,state,energyType);
    }
    @Override protected boolean canConnectTo(LevelAccessor level,BlockPos pos) {
        if(level instanceof Level world && !world.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))return false;
        return level.getBlockEntity(pos) instanceof ILatexMultiEnergyHandler handler && handler.supportsTypedEnergy(energyType, null);
    }
    private boolean connects(LevelAccessor level, BlockPos target, Direction targetSide) {
        if (level instanceof Level world && !world.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4)) return false;
        return level.getBlockEntity(target) instanceof ILatexMultiEnergyHandler handler && handler.supportsTypedEnergy(energyType, targetSide);
    }
    private static BooleanProperty port(Direction side) {
        return switch (side) { case NORTH -> NORTH; case SOUTH -> SOUTH; case EAST -> EAST; case WEST -> WEST; case UP -> UP; case DOWN -> DOWN; };
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values()) state = state.setValue(port(side), connects(context.getLevel(), context.getClickedPos().relative(side), side.getOpposite()));
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos target) {
        return state.setValue(port(side), connects(level, target, side.getOpposite()));
    }
}
