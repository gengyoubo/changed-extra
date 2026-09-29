package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.BasicCrystalGeneratorBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicCrystalGeneratorBlock extends BasicGeneratorBlock {
    private static final Component TITLE = Component.translatable("block.changede.basic_crystal_generator");

    public BasicCrystalGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BasicCrystalGeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (type != CELPBlockEntity.BASIC_CRYSTAL_GENERATOR_BLOCK_ENTITY.get()) return null;
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof GeneratorBlockEntity generator) generator.tick();
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inventory, accessPlayer) -> new BasicGeneratorBlockEntityMenu(id, inventory, pos),
                    TITLE
            );
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }
        return InteractionResult.CONSUME;
    }
}
