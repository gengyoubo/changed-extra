package github.com.gengyoubo.CE.LP.energy;

import java.util.LinkedHashSet;
import java.util.Set;

/** The smallest rectangular chunk footprint covering the tower and its adjacent blocks. */
public final class DimensionTowerChunks {
    private DimensionTowerChunks() { }

    public record Chunk(int x, int z) { }

    public static Set<Chunk> around(int blockX, int blockZ) {
        Set<Chunk> chunks = new LinkedHashSet<>();
        for (int x = (blockX - 1) >> 4; x <= (blockX + 1) >> 4; x++) {
            for (int z = (blockZ - 1) >> 4; z <= (blockZ + 1) >> 4; z++) {
                chunks.add(new Chunk(x, z));
            }
        }
        return Set.copyOf(chunks);
    }
}
