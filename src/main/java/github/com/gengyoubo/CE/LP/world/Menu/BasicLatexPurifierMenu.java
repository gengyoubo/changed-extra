package github.com.gengyoubo.CE.LP.world.Menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class BasicLatexPurifierMenu extends AbstractContainerMenu {
    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;
    private IItemHandler items = new ItemStackHandler(2);

    public BasicLatexPurifierMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), new SimpleContainerData(8));
    }

    public BasicLatexPurifierMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, createData(inventory.player.level(), pos));
    }

    private BasicLatexPurifierMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(CEMenus.BASIC_LATEX_PURIFIER.get(), id);
        this.level = inventory.player.level();
        this.pos = pos;
        this.data = data;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> items = handler);
        addSlot(new SlotItemHandler(items, 0, 35, 35));
        addSlot(new SlotItemHandler(items, 1, 125, 35) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 99 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 157));
        checkContainerDataCount(data, 8);
        addDataSlots(data);
    }

    private static ContainerData createData(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override public int get(int index) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity == null) return 0;
                return switch (index) {
                    case 0 -> blockEntity instanceof github.com.gengyoubo.CE.LP.BlockEntity.BaseEnergyBlockEntity energy ? energy.getEnergyStored() : 0;
                    case 1 -> blockEntity instanceof github.com.gengyoubo.CE.LP.BlockEntity.BaseEnergyBlockEntity energy ? energy.getMaxEnergyStored() : 0;
                    case 2 -> blockEntity instanceof github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicLatexPurifierBlockEntity purifier ? purifier.getStoredFluid().getAmount() : 0;
                    case 3 -> 10_000;
                    case 4 -> blockEntity instanceof github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicLatexPurifierBlockEntity purifier ? purifier.getItemProgress() : 0;
                    case 5 -> blockEntity instanceof github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicLatexPurifierBlockEntity purifier ? purifier.getFluidProgress() : 0;
                    case 6, 7 -> 100;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 8; }
        };
    }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public int getFluidAmount() { return data.get(2); }
    public int getTankCapacity() { return data.get(3); }
    public int getItemProgress() { return data.get(4); }
    public int getFluidProgress() { return data.get(5); }
    public int getMaxProgress() { return data.get(6); }
    public boolean stillValid(@NotNull Player player) { return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos), player, github.com.gengyoubo.CE.LP.init.CELPBlock.BASIC_LATEX_PURIFIER.get()); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        }
        return original;
    }
}
