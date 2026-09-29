package github.com.gengyoubo.CE.compat.synergy;

import github.com.gengyoubo.CE.init.CEItem;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;
import net.parkabird.changedsynergy.ai.RelationshipFavorService;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes bonded Synergy latex creatures eat from their own inventory to stay alive.
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
    private static final Map<UUID, Long> LAST_EAT = new ConcurrentHashMap<>();

    private ChangedSynergyAutoEat() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                LivingEvent.LivingTickEvent.class, ChangedSynergyAutoEat::onLivingTick);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity creature)
                || creature.level().isClientSide()
                || !(creature.level() instanceof ServerLevel)) {
            return;
        }
        if (!creature.isAlive() || creature.isRemoved()) {
            LAST_EAT.remove(creature.getUUID());
            return;
        }
        if (!LatexSocialMemory.hasActiveBond(creature)) {
            LAST_EAT.remove(creature.getUUID());
            return;
        }

        long now = creature.level().getGameTime();
        Long lastEat = LAST_EAT.get(creature.getUUID());
        if (lastEat != null && now - lastEat < EAT_COOLDOWN_TICKS) {
            return;
        }

        BondedCreatureInventory inventory = new BondedCreatureInventory(creature);
        if (tryAutoEat(creature, inventory, now)) {
            LAST_EAT.put(creature.getUUID(), now);
        }
    }

    private static boolean tryAutoEat(ChangedEntity creature, BondedCreatureInventory inventory, long now) {
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
                eat(creature, inventory, slot, candidate);
                return true;
            }
        }
        return false;
    }

    private static int findFoodSlot(ChangedEntity creature, BondedCreatureInventory inventory, FoodTier tier) {
        for (int slot = 0; slot < 24; slot++) {
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
        return null;
    }

    private static void eat(ChangedEntity creature, BondedCreatureInventory inventory, int slot, FoodTier tier) {
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }

        switch (tier) {
            case ORDINARY -> creature.heal(2.0F);
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
        inventory.consumeOne(slot);
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
