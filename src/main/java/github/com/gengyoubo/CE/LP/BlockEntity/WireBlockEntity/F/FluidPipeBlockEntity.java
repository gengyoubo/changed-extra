package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.F;

import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.PipeConnectionMode;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class FluidPipeBlockEntity extends BasePipeBlockEntity {

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        this(CELPBlockEntity.BASIC_FLUID_PIPE_BLOCK_ENTITY.get(), pos, state);
    }

    protected FluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TransportType.FLUID);
    }

    @Override
    protected boolean canConnectToPipe(BasePipeBlockEntity other, Direction direction) {
        return other.getTransportType() == TransportType.FLUID;
    }

    @Override
    protected boolean canConnectToMachine(BlockEntity target, Direction direction) {
        return target.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).isPresent();
    }

    @Override
    protected void transfer() {
        List<Endpoint> endpoints = getNetworkEndpoints();
        for (Endpoint source : endpoints) {
            if (!source.mode().canSource()) continue;
            for (Endpoint target : endpoints) {
                if (!target.mode().canSink() || source.pos().equals(target.pos())) continue;
                if (moveFluid(source.handler(), target.handler())) return;
            }
        }
    }

    public FluidStack getNetworkContents() {
        for (Endpoint endpoint : getNetworkEndpoints()) {
            IFluidHandler handler = endpoint.handler();
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stack = handler.getFluidInTank(tank);
                if (!stack.isEmpty()) return stack.copy();
            }
        }
        return FluidStack.EMPTY;
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
                if (neighbor instanceof BasePipeBlockEntity pipe && pipe.getTransportType() == TransportType.FLUID) {
                    if (visited.add(neighborPos)) queue.addLast(neighborPos);
                } else if (neighbor != null && currentPipe.getConnectionMode(direction) != PipeConnectionMode.DISABLED) {
                    neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                            .ifPresent(handler -> endpoints.add(new Endpoint(neighborPos, handler, currentPipe.getConnectionMode(direction))));
                }
            }
        }
        return endpoints;
    }

    private static boolean moveFluid(IFluidHandler source, IFluidHandler target) {
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack tankContents = source.getFluidInTank(tank);
            if (tankContents.isEmpty()) continue;
            FluidStack offered = tankContents.copy();
            offered.setAmount(Math.min(1000, offered.getAmount()));
            FluidStack simulatedDrain = source.drain(offered, IFluidHandler.FluidAction.SIMULATE);
            if (simulatedDrain.isEmpty()) continue;
            int accepted = Math.min(simulatedDrain.getAmount(), target.fill(simulatedDrain, IFluidHandler.FluidAction.SIMULATE));
            if (accepted <= 0) continue;
            FluidStack requested = offered.copy();
            requested.setAmount(accepted);
            FluidStack drained = source.drain(requested, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) continue;
            int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            if (filled < drained.getAmount()) {
                FluidStack remainder = drained.copy();
                remainder.setAmount(drained.getAmount() - filled);
                source.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
            }
            if (filled > 0) return true;
        }
        return false;
    }

    private record Endpoint(BlockPos pos, IFluidHandler handler, PipeConnectionMode mode) { }
}
