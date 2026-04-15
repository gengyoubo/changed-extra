package github.com.gengyoubo.LP.world.Menu;

import github.com.gengyoubo.LP.BlockEntity.BaseEnergyBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.MachineBlockEntity.MachineBlockEntity;
import io.netty.buffer.Unpooled;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ElectricFurnaceMenu extends AbstractContainerMenu {
    private final Level world;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private IItemHandler internal = new ItemStackHandler(2);

    public ElectricFurnaceMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos(), new SimpleContainerData(4));
    }

    public ElectricFurnaceMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, pos, createData(inv.player.level(), pos));
    }

    public ElectricFurnaceMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(CEMenus.ELECTRIC_FURNACE.get(), id);
        this.world = inv.player.level();
        this.pos = pos;
        this.access = ContainerLevelAccess.create(world, pos);
        this.data = data;

        bindBlockEntityInventory();
        addFurnaceSlots();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        checkContainerDataCount(this.data, 4);
        addDataSlots(this.data);
    }

    private static ContainerData createData(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                return switch (index) {
                    case 0 -> blockEntity instanceof BaseEnergyBlockEntity energyBlockEntity ? energyBlockEntity.getEnergyStored() : 0;
                    case 1 -> blockEntity instanceof BaseEnergyBlockEntity energyBlockEntity ? energyBlockEntity.getMaxEnergyStored() : 0;
                    case 2 -> blockEntity instanceof MachineBlockEntity machineBlockEntity ? machineBlockEntity.getProgress() : 0;
                    case 3 -> blockEntity instanceof MachineBlockEntity machineBlockEntity ? machineBlockEntity.getMaxProgressValue() : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    private void bindBlockEntityInventory() {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> this.internal = handler);
        }
    }

    private void addFurnaceSlots() {
        this.addSlot(new SlotItemHandler(internal, 0, 56, 35));
        this.addSlot(new SlotItemHandler(internal, 1, 116, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && AbstractContainerMenu.stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < 2) {
                if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    public int getEnergyStored() {
        return data.get(0);
    }

    public int getMaxEnergyStored() {
        return data.get(1);
    }

    public int getProgress() {
        return data.get(2);
    }

    public int getMaxProgress() {
        return data.get(3);
    }

    public int getScaledProgress(int pixels) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return progress * pixels / maxProgress;
    }

    public BlockPos getBlockPos() {
        return pos;
    }
}
