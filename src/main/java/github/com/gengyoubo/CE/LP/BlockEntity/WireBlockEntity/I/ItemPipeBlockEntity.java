package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.I;

import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.PipeConnectionMode;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ItemPipeBlockEntity extends BasePipeBlockEntity {

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        this(CELPBlockEntity.BASIC_ITEM_PIPE_BLOCK_ENTITY.get(), pos, state);
    }

    protected ItemPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TransportType.ITEM);
    }

    @Override
    protected boolean canConnectToPipe(BasePipeBlockEntity other, Direction direction) {
        return other.getTransportType() == TransportType.ITEM;
    }

    @Override
    protected boolean canConnectToMachine(BlockEntity target, Direction direction) {
        return target.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).isPresent();
    }

    @Override
    protected void transfer() {
        List<Endpoint> endpoints = getNetworkEndpoints();
        for (Endpoint source : endpoints) {
            if (!source.mode().canSource()) continue;
            for (Endpoint target : endpoints) {
                if (!target.mode().canSink() || source.pos().equals(target.pos())) continue;
                if (moveOne(source.handler(), target.handler())) return;
            }
        }
    }

    public ItemStack getNetworkContents() {
        for (Endpoint endpoint : getNetworkEndpoints()) {
            IItemHandler handler = endpoint.handler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private List<Endpoint> getNetworkEndpoints() {
        if (level == null) return List.of();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<Endpoint> endpoints = new ArrayList<>();
        queue.add(worldPosition);
        visited.add(worldPosition);
        while (!queue.isEmpty()) {
            BlockPos pipePos = queue.removeFirst();
            BlockEntity current = level.getBlockEntity(pipePos);
            if (!(current instanceof BasePipeBlockEntity currentPipe)) continue;
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pipePos.relative(direction);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor instanceof BasePipeBlockEntity pipe && pipe.getTransportType() == TransportType.ITEM) {
                    if (visited.add(neighborPos)) queue.addLast(neighborPos);
                } else if (neighbor != null && currentPipe.getConnectionMode(direction) != PipeConnectionMode.DISABLED) {
                    neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                            .ifPresent(handler -> endpoints.add(new Endpoint(neighborPos, handler, currentPipe.getConnectionMode(direction))));
                }
            }
        }
        return endpoints;
    }

    private static boolean moveOne(IItemHandler source, IItemHandler target) {
        for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
            ItemStack offered = source.extractItem(sourceSlot, 1, true);
            if (offered.isEmpty()) continue;
            for (int targetSlot = 0; targetSlot < target.getSlots(); targetSlot++) {
                ItemStack remainder = target.insertItem(targetSlot, offered, true);
                int accepted = offered.getCount() - remainder.getCount();
                if (accepted <= 0) continue;
                ItemStack extracted = source.extractItem(sourceSlot, accepted, false);
                ItemStack rejected = target.insertItem(targetSlot, extracted, false);
                if (!rejected.isEmpty()) source.insertItem(sourceSlot, rejected, false);
                if (rejected.getCount() < extracted.getCount()) return true;
            }
        }
        return false;
    }

    private record Endpoint(BlockPos pos, IItemHandler handler, PipeConnectionMode mode) { }
}
