package github.com.gengyoubo.LP.Block;

import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.E.BasicEnergyPipeBlockEntity;
import github.com.gengyoubo.LP.CELPRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicEnergyPipeBlock extends BaseEntityBlock {

    public BasicEnergyPipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BasicEnergyPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            @NotNull Level level,
            @NotNull BlockState state,
            @NotNull BlockEntityType<T> type
    ) {
        if (type != CELPRegister.BASIC_WIRE_BLOCK_ENTITIES.get()) {
            return null;
        }

        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof BasePipeBlockEntity pipeBlockEntity) {
                pipeBlockEntity.tick();
            }
        };
    }
}
