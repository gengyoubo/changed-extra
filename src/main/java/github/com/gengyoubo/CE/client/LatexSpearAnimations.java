package github.com.gengyoubo.CE.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.CE.items.LatexSpearItem;
import github.com.gengyoubo.CE.items.SpearCombatRules;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Client-only STAB state. Only accepted server jabs start an animation. */
@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class LatexSpearAnimations {
    private static final Map<LivingEntity, Integer> JABS = new WeakHashMap<>();

    public static void receiveJab(int entityId, UUID playerId, ResourceLocation dimension) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(dimension)) return;
        if (level.getEntity(entityId) instanceof LivingEntity entity && entity.getUUID().equals(playerId)) {
            JABS.put(entity, entity.tickCount);
        }
    }

    private static LivingEntity owner(LivingEntity entity) {
        return entity instanceof ChangedEntity changed && changed.getUnderlyingPlayer() != null
                ? changed.getUnderlyingPlayer() : entity;
    }

    public static float jabProgress(LivingEntity entity, float partialTick) {
        entity = owner(entity);
        Integer start = JABS.get(entity);
        if (start == null || !entity.isAlive() || entity.isUsingItem()
                || !(entity.getMainHandItem().getItem() instanceof LatexSpearItem)) return -1;
        float elapsed = entity.tickCount - start + partialTick;
        return elapsed >= 0 && elapsed < SpearJabAnimation.DURATION_TICKS
                ? elapsed / SpearJabAnimation.DURATION_TICKS : -1;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var level = Minecraft.getInstance().level;
        JABS.entrySet().removeIf(entry -> entry.getKey().level() != level || entry.getKey().isRemoved()
                || jabProgress(entry.getKey(), 0) < 0);
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { JABS.clear(); }

    @SubscribeEvent public static void firstPerson(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        ItemStack stack = event.getItemStack();
        if (player == null || player.isScoping()) return;
        // Apply visibility before filtering the rendered stack: the other hand may be empty,
        // hold a sword/food, or hold a second spear.
        if (hideOtherHand(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        if (!(stack.getItem() instanceof LatexSpearItem)) return;
        event.setCanceled(true);
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite();
        int sign = arm == HumanoidArm.RIGHT ? 1 : -1;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(sign * 0.56F, -0.52F - event.getEquipProgress() * 0.6F, -0.72F);
            float progress = event.getHand() == InteractionHand.MAIN_HAND
                    ? jabProgress(player, event.getPartialTick()) : -1;
            if (progress >= 0) firstPersonAttackItem(pose, arm, progress);
            else if (using(player, event.getHand(), stack)) {
                firstPersonUseItem(pose, arm, useTicks(player, event.getPartialTick()));
            }
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                    arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                            : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    arm == HumanoidArm.LEFT, pose, event.getMultiBufferSource(), event.getPackedLight());
        } finally { pose.popPose(); }
    }

    public static boolean hideOtherHand(LivingEntity entity, InteractionHand renderedHand) {
        LivingEntity source = owner(entity);
        return source.isUsingItem() && source.getUseItem().getItem() instanceof LatexSpearItem
                && source.getUsedItemHand() != renderedHand;
    }

    public static void firstPersonAttackItem(PoseStack pose, HumanoidArm arm, float progress) {
        var sample = SpearJabAnimation.sample(progress);
        int sign = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(-sign * 0.08F * sample.thrust(), -0.06F * sample.raise(), -0.85F * sample.thrust());
        pose.mulPose(Axis.XP.rotationDegrees(-70 * sample.raise()));
        pose.mulPose(Axis.ZP.rotationDegrees(sign * 15 * sample.raise()));
    }

    /** Applied after hand attachment/base transforms, before the held item is drawn. */
    public static void thirdPersonAttackItem(PoseStack pose, float progress) {
        var sample = SpearJabAnimation.sample(progress);
        // The CE model's long axis is local Y. Counter the arm pitch before extending along it.
        // The base hand transform includes a 180-degree Y rotation, reversing local X.
        pose.mulPose(Axis.XP.rotationDegrees(sample.armPitch()));
        pose.translate(0, 0.7F * sample.thrust(), 0);
    }

    public static void thirdPersonItem(LivingEntity entity, ItemStack stack, ItemDisplayContext display,
                                       PoseStack pose, float partialTick) {
        if (!(stack.getItem() instanceof LatexSpearItem)
                || (display != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    && display != ItemDisplayContext.THIRD_PERSON_LEFT_HAND)) return;
        HumanoidArm arm = display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        float progress = hand == InteractionHand.MAIN_HAND ? jabProgress(entity, partialTick) : -1;
        if (progress >= 0) thirdPersonAttackItem(pose, progress);
        else if (using(entity, hand, stack)) {
            float ticks = useTicks(entity, partialTick);
            pose.mulPose(Axis.XP.rotationDegrees(-50 * chargeRaise(ticks)));
        }
    }

    public static void thirdPersonHand(LivingEntity entity, ModelPart arm, ModelPart head,
                                       HumanoidArm side, float partialTick) {
        InteractionHand hand = side == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        float progress = hand == InteractionHand.MAIN_HAND ? jabProgress(entity, partialTick) : -1;
        int sign = side == HumanoidArm.RIGHT ? 1 : -1;
        float aimPitch = entity.isFallFlying() && !entity.isAutoSpinAttack()
                ? SpearFlightPose.aimPitch(entity.getViewXRot(partialTick), entity.getXRot(),
                    entity.getFallFlyingTicks() + partialTick) * Mth.DEG_TO_RAD
                : head.xRot;
        if (progress >= 0) {
            var sample = SpearJabAnimation.sample(progress);
            arm.xRot = Mth.lerp(sample.raise(), arm.xRot, aimPitch) + sample.armPitch() * Mth.DEG_TO_RAD;
            arm.yRot = Mth.lerp(sample.raise(), arm.yRot, head.yRot - sign * 0.1F);
            arm.zRot *= 1 - sample.raise();
        } else if (using(entity, hand, entity.getItemInHand(hand))) {
            float ticks = useTicks(entity, partialTick);
            float raise = chargeRaise(ticks);
            float tired = ramp(ticks, SpearCombatRules.WARMUP + SpearCombatRules.TIRED, 20);
            float lower = ramp(ticks, SpearCombatRules.WARMUP + SpearCombatRules.DISENGAGED, 20);
            arm.xRot = aimPitch + (-50 * raise + 20 * tired + 30 * lower) * Mth.DEG_TO_RAD;
            arm.yRot = head.yRot - sign * 0.1F;
            arm.zRot = sign * Mth.sin(ticks * 0.3F) * 0.015F * tired;
        }
    }

    private static boolean using(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        LivingEntity source = owner(entity);
        return stack.getItem() instanceof LatexSpearItem && source.isUsingItem()
                && source.getUseItem().getItem() instanceof LatexSpearItem
                && source.getUsedItemHand() == hand && source.getUseItem() == source.getItemInHand(hand);
    }

    private static float useTicks(LivingEntity entity, float partialTick) {
        return owner(entity).getTicksUsingItem() + partialTick;
    }
    private static float chargeRaise(float ticks) { return ramp(ticks, 0, SpearCombatRules.WARMUP); }
    private static float ramp(float ticks, float start, float duration) { return Mth.clamp((ticks - start) / duration, 0, 1); }

    private static void firstPersonUseItem(PoseStack pose, HumanoidArm arm, float ticks) {
        float raise = chargeRaise(ticks);
        int sign = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(-sign * 0.12F * raise, 0.05F * raise, -0.15F * raise);
        pose.mulPose(Axis.XP.rotationDegrees(-45 * raise));
    }

    private LatexSpearAnimations() { }
}
