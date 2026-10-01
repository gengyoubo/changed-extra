package github.com.gengyoubo.CE.compat.synergy;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.entity.beast.AbstractDarkLatexEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;

import java.util.UUID;

/** Runs only in an explicitly enabled, disposable server, using real native inventories and mixins. */
public final class InventoryBackupRegressionChecks {
    private InventoryBackupRegressionChecks() {}

    public static void verify(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "InventoryTest"));
        AbstractDarkLatexEntity creature = create(level);
        try {
            check(creature.getInventory() == null, "Untamed creature has no native inventory");
            creature.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            creature.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            var storage = new net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory(creature);
            storage.setItem(4, new ItemStack(Items.DIAMOND, 7));
            LatexSocialMemory.promoteNativePet(creature, player);
            check(creature.getInventory() != null, "Actual CS promotion initializes native inventory");
            check(creature.getMainHandItem().is(Items.IRON_SWORD), "CS promotion preserves the main hand");
            check(creature.getOffhandItem().is(Items.SHIELD), "CS promotion preserves the offhand");
            check(new net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory(creature)
                    .getItem(4).getCount() == 7, "CS storage survives native promotion independently");

            creature.getInventory().setItem(3, new ItemStack(Items.GOLD_INGOT, 11));
            creature.getInventory().selected = 3;
            creature.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            CompoundTag snapshot = CreatureInventoryBackup.capture(creature);
            CreatureInventoryBackup.checkpoint(creature, "test_snapshot");
            CompoundTag expectedHistory = creature.getPersistentData().getCompound("changede_inventory_backup").copy();
            // Wipe real storage and equipment, then verify empty states cannot erase recovery history.
            creature.getInventory().clearContent();
            new net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory(creature).clearContent();
            for (int i = 0; i < 10; i++) CreatureInventoryBackup.checkpoint(creature, "test_empty");
            check(creature.getPersistentData().getCompound("changede_inventory_backup").getList("History", Tag.TAG_COMPOUND)
                    .equals(expectedHistory.getList("History", Tag.TAG_COMPOUND)), "Empty snapshots retain nonempty history");
            creature.getInventory().selected = 0;
            check(CreatureInventoryBackup.restore(creature, snapshot) > 0, "Empty native and CS slots can be recovered");
            check(creature.getInventory().getItem(3).getCount() == 11 && creature.getInventory().selected == 3,
                    "Selected native slot and exact stack count are restored");
            check(creature.getOffhandItem().is(Items.SHIELD), "Native offhand is restored");
            check(creature.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "Armor is restored");
            check(new net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory(creature)
                    .getItem(4).getCount() == 7, "CS stack count is restored");
            check(CreatureInventoryBackup.restore(creature, snapshot) == 0, "Reapplying a snapshot cannot duplicate filled slots");

            // A conflicting occupied slot must not turn a copied main hand into another stored stack.
            creature.getInventory().setItem(3, new ItemStack(Items.EMERALD, 2));
            creature.getInventory().selected = 2;
            check(CreatureInventoryBackup.restore(creature, snapshot) == 0, "Recovery preserves conflicting occupied slots");
            check(creature.getInventory().getItem(3).is(Items.EMERALD) && creature.getInventory().getItem(2).isEmpty(),
                    "Recovery does not duplicate a native main hand into a different selected slot");

            AbstractDarkLatexEntity reloaded = create(level);
            reloaded.load(creature.saveWithoutId(new CompoundTag()));
            check(reloaded.getPersistentData().getCompound("changede_inventory_backup")
                    .equals(creature.getPersistentData().getCompound("changede_inventory_backup")), "Backups survive entity save/load");
            reloaded.setUUID(UUID.randomUUID());
            net.parkabird.changedsynergy.ai.CreatureMorphContinuity.transferForced(creature, reloaded);
            check(!reloaded.getPersistentData().getCompound("changede_inventory_backup").getList("History", Tag.TAG_COMPOUND).isEmpty(),
                    "Morph continuity carries recovery history");
            creature.getPersistentData().getCompound("changede_inventory_backup").getList("History", Tag.TAG_COMPOUND).clear();
            check(!reloaded.getPersistentData().getCompound("changede_inventory_backup").getList("History", Tag.TAG_COMPOUND).isEmpty(),
                    "Transferred history does not share mutable NBT with the old body");
            reloaded.discard();
            for (int i = 1; i <= 8; i++) {
                creature.getInventory().setItem(10, new ItemStack(Items.COPPER_INGOT, i));
                CreatureInventoryBackup.checkpoint(creature, "test_history_limit");
            }
            check(creature.getPersistentData().getCompound("changede_inventory_backup").getList("History", Tag.TAG_COMPOUND).size() == 5,
                    "Recovery history is bounded at five records");
            changede.LOGGER.info("INVENTORY BACKUP REGRESSION CHECKS PASSED");
        } finally {
            creature.discard();
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
        changede.LOGGER.info("Inventory backup PASS: {}", message);
    }
}
