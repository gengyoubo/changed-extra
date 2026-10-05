package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.LP.network.packet.LatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.util.BoundedRequestTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public class LatexPaintingPortalPreviewCache {
    private static final long REQUEST_INTERVAL_TICKS = 200L;
    private static final long IDLE_TICKS = 1200L;
    private static final int MAX_ENTRIES = 64;
    private static final Map<Key, CachedSnapshot> SNAPSHOTS = new LinkedHashMap<>(16, 0.75F, true);
    private static final BoundedRequestTracker<Key> REQUESTS = new BoundedRequestTracker<>(REQUEST_INTERVAL_TICKS, MAX_ENTRIES);
    private static final BoundedRequestTracker<Boolean> GLOBAL_REQUESTS = new BoundedRequestTracker<>(2, 1);
    private static ClientLevel session;

    public static boolean shouldRequest(ResourceLocation dimension, BlockPos pos, long gameTime) {
        ensureSession();
        if (session == null || !session.dimension().location().equals(dimension)) {
            return false;
        }
        Key key = new Key(dimension, pos.immutable());
        return REQUESTS.isReady(key, gameTime) && GLOBAL_REQUESTS.shouldRequest(true, gameTime)
                && REQUESTS.shouldRequest(key, gameTime);
    }

    public static void update(ResourceLocation dimension, BlockPos pos, int skyColor, List<LatexPaintingPortalPreviewPacket.Entry> entries) {
        ensureSession();
        if (session == null || !session.dimension().location().equals(dimension)) {
            return;
        }
        List<PreviewBlock> blocks = new ArrayList<>(entries.size());
        for (LatexPaintingPortalPreviewPacket.Entry entry : entries) {
            BlockState state = Block.stateById(entry.stateId());
            if (!state.isAir()) {
                blocks.add(new PreviewBlock(entry.dx(), entry.dy(), entry.dz(), state));
            }
        }

        SNAPSHOTS.put(new Key(dimension, pos.immutable()), new CachedSnapshot(new Snapshot(List.copyOf(blocks), skyColor), session.getGameTime()));
        if (SNAPSHOTS.size() > MAX_ENTRIES) {
            SNAPSHOTS.remove(SNAPSHOTS.keySet().iterator().next());
        }
    }

    public static Snapshot get(ResourceLocation dimension, BlockPos pos) {
        ensureSession();
        Key key = new Key(dimension, pos);
        CachedSnapshot cached = SNAPSHOTS.get(key);
        if (cached == null || session == null) {
            return null;
        }
        SNAPSHOTS.put(key, new CachedSnapshot(cached.snapshot(), session.getGameTime()));
        return cached.snapshot();
    }

    private static void ensureSession() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != session) {
            clear();
            session = current;
        }
    }

    public static void clear() {
        SNAPSHOTS.clear();
        REQUESTS.clear();
        GLOBAL_REQUESTS.clear();
        session = null;
        github.com.gengyoubo.CE.client.renderer.LatexPaintingPortalProjectionRenderer.clearCache();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ensureSession();
        if (session != null && session.getGameTime() % 100 == 0) {
            long cutoff = session.getGameTime() - IDLE_TICKS;
            SNAPSHOTS.values().removeIf(cached -> cached.lastAccess() < cutoff);
            REQUESTS.pruneBefore(cutoff);
        }
    }

    private record CachedSnapshot(Snapshot snapshot, long lastAccess) {
    }

    public static final class Snapshot {
        private final List<PreviewBlock> blocks;
        private final int skyColor;

        public Snapshot(List<PreviewBlock> blocks, int skyColor) {
            this.blocks = List.copyOf(blocks);
            this.skyColor = skyColor;
        }

        public List<PreviewBlock> blocks() {
            return blocks;
        }

        public int skyColor() {
            return skyColor;
        }
    }

    public record PreviewBlock(int dx, int dy, int dz, BlockState state) {
    }

    private record Key(ResourceLocation dimension, BlockPos pos) {
    }
}
