package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.BasicLatexFluidGeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicPumpBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.LatexEnergyConverterBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.OrangeProducerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

/** Status and output UI for tank machines, without exposing their internal fuel slots. */
public class MachineStatusMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 22;
    private final Level level;
    private final BlockPos pos;
    private final BlockEntity machine;
    private final ContainerData data;
    private final int machineSlots;

    public MachineStatusMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    public MachineStatusMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, serverData(inventory.player.level(), pos));
    }

    private MachineStatusMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(CEMenus.MACHINE_STATUS.get(), id);
        this.level = inventory.player.level();
        this.pos = pos;
        this.machine = level.getBlockEntity(pos);
        this.data = data;
        machineSlots = machine instanceof OrangeProducerBlockEntity ? 1 : 0;
        if (machineSlots > 0) {
            var handler = machine.getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .orElse(new ItemStackHandler(1));
            addSlot(new SlotItemHandler(handler, 0, 132, 56) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 112 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 170));
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
    }

    private static ContainerData serverData(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override public int get(int index) {
                int value = value(level.getBlockEntity(pos), index / 2);
                // Container data packets carry signed shorts: split all 32-bit values.
                return (index % 2 == 0 ? value : value >>> 16) & 0xFFFF;
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    private static int value(BlockEntity be, int index) {
        if (be == null) return 0;
        ILatexEnergyHandler energy = be instanceof ILatexEnergyHandler handler ? handler : null;
        var fluidHandler = be.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
        FluidStack fluid = fluidHandler != null && fluidHandler.getTanks() > 0
                ? fluidHandler.getFluidInTank(0) : FluidStack.EMPTY;
        return switch (index) {
            case 0 -> energy == null ? 0 : energy.getEnergyStored();
            case 1 -> energy == null ? 0 : energy.getMaxEnergyStored();
            case 2 -> be instanceof LatexEnergyConverterBlockEntity c ? c.getOutputStored() : 0;
            case 3 -> be instanceof LatexEnergyConverterBlockEntity c ? c.getOutputCapacity() : 0;
            case 4 -> fluid.getAmount();
            case 5 -> fluidHandler != null && fluidHandler.getTanks() > 0 ? fluidHandler.getTankCapacity(0) : 0;
            case 6 -> BuiltInRegistries.FLUID.getId(fluid.getFluid());
            case 7 -> be instanceof OrangeProducerBlockEntity orange ? orange.getProgress() : 0;
            case 8 -> be instanceof OrangeProducerBlockEntity ? 200 : 0;
            case 9 -> kind(be);
            case 10 -> be instanceof BasicLatexFluidGeneratorBlockEntity generator ? generator.getRedstoneMode().ordinal() : 0;
            default -> 0;
        };
    }

    private static int kind(BlockEntity be) {
        if (be instanceof BasicPumpBlockEntity) return 0;
        if (be instanceof BasicLatexFluidGeneratorBlockEntity) return 1;
        if (be instanceof LatexEnergyConverterBlockEntity converter)
            return converter.getOutputType() == github.com.gengyoubo.CE.LP.LatexEnergyType.WLP ? 2 : 3;
        return 4;
    }

    public int getValue(int index) { return (data.get(index * 2) & 0xFFFF) | ((data.get(index * 2 + 1) & 0xFFFF) << 16); }
    public FluidStack getFluid() {
        var fluid = BuiltInRegistries.FLUID.byId(getValue(6));
        return fluid == null || getValue(4) <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, getValue(4));
    }
    public ItemStack getMachineIcon() { return machine == null ? ItemStack.EMPTY : new ItemStack(machine.getBlockState().getBlock()); }

    @Override public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id != 0 || !stillValid(player) || !(machine instanceof BasicLatexFluidGeneratorBlockEntity generator)) return false;
        generator.cycleRedstoneMode();
        return true;
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return machine != null && level.getBlockEntity(pos) == machine
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index < machineSlots + 27) {
            if (!moveItemStackTo(stack, machineSlots + 27, slots.size(), false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, machineSlots, machineSlots + 27, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        Component title = player.level().getBlockState(pos).getBlock().getName();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, user) -> new MachineStatusMenu(id, inventory, pos), title), pos);
    }
}
