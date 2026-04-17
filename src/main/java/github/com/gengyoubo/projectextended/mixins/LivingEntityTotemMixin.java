package github.com.gengyoubo.projectextended.mixins;

import github.com.gengyoubo.changede;
import github.com.gengyoubo.projectextended.items.CETotemOfUndying;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTotemMixin {
    @Shadow
    public abstract ItemStack getItemInHand(InteractionHand hand);

    @Shadow
    public abstract void setHealth(float health);

    @Shadow
    public abstract boolean removeAllEffects();

    @Shadow
    public abstract boolean addEffect(MobEffectInstance effect);

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void changede$useMatterTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if(!changede.PROJECTE) {
            return;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        LivingEntity livingEntity = (LivingEntity) (Object) this;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = this.getItemInHand(hand);
            if (!(stack.getItem() instanceof CETotemOfUndying totem)) {
                continue;
            }
            if (livingEntity instanceof ServerPlayer serverPlayer && serverPlayer.getCooldowns().isOnCooldown(totem)) {
                continue;
            }

            if (livingEntity instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, stack);
                serverPlayer.awardStat(Stats.ITEM_USED.get(totem));
                if (totem.hasCooldown()) {
                    serverPlayer.getCooldowns().addCooldown(totem, totem.getCooldownTicks());
                }
            }

            this.setHealth(1.0F);
            this.removeAllEffects();
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            this.changede$spawnActivationParticles(livingEntity, totem);
            livingEntity.level().broadcastEntityEvent(livingEntity, (byte) 35);
            cir.setReturnValue(true);
            return;
        }
    }

    @Unique
    private void changede$spawnActivationParticles(LivingEntity livingEntity, CETotemOfUndying totem) {
        if(changede.PROJECTE) {
            return;
        }
        if (!(livingEntity.level() instanceof ServerLevel serverLevel) || !totem.isTrueMatterTotem()) {
            return;
        }

        double x = livingEntity.getX();
        double y = livingEntity.getY() + livingEntity.getBbHeight() * 0.5D;
        double z = livingEntity.getZ();

        serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 24, 0.35D, 0.65D, 0.35D, 0.03D);

        for (int i = 0; i < 30; i++) {
            float hue = i / 30.0F;
            int rgb = Mth.hsvToRgb(hue, 1.0F, 1.0F);
            float red = ((rgb >> 16) & 255) / 255.0F;
            float green = ((rgb >> 8) & 255) / 255.0F;
            float blue = (rgb & 255) / 255.0F;
            double dx = (livingEntity.getRandom().nextDouble() - 0.5D) * 0.7D;
            double dy = livingEntity.getRandom().nextDouble() * 0.45D + 0.08D;
            double dz = (livingEntity.getRandom().nextDouble() - 0.5D) * 0.7D;
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(red, green, blue), 1.5F),
                    x, y, z, 2, dx, dy, dz, 0.02D);
        }
    }
}
