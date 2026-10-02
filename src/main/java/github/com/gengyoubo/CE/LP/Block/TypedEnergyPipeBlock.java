package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.ILatexTypedEnergyHandler;
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
        return level.getBlockEntity(pos) instanceof ILatexTypedEnergyHandler handler && handler.getEnergyType()==energyType;
    }
}
