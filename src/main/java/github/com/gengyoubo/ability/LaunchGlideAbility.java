package github.com.gengyoubo.ability;

import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.ability.SimpleAbility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class LaunchGlideAbility extends SimpleAbility {
    private static final double FORWARD_BOOST = 1.15D;
    private static final double UPWARD_BOOST = 0.72D;

    @Override
    public boolean canUse(IAbstractChangedEntity entity) {
        LivingEntity living = entity.getEntity();
        return living instanceof Player
                && living.onGround()
                && !living.isFallFlying()
                && !living.isInWaterOrBubble();
    }

    @Override
    public void startUsing(IAbstractChangedEntity entity) {
        super.startUsing(entity);

        LivingEntity living = entity.getEntity();
        if (!(living instanceof Player player) || !living.onGround() || living.isFallFlying()) return;

        Vec3 look = living.getLookAngle().normalize();
        living.setDeltaMovement(
                look.x * FORWARD_BOOST,
                UPWARD_BOOST,
                look.z * FORWARD_BOOST
        );
        living.setOnGround(false);
        player.startFallFlying();
        living.hurtMarked = true;
    }
}
