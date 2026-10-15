package github.com.gengyoubo.CE.compat.mekanism.gas;

import github.com.gengyoubo.CE.LP.LatexEnergyType;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistry;

/** Split 32-bit values into two data slots; vanilla data packets carry signed shorts. */
public final class ReverserMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 24;
    private final ContainerData data;
    private final Level level;
    private final BlockPos pos;
    public ReverserMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos(), null); }
    public ReverserMenu(int id, Inventory inventory, BlockPos pos) { this(id, inventory, pos, inventory.player.level()); }
    private ReverserMenu(int id, Inventory inventory, BlockPos pos, Level serverLevel) {
        super(GasCompat.MENU.get(), id); this.pos = pos; level = inventory.player.level();
        data = serverLevel == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override public int get(int index) {
                if (!(level.getBlockEntity(pos) instanceof ReverserBlockEntity tile)) return 0;
                int value = switch (index / 2) {
                    case 0 -> tile.getTypedEnergyStored(LatexEnergyType.WLP); case 1 -> tile.getTypedEnergyStored(LatexEnergyType.DLP);
                    case 2 -> tile.progress(); case 3 -> tile.processTicks(); case 4 -> tile.whiteRate(); case 5 -> tile.darkRate();
                    case 6 -> registry().getID(tile.inputTank.getType()); case 7 -> (int) tile.inputTank.getStored();
                    case 8 -> registry().getID(tile.outputTank.getType()); case 9 -> (int) tile.outputTank.getStored();
                    case 10 -> tile.status().ordinal(); case 11 -> tile.isDraining() ? 1 : 0; default -> 0;
                };
                return index % 2 == 0 ? value & 0xFFFF : value >>> 16;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
        addDataSlots(data);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 135 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 193));
    }
    @SuppressWarnings("unchecked") private static ForgeRegistry<Gas> registry() { return (ForgeRegistry<Gas>) MekanismAPI.gasRegistry(); }
    public int value(int index) { return (data.get(index * 2) & 0xFFFF) | ((data.get(index * 2 + 1) & 0xFFFF) << 16); }
    public GasStack gas(boolean output) {
        Gas gas = registry().getValue(value(output ? 8 : 6));
        int amount = value(output ? 9 : 7);
        return gas == null || amount <= 0 ? GasStack.EMPTY : new GasStack(gas, amount);
    }
    @Override public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(level, pos), player, GasCompat.REVERSER.getBlock()); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || level.isClientSide || !stillValid(player) || !(level.getBlockEntity(pos) instanceof ReverserBlockEntity tile)) return false;
        tile.toggleDrain(); broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem(); var original = stack.copy();
        if (!moveItemStackTo(stack, index < 27 ? 27 : 0, index < 27 ? 36 : 27, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, stack); return original;
    }
}
