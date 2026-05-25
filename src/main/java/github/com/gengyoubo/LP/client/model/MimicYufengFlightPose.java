package github.com.gengyoubo.LP.client.model;

import github.com.gengyoubo.LP.item.MimicYufengWingsItem;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class MimicYufengFlightPose {
    private MimicYufengFlightPose() {
    }

    public static boolean shouldPose(LivingEntity entity) {
        if (entity.isFallFlying()) {
            return false;
        }
        if (entity instanceof Player player) {
            return MimicYufengWingsItem.isWornBy(player) && player.getAbilities().flying;
        }
        if (entity instanceof ChangedEntity changedEntity) {
            Player player = changedEntity.getUnderlyingPlayer();
            return player != null
                    && (MimicYufengWingsItem.isWornBy(changedEntity) || MimicYufengWingsItem.isWornBy(player))
                    && player.getAbilities().flying;
        }
        return false;
    }

    public static void apply(HumanoidModel<?> model, LivingEntity entity, float ageInTicks) {
        if (!shouldPose(entity)) {
            return;
        }

        float bob = Mth.cos(ageInTicks * 0.18F) * 0.025F;
        model.body.xRot = -0.12F + bob;
        model.head.xRot += 0.08F;
        model.rightArm.xRot = -0.42F - bob;
        model.leftArm.xRot = -0.42F - bob;
        model.rightArm.zRot += 0.09F;
        model.leftArm.zRot -= 0.09F;
        model.rightLeg.xRot = 0.12F + bob * 0.5F;
        model.leftLeg.xRot = 0.12F - bob * 0.5F;
    }
}
