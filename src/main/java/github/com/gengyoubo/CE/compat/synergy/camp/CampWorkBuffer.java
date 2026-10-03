package github.com.gengyoubo.CE.compat.synergy.camp;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

import java.util.*;

/** Entity-owned, paid work materials. This common class has no CS dependency. */
final class CampWorkBuffer {
    static final String ROOT = "changede_camp_work";
    private static final String ROD_LEASE = "changede_camp_rod_lease";
    private CampWorkBuffer() {}
    private static CompoundTag root(ChangedEntity mob) {
        if (!mob.getPersistentData().contains(ROOT, Tag.TAG_COMPOUND)) mob.getPersistentData().put(ROOT, new CompoundTag());
        CompoundTag root = mob.getPersistentData().getCompound(ROOT);
        if (!root.hasUUID("OwnerToken")) root.putUUID("OwnerToken", UUID.randomUUID());
        return root;
    }
    static boolean hasCargo(ChangedEntity mob) { return !mob.getPersistentData().getCompound(ROOT).getList("Cargo", Tag.TAG_COMPOUND).isEmpty(); }
    static List<ItemStack> takeCargo(ChangedEntity mob) {
        if (!hasCargo(mob)) return new ArrayList<>();
        ListTag saved = root(mob).getList("Cargo", Tag.TAG_COMPOUND);
        root(mob).remove("Cargo"); // Relinquish the serialized owner before handing out real items.
        List<ItemStack> items = new ArrayList<>();
        for (Tag entry : saved) { ItemStack stack = ItemStack.of((CompoundTag) entry); if (!stack.isEmpty()) items.add(stack); }
        return items;
    }
    static void addCargo(ChangedEntity mob, Collection<ItemStack> items) {
        List<ItemStack> all = takeCargo(mob);
        for (ItemStack incoming : items) {
            if (incoming.isEmpty()) continue;
            for (ItemStack present : all) if (ItemStack.isSameItemSameTags(present, incoming)) {
                int count = Math.min(incoming.getCount(), present.getMaxStackSize() - present.getCount());
                present.grow(count); incoming.shrink(count); if (incoming.isEmpty()) break;
            }
            if (!incoming.isEmpty()) all.add(incoming);
        }
        ListTag saved = new ListTag(); all.forEach(stack -> saved.add(stack.save(new CompoundTag()))); root(mob).put("Cargo", saved);
    }
    static int countCargo(ChangedEntity mob, ItemStack prototype) {
        int count = 0;
        for (Tag entry : mob.getPersistentData().getCompound(ROOT).getList("Cargo", Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.of((CompoundTag) entry);
            if (ItemStack.isSameItemSameTags(stack, prototype)) count += stack.getCount();
        }
        return count;
    }
    static CompoundTag kitchen(ChangedEntity mob) { return mob.getPersistentData().getCompound(ROOT).getCompound("Kitchen"); }
    static void kitchen(ChangedEntity mob, CompoundTag state) { root(mob).put("Kitchen", state); }
    static NonNullList<ItemStack> ingredients(CompoundTag state) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(state, items); return items;
    }
    static void clearKitchen(ChangedEntity mob) { root(mob).remove("Kitchen"); }
    static void cancelKitchen(ChangedEntity mob) {
        CompoundTag state = kitchen(mob);
        if (state.isEmpty()) return;
        clearKitchen(mob);
        List<ItemStack> items = new ArrayList<>(ingredients(state));
        ItemStack fuel = ItemStack.of(state.getCompound("Fuel")); if (!fuel.isEmpty()) items.add(fuel);
        addCargo(mob, items);
    }
    static InteractionHand rodHand(ChangedEntity mob, Container inventory) {
        if (mob.getMainHandItem().getItem() instanceof FishingRodItem) return InteractionHand.MAIN_HAND;
        if (mob.getOffhandItem().getItem() instanceof FishingRodItem) return InteractionHand.OFF_HAND;
        for (int slot = 0; slot < Math.min(24, inventory.getContainerSize()); slot++) {
            if (!(inventory.getItem(slot).getItem() instanceof FishingRodItem)) continue;
            ItemStack rod = inventory.removeItem(slot, 1); if (rod.isEmpty()) continue;
            UUID token = UUID.randomUUID(); CompoundTag lease = new CompoundTag();
            lease.putUUID("Token", token); lease.putInt("Slot", slot);
            lease.put("Original", mob.getMainHandItem().save(new CompoundTag()));
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            root(mob).put("Lease", lease); rod.getOrCreateTag().putUUID(ROD_LEASE, token);
            mob.setItemSlot(EquipmentSlot.MAINHAND, rod); inventory.setChanged(); return InteractionHand.MAIN_HAND;
        }
        return null;
    }
    static void restoreTool(ChangedEntity mob, Container inventory) {
        CompoundTag lease = mob.getPersistentData().getCompound(ROOT).getCompound("Lease");
        if (lease.isEmpty()) return;
        root(mob).remove("Lease");
        ItemStack original = ItemStack.of(lease.getCompound("Original"));
        ItemStack held = mob.getMainHandItem();
        if (ownsRod(held, lease)) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            held.getTag().remove(ROD_LEASE); if (held.getTag().isEmpty()) held.setTag(null);
            returnStorage(mob, inventory, held, lease.getInt("Slot"));
        }
        if (mob.getMainHandItem().isEmpty()) mob.setItemSlot(EquipmentSlot.MAINHAND, original);
        else returnStorage(mob, inventory, original, -1);
    }
    private static boolean ownsRod(ItemStack held, CompoundTag lease) {
        return lease.hasUUID("Token") && held.hasTag() && held.getTag().hasUUID(ROD_LEASE)
                && held.getTag().getUUID(ROD_LEASE).equals(lease.getUUID("Token"));
    }
    private static void returnStorage(ChangedEntity mob, Container inventory, ItemStack stack, int preferred) {
        if (stack.isEmpty()) return;
        if (inventory != null) {
            int size = Math.min(24, inventory.getContainerSize());
            if (preferred >= 0 && preferred < size && inventory.getItem(preferred).isEmpty()) {
                inventory.setItem(preferred, stack); inventory.setChanged(); return;
            }
            for (int i = 0; i < size; i++) if (inventory.getItem(i).isEmpty()) { inventory.setItem(i, stack); inventory.setChanged(); return; }
        }
        if (mob.spawnAtLocation(stack) == null) addCargo(mob, List.of(stack));
    }
    static void transfer(ChangedEntity previous, ChangedEntity replacement) {
        if (previous == replacement || !previous.getPersistentData().contains(ROOT, Tag.TAG_COMPOUND)) return;
        CompoundTag source = previous.getPersistentData().getCompound(ROOT);
        CompoundTag destination = replacement.getPersistentData().getCompound(ROOT);
        boolean cloned = source.hasUUID("OwnerToken") && destination.hasUUID("OwnerToken")
                && source.getUUID("OwnerToken").equals(destination.getUUID("OwnerToken"));
        if (cloned || !source.hasUUID("OwnerToken") && source.equals(destination)) replacement.getPersistentData().remove(ROOT);
        cancelKitchen(previous);
        List<ItemStack> incoming = takeCargo(previous);
        CompoundTag lease = root(previous).getCompound("Lease");
        if (!lease.isEmpty()) {
            ItemStack rod = previous.getMainHandItem();
            if (ownsRod(rod, lease)) {
                previous.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                if (replacement.getMainHandItem().isEmpty()) replacement.setItemSlot(EquipmentSlot.MAINHAND, rod);
                else if (!ownsRod(replacement.getMainHandItem(), lease)) {
                    rod.getTag().remove(ROD_LEASE); incoming.add(rod);
                }
            }
            if (ownsRod(replacement.getMainHandItem(), lease) && root(replacement).getCompound("Lease").isEmpty()) root(replacement).put("Lease", lease.copy());
            else { ItemStack original = ItemStack.of(lease.getCompound("Original")); if (!original.isEmpty()) incoming.add(original); }
        }
        previous.getPersistentData().remove(ROOT); addCargo(replacement, incoming);
    }
}
