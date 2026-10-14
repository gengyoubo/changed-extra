package github.com.gengyoubo.CE.compat.jer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Optional channel: authoritative worldgen reaches JER clients on both local and dedicated servers. */
@Mod.EventBusSubscriber(modid = "changede")
public final class JerOreNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.parse("changede:jer_ores"), () -> "1",
            NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1"));
    private static MinecraftServer cachedServer;
    private static List<OreGenerationData> cached;
    private static int lastFlags;
    private static int ticks;

    private JerOreNetwork() {}

    public static void initialize() {
        CHANNEL.registerMessage(0, Request.class, (packet, buffer) -> {}, buffer -> new Request(),
                JerOreNetwork::request, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Batch.class, JerOreNetwork::encode, JerOreNetwork::decode,
                JerOreNetwork::receive, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static int flags(MinecraftServer server) {
        int flags = (OreGenerationScanner.painiteEnabled(server.overworld()) ? 1 : 0)
                | (OreGenerationScanner.createEnabled() ? 2 : 0);
        return 31 * flags + (net.minecraftforge.fml.ModList.get().isLoaded("mekanism")
                ? MekanismOreCompat.configFingerprint() : 0);
    }

    public static List<OreGenerationData> snapshot(MinecraftServer server) {
        int state = flags(server);
        if (cached == null || cachedServer != server || state != lastFlags) {
            cachedServer = server;
            cached = OreGenerationScanner.scan(server);
            lastFlags = state;
        }
        return cached;
    }

    private static void request(Request packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) send(player);
        });
        context.setPacketHandled(true);
    }

    private static void send(ServerPlayer player) {
        if (!CHANNEL.isRemotePresent(player.connection.connection)) return;
        var entries = snapshot(player.server);
        if (entries.isEmpty()) {
            CHANNEL.sendTo(new Batch(true, true, List.of()), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
        for (int offset = 0; offset < entries.size(); offset += 32) {
            int end = Math.min(offset + 32, entries.size());
            CHANNEL.sendTo(new Batch(offset == 0, end == entries.size(), entries.subList(offset, end)),
                    player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    @SubscribeEvent
    public static void reload(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return; // Login is requested by the client after its level exists.
        cached = null;
        event.getPlayerList().getPlayers().forEach(JerOreNetwork::send);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % 100 != 0 || cached == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && flags(server) != lastFlags) {
            cached = null;
            server.getPlayerList().getPlayers().forEach(JerOreNetwork::send);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { cached = null; cachedServer = null; ticks = 0; }

    private static void encode(Batch packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.first());
        buffer.writeBoolean(packet.last());
        buffer.writeVarInt(packet.entries().size());
        for (var entry : packet.entries()) {
            buffer.writeResourceLocation(entry.block());
            buffer.writeResourceLocation(entry.dimension());
            buffer.writeVarInt(entry.biomes().size());
            entry.biomes().forEach(buffer::writeResourceLocation);
            for (float density : entry.distribution()) buffer.writeFloat(density);
            buffer.writeVarInt(entry.drops().size());
            for (var drop : entry.drops()) {
                buffer.writeItem(drop.stack());
                buffer.writeVarInt(drop.min());
                buffer.writeVarInt(drop.max());
            }
        }
    }

    private static Batch decode(FriendlyByteBuf buffer) {
        boolean first = buffer.readBoolean();
        boolean last = buffer.readBoolean();
        int count = bounded(buffer.readVarInt(), 32);
        List<OreGenerationData> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            var block = buffer.readResourceLocation();
            var dimension = buffer.readResourceLocation();
            int biomeCount = bounded(buffer.readVarInt(), 4096);
            List<ResourceLocation> biomes = new ArrayList<>(biomeCount);
            for (int j = 0; j < biomeCount; j++) biomes.add(buffer.readResourceLocation());
            float[] distribution = new float[OreGenerationData.HEIGHTS];
            for (int j = 0; j < distribution.length; j++) distribution[j] = buffer.readFloat();
            int dropCount = bounded(buffer.readVarInt(), 128);
            List<OreGenerationData.Drop> drops = new ArrayList<>(dropCount);
            for (int j = 0; j < dropCount; j++) {
                drops.add(new OreGenerationData.Drop(buffer.readItem(), buffer.readVarInt(), buffer.readVarInt()));
            }
            entries.add(new OreGenerationData(block, dimension, biomes, distribution, drops));
        }
        return new Batch(first, last, List.copyOf(entries));
    }

    private static int bounded(int count, int max) {
        if (count < 0 || count > max) throw new IllegalArgumentException("Invalid ore data count: " + count);
        return count;
    }

    private static void receive(Batch packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> JerOreClientState.receive(packet)));
        context.setPacketHandled(true);
    }

    public record Request() {}
    public record Batch(boolean first, boolean last, List<OreGenerationData> entries) {}
}
