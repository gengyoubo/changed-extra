package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.skill.SkillFoodData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class SkillFoodOwnerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void changede$bindFoodOwner(CallbackInfo ci) { changede$bind(); }
    @Inject(method = "tick", at = @At("HEAD"))
    private void changede$refreshFoodOwner(CallbackInfo ci) { changede$bind(); }
    @Unique private void changede$bind() {
        Player player = (Player) (Object) this;
        if (player.getFoodData() instanceof SkillFoodData data) data.changede$setOwner(player);
    }
}
