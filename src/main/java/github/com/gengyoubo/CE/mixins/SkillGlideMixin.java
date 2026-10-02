package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.skill.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Slows glide translation and steering without moving the player's camera. */
@Mixin(LivingEntity.class)
public abstract class SkillGlideMixin {
    @ModifyArg(method="travel",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),index=1)
    private Vec3 changede$glideSpeed(Vec3 motion) {
        if((Object)this instanceof Player player && player.isFallFlying() && !player.isCreative() && !player.isSpectator()
                && SkillMechanics.value(player,"air_core")>0)
            return motion.multiply(SkillMechanics.flightMultiplier(player),1,SkillMechanics.flightMultiplier(player));
        return motion;
    }
    @Redirect(method="travel",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 changede$glideSteering(LivingEntity entity) {
        Vec3 look=entity.getLookAngle();
        if(entity instanceof Player player && player.isFallFlying() && SkillMechanics.value(player,"air_core")>0) {
            double response=SkillMechanics.flightMultiplier(player);
            Vec3 motion=player.getDeltaMovement().normalize();
            if(motion.lengthSqr()>0)return look.scale(response).add(motion.scale(1-response)).normalize();
        }
        return look;
    }
}
