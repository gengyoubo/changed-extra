package github.com.gengyoubo.ability;

import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.ability.SimpleAbility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class AcceleratedGlideAbility extends SimpleAbility {
    private static final double BOOST = 1.35D;
    private static final double UPWARD_BOOST = 0.02D;

    @Override
    public boolean canUse(IAbstractChangedEntity entity) {
        LivingEntity living = entity.getEntity();
        if (!entity.isPlayer()) return false;
        return living.isFallFlying();
    }

    @Override
    public void startUsing(IAbstractChangedEntity entity) {
        super.startUsing(entity);

        LivingEntity living = entity.getEntity();
        if (!living.isFallFlying()) return;

        Vec3 look = living.getLookAngle().normalize();
        Vec3 velocity = living.getDeltaMovement();

        Vec3 boosted = velocity.add(
                look.x * BOOST,
                Math.max(look.y * BOOST, UPWARD_BOOST),
                look.z * BOOST
        );

        living.setDeltaMovement(boosted);
        living.hurtMarked = true;
    }
}