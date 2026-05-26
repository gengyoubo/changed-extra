package github.com.gengyoubo.CE.ability;

import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.ability.SimpleAbility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class LaunchGlideAbility extends SimpleAbility {
    private static final double FORWARD_BOOST = 1.15D;
    private static final double LAUNCH_UPWARD_BOOST = 0.72D;
    private static final double GLIDE_BOOST = 1.35D;
    private static final double GLIDE_UPWARD_BOOST = 0.02D;

    @Override
    public boolean canUse(IAbstractChangedEntity entity) {
        LivingEntity living = entity.getEntity();
        if (!(living instanceof Player)) return false;
        if (living.isInWaterOrBubble()) return false;
        return living.isFallFlying() || living.onGround();
    }

    @Override
    public void startUsing(IAbstractChangedEntity entity) {
        super.startUsing(entity);

        LivingEntity living = entity.getEntity();
        if (!(living instanceof Player player) || living.isInWaterOrBubble()) return;

        Vec3 look = living.getLookAngle().normalize();

        if (living.isFallFlying()) {
            Vec3 velocity = living.getDeltaMovement();
            living.setDeltaMovement(
                    velocity.x + look.x * GLIDE_BOOST,
                    velocity.y + Math.max(look.y * GLIDE_BOOST, GLIDE_UPWARD_BOOST),
                    velocity.z + look.z * GLIDE_BOOST
            );
        } else if (living.onGround()) {
            living.setDeltaMovement(
                    look.x * FORWARD_BOOST,
                    LAUNCH_UPWARD_BOOST,
                    look.z * FORWARD_BOOST
            );
            living.setOnGround(false);
            player.startFallFlying();
        } else {
            return;
        }

        living.hurtMarked = true;
    }
}
