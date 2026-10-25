package github.com.gengyoubo.CE.compat.curios;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.ltxprogrammer.changed.data.AccessorySlotType;
import net.ltxprogrammer.changed.data.AccessorySlots;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.util.EntityUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** A transient Changed API view. Item ownership and persistence remain entirely in Curios. */
public final class CuriosAccessorySlots extends AccessorySlots {
    private final ICuriosItemHandler curios;

    public static Optional<AccessorySlots> forEntity(LivingEntity entity, boolean rendering) {
        if (!(EntityUtil.maybeGetUnderlying(entity) instanceof Player player)) return Optional.empty();
        return CuriosApi.getCuriosInventory(player).resolve()
                .map(curios -> new CuriosAccessorySlots(player, curios, rendering));
    }

    private CuriosAccessorySlots(Player player, ICuriosItemHandler curios, boolean rendering) {
        super(player);
        this.curios = curios;
        super.initialize(type -> hasCuriosType(type) || find(type, rendering) != null, ignored -> {});
        for (var type : getOrderedSlots()) {
            var equipped = find(type, rendering);
            super.setItem(type, equipped == null ? ItemStack.EMPTY : equipped.stack());
        }
    }

    private boolean hasCuriosType(AccessorySlotType type) {
        return ChangedCuriosSlots.aliases(type).stream().anyMatch(id -> {
            var handler = curios.getCurios().get(id);
            return handler != null && handler.getStacks().getSlots() > 0;
        });
    }

    private Binding find(AccessorySlotType type, boolean rendering) {
        for (var entry : handlersFor(type)) {
            var handler = entry.getValue();
            var stacks = handler.getStacks();
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (rendering && handler.canToggleRendering() && !handler.getRenders().get(index)) continue;
                IDynamicStackHandler source = stacks;
                if (rendering && handler.hasCosmetic() && !handler.getCosmeticStacks().getStackInSlot(index).isEmpty()) {
                    source = handler.getCosmeticStacks();
                }
                ItemStack stack = source.getStackInSlot(index);
                if (!stack.isEmpty() && stack.is(type.getItemTag())) return new Binding(source, index);
            }
        }
        return null;
    }

    @Override public boolean isItemAllowedWithOthers(AccessorySlotType type, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!stack.is(type.getItemTag())) return false;
        for (var entry : handlersFor(type)) {
            var stacks = entry.getValue().getStacks();
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (stacks.getStackInSlot(index).isEmpty() && stacks.isItemValid(index, stack)) return true;
            }
        }
        return false;
    }

    @Override public boolean moveToSlot(AccessorySlotType type, ItemStack stack) {
        if (type == null || stack.isEmpty() || !stack.is(type.getItemTag())) return false;
        // ItemStackHandler may keep the supplied instance on a full insertion. Keep
        // the caller's stack separate before consuming it, or the equipped copy empties too.
        ItemStack remaining = stack.copy();
        for (var entry : handlersFor(type)) {
            var stacks = entry.getValue().getStacks();
            for (int index = 0; index < stacks.getSlots() && !remaining.isEmpty(); index++) {
                if (stacks.getStackInSlot(index).isEmpty()) remaining = stacks.insertItem(index, remaining, false);
            }
            if (remaining.isEmpty()) break;
        }
        stack.setCount(remaining.getCount());
        refresh(type);
        return stack.isEmpty();
    }

    private List<Map.Entry<String, ICurioStacksHandler>> handlersFor(AccessorySlotType type) {
        return curios.getCurios().entrySet().stream()
                .filter(entry -> ChangedCuriosSlots.aliases(type).contains(entry.getKey()) || entry.getKey().equals("curio"))
                .sorted(java.util.Comparator.comparing(entry -> entry.getKey().equals("curio")))
                .toList();
    }

    @Override public boolean hasSpaceFor(ItemStack stack) {
        return ChangedRegistry.ACCESSORY_SLOTS.get().getValues().stream()
                .anyMatch(type -> stack.is(type.getItemTag()) && isItemAllowedWithOthers(type, stack));
    }

    @Override public void setItem(AccessorySlotType type, ItemStack stack) {
        if (type == null) return;
        var equipped = find(type, false);
        if (equipped != null) {
            if (stack.isEmpty() || equipped.handler().isItemValid(equipped.index(), stack)) {
                equipped.handler().setStackInSlot(equipped.index(), stack);
            }
        } else if (!stack.isEmpty()) {
            moveToSlot(type, stack);
        }
        refresh(type);
    }

    @Override public void setItem(int index, ItemStack stack) { setItem(getSlotTypeByIndex(index), stack); }

    @Override public ItemStack removeItemNoUpdate(int index) {
        var type = getSlotTypeByIndex(index);
        if (type == null) return ItemStack.EMPTY;
        var binding = find(type, false);
        if (binding == null) return ItemStack.EMPTY;
        ItemStack removed = binding.stack();
        binding.handler().setStackInSlot(binding.index(), ItemStack.EMPTY);
        refresh(type);
        return removed;
    }

    @Override public ItemStack removeItem(int index, int count) {
        var type = getSlotTypeByIndex(index);
        var binding = type == null ? null : find(type, false);
        if (binding == null || count <= 0) return ItemStack.EMPTY;
        ItemStack removed = binding.stack().split(count);
        if (binding.stack().isEmpty()) binding.handler().setStackInSlot(binding.index(), ItemStack.EMPTY);
        refresh(type);
        return removed;
    }

    private void refresh(AccessorySlotType type) {
        var equipped = find(type, false);
        super.setItem(type, equipped == null ? ItemStack.EMPTY : equipped.stack());
    }

    @Override public boolean initialize(Predicate<AccessorySlotType> allowed, Consumer<ItemStack> removed) {
        boolean changed = false;
        for (var type : getOrderedSlots()) {
            var binding = find(type, false);
            if (binding != null && (!allowed.test(type) || !type.canHoldItem(binding.stack(), owner))) {
                ItemStack stack = binding.stack();
                binding.handler().setStackInSlot(binding.index(), ItemStack.EMPTY);
                removed.accept(stack);
                refresh(type);
                changed = true;
            }
        }
        return changed;
    }

    @Override public void clearContent() {
        for (var type : getOrderedSlots()) setItem(type, ItemStack.EMPTY);
    }

    // Curios handles ticks, death, cloning, disk saves and its own synchronization.
    // In particular, Changed's now-empty legacy sync packets must never clear Curios equipment.
    @Override public void tick() {}
    @Override public void dropAll(Consumer<ItemStack> consumer) {}
    @Override public void setAll(AccessorySlots other, boolean empty) {}
    @Override public void replaceWith(AccessorySlots other) {}
    @Override public void load(CompoundTag tag) {}
    @Override public void readNetwork(FriendlyByteBuf buffer) {}

    private record Binding(IDynamicStackHandler handler, int index) {
        ItemStack stack() { return handler.getStackInSlot(index); }
    }
}
