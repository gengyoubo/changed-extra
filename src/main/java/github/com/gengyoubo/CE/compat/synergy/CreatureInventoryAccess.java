package github.com.gengyoubo.CE.compat.synergy;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventoryMenu;

/** Uses the same storage as Synergy's screen, including its live per-open view. */
public final class CreatureInventoryAccess {
    private CreatureInventoryAccess() {}

    public static Container resolve(ChangedEntity creature) {
        if (creature.level() instanceof ServerLevel level) {
            for (var player : level.players()) {
                if (player.containerMenu instanceof BondedCreatureInventoryMenu menu
                        && menu.getPet() == creature) {
                    // Storage starts at menu slot 41; equipment/player slots precede it.
                    return menu.getSlot(41).container;
                }
            }
        }
        if (creature.getPersistentData().contains("ChangedSynergyBondedInventory")) {
            return new BondedCreatureInventory(creature);
        }
        Container nativeInventory = CreatureInventoryBackup.nativeInventory(creature);
        return nativeInventory != null ? nativeInventory : new BondedCreatureInventory(creature);
    }

    public static ItemStack insertStorage(Container inventory, ItemStack incoming) {
        ItemStack remainder = incoming.copy();
        int size = Math.min(24, inventory.getContainerSize());
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < size && !remainder.isEmpty(); slot++) {
                ItemStack present = inventory.getItem(slot);
                if (pass == 0 && !present.isEmpty() && ItemStack.isSameItemSameTags(present, remainder)) {
                    int moved = Math.min(remainder.getCount(),
                            Math.min(inventory.getMaxStackSize(), present.getMaxStackSize()) - present.getCount());
                    if (moved > 0) {
                        present.grow(moved);
                        remainder.shrink(moved);
                        inventory.setChanged();
                    }
                } else if (pass == 1 && present.isEmpty()) {
                    int moved = Math.min(remainder.getCount(),
                            Math.min(inventory.getMaxStackSize(), remainder.getMaxStackSize()));
                    ItemStack inserted = remainder.copy();
                    inserted.setCount(moved);
                    inventory.setItem(slot, inserted);
                    remainder.shrink(moved);
                }
            }
        }
        return remainder;
    }
}
