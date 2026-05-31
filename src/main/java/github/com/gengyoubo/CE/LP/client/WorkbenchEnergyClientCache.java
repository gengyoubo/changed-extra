package github.com.gengyoubo.CE.LP.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class WorkbenchEnergyClientCache {
    private static final long REQUEST_INTERVAL_TICKS = 10L;
    private static final Map<Key, Entry> ENTRIES = new HashMap<>();
    private static final Map<Key, Long> LAST_REQUEST_TICKS = new HashMap<>();

    private WorkbenchEnergyClientCache() {
    }

    public static void update(ResourceLocation dimension, BlockPos pos, int stored, int max) {
        ENTRIES.put(new Key(dimension, pos.immutable()), new Entry(stored, max));
    }

    public static Optional<Entry> get(ResourceLocation dimension, BlockPos pos) {
        return Optional.ofNullable(ENTRIES.get(new Key(dimension, pos.immutable())));
    }

    public static boolean shouldRequest(ResourceLocation dimension, BlockPos pos, long gameTime) {
        Key key = new Key(dimension, pos.immutable());
        long lastRequest = LAST_REQUEST_TICKS.getOrDefault(key, Long.MIN_VALUE);
        if (gameTime - lastRequest < REQUEST_INTERVAL_TICKS) {
            return false;
        }

        LAST_REQUEST_TICKS.put(key, gameTime);
        return true;
    }

    private record Key(ResourceLocation dimension, BlockPos pos) {
    }

    public record Entry(int stored, int max) {
    }
}
