package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.PipeConnectionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class PipeConfigMenu extends AbstractContainerMenu {
    private static final int FACE_COUNT = Direction.values().length;

    private final Level level;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final SimpleContainerData data;

    public PipeConfigMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, extraData.readBlockPos(), new SimpleContainerData(FACE_COUNT));
    }

    public PipeConfigMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, createData(inventory.player.level(), pos));
    }

    private PipeConfigMenu(int id, Inventory inventory, BlockPos pos, SimpleContainerData data) {
        super(CEMenus.PIPE_CONFIG.get(), id);
        this.level = inventory.player.level();
        this.pos = pos;
        this.access = ContainerLevelAccess.create(level, pos);
        this.data = data;
        addDataSlots(data);
    }

    private static SimpleContainerData createData(Level level, BlockPos pos) {
        return new SimpleContainerData(FACE_COUNT) {
            @Override
            public int get(int index) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (!(blockEntity instanceof BasePipeBlockEntity pipe)) return PipeConnectionMode.BIDIRECTIONAL.ordinal();
                return pipe.getConnectionMode(Direction.values()[index]).ordinal();
            }
        };
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id < 0 || id >= FACE_COUNT || !(player.level().getBlockEntity(pos) instanceof BasePipeBlockEntity pipe)) {
            return false;
        }
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return false;

        pipe.cycleConnectionMode(Direction.values()[id]);
        level.sendBlockUpdated(pos, pipe.getBlockState(), pipe.getBlockState(), 3);
        return true;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return level.getBlockEntity(pos) instanceof BasePipeBlockEntity
                && AbstractContainerMenu.stillValid(access, player, level.getBlockState(pos).getBlock());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    public PipeConnectionMode getMode(Direction direction) {
        int ordinal = data.get(direction.ordinal());
        PipeConnectionMode[] modes = PipeConnectionMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : PipeConnectionMode.BIDIRECTIONAL;
    }
}
