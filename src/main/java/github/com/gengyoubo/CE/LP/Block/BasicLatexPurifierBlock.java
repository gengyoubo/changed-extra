package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicLatexPurifierBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.LP.world.Menu.BasicLatexPurifierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class BasicLatexPurifierBlock extends BaseEntityBlock {
    private static final Component TITLE = Component.translatable("block.changede.basic_latex_purifier");

    public BasicLatexPurifierBlock(Properties properties) {
        super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1f, 10f));
    }

    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.MODEL; }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof BasicLatexPurifierBlockEntity purifier) {
            for (int slot = 0; slot < purifier.getItemHandler().getSlots(); slot++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        purifier.getItemHandler().getStackInSlot(slot));
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BasicLatexPurifierBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                                                     @NotNull Player player, @NotNull InteractionHand hand,
                                                     @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider((id, inventory, accessPlayer) ->
                    new BasicLatexPurifierMenu(id, inventory, pos), TITLE);
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (type != CELPBlockEntity.BASIC_LATEX_PURIFIER.get()) return null;
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof BasicLatexPurifierBlockEntity purifier) purifier.tick();
        };
    }
}
