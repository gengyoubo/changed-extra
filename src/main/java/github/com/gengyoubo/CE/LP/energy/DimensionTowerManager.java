package github.com.gengyoubo.CE.LP.energy;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.DimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.FluidDimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.changede;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = changede.MODID)
public final class DimensionTowerManager {
    private static final Map<MinecraftServer, DimensionTowerChannels> NETWORKS = new IdentityHashMap<>();
    private static final Map<MinecraftServer, DimensionFluidChannels> FLUID_NETWORKS = new IdentityHashMap<>();

    private DimensionTowerManager() { }

    public static DimensionTowerChannels network(MinecraftServer server) {
        return NETWORKS.computeIfAbsent(server, ignored -> new DimensionTowerChannels());
    }

    public static DimensionFluidChannels fluidNetwork(MinecraftServer server) {
        return FLUID_NETWORKS.computeIfAbsent(server, ignored -> new DimensionFluidChannels());
    }

    public static void unregisterFluid(MinecraftServer server, FluidDimensionSpaceTowerBlockEntity tower) {
        DimensionFluidChannels network = FLUID_NETWORKS.get(server);
        if (network != null) network.unregister(tower);
    }

    public static int fluidPeerCount(MinecraftServer server, FluidDimensionSpaceTowerBlockEntity tower) {
        DimensionFluidChannels network = FLUID_NETWORKS.get(server);
        return network == null ? 0 : network.peerCount(tower);
    }

    public static void unregister(MinecraftServer server, DimensionSpaceTowerBlockEntity tower) {
        DimensionTowerChannels network = NETWORKS.get(server);
        if (network != null) network.unregister(tower);
    }

    public static int peerCount(MinecraftServer server, DimensionSpaceTowerBlockEntity tower) {
        DimensionTowerChannels network = NETWORKS.get(server);
        return network == null ? 0 : network.peerCount(tower);
    }

    public static void validateTickets(ServerLevel level, ForgeChunkManager.TicketHelper helper) {
        helper.getBlockTickets().forEach((owner, tickets) -> {
            // A one-time startup lookup validates the saved owner. No tick-time chunk fetching.
            var blockEntity = level.getBlockEntity(owner);
            boolean enabled = blockEntity instanceof DimensionSpaceTowerBlockEntity tower && tower.isEnabled()
                    || blockEntity instanceof FluidDimensionSpaceTowerBlockEntity fluidTower && fluidTower.isEnabled();
            if (!enabled) {
                helper.removeAllTickets(owner);
                return;
            }
            var expected = DimensionTowerChunks.around(owner.getX(), owner.getZ());
            for (long chunk : tickets.getFirst()) helper.removeTicket(owner, chunk, false);
            for (long chunk : tickets.getSecond()) {
                ChunkPos pos = new ChunkPos(chunk);
                if (!expected.contains(new DimensionTowerChunks.Chunk(pos.x, pos.z))) helper.removeTicket(owner, chunk, true);
            }
        });
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        DimensionTowerChannels network = NETWORKS.get(event.getServer());
        if (network != null) network.tick();
        DimensionFluidChannels fluidNetwork = FLUID_NETWORKS.get(event.getServer());
        if (fluidNetwork != null) fluidNetwork.tick();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        DimensionTowerChannels network = NETWORKS.remove(event.getServer());
        if (network != null) network.clear();
        DimensionFluidChannels fluidNetwork = FLUID_NETWORKS.remove(event.getServer());
        if (fluidNetwork != null) fluidNetwork.clear();
        // Forge persists tickets; dropping them here would stop remote factories after restart.
    }
}
