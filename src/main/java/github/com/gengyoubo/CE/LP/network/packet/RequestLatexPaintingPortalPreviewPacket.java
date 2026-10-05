package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
import github.com.gengyoubo.CE.BlockEntity.LatexPaintingPortalBlockEntity;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import github.com.gengyoubo.CE.util.BoundedRequestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "changede")
public class RequestLatexPaintingPortalPreviewPacket {
    private static final int RADIUS = 24;
    private static final int VERTICAL_ABOVE = 16;
    private static final int VERTICAL_BELOW = 12;
    private static final int MAX_BLOCKS = 4096;
    private static final int SAMPLES_PER_TICK = 2048;
    private static final int SAMPLES_PER_JOB = 256;
    private static final int MAX_PENDING_JOBS = 32;
    private static final double PORTAL_HALF_WIDTH = 1.5D;
    private static final double PORTAL_HALF_HEIGHT = 1.5D;
    private static final double HORIZONTAL_VIEW_SPREAD = 0.95D;
    private static final double VERTICAL_VIEW_SPREAD = 0.68D;
    private static final List<BlockPos> COLUMNS = makeColumns();
    private static final ArrayDeque<PreviewJob> JOBS = new ArrayDeque<>();
    private static final BoundedRequestTracker<UUID> PLAYER_REQUESTS = new BoundedRequestTracker<>(2, 1024);
    private static final BoundedRequestTracker<RequestKey> PORTAL_REQUESTS = new BoundedRequestTracker<>(200, 4096);

    private final BlockPos portalPos;
    private final int portalEntityId;

    public RequestLatexPaintingPortalPreviewPacket(BlockPos portalPos) {
        this(portalPos, -1);
    }

    public RequestLatexPaintingPortalPreviewPacket(BlockPos portalPos, int portalEntityId) {
        this.portalPos = portalPos.immutable();
        this.portalEntityId = portalEntityId;
    }

    public static void encode(RequestLatexPaintingPortalPreviewPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.portalPos);
        buffer.writeVarInt(packet.portalEntityId);
    }

    public static RequestLatexPaintingPortalPreviewPacket decode(FriendlyByteBuf buffer) {
        return new RequestLatexPaintingPortalPreviewPacket(buffer.readBlockPos(), buffer.readVarInt());
    }

    public static void handle(RequestLatexPaintingPortalPreviewPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || JOBS.size() >= MAX_PENDING_JOBS) {
                return;
            }
            ServerLevel source = player.serverLevel();
            long now = source.getGameTime();
            if (!PLAYER_REQUESTS.shouldRequest(player.getUUID(), now)
                    || !player.blockPosition().closerThan(packet.portalPos, 128.0D)
                    || !hasPortalSource(source, packet.portalPos, packet.portalEntityId)) {
                return;
            }
            RequestKey key = new RequestKey(player.getUUID(), source.dimension().location(), packet.portalPos, packet.portalEntityId);
            if (!PORTAL_REQUESTS.shouldRequest(key, now)) {
                return;
            }

            PortalTarget target = findPortalTarget(source, packet.portalPos, packet.portalEntityId);
            ServerLevel destination = target == null ? LatexPaintingPortalBlock.getDestinationLevel(source) : target.level();
            if (destination == null) {
                return;
            }
            BlockPos center;
            Direction facing = target == null ? null : target.viewFacing();
            if (target == null) {
                var chunk = destination.getChunkSource().getChunkNow(packet.portalPos.getX() >> 4, packet.portalPos.getZ() >> 4);
                int y = chunk == null ? packet.portalPos.getY()
                        : chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, packet.portalPos.getX(), packet.portalPos.getZ()) + 1;
                center = new BlockPos(packet.portalPos.getX(), y, packet.portalPos.getZ());
            } else {
                Direction side = target.sideFacing().getAxis().isHorizontal() ? target.sideFacing() : Direction.NORTH;
                center = target.pos().relative(side, 2);
            }
            if (facing != null && !facing.getAxis().isHorizontal()) {
                facing = Direction.NORTH;
            }
            JOBS.addLast(new PreviewJob(player, source, destination, packet.portalPos, packet.portalEntityId, center, facing, now));
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        int budget = SAMPLES_PER_TICK;
        // Rotate jobs fairly; the budget is global, independent of players or portal count.
        while (budget > 0 && !JOBS.isEmpty()) {
            PreviewJob job = JOBS.removeFirst();
            if (!job.isValid()) {
                continue;
            }
            budget -= job.step(Math.min(SAMPLES_PER_JOB, budget));
            if (job.isComplete()) {
                job.send();
            } else {
                JOBS.addLast(job);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        JOBS.clear();
        PLAYER_REQUESTS.clear();
        PORTAL_REQUESTS.clear();
    }

    private static boolean hasPortalSource(ServerLevel level, BlockPos pos, int entityId) {
        if (entityId >= 0) {
            return level.getEntity(entityId) instanceof LatexPaintingPortalEntity portal
                    && !portal.isRemoved() && portal.blockPosition().equals(pos);
        }
        return entityId == -1 && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                && level.getBlockEntity(pos) instanceof LatexPaintingPortalBlockEntity;
    }

    private static PortalTarget findPortalTarget(ServerLevel sourceLevel, BlockPos pos, int entityId) {
        LatexPaintingPortalEntity portal = null;
        if (entityId >= 0 && sourceLevel.getEntity(entityId) instanceof LatexPaintingPortalEntity entityPortal) {
            portal = entityPortal;
        }

        if (portal == null) {
            AABB area = new AABB(pos).inflate(4.0D);
            List<LatexPaintingPortalEntity> portals = sourceLevel.getEntitiesOfClass(LatexPaintingPortalEntity.class, area);
            if (!portals.isEmpty()) {
                portal = portals.get(0);
            }
        }

        if (portal == null) {
            return null;
        }

        ServerLevel targetLevel = sourceLevel.getServer().getLevel(portal.getTargetDimension());
        if (targetLevel == null) {
            return null;
        }

        LatexPaintingPortalEntity targetPortal = findNearestPortal(targetLevel, portal.getTargetPos());
        BlockPos targetPos = targetPortal == null ? portal.getTargetPos() : targetPortal.blockPosition();
        Direction sideFacing = targetPortal == null ? portal.getTargetFacing() : targetPortal.getFacing();
        return new PortalTarget(targetLevel, targetPos, portal.getTargetFacing(), sideFacing);
    }

    private static @Nullable LatexPaintingPortalEntity findNearestPortal(ServerLevel level, BlockPos center) {
        AABB area = new AABB(
                center.getX() + 0.5D - 16.0D,
                level.getMinBuildHeight(),
                center.getZ() + 0.5D - 16.0D,
                center.getX() + 0.5D + 16.0D,
                level.getMaxBuildHeight(),
                center.getZ() + 0.5D + 16.0D
        );
        List<LatexPaintingPortalEntity> portals = level.getEntitiesOfClass(
                LatexPaintingPortalEntity.class,
                area,
                portal -> !portal.isRemoved() && horizontalDistanceSqr(portal, center) <= 16.0D * 16.0D
        );
        return portals.stream()
                .min(Comparator.comparingDouble(portal -> horizontalDistanceSqr(portal, center)))
                .orElse(null);
    }

    private static double horizontalDistanceSqr(LatexPaintingPortalEntity portal, BlockPos center) {
        double dx = portal.getX() - (center.getX() + 0.5D);
        double dz = portal.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }


    private static List<BlockPos> makeColumns() {
        List<BlockPos> columns = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                columns.add(new BlockPos(x, 0, z));
            }
        }
        columns.sort(Comparator.comparingInt(pos -> pos.getX() * pos.getX() + pos.getZ() * pos.getZ()));
        return List.copyOf(columns);
    }

    private static boolean isInsidePortalView(int x, int y, int depth) {
        return depth >= 1 && Math.abs(x) <= PORTAL_HALF_WIDTH + depth * HORIZONTAL_VIEW_SPREAD
                && Math.abs(y) <= PORTAL_HALF_HEIGHT + depth * VERTICAL_VIEW_SPREAD;
    }

    private static boolean isExposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            var chunk = level.getChunkSource().getChunkNow(neighborPos.getX() >> 4, neighborPos.getZ() >> 4);
            if (chunk == null || level.isOutsideBuildHeight(neighborPos)) {
                return true;
            }
            BlockState neighbor = chunk.getBlockState(neighborPos);
            if (neighbor.isAir() || neighbor.getRenderShape() == RenderShape.INVISIBLE
                    || !neighbor.getFluidState().isEmpty() || !neighbor.isCollisionShapeFullBlock(level, neighborPos)) {
                return true;
            }
        }
        return false;
    }

    private static int skyColorFor(ServerLevel level, BlockPos center) {
        ResourceLocation biome = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getKey(level.getBiome(center).value());
        if (biome != null) {
            String id = biome.toString();
            if (id.contains("dark_latex")) {
                return 0x303030;
            }
            if (id.contains("white_latex")) {
                return 0xF8F8F8;
            }
        }

        return 0x9DB7D9;
    }


    private static final class PreviewJob {
        private final ServerPlayer player;
        private final ServerLevel source;
        private final ServerLevel destination;
        private final BlockPos portalPos;
        private final int entityId;
        private final BlockPos center;
        private final Direction facing;
        private final long started;
        private final List<LatexPaintingPortalPreviewPacket.Entry> entries = new ArrayList<>();
        private int column;
        private int y = Integer.MIN_VALUE;

        private PreviewJob(ServerPlayer player, ServerLevel source, ServerLevel destination, BlockPos portalPos,
                           int entityId, BlockPos center, @Nullable Direction facing, long started) {
            this.player = player;
            this.source = source;
            this.destination = destination;
            this.portalPos = portalPos;
            this.entityId = entityId;
            this.center = center;
            this.facing = facing;
            this.started = started;
        }

        private boolean isValid() {
            return source.getServer().getPlayerList().getPlayer(player.getUUID()) == player
                    && player.serverLevel() == source
                    && player.blockPosition().closerThan(portalPos, 128.0D)
                    && hasPortalSource(source, portalPos, entityId);
        }

        private int step(int budget) {
            int work = 0;
            int minY = Math.max(destination.getMinBuildHeight(), center.getY() - VERTICAL_BELOW);
            int maxY = Math.min(destination.getMaxBuildHeight() - 1, center.getY() + VERTICAL_ABOVE);
            while (work < budget && !isComplete()) {
                // Skipped/unloaded columns also consume budget, so empty terrain cannot bypass the limit.
                work++;
                BlockPos offset = COLUMNS.get(column);
                int dx = offset.getX();
                int dz = offset.getZ();
                int localX = facing == null ? dx : dx * facing.getClockWise().getStepX() + dz * facing.getClockWise().getStepZ();
                int depth = facing == null ? dz : dx * facing.getStepX() + dz * facing.getStepZ();
                int worldX = center.getX() + dx;
                int worldZ = center.getZ() + dz;
                var chunk = destination.getChunkSource().getChunkNow(worldX >> 4, worldZ >> 4);
                if (chunk == null || minY > maxY || (facing != null && (depth < 1
                        || Math.abs(localX) > PORTAL_HALF_WIDTH + depth * HORIZONTAL_VIEW_SPREAD))) {
                    column++;
                    y = Integer.MIN_VALUE;
                    continue;
                }
                if (y == Integer.MIN_VALUE) {
                    y = maxY;
                }
                BlockPos sample = new BlockPos(worldX, y, worldZ);
                int localY = y - center.getY();
                if (facing == null || isInsidePortalView(localX, localY, depth)) {
                    BlockState state = chunk.getBlockState(sample);
                    if (!state.isAir() && state.getRenderShape() == RenderShape.MODEL && isExposed(destination, sample)) {
                        entries.add(new LatexPaintingPortalPreviewPacket.Entry(localX, localY, depth, Block.getId(state)));
                    }
                }
                if (--y < minY) {
                    column++;
                    y = Integer.MIN_VALUE;
                }
            }
            return work;
        }

        private boolean isComplete() {
            return column >= COLUMNS.size() || entries.size() >= MAX_BLOCKS || source.getGameTime() - started >= 200;
        }

        private void send() {
            int sky = destination.getChunkSource().getChunkNow(center.getX() >> 4, center.getZ() >> 4) == null
                    ? 0x9DB7D9 : skyColorFor(destination, center);
            CENetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new LatexPaintingPortalPreviewPacket(source.dimension().location(), portalPos, sky, entries));
        }
    }

    private record RequestKey(UUID player, ResourceLocation dimension, BlockPos pos, int entityId) {
    }

    private record PortalTarget(ServerLevel level, BlockPos pos, Direction viewFacing, Direction sideFacing) {
    }
}
