package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicLatexPurifierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class BasicLatexPurifierMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 9;
    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;
    private IItemHandler items = new ItemStackHandler(2);

    public BasicLatexPurifierMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), new SimpleContainerData(DATA_COUNT));
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
        addSlot(new SlotItemHandler(items, 0, 35, 35) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return BasicLatexPurifierBlockEntity.isValidInput(level, stack) && super.mayPlace(stack);
            }
        });
        addSlot(new SlotItemHandler(items, 1, 125, 35) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 99 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 157));
        checkContainerDataCount(data, DATA_COUNT);
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
                    case 6 -> 100;
                    // Container data travels as signed shorts. Split the fluid registry ID.
                    case 7 -> fluidId(blockEntity) & 0xFFFF;
                    case 8 -> fluidId(blockEntity) >>> 16;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        };
    }
    @SuppressWarnings("deprecation")
    private static int fluidId(BlockEntity blockEntity) {
        return blockEntity instanceof BasicLatexPurifierBlockEntity purifier
                ? BuiltInRegistries.FLUID.getId(purifier.getStoredFluid().getFluid())
                : BuiltInRegistries.FLUID.getId(Fluids.EMPTY);
    }
    @SuppressWarnings("deprecation")
    public FluidStack getStoredFluid() {
        int amount = getFluidAmount();
        Fluid fluid = BuiltInRegistries.FLUID.byId((data.get(7) & 0xFFFF) | ((data.get(8) & 0xFFFF) << 16));
        return amount <= 0 || fluid == null || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
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
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // Validate before moving: merging into an existing stack can bypass Slot.mayPlace.
                if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return original;
    }
}
