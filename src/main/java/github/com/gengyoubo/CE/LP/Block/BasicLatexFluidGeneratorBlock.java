package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.BasicLatexFluidGeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.GeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.LP.world.Menu.BasicGeneratorBlockEntityMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicLatexFluidGeneratorBlock extends BasicGeneratorBlock {
    private static final Component TITLE = Component.translatable("block.changede.basic_latex_fluid_generator");

    public BasicLatexFluidGeneratorBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BasicLatexFluidGeneratorBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (type != CELPBlockEntity.BASIC_LATEX_FLUID_GENERATOR_BLOCK_ENTITY.get()) return null;
        return (tickLevel, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof GeneratorBlockEntity generator) generator.tick();
        };
    }

    @SuppressWarnings("deprecation")
    @Override public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
            @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            github.com.gengyoubo.CE.LP.world.Menu.MachineStatusMenu.open(serverPlayer, pos);
        }
        return InteractionResult.CONSUME;
    }
}
