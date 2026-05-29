package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.LP.network.packet.LatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.changede;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LatexPaintingPortalPreviewCache {
    private static final long REQUEST_INTERVAL_TICKS = 40L;
    private static final Map<Key, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final Map<Key, Long> LAST_REQUESTS = new HashMap<>();

    public static boolean shouldRequest(ResourceLocation dimension, BlockPos pos, long gameTime) {
        Key key = new Key(dimension, pos);
        Long lastRequest = LAST_REQUESTS.get(key);
        if (lastRequest != null && gameTime - lastRequest < REQUEST_INTERVAL_TICKS) {
            return false;
        }

        LAST_REQUESTS.put(key, gameTime);
        changede.LOGGER.warn("Renderer requesting latex painting portal preview for {} at {}", dimension, pos);
        return true;
    }

    public static void update(ResourceLocation dimension, BlockPos pos, int skyColor, List<LatexPaintingPortalPreviewPacket.Entry> entries) {
        List<PreviewBlock> blocks = new ArrayList<>(entries.size());
        for (LatexPaintingPortalPreviewPacket.Entry entry : entries) {
            BlockState state = Block.stateById(entry.stateId());
            if (!state.isAir()) {
                blocks.add(new PreviewBlock(entry.dx(), entry.dy(), entry.dz(), state));
            }
        }

        SNAPSHOTS.put(new Key(dimension, pos), new Snapshot(blocks, skyColor));
        changede.LOGGER.warn("Updated latex painting portal preview for {} at {}, blocks={}", dimension, pos, blocks.size());
    }

    public static Snapshot get(ResourceLocation dimension, BlockPos pos) {
        return SNAPSHOTS.get(new Key(dimension, pos));
    }

    public record Snapshot(List<PreviewBlock> blocks, int skyColor) {
    }

    public record PreviewBlock(int dx, int dy, int dz, BlockState state) {
    }

    private record Key(ResourceLocation dimension, BlockPos pos) {
    }
}
