package github.com.gengyoubo.LP.procedures;

import net.ltxprogrammer.changed.init.ChangedItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Supplier;

public class CloseTextures {
    public static boolean execute(Player player) {
        if(player==null) {
            return false;
        }
        Item item = getSlot0Item(player).getItem();

        return item != ChangedItems.WHITE_LATEX_GOO.get()
                && item != ChangedItems.DARK_LATEX_GOO.get();
    }

    private static ItemStack getSlot0Item(Player player) {
        if (player.containerMenu instanceof Supplier<?> supplier) {
            Object value = supplier.get();
            if (value instanceof Map<?, ?> slots) {
                Object slot = slots.get(0);
                if (slot instanceof Slot realSlot) {
                    return realSlot.getItem();
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
