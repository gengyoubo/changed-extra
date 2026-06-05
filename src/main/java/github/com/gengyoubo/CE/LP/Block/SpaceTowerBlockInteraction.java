package github.com.gengyoubo.CE.LP.Block;

import github.com.gengyoubo.CE.LP.world.Menu.SpaceTowerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

final class SpaceTowerBlockInteraction {
    private static final Component TITLE = Component.translatable("screen.changede.space_tower.title");

    private SpaceTowerBlockInteraction() {
    }

    static InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inventory, accessPlayer) -> new SpaceTowerMenu(id, inventory, pos),
                    TITLE
            );
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }

        return InteractionResult.CONSUME;
    }
}
