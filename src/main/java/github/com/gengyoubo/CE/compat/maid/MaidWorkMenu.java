package github.com.gengyoubo.CE.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MaidWorkMenu extends AbstractContainerMenu {
    private final int creatureId;
    private final boolean fromWheel;
    private final DataSlot selectedTask = DataSlot.standalone();

    public MaidWorkMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, extraData.readInt(), extraData.readBoolean());
    }

    MaidWorkMenu(int id, Inventory inventory, int creatureId, boolean fromWheel) {
        super(LatexMaidCompat.WORK_MENU.get(), id);
        this.creatureId = creatureId;
        this.fromWheel = fromWheel;
        addDataSlot(selectedTask);
        selectedTask.set(-1);
        ChangedEntity creature = getCreature(inventory.player);
        if (inventory.player instanceof ServerPlayer && creature != null) {
            ResourceLocation current = ResourceLocation.tryParse(creature.getPersistentData().getString(LatexMaidCompat.taskTag()));
            List<IMaidTask> available = tasks();
            for (int index = 0; index < available.size(); index++) {
                if (available.get(index).getUid().equals(current)) {
                    selectedTask.set(index);
                    break;
                }
            }
        }
        // Match the player inventory positions in Touhou Little Maid's main GUI.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 94 + 88 + column * 18, 174 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 94 + 88 + column * 18, 232));
        }
        addSlot(new Slot(new MainHandContainer(inventory.player, creatureId), 0, 94 + 31, 225));
    }

    public List<IMaidTask> tasks() { return LatexMaidCompat.tasks(); }
    public int creatureId() { return creatureId; }
    public boolean fromWheel() { return fromWheel; }
    public int selectedTaskIndex() { return selectedTask.get(); }

    @Override
    public boolean stillValid(@NotNull Player player) {
        ChangedEntity creature = getCreature(player);
        if (creature == null || !creature.isAlive() || player.distanceToSqr(creature) > 64.0D) return false;
        return player.level().isClientSide() || player instanceof ServerPlayer serverPlayer
                && github.com.gengyoubo.CE.compat.synergy.ChangedSynergyFeedApi.hasMaximumFamiliarity(creature, serverPlayer)
                && !LatexMaidCompat.isWorkBoundToOtherPlayer(creature, serverPlayer);
    }

    private ChangedEntity getCreature(Player player) {
        return player.level().getEntity(creatureId) instanceof ChangedEntity changed ? changed : null;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        List<IMaidTask> available = tasks();
        if (buttonId < 0 || buttonId > available.size() || !(player instanceof ServerPlayer serverPlayer)) return false;
        ChangedEntity creature = getCreature(player);
        if (creature == null || !creature.isAlive() || player.distanceToSqr(creature) > 64.0D
                || !github.com.gengyoubo.CE.compat.synergy.ChangedSynergyFeedApi.hasMaximumFamiliarity(creature, serverPlayer)
                || LatexMaidCompat.isWorkBoundToOtherPlayer(creature, serverPlayer)) return false;

        if (buttonId == available.size()) {
            creature.getPersistentData().remove(LatexMaidCompat.taskTag());
            selectedTask.set(-1);
            return true;
        }
        ResourceLocation taskId = available.get(buttonId).getUid();
        creature.getPersistentData().putString(LatexMaidCompat.taskTag(), taskId.toString());
        creature.getPersistentData().putUUID(LatexMaidCompat.workOwnerTag(), serverPlayer.getUUID());
        selectedTask.set(buttonId);
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int start = index == 36 ? 0 : index < 27 ? 27 : 0;
        int end = index == 36 ? 36 : index < 27 ? 36 : 27;
        if (!moveItemStackTo(stack, start, end, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    private static final class MainHandContainer extends SimpleContainer {
        private final Player player;
        private final int creatureId;

        MainHandContainer(Player player, int creatureId) {
            super(1);
            this.player = player;
            this.creatureId = creatureId;
        }

        private ChangedEntity creature() {
            return player.level().getEntity(creatureId) instanceof ChangedEntity changed ? changed : null;
        }

        @Override public ItemStack getItem(int slot) {
            ChangedEntity creature = creature();
            return slot == 0 && creature != null ? creature.getMainHandItem() : ItemStack.EMPTY;
        }

        @Override public void setItem(int slot, ItemStack stack) {
            ChangedEntity creature = creature();
            if (slot == 0 && creature != null) creature.setItemSlot(EquipmentSlot.MAINHAND, stack);
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            ChangedEntity creature = creature();
            if (slot != 0 || creature == null || amount <= 0) return ItemStack.EMPTY;
            ItemStack remainder = creature.getMainHandItem().copy();
            ItemStack removed = remainder.split(amount);
            setItem(0, remainder);
            return removed;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            ChangedEntity creature = creature();
            if (slot != 0 || creature == null) return ItemStack.EMPTY;
            ItemStack removed = creature.getMainHandItem();
            setItem(0, ItemStack.EMPTY);
            return removed;
        }

        @Override public boolean isEmpty() { return getItem(0).isEmpty(); }
        @Override public void clearContent() { setItem(0, ItemStack.EMPTY); }
        @Override public int getMaxStackSize() { return 1; }
    }
}
