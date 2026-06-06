package github.com.gengyoubo.CE.LP.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class Ae2SpaceTowerCompat {
    private Ae2SpaceTowerCompat() {
    }

    public static BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return OptionalSpaceTowerBlockEntities.createAe2(pos, state);
    }
}
