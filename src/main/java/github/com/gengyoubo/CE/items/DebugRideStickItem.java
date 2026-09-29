package github.com.gengyoubo.CE.items;

import github.com.gengyoubo.CE.events.DebugRideStickEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class DebugRideStickItem extends Item {
    private final Mode mode;

    public DebugRideStickItem(Mode mode, Properties properties) {
        super(properties);
        this.mode = mode;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                           @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!player.level().isClientSide) {
            switch (mode) {
                case PLAYER_RIDES_TARGET -> DebugRideStickEvents.makePlayerRideTarget(player, target);
                case TARGET_RIDES_PLAYER -> DebugRideStickEvents.makeTargetRidePlayer(player, target);
            }
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    public enum Mode {
        PLAYER_RIDES_TARGET,
        TARGET_RIDES_PLAYER
    }
}
