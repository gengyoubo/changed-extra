package github.com.gengyoubo.LP.world.Menu;

import github.com.gengyoubo.LP.BlockEntity.BaseEnergyBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.GeneratorBlockEntity.GeneratorBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.RedstoneMode;
import io.netty.buffer.Unpooled;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BasicGeneratorBlockEntityMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    public static final HashMap<String, Object> guistate = new HashMap<>();

    private final Level world;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private final ContainerData data;
    private IItemHandler internal = new ItemStackHandler(1);

    public BasicGeneratorBlockEntityMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos(), new SimpleContainerData(3));
    }

    public BasicGeneratorBlockEntityMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, pos, createEnergyData(inv.player.level(), pos));
    }

    public BasicGeneratorBlockEntityMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(CEMenus.BASIC_GENERATOR_BLOCK_ENTITY.get(), id);

        this.world = inv.player.level();
        this.pos = pos;
        this.access = ContainerLevelAccess.create(world, pos);
        this.data = data;

        bindBlockEntityInventory();
        addCustomSlots();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        checkContainerDataCount(this.data, 3);
        addDataSlots(this.data);
    }

    private static ContainerData createEnergyData(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof BaseEnergyBlockEntity energyBlockEntity) {
                    return switch (index) {
                        case 0 -> energyBlockEntity.getEnergyStored();
                        case 1 -> energyBlockEntity.getMaxEnergyStored();
                        case 2 -> blockEntity instanceof GeneratorBlockEntity generatorBlockEntity
                                ? generatorBlockEntity.getRedstoneMode().ordinal()
                                : 0;
                        default -> 0;
                    };
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    private void bindBlockEntityInventory() {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> this.internal = handler);
        }
    }

    private void addCustomSlots() {
        this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 70, 39) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() == ChangedItems.WHITE_LATEX_GOO.get()
                        || stack.getItem() == ChangedItems.DARK_LATEX_GOO.get();
            }
        }));
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + (row + 1) * 9, 35 + col * 18, 91 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 35 + col * 18, 149));
        }
    }

    private static FriendlyByteBuf writePos(BlockPos pos) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBlockPos(pos);
        return buffer;
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

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
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
        return this.data.get(0);
    }

    public int getMaxEnergyStored() {
        return this.data.get(1);
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public RedstoneMode getRedstoneMode() {
        int ordinal = this.data.get(2);
        RedstoneMode[] modes = RedstoneMode.values();
        if (ordinal < 0 || ordinal >= modes.length) {
            return RedstoneMode.ALWAYS_ON;
        }
        return modes[ordinal];
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }
}
