package github.com.gengyoubo.CE.LP.compat;

import github.com.gengyoubo.CE.changede;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.InvocationTargetException;

final class OptionalSpaceTowerBlockEntities {
    private static final String AE2_SPACE_TOWER =
            "github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.Ae2SpaceTowerBlockEntity";
    private static final String CREATE_AE2_SPACE_TOWER =
            "github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.CreateAe2SpaceTowerBlockEntity";

    private OptionalSpaceTowerBlockEntities() {
    }

    static BlockEntity createAe2(BlockPos pos, BlockState state) {
        return create(AE2_SPACE_TOWER, pos, state);
    }

    static BlockEntity createCreateAe2(BlockPos pos, BlockState state) {
        return create(CREATE_AE2_SPACE_TOWER, pos, state);
    }

    private static BlockEntity create(String className, BlockPos pos, BlockState state) {
        try {
            Object instance = Class.forName(className)
                    .getConstructor(BlockPos.class, BlockState.class)
                    .newInstance(pos, state);
            if (instance instanceof BlockEntity blockEntity) {
                return blockEntity;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException | LinkageError exception) {
            changede.LOGGER.warn("Failed to create optional space tower block entity {}", className, exception);
        }
        return null;
    }
}
