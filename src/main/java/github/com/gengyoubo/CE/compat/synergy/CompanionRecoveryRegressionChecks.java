package github.com.gengyoubo.CE.compat.synergy;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.CE.changede;
import github.com.gengyoubo.CE.init.CEItem;
import net.ltxprogrammer.changed.entity.beast.AbstractDarkLatexEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ai.RelationshipFavorService;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventoryMenu;

import java.util.UUID;

/** Explicitly enabled checks in a disposable server, using actual CS/native containers. */
public final class CompanionRecoveryRegressionChecks {
    private CompanionRecoveryRegressionChecks() {}

    public static void verify(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "RecoveryTest"));
        AbstractDarkLatexEntity creature = create(level);
        AbstractDarkLatexEntity wild = create(level);
        var hostile = EntityType.ZOMBIE.create(level);
        boolean natural = level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        try {
            creature.setOwnerUUID(player.getUUID());
            creature.setTame(true);
            var nativeInventory = creature.getInventory();
            check(CreatureInventoryAccess.resolve(creature) == nativeInventory, "Uses native inventory without creating CS storage");
            nativeInventory.setItem(2, new ItemStack(Items.SWEET_BERRIES, 3));
            creature.setHealth(creature.getMaxHealth() * 0.5F);
            float health = creature.getHealth();
            ChangedSynergyAutoEat.tickCompanion(creature, 1000);
            check(creature.getHealth() > health && nativeInventory.getItem(2).getCount() == 2, "Native food heals and consumes exactly one");
            ChangedSynergyAutoEat.tickCompanion(creature, 1001);
            check(nativeInventory.getItem(2).getCount() == 2, "Eating respects its cooldown");
            check(!creature.getPersistentData().contains("ChangedSynergyBondedInventory"), "Native eating does not switch the visible backpack");

            nativeInventory.clearContent();
            var golden = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("changed_addon", "golden_orange"));
            check(golden != null, "Linked golden orange is registered");
            nativeInventory.setItem(1, new ItemStack(golden, 2));
            creature.setHealth(creature.getMaxHealth() * 0.2F);
            ChangedSynergyAutoEat.tickCompanion(creature, 1040);
            check(nativeInventory.getItem(1).getCount() == 1 && creature.hasEffect(MobEffects.REGENERATION), "Linked golden orange grants recovery effects");
            creature.removeAllEffects();
            nativeInventory.clearContent();
            var diet = ForgeRegistries.ITEMS.getValues().stream()
                    .filter(item -> RelationshipFavorService.isDedicatedDietFood(creature, new ItemStack(item)))
                    .findFirst().orElseThrow(() -> new AssertionError("No wolf diet food was registered"));
            nativeInventory.setItem(3, new ItemStack(diet, 2));
            creature.setHealth(creature.getMaxHealth() * 0.5F);
            health = creature.getHealth();
            ChangedSynergyAutoEat.tickCompanion(creature, 1080);
            check(nativeInventory.getItem(3).getCount() == 1 && creature.getHealth() == Math.min(creature.getMaxHealth(), health + 6), "CS diet tags retain their six-point healing");
            nativeInventory.clearContent();
            nativeInventory.setItem(1, new ItemStack(CEItem.ENCHANTED_GOLDEN_ORANGE.get(), 2));
            creature.setHealth(creature.getMaxHealth() * 0.05F);
            ChangedSynergyAutoEat.tickCompanion(creature, 1120);
            check(nativeInventory.getItem(1).getCount() == 1 && creature.hasEffect(MobEffects.FIRE_RESISTANCE), "Enchanted golden orange retains emergency benefits");
            creature.removeAllEffects();
            nativeInventory.clearContent();

            var storage = new BondedCreatureInventory(creature);
            storage.setItem(0, new ItemStack(Items.SWEET_BERRIES, 3));
            level.players().add(player);
            player.containerMenu = new BondedCreatureInventoryMenu(1, player.getInventory(), creature, storage);
            check(CreatureInventoryAccess.resolve(creature) == storage, "Reuses the open screen's live CS container");
            creature.setHealth(creature.getMaxHealth() * 0.5F);
            ChangedSynergyAutoEat.tickCompanion(creature, 1160);
            check(player.containerMenu.getSlot(41).getItem().getCount() == 2, "Open menu sees consumed food immediately");
            storage.setItem(4, new ItemStack(Items.DIAMOND));
            check(new BondedCreatureInventory(creature).getItem(0).getCount() == 2, "Later GUI edits cannot resurrect consumed food");
            var remainder = CreatureInventoryAccess.insertStorage(CreatureInventoryAccess.resolve(creature), new ItemStack(Items.SWEET_BERRIES, 2));
            check(remainder.isEmpty() && storage.getItem(0).getCount() == 4, "Work-task insertion shares the live inventory");
            player.containerMenu = player.inventoryMenu;
            level.players().remove(player);
            storage.clearContent();
            creature.getPersistentData().remove("ChangedSynergyBondedInventory");
            creature.getPersistentData().remove("changede_next_companion_recovery");
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(true, event.getServer());
            creature.setHealth(creature.getMaxHealth() - 4);
            health = creature.getHealth();
            ChangedSynergyAutoEat.tickCompanion(creature, 1200);
            ChangedSynergyAutoEat.tickCompanion(creature, 1299);
            check(creature.getHealth() == health, "Natural recovery waits five seconds");
            ChangedSynergyAutoEat.tickCompanion(creature, 1300);
            check(creature.getHealth() == health + 1, "Idle companions recover one health point");
            creature.setTarget(hostile);
            check(hostile != null && creature.getTarget() == hostile, "CS accepts a hostile combat target");
            ChangedSynergyAutoEat.tickCompanion(creature, 1400);
            check(creature.getHealth() == health + 1, "Combat prevents passive recovery");
            creature.setTarget(null);
            ChangedSynergyAutoEat.tickCompanion(creature, 1499);
            check(creature.getHealth() == health + 1, "Recovery waits after combat ends");
            ChangedSynergyAutoEat.tickCompanion(creature, 1500);
            check(creature.getHealth() == health + 2, "Recovery resumes after combat");
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, event.getServer());
            ChangedSynergyAutoEat.tickCompanion(creature, 1600);
            check(creature.getHealth() == health + 2, "Natural regeneration game rule is respected");
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(true, event.getServer());
            long now = level.getGameTime();
            MinecraftForge.EVENT_BUS.post(new LivingDamageEvent(creature, creature.damageSources().generic(), 2));
            ChangedSynergyAutoEat.tickCompanion(creature, now + 99);
            check(creature.getHealth() == health + 2, "Damage events restart the recovery delay");
            creature.setHealth(creature.getMaxHealth());
            nativeInventory.setItem(1, new ItemStack(Items.SWEET_BERRIES, 2));
            ChangedSynergyAutoEat.tickCompanion(creature, 1800);
            check(nativeInventory.getItem(1).getCount() == 2, "Full-health creatures do not waste food");
            wild.setHealth(wild.getMaxHealth() - 5);
            health = wild.getHealth();
            ChangedSynergyAutoEat.tickCompanion(wild, 1800);
            ChangedSynergyAutoEat.tickCompanion(wild, 2000);
            check(wild.getHealth() == health, "Wild creatures receive no companion recovery");
            changede.LOGGER.info("COMPANION RECOVERY REGRESSION CHECKS PASSED");
        } finally {
            player.containerMenu = player.inventoryMenu;
            level.players().remove(player);
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(natural, event.getServer());
            creature.discard();
            wild.discard();
            if (hostile != null) hostile.discard();
            event.getServer().halt(false);
        }
    }

    private static AbstractDarkLatexEntity create(ServerLevel level) {
        AbstractDarkLatexEntity creature = ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        if (creature == null) throw new AssertionError("Could not create dark latex wolf");
        return creature;
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        changede.LOGGER.info("Companion recovery PASS: {}", message);
    }
}
