package github.com.gengyoubo.projectextended.events;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import github.com.gengyoubo.projectextended.items.CEShield;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede")
public class CEShieldEvents {

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        ItemStack shieldStack = blocker.getUseItem();
        if (!(shieldStack.getItem() instanceof CEShield shield) || !shield.canReflect(shieldStack)) {
            return;
        }

        event.setShieldTakesDamage(false);
        float reflectedDamage = event.getBlockedDamage() * shield.getReflectMultiplier(shieldStack);
        if (reflectedDamage <= 0) {
            return;
        }

        DamageSource reflectedSource = blocker.damageSources().thorns(blocker);
        Entity attacker = event.getDamageSource().getEntity();
        if (attacker instanceof LivingEntity livingAttacker) {
            livingAttacker.hurt(reflectedSource, reflectedDamage);
            notifyReflectSuccess(blocker);
            return;
        }

        Entity directEntity = event.getDamageSource().getDirectEntity();
        if (directEntity instanceof LivingEntity livingDirect) {
            livingDirect.hurt(reflectedSource, reflectedDamage);
            notifyReflectSuccess(blocker);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.isUsingItem()) {
            return;
        }

        ItemStack shieldStack = target.getUseItem();
        if (!(shieldStack.getItem() instanceof CEShield shield) || !shield.canNegateDamage(shieldStack)) {
            return;
        }

        if (shield.canNegateAllDamage(shieldStack) || target.isDamageSourceBlocked(event.getSource())) {
            event.setCanceled(true);
        }
    }

    private static void notifyReflectSuccess(LivingEntity blocker) {
        if (blocker instanceof ServerPlayer serverPlayer) {
            awardAdvancement(serverPlayer, ResourceLocation.fromNamespaceAndPath("changede", "shield_rebound"));
        }
    }

    private static void awardAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
