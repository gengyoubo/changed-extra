package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicAlloyFurnaceBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class BasicAlloyFurnaceMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 10;
    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;

    public BasicAlloyFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }
    public BasicAlloyFurnaceMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, createData(inventory.player.level(), pos));
    }
    private BasicAlloyFurnaceMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(CEMenus.BASIC_ALLOY_FURNACE.get(), id);
        this.level = inventory.player.level();
        this.pos = pos;
        this.data = data;
        IItemHandler items = level.getBlockEntity(pos) instanceof BasicAlloyFurnaceBlockEntity furnace
                ? furnace.getItemHandler() : new ItemStackHandler(3);
        for (int slot = 0; slot < 2; slot++) {
            addSlot(new SlotItemHandler(items, slot, 36 + slot * 24, 32) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) {
                    return BasicAlloyFurnaceBlockEntity.isValidInput(level, stack) && super.mayPlace(stack);
                }
            });
        }
        addSlot(new SlotItemHandler(items, 2, 120, 32) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 102 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 160));
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
    }

    private static ContainerData createData(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override public int get(int index) {
                if (!(level.getBlockEntity(pos) instanceof BasicAlloyFurnaceBlockEntity furnace)) return 0;
                int value = switch (index / 2) {
                    case 0 -> furnace.getEnergyStored();
                    case 1 -> furnace.getMaxEnergyStored();
                    case 2 -> furnace.getProgress();
                    case 3 -> furnace.getProcessTicks();
                    case 4 -> furnace.getLpPerSecond();
                    default -> 0;
                };
                // Preserve all 32 bits, including capacities above the signed-short limit.
                return index % 2 == 0 ? value & 0xFFFF : value >>> 16;
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    private int value(int index) { return (data.get(index * 2) & 0xFFFF) | ((data.get(index * 2 + 1) & 0xFFFF) << 16); }
    public int getEnergyStored() { return value(0); }
    public int getMaxEnergyStored() { return value(1); }
    public int getProgress() { return value(2); }
    public int getProcessTicks() { return value(3); }
    public int getLpPerSecond() { return value(4); }
    public BlockPos getBlockPos() { return pos; }
    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, pos), player, CELPBlock.BASIC_ALLOY_FURNACE.get());
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!BasicAlloyFurnaceBlockEntity.isValidInput(level, stack)
                    || !moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
