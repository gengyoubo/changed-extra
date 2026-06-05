package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.SpaceTowerAccess;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class SpaceTowerMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 8 + SpaceTowerEnergyType.values().length;

    private final Level world;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public SpaceTowerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    public SpaceTowerMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, pos, createData(inv.player.level(), pos));
    }

    public SpaceTowerMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(CEMenus.SPACE_TOWER.get(), id);
        this.world = inv.player.level();
        this.pos = pos;
        this.access = ContainerLevelAccess.create(world, pos);
        this.data = data;

        checkContainerDataCount(this.data, DATA_COUNT);
        addDataSlots(this.data);
    }

    private static ContainerData createData(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (!(blockEntity instanceof SpaceTowerAccess tower)) {
                    return 0;
                }

                if (index >= 8) {
                    int typeIndex = index - 8;
                    SpaceTowerEnergyType[] types = SpaceTowerEnergyType.values();
                    if (typeIndex < types.length) {
                        return tower.getMode(types[typeIndex]).ordinal();
                    }
                    return 0;
                }

                return switch (index) {
                    case 0 -> tower.getEnergyStored();
                    case 1 -> tower.getMaxEnergyStored();
                    case 2 -> tower.getJouleBufferDisplay();
                    case 3 -> tower.getCeStoredLpDisplay();
                    case 4 -> tower.getMaxCeStoredLp();
                    case 5 -> tower.getCeRpm();
                    case 6 -> tower.getCeSu();
                    case 7 -> tower.getCeCostPerMinute();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && AbstractContainerMenu.stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public int getEnergyStored() {
        return data.get(0);
    }

    public int getMaxEnergyStored() {
        return data.get(1);
    }

    public int getJouleBuffer() {
        return data.get(2);
    }

    public int getCeStoredLp() {
        return data.get(3);
    }

    public int getMaxCeStoredLp() {
        return data.get(4);
    }

    public int getCeRpm() {
        return data.get(5);
    }

    public int getCeSu() {
        return data.get(6);
    }

    public int getCeCostPerMinute() {
        return data.get(7);
    }

    public IOType getMode(SpaceTowerEnergyType type) {
        int ordinal = data.get(8 + type.ordinal());
        IOType[] values = IOType.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return IOType.INPUT;
        }
        return values[ordinal];
    }

    public int getCeStoredSeconds() {
        int cost = getCeCostPerMinute();
        if (cost <= 0) {
            return 0;
        }
        return Math.round(getCeStoredLp() * 60.0F / cost);
    }
}
