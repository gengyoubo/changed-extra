package github.com.gengyoubo.CE.LP.compat;

import github.com.gengyoubo.CE.LP.Block.CreateSpaceTowerBlock;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.CreateAe2SpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.CreateSpaceTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class CreateSpaceTowerCompat {
    private CreateSpaceTowerCompat() {
    }

    public static Block createBlock(BlockBehaviour.Properties properties) {
        return new CreateSpaceTowerBlock(properties);
    }

    public static BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if (SpaceTowerCompat.isAe2Loaded()) {
            return new CreateAe2SpaceTowerBlockEntity(pos, state);
        }
        return new CreateSpaceTowerBlockEntity(pos, state);
    }
}
