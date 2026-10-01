package github.com.gengyoubo.CE.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

/** Bounded air connectivity search; shared results avoid repeating it for nearby entities. */
public final class WhiteFogExposure {
    private static final int MAX_CELLS = 8192;
    private static final int MAX_DISTANCE = 48;
    private static final Map<Level, Cache> CACHES = new WeakHashMap<>();

    private WhiteFogExposure() { }
    @SuppressWarnings("deprecation")
    public static boolean isExposed(Level level, BlockPos origin) {
        Cache cache;
        synchronized (CACHES) { cache = CACHES.computeIfAbsent(level, ignored -> new Cache()); }
        synchronized (cache) {
            long tick = level.getGameTime();
            if (tick != cache.tick) { cache.tick = tick; cache.results.clear(); }
            Boolean known = cache.results.get(origin);
            if (known != null) return known;
            var queue = new ArrayDeque<BlockPos>();
            var visited = new HashSet<BlockPos>();
            queue.add(origin.immutable());
            visited.add(origin.immutable());
            boolean exposed = false;
            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeFirst();
                if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)
                        || visited.size() >= MAX_CELLS || pos.distManhattan(origin) >= MAX_DISTANCE
                        || pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ())) {
                    exposed = true;
                    break;
                }
                Boolean cached = cache.results.get(pos);
                if (Boolean.TRUE.equals(cached)) { exposed = true; break; }
                for (Direction direction : Direction.values()) {
                    BlockPos next = pos.relative(direction);
                    if (!visited.contains(next) && permeable(level, pos, next, direction)) {
                        visited.add(next);
                        queue.addLast(next);
                    }
                }
            }
            if (cache.results.size() + visited.size() > 32768) cache.results.clear();
            for (BlockPos pos : visited) cache.results.put(pos, exposed);
            return exposed;
        }
    }
    @SuppressWarnings("deprecation")
    private static boolean permeable(Level level, BlockPos from, BlockPos pos, Direction direction) {
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)) return true;
        var state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) {
            if (!state.getValue(BlockStateProperties.OPEN)) return false;
        } else if (!state.is(BlockTags.LEAVES)
                && Block.isFaceFull(state.getCollisionShape(level, pos), direction.getOpposite())) {
            return false;
        }
        var previous = level.getBlockState(from);
        if (previous.getBlock() instanceof DoorBlock || previous.getBlock() instanceof TrapDoorBlock) {
            return previous.getValue(BlockStateProperties.OPEN);
        }
        return previous.is(BlockTags.LEAVES)
                || !Block.isFaceFull(previous.getCollisionShape(level, from), direction);
    }

    private static final class Cache {
        private long tick = Long.MIN_VALUE;
        private final Map<BlockPos, Boolean> results = new HashMap<>();
    }
}
