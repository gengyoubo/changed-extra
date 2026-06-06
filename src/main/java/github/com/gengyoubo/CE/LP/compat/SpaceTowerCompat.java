package github.com.gengyoubo.CE.LP.compat;

import github.com.gengyoubo.CE.LP.Block.SpaceTowerBlock;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.SpaceTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

public final class SpaceTowerCompat {
    private SpaceTowerCompat() {
    }

    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }

    public static boolean isAe2Loaded() {
        return ModList.get().isLoaded("ae2");
    }

    public static Block createBlock(BlockBehaviour.Properties properties) {
        if (isCreateLoaded()) {
            return CreateSpaceTowerCompat.createBlock(properties);
        }
        return new SpaceTowerBlock(properties);
    }

    public static BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        boolean createLoaded = isCreateLoaded();
        if (createLoaded) {
            return CreateSpaceTowerCompat.createBlockEntity(pos, state);
        }
        if (isAe2Loaded()) {
            BlockEntity ae2BlockEntity = OptionalSpaceTowerBlockEntities.createAe2(pos, state);
            if (ae2BlockEntity != null) {
                return ae2BlockEntity;
            }
        }
        return new SpaceTowerBlockEntity(pos, state);
    }
}
