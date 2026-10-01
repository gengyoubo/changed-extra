package github.com.gengyoubo.CE.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import github.com.gengyoubo.CE.events.LatexSpearAdvancements;
import github.com.gengyoubo.CE.init.CEItem;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.SpearJabAnimationPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/*TODOFIX
| Bug | 问题 | 对你 CE 矛的参考价值 |
|---|---|---|
| **MC-302677** | 攻击指示器没有正确考虑矛的攻击距离 | ★★ |
| **MC-302882** | Piglin 不会使用矛进行 Charge | NPC/AI 才相关 |
| **MC-302900** | 矛的 reach 覆盖玩家原本的实体交互距离 | **★★★★★** |
| **MC-303052** | 蜘蛛网/甜浆果丛会阻止玩家使用矛突进 | 实现不同，参考有限 |
| **MC-302895** | 副手拿矛时，使用主手物品也会播放蓄力动画 | ★★★ |
| **MC-304045** | 矛命中播放普通攻击音效 | ★★ |
| **MC-304407** | 目标在 3～4.5 格时 Charge 无法攻击 | **★★★★★** |
| **MC-304593** | 副手矛 Charge + 主手斧能错误禁用盾牌 | **★★★★** |
bug:本来应该是攻击完应该有一个冷却的，但是变成了瞄准冷却后才能攻击了
 */
public final class LatexSpearItem extends Item {
    private record HitContext(Player attacker, LivingEntity target) { }
    private static final ThreadLocal<HitContext> CURRENT_HIT = new ThreadLocal<>();
    private final Multimap<Attribute, AttributeModifier> attributes;
    private final Map<LivingEntity, Map<Integer, Integer>> chargeHits = new WeakHashMap<>();

    public LatexSpearItem() {
        super(new Properties().durability(SpearCombatRules.DURABILITY));
        attributes = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Spear damage",
                        SpearCombatRules.JAB_DAMAGE - 1, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Spear speed",
                        SpearCombatRules.ATTACK_SPEED - 4, AttributeModifier.Operation.ADDITION)).build();
    }

    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        // MC-302900: spear range must never change interaction reach.
        return slot == EquipmentSlot.MAINHAND ? attributes : ImmutableMultimap.of();
    }
    @Override public int getEnchantmentValue(ItemStack stack) { return 12; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack repair) { return repair.is(CEItem.LATEX_INGOT.get()); }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment != Enchantments.SWEEPING_EDGE
                && (enchantment.category == EnchantmentCategory.WEAPON || super.canApplyAtEnchantingTable(stack, enchantment));
    }
    @Override public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level,
                                           net.minecraft.core.BlockPos pos, Player player) { return false; }
    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        // Also protect the native attack packet path from bypassing the spear's minimum range/cooldown.
        jab(player);
        return true;
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.SPEAR; }
    @Override public int getUseDuration(ItemStack stack) { return SpearCombatRules.USE_DURATION; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        chargeHits.remove(player);
        player.startUsingItem(hand);
        LatexSpearAdvancements.beginCharge(player, stack);
        if (!level.isClientSide) player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.consume(stack);
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) { endCharge(entity); }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        endCharge(entity);
        return stack;
    }
    private void endCharge(LivingEntity entity) {
        chargeHits.remove(entity);
        LatexSpearAdvancements.endCharge(entity);
        if (entity instanceof Player player) player.getCooldowns().addCooldown(this, 20);
    }

    public void jab(Player player) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator() || player.isUsingItem()
                || !player.getMainHandItem().is(this) || player.getCooldowns().isOnCooldown(this)) return;
        ItemStack stack = player.getMainHandItem();
        if (player instanceof net.minecraft.server.level.ServerPlayer
                && !(player instanceof net.minecraftforge.common.util.FakePlayer)) {
            CENetwork.INSTANCE.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new SpearJabAnimationPacket(player.getId(), player.getUUID(), player.level().dimension().location()));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, player.getSoundSource(), 0.5F, 1.4F);
        for (LivingEntity target : targets(player)) {
            // The spear enforces a post-attack cooldown; swapping to it must not weaken its first jab.
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    + EnchantmentHelper.getDamageBonus(stack, target.getMobType());
            if (hit(player, target, stack, damage, InteractionHand.MAIN_HAND)) {
                target.knockback(0.35 + stack.getEnchantmentLevel(Enchantments.KNOCKBACK) * 0.5,
                        player.getX() - target.getX(), player.getZ() - target.getZ());
            }
            if (stack.isEmpty()) break;
        }
        // Native attack hooks run before resetting strength. Keep that ordering for compatibility.
        player.resetAttackStrengthTicker();
        player.getCooldowns().addCooldown(this, SpearCombatRules.JAB_COOLDOWN);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player) || !player.isAlive() || player.isSpectator()) return;
        int ticks = getUseDuration(stack) - remaining;
        if (ticks < SpearCombatRules.WARMUP) return;
        Vec3 look = player.getLookAngle(), motion = motion(player);
        double speed = Math.max(0, motion.dot(look) * 20);
        var hits = chargeHits.computeIfAbsent(player, ignored -> new HashMap<>());
        hits.entrySet().removeIf(entry -> ticks - entry.getValue() >= SpearCombatRules.TARGET_COOLDOWN);
        for (LivingEntity target : targets(player)) {
            if (hits.containsKey(target.getId())) continue;
            float damage = SpearCombatRules.chargeDamage(player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE),
                    Math.max(0, motion.subtract(motion(target)).dot(look) * 20), ticks);
            if (damage <= 0) continue;
            hits.put(target.getId(), ticks);
            InteractionHand hand = player.getUsedItemHand();
            damage += EnchantmentHelper.getDamageBonus(stack, target.getMobType());
            if (hand == InteractionHand.OFF_HAND) {
                float strength = player.getAttackStrengthScale(0.5F);
                damage *= 0.2F + strength * strength * 0.8F;
            }
            if (hit(player, target, stack, damage, hand)) {
                if (SpearCombatRules.canDismount(ticks, speed) && target.isPassenger()) target.stopRiding();
                if (SpearCombatRules.canKnockback(ticks, speed)) target.knockback(Math.min(1.2, speed * 0.08),
                        -look.x, -look.z);
            }
            if (stack.isEmpty()) { player.stopUsingItem(); break; }
        }
    }
    private static Vec3 motion(Entity entity) {
        return SpearMovement.motion(entity);
    }
    public static List<LivingEntity> targets(Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 start = eye.add(look.scale(SpearCombatRules.MIN_REACH));
        Vec3 end = eye.add(look.scale(player.isCreative() ? SpearCombatRules.CREATIVE_REACH : SpearCombatRules.MAX_REACH));
        HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        if (eye.distanceToSqr(end) <= SpearCombatRules.MIN_REACH * SpearCombatRules.MIN_REACH) return List.of();
        Vec3 tip = end;
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.25), target ->
                target != player && target.isAlive() && target.isPickable() && !target.isSpectator()
                        && !target.isAlliedTo(player) && !target.isPassengerOfSameVehicle(player)
                        && (!(target instanceof Player other) || player.canHarmPlayer(other))
                        && SpearTargeting.intersects(target.getBoundingBox(), start, tip));
    }
    private static boolean hit(Player player, LivingEntity target, ItemStack stack, float damage, InteractionHand hand) {
        if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target))) return false;
        HitContext previous = CURRENT_HIT.get();
        CURRENT_HIT.set(new HitContext(player, target));
        boolean wasAtFullHealth = target.getHealth() >= target.getMaxHealth();
        boolean damaged;
        try { damaged = target.hurt(player.damageSources().playerAttack(player), damage); }
        finally { if (previous == null) CURRENT_HIT.remove(); else CURRENT_HIT.set(previous); }
        if (!damaged) return false;
        // Count only the spear's direct damage, before fire or post-attack enchantment effects.
        LatexSpearAdvancements.onHit(player, target, stack, wasAtFullHealth);
        player.setLastHurtMob(target);
        EnchantmentHelper.doPostHurtEffects(target, player);
        stack.getAllEnchantments().forEach((enchantment, level) -> enchantment.doPostAttack(player, target, level));
        int fire = stack.getEnchantmentLevel(Enchantments.FIRE_ASPECT);
        if (fire > 0) target.setSecondsOnFire(fire * 4);
        stack.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(hand));
        player.level().playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, player.getSoundSource(), 0.7F, 1.2F);
        return true;
    }
    public static boolean resolvingHitOn(LivingEntity target) {
        HitContext context = CURRENT_HIT.get();
        return context != null && context.target == target;
    }
    public static boolean blocksMainHandShieldCheck(LivingEntity attacker, LivingEntity target) {
        HitContext context = CURRENT_HIT.get();
        return context != null && context.attacker == attacker && context.target == target;
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.changede.latex_spear.jab"));
        tooltip.add(Component.translatable("tooltip.changede.latex_spear.charge"));
    }
}
