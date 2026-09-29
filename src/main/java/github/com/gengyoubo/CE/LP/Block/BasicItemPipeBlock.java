package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.I.BasicItemPipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicItemPipeBlock extends BasicEnergyPipeBlock {
    public BasicItemPipeBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override protected TransportType getTransportType() { return TransportType.ITEM; }
    @Override protected BlockEntityType<?> getPipeBlockEntityType() { return CELPBlockEntity.BASIC_ITEM_PIPE_BLOCK_ENTITY.get(); }
    @Override public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) { return new BasicItemPipeBlockEntity(pos, state); }
}
