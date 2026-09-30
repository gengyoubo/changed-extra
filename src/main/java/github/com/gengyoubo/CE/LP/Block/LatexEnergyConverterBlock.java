package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.LatexEnergyConverterBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LatexEnergyConverterBlock extends BaseEntityBlock {
    private final boolean white;
    public LatexEnergyConverterBlock(BlockBehaviour.Properties properties, boolean white) {
        super(BlockBehaviour.Properties.of().strength(2f, 10f));
        this.white = white;
    }
    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.MODEL; }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            github.com.gengyoubo.CE.LP.world.Menu.MachineStatusMenu.open(serverPlayer, pos);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LatexEnergyConverterBlockEntity(pos, state, white);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityType<?> expected = white ? CELPBlockEntity.WHITE_LATEX_POWER_CONVERTER.get() : CELPBlockEntity.DARK_LATEX_POWER_CONVERTER.get();
        if (type != expected || level.isClientSide) return null;
        return (world, pos, blockState, be) -> { if (be instanceof LatexEnergyConverterBlockEntity converter) converter.tick(); };
    }
}
