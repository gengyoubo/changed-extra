package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.LP.item.MimicSirenTailItem;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MimicSirenSwimmingMixin {
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void changede$swimWithFishTail(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.getForcedPose() == null && MimicSirenTailItem.shouldSwimUnderwater(player)) {
            player.setPose(Pose.SWIMMING);
            ci.cancel();
        }
    }
}
