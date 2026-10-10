package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.FluidDimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.BlockEntity.RedstoneMode;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FluidDimensionSpaceTowerMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 20;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public FluidDimensionSpaceTowerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    public FluidDimensionSpaceTowerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, serverData(inventory.player.level(), pos));
    }

    private FluidDimensionSpaceTowerMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(CEMenus.FLUID_DIMENSION_SPACE_TOWER.get(), id);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        this.data = data;
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
    }

    private static ContainerData serverData(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override public int get(int index) {
                int value = 0;
                if (level.getBlockEntity(pos) instanceof FluidDimensionSpaceTowerBlockEntity tower) {
                    value = switch (index / 2) {
                        case 0 -> tower.getChannel();
                        case 1 -> tower.isSwitchedOn() ? 1 : 0;
                        case 2 -> BuiltInRegistries.FLUID.getId(tower.getFluid().getFluid());
                        case 3 -> tower.getMode().ordinal();
                        case 4 -> tower.getRedstoneMode().ordinal();
                        case 5 -> tower.active() ? 1 : 0;
                        case 6 -> tower.getLoadedChunkCount();
                        case 7 -> tower.getPeerCount();
                        case 8 -> tower.getFluid().getAmount();
                        case 9 -> tower.getFluid().hasTag() ? 1 : 0;
                        default -> 0;
                    };
                }
                // Vanilla data slots transmit signed shorts; retain full 50,000-unit buffers.
                return (index % 2 == 0 ? value : value >>> 16) & 0xFFFF;
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    private int value(int index) { return (data.get(index * 2) & 0xFFFF) | (data.get(index * 2 + 1) & 0xFFFF) << 16; }
    public BlockPos getBlockPos() { return pos; }
    public int getChannel() { return value(0); }
    public boolean isEnabled() { return value(1) != 0; }
    public FluidStack getFluid() {
        var fluid = BuiltInRegistries.FLUID.byId(value(2));
        return fluid == null || value(8) <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, value(8));
    }
    public boolean hasFluidTag() { return value(9) != 0; }
    public IOType getMode() { return IOType.values()[Math.max(0, Math.min(2, value(3)))]; }
    public RedstoneMode getRedstoneMode() { return RedstoneMode.values()[Math.max(0, Math.min(3, value(4)))]; }
    public boolean isActive() { return value(5) != 0; }
    public int getLoadedChunkCount() { return value(6); }
    public int getPeerCount() { return value(7); }

    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, CELPBlock.FLUID_DIMENSION_SPACE_TOWER.get());
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
