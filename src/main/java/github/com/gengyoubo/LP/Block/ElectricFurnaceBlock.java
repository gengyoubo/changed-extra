package github.com.gengyoubo.LP.Block;

import github.com.gengyoubo.LP.BlockEntity.MachineBlockEntity.ElectricFurnaceBlockEntity;
import github.com.gengyoubo.LP.CELPRegister;
import github.com.gengyoubo.LP.world.Menu.ElectricFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
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

public class ElectricFurnaceBlock extends BaseEntityBlock implements EntityBlock {
    private static final Component TITLE = Component.literal("Electric Furnace");

    public ElectricFurnaceBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.GRAVEL).strength(1f, 10f));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ElectricFurnaceBlockEntity electricFurnaceBlockEntity) {
                for (int slot = 0; slot < electricFurnaceBlockEntity.getItemHandler().getSlots(); slot++) {
                    Containers.dropItemStack(
                            level,
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            electricFurnaceBlockEntity.getItemHandler().getStackInSlot(slot)
                    );
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (type != CELPRegister.ELECTRIC_FURNACE_BLOCK_ENTITY.get()) {
            return null;
        }

        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof ElectricFurnaceBlockEntity electricFurnaceBlockEntity) {
                electricFurnaceBlockEntity.tick();
            }
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inventory, accessPlayer) -> new ElectricFurnaceMenu(id, inventory, pos),
                    TITLE
            );
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }

        return InteractionResult.CONSUME;
    }
}
