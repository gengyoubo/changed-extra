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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ElectricFurnaceMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    public static final HashMap<String, Object> guistate = new HashMap<>();

    public final Level world;
    public final Player entity;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    public int x;
    public int y;
    private final BlockPos pos;
    public int z;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private IItemHandler internal = new ItemStackHandler(2);

    public ElectricFurnaceMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos(), new SimpleContainerData(4));
    }

    public ElectricFurnaceMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, pos, createData(inv.player.level(), pos));
    }

    public ElectricFurnaceMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(CEMenus.ELECTRIC_FURNACE.get(), id);
        this.entity = inv.player;
        this.world = inv.player.level();
        this.pos = pos;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
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

    private static FriendlyByteBuf writePos(BlockPos pos) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBlockPos(pos);
        return buffer;
    }

    private void bindBlockEntityInventory() {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(capability -> this.internal = capability);
        }
    }

    private void addFurnaceSlots() {
        this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 44, 32)));
        this.customSlots.put(1, this.addSlot(new SlotItemHandler(internal, 1, 116, 32) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        }));
    }

    private void addPlayerInventory(Inventory inv) {
        for (int si = 0; si < 3; ++si) {
            for (int sj = 0; sj < 9; ++sj) {
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 8 + sj * 18, 84 + si * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int si = 0; si < 9; ++si) {
            this.addSlot(new Slot(inv, si, 8 + si * 18, 142));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && AbstractContainerMenu.stillValid(this.access, player, blockEntity.getBlockState().getBlock());
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

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 2) {
                if (!this.moveItemStackTo(itemstack1, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                if (index < 29) {
                    if (!this.moveItemStackTo(itemstack1, 29, this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }
            if (itemstack1.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }
}
