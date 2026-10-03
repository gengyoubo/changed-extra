package github.com.gengyoubo.CE.compat.synergy;

import github.com.gengyoubo.CE.init.CEItem;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;
import net.parkabird.changedsynergy.ai.RelationshipFavorService;

/**
 * Makes bonded Synergy companions eat from their visible inventory and recover out of combat.
 *
 * <p>Food tiers (from {@code RelationshipFavorService} in Changed: Synergy):
 * <ul>
 *   <li>sweet berries - ordinary, heal 2, eaten below 90% HP</li>
 *   <li>dedicated diet food - heal 6, eaten below 75% HP (priority)</li>
 *   <li>golden orange - heal 4 + regen/absorption, eaten below 25% HP or in combat</li>
 *   <li>enchanted golden orange - heal 8 + strong buffs, eaten below 10% HP (dying)</li>
 * </ul>
 */
public final class ChangedSynergyAutoEat {
    private static final long EAT_COOLDOWN_TICKS = 20L;
    private static final long RECOVERY_INTERVAL_TICKS = 100L;
    private static final String NEXT_EAT = "changede_next_auto_eat";
    private static final String NEXT_RECOVERY = "changede_next_companion_recovery";

    private ChangedSynergyAutoEat() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                LivingEvent.LivingTickEvent.class, ChangedSynergyAutoEat::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                LivingDamageEvent.class, ChangedSynergyAutoEat::onDamage);
        if (Boolean.getBoolean("changede.verifyCompanionRecovery")) {
            MinecraftForge.EVENT_BUS.addListener(CompanionRecoveryRegressionChecks::verify);
        }
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity creature)
                || creature.level().isClientSide()
                || !(creature.level() instanceof ServerLevel)) {
            return;
        }
        tickCompanion(creature, creature.level().getGameTime());
    }

    private static void onDamage(LivingDamageEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ChangedEntity creature
                && !creature.level().isClientSide()) {
            creature.getPersistentData().putLong(NEXT_RECOVERY,
                    creature.level().getGameTime() + RECOVERY_INTERVAL_TICKS);
        }
    }

    static void tickCompanion(ChangedEntity creature, long now) {
        if (!creature.isAlive() || creature.isRemoved() || creature.level().isClientSide()
                || !(LatexSocialMemory.hasActiveBond(creature)
                || LatexSocialMemory.petOwnerUuid(creature).isPresent())) return;
        var data = creature.getPersistentData();
        if (now >= data.getLong(NEXT_EAT) && creature.getHealth() < creature.getMaxHealth()) {
            data.putLong(NEXT_EAT, now + EAT_COOLDOWN_TICKS);
            tryAutoEat(creature, CreatureInventoryAccess.resolve(creature));
        }
        if (creature.getHealth() >= creature.getMaxHealth() || creature.getTarget() != null
                || creature.hurtTime > 0
                || !creature.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
            data.putLong(NEXT_RECOVERY, now + RECOVERY_INTERVAL_TICKS);
            return;
        }
        if (!data.contains(NEXT_RECOVERY)) {
            data.putLong(NEXT_RECOVERY, now + RECOVERY_INTERVAL_TICKS);
        } else if (now >= data.getLong(NEXT_RECOVERY)) {
            creature.heal(1.0F);
            data.putLong(NEXT_RECOVERY, now + RECOVERY_INTERVAL_TICKS);
        }
    }

    private static boolean tryAutoEat(ChangedEntity creature, Container inventory) {
        float maxHealth = creature.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return false;
        }
        float ratio = creature.getHealth() / maxHealth;
        boolean combat = creature.getTarget() != null;

        FoodTier tier;
        if (ratio < 0.10F) {
            tier = FoodTier.ENCHANTED;
        } else if (ratio < 0.25F || combat) {
            tier = FoodTier.GOLDEN;
        } else if (ratio < 0.75F) {
            tier = FoodTier.DIET;
        } else if (ratio < 0.90F) {
            tier = FoodTier.ORDINARY;
        } else {
            return false;
        }

        for (FoodTier candidate : tier.fallbacks()) {
            int slot = findFoodSlot(creature, inventory, candidate);
            if (slot >= 0) {
                return eat(creature, inventory, slot, candidate);
            }
        }
        return false;
    }

    private static int findFoodSlot(ChangedEntity creature, Container inventory, FoodTier tier) {
        for (int slot = 0; slot < Math.min(24, inventory.getContainerSize()); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && classify(creature, stack) == tier) {
                return slot;
            }
        }
        return -1;
    }

    private static FoodTier classify(ChangedEntity creature, ItemStack stack) {
        if (stack.is(CEItem.ENCHANTED_GOLDEN_ORANGE.get())) {
            return FoodTier.ENCHANTED;
        }
        if (RelationshipFavorService.isGoldenOrange(stack)) {
            return FoodTier.GOLDEN;
        }
        if (RelationshipFavorService.isDedicatedDietFood(creature, stack)) {
            return FoodTier.DIET;
        }
        if (stack.is(Items.SWEET_BERRIES)) {
            return FoodTier.ORDINARY;
        }
        if (RelationshipFavorService.isOrange(stack) && RelationshipFavorService.acceptsOrange(creature)) {
            return FoodTier.ORDINARY;
        }
        return null;
    }

    private static boolean eat(ChangedEntity creature, Container inventory, int slot, FoodTier tier) {
        ItemStack stack = inventory.removeItem(slot, 1);
        if (stack.isEmpty()) {
            return false;
        }
        inventory.setChanged();

        switch (tier) {
            case ORDINARY -> creature.heal(RelationshipFavorService.isOrange(stack) ? 4.0F : 2.0F);
            case DIET -> creature.heal(6.0F);
            case GOLDEN -> {
                creature.heal(4.0F);
                RelationshipFavorService.applyGoldenOrangeBenefits(creature, stack);
            }
            case ENCHANTED -> {
                creature.heal(8.0F);
                creature.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                creature.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
                creature.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0));
                creature.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
                creature.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
            }
        }
        return true;
    }

    private enum FoodTier {
        ORDINARY,
        DIET,
        GOLDEN,
        ENCHANTED;

        FoodTier[] fallbacks() {
            return switch (this) {
                case ENCHANTED -> new FoodTier[]{ENCHANTED, GOLDEN, DIET, ORDINARY};
                case GOLDEN -> new FoodTier[]{GOLDEN, DIET, ORDINARY};
                case DIET -> new FoodTier[]{DIET, ORDINARY};
                case ORDINARY -> new FoodTier[]{ORDINARY};
            };
        }
    }
}
