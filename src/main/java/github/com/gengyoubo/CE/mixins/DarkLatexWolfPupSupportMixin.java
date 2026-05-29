package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.events.DarkLatexWolfPupSupportEvents;
import net.ltxprogrammer.changed.entity.beast.DarkLatexWolfPup;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DarkLatexWolfPup.class, remap = false)
public class DarkLatexWolfPupSupportMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void changede$directAttackWhenUnsupported(Entity target, CallbackInfoReturnable<Boolean> cir) {
        DarkLatexWolfPup pup = (DarkLatexWolfPup)(Object)this;
        if (!DarkLatexWolfPupSupportEvents.hasNonPupDarkLatexSupport(pup)) {
            cir.setReturnValue(changede$doDirectMeleeAttack(pup, target));
        }
    }

    @Unique
    private boolean changede$doDirectMeleeAttack(DarkLatexWolfPup pup, Entity target) {
        float damage = (float)pup.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float knockback = (float)pup.getAttributeValue(Attributes.ATTACK_KNOCKBACK);

        if (target instanceof LivingEntity livingTarget) {
            damage += EnchantmentHelper.getDamageBonus(pup.getMainHandItem(), livingTarget.getMobType());
            knockback += EnchantmentHelper.getKnockbackBonus(pup);
        }

        int fireAspect = EnchantmentHelper.getFireAspect(pup);
        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }

        boolean hurt = target.hurt(pup.damageSources().mobAttack(pup), damage);
        if (hurt) {
            if (knockback > 0.0F && target instanceof LivingEntity livingTarget) {
                float yRotRadians = pup.getYRot() * Mth.DEG_TO_RAD;
                livingTarget.knockback(knockback * 0.5F, Mth.sin(yRotRadians), -Mth.cos(yRotRadians));
                pup.setDeltaMovement(pup.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }

            pup.doEnchantDamageEffects(pup, target);
            pup.setLastHurtMob(target);
        }

        return hurt;
    }
}
