package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.skill.SkillMechanics;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SkillArthropodMixin {
    @Inject(method="getMobType",at=@At("RETURN"),cancellable=true)
    private void changede$arthropod(CallbackInfoReturnable<MobType> ci) {
        if((Object)this instanceof Player player && SkillMechanics.value(player,"arthropod")>0)ci.setReturnValue(MobType.ARTHROPOD);
    }
}
