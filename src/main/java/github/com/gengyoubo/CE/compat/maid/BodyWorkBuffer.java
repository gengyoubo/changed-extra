package github.com.gengyoubo.CE.compat.maid;

import github.com.gengyoubo.CE.compat.synergy.CreatureInventoryAccess;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.BooleanSupplier;

/** TLM's temporary hand/task slots belong to the body, including across chunk unloads. */
final class BodyWorkBuffer extends ItemStackHandler {
    private static final String DATA_KEY = "changede_maid_work_buffers";
    private final ChangedEntity body;
    private final String name;
    private final BooleanSupplier active;

    BodyWorkBuffer(ChangedEntity body, String name, int size, BooleanSupplier active) {
        super(size);
        this.body = body;
        this.name = name;
        this.active = active;
    }

    @Override public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return active.getAsBoolean() ? ItemStack.of(body.getPersistentData().getCompound(DATA_KEY)
                .getCompound(name).getCompound(Integer.toString(slot))) : ItemStack.EMPTY;
    }

    @Override public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        if (!active.getAsBoolean()) return;
        CompoundTag buffers = body.getPersistentData().getCompound(DATA_KEY);
        CompoundTag slots = buffers.getCompound(name);
        if (stack.isEmpty()) slots.remove(Integer.toString(slot));
        else slots.put(Integer.toString(slot), stack.save(new CompoundTag()));
        if (slots.isEmpty()) buffers.remove(name);
        else buffers.put(name, slots);
        if (buffers.isEmpty()) body.getPersistentData().remove(DATA_KEY);
        else body.getPersistentData().put(DATA_KEY, buffers);
    }

    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if (!active.getAsBoolean() || stack.isEmpty()) return stack;
        ItemStack present = getStackInSlot(slot);
        if (!present.isEmpty() && !ItemStack.isSameItemSameTags(present, stack)) return stack;
        int moved = Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()) - present.getCount());
        if (moved <= 0) return stack;
        if (!simulate) setStackInSlot(slot, stack.copyWithCount(present.getCount() + moved));
        return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (!active.getAsBoolean() || amount <= 0) return ItemStack.EMPTY;
        ItemStack present = getStackInSlot(slot);
        int extracted = Math.min(amount, present.getCount());
        if (!simulate) setStackInSlot(slot, present.copyWithCount(present.getCount() - extracted));
        return present.copyWithCount(extracted);
    }

    /** Move ownership before retiring the old body's worker during a species replacement. */
    static void transfer(ChangedEntity previous, ChangedEntity replacement) {
        CompoundTag buffers = previous.getPersistentData().getCompound(DATA_KEY);
        if (buffers.isEmpty()) return;
        CompoundTag existing = replacement.getPersistentData().getCompound(DATA_KEY);
        if (!existing.isEmpty() && !existing.equals(buffers)) release(replacement);
        replacement.getPersistentData().put(DATA_KEY, buffers.copy());
        previous.getPersistentData().remove(DATA_KEY);
    }

    /** Return interrupted work items once. Unloaded bodies keep their own NBT until reloaded. */
    static void release(ChangedEntity body) {
        if (body == null || body.isRemoved()) return;
        CompoundTag buffers = body.getPersistentData().getCompound(DATA_KEY);
        if (buffers.isEmpty()) return;
        body.getPersistentData().remove(DATA_KEY);
        var inventory = body.isAlive() ? CreatureInventoryAccess.resolve(body) : null;
        for (String name : buffers.getAllKeys()) {
            CompoundTag slots = buffers.getCompound(name);
            for (String slot : slots.getAllKeys()) {
                ItemStack stack = ItemStack.of(slots.getCompound(slot));
                if (stack.isEmpty()) continue;
                ItemStack remainder = inventory == null ? stack : CreatureInventoryAccess.insertStorage(inventory, stack);
                if (!remainder.isEmpty()) body.spawnAtLocation(remainder);
            }
        }
    }
}
