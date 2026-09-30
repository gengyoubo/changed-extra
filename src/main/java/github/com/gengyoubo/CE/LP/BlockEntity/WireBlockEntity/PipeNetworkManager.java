package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Caches pipe-network membership and elects one pipe to transfer for the network each tick. */
final class PipeNetworkManager {
    private static final Map<Level, Map<TransportType, Map<BlockPos, BlockPos>>> LEADERS = new WeakHashMap<>();

    private PipeNetworkManager() { }

    static synchronized boolean isLeader(BasePipeBlockEntity pipe) {
        Level level = pipe.getLevel();
        if (level == null) return false;
        Map<TransportType, Map<BlockPos, BlockPos>> byType = LEADERS.computeIfAbsent(level,
                ignored -> new EnumMap<>(TransportType.class));
        Map<BlockPos, BlockPos> leaders = byType.computeIfAbsent(pipe.getTransportType(), ignored -> new HashMap<>());
        BlockPos position = pipe.getBlockPos();
        BlockPos leader = leaders.get(position);
        if (leader == null) {
            leader = discoverNetwork(pipe, leaders);
        }
        return position.equals(leader);
    }

    private static BlockPos discoverNetwork(BasePipeBlockEntity start, Map<BlockPos, BlockPos> leaders) {
        Level level = start.getLevel();
        TransportType type = start.getTransportType();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos first = start.getBlockPos();
        queue.add(first);
        visited.add(first);
        BlockPos leader = first;
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.removeFirst();
            BlockEntity entity = level.getBlockEntity(currentPos);
            if (!(entity instanceof BasePipeBlockEntity currentPipe) || currentPipe.getTransportType() != type) continue;
            if (currentPos.compareTo(leader) < 0) leader = currentPos;
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(direction);
                BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
                if (neighborEntity instanceof BasePipeBlockEntity neighborPipe
                        && neighborPipe.getTransportType() == type
                        && currentPipe.canConnectToPipe(neighborPipe, direction)
                        && neighborPipe.canConnectToPipe(currentPipe, direction.getOpposite())
                        && visited.add(neighborPos)) {
                    queue.addLast(neighborPos);
                }
            }
        }
        for (BlockPos member : visited) leaders.put(member, leader);
        return leader;
    }

    static synchronized void invalidate(Level level) {
        if (level != null) LEADERS.remove(level);
    }
}
