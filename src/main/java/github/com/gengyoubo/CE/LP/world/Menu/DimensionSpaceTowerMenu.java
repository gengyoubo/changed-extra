package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.DimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.BlockEntity.RedstoneMode;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DimensionSpaceTowerMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 22;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public DimensionSpaceTowerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    public DimensionSpaceTowerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, serverData(inventory.player.level(), pos));
    }

    private DimensionSpaceTowerMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(CEMenus.DIMENSION_SPACE_TOWER.get(), id);
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
                if (level.getBlockEntity(pos) instanceof DimensionSpaceTowerBlockEntity tower) {
                    value = switch (index / 2) {
                        case 0 -> tower.getChannel();
                        case 1 -> tower.isSwitchedOn() ? 1 : 0;
                        case 2 -> tower.getEnergyType().ordinal();
                        case 3 -> tower.getMode().ordinal();
                        case 4 -> tower.getRedstoneMode().ordinal();
                        case 5 -> tower.active() ? 1 : 0;
                        case 6 -> tower.getLoadedChunkCount();
                        case 7 -> tower.getPeerCount();
                        case 8 -> tower.getStoredEnergy(LatexEnergyType.LP);
                        case 9 -> tower.getStoredEnergy(LatexEnergyType.WLP);
                        case 10 -> tower.getStoredEnergy(LatexEnergyType.DLP);
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
    public LatexEnergyType getEnergyType() { return LatexEnergyType.values()[Math.max(0, Math.min(2, value(2)))]; }
    public IOType getMode() { return IOType.values()[Math.max(0, Math.min(2, value(3)))]; }
    public RedstoneMode getRedstoneMode() { return RedstoneMode.values()[Math.max(0, Math.min(3, value(4)))]; }
    public boolean isActive() { return value(5) != 0; }
    public int getLoadedChunkCount() { return value(6); }
    public int getPeerCount() { return value(7); }
    public int getStoredEnergy(LatexEnergyType type) { return value(8 + type.ordinal()); }

    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, CELPBlock.DIMENSION_SPACE_TOWER.get());
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
