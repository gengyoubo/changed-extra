package github.com.gengyoubo.CE.LP.Block;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.CreateSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class CreateSpaceTowerBlock extends DirectionalKineticBlock implements IBE<CreateSpaceTowerBlockEntity> {
    public CreateSpaceTowerBlock(Properties properties) {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.0F, 12.0F).noOcclusion());
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return true;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        return SpaceTowerBlockInteraction.openMenu(level, pos, player);
    }

    @Override
    public Class<CreateSpaceTowerBlockEntity> getBlockEntityClass() {
        return CreateSpaceTowerBlockEntity.class;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BlockEntityType<? extends CreateSpaceTowerBlockEntity> getBlockEntityType() {
        return (BlockEntityType) CELPBlockEntity.SPACE_TOWER_BLOCK_ENTITY.get();
    }
}
