package github.com.gengyoubo.CE.LP.energy;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.FluidDimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.IOType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fluid stays in the loaded block entities; this network holds only live addresses. */
public final class DimensionFluidChannels {
    private final Map<Object, FluidDimensionSpaceTowerBlockEntity> endpoints = new LinkedHashMap<>();
    private long tick;

    public void register(FluidDimensionSpaceTowerBlockEntity tower) { endpoints.put(tower.address(), tower); }
    public void unregister(FluidDimensionSpaceTowerBlockEntity tower) { endpoints.remove(tower.address(), tower); }
    public void clear() { endpoints.clear(); }
    public int peerCount(FluidDimensionSpaceTowerBlockEntity tower) {
        return (int) endpoints.values().stream().filter(other -> other != tower && other.active()
                && other.getChannel() == tower.getChannel()).count();
    }

    public void tick() {
        Map<Integer, List<FluidDimensionSpaceTowerBlockEntity>> channels = new LinkedHashMap<>();
        for (var tower : endpoints.values()) {
            if (tower.active()) channels.computeIfAbsent(tower.getChannel(), ignored -> new ArrayList<>()).add(tower);
        }
        for (var channel : channels.values()) transfer(channel);
        tick++;
    }

    private void transfer(List<FluidDimensionSpaceTowerBlockEntity> channel) {
        var sources = channel.stream().filter(t -> t.getMode() == IOType.INPUT && !t.getFluid().isEmpty()).toList();
        var targets = channel.stream().filter(t -> t.getMode() == IOType.OUTPUT
                && t.getFluid().getAmount() < FluidDimensionSpaceTowerBlockEntity.CAPACITY).toList();
        if (sources.isEmpty() || targets.isEmpty()) return;
        // Rotate both ends so a busy receiver cannot permanently monopolize a channel.
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.get((int) Math.floorMod(tick + i, sources.size()));
            for (int j = 0; j < targets.size() && !source.getFluid().isEmpty(); j++) {
                var target = targets.get((int) Math.floorMod(tick + j, targets.size()));
                var offered = source.getFluid();
                int accepted = target.networkFill(offered, FluidAction.SIMULATE);
                if (accepted <= 0) continue;
                offered.setAmount(accepted);
                // Internal tanks obey the simulation and execute on the same server thread.
                int moved = target.networkFill(offered, FluidAction.EXECUTE);
                source.networkDrain(moved);
            }
        }
    }
}
