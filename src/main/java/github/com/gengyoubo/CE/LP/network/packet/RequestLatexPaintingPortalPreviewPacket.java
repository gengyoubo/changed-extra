package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
import github.com.gengyoubo.CE.BlockEntity.LatexPaintingPortalBlockEntity;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.changede;
import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class RequestLatexPaintingPortalPreviewPacket {
    private static final int RADIUS = 48;
    private static final int VERTICAL_ABOVE = 40;
    private static final int VERTICAL_BELOW = 24;
    private static final int MAX_BLOCKS = 32760;
    private static final double PORTAL_HALF_WIDTH = 1.5D;
    private static final double PORTAL_HALF_HEIGHT = 1.5D;
    private static final double HORIZONTAL_VIEW_SPREAD = 0.95D;
    private static final double VERTICAL_VIEW_SPREAD = 0.68D;

    private final BlockPos portalPos;
    private final int portalEntityId;

    public RequestLatexPaintingPortalPreviewPacket(BlockPos portalPos) {
        this(portalPos, -1);
    }

    public RequestLatexPaintingPortalPreviewPacket(BlockPos portalPos, int portalEntityId) {
        this.portalPos = portalPos;
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
            if (player == null) {
                changede.LOGGER.warn("Ignoring latex painting portal preview request without a server player");
                return;
            }

            if (!player.blockPosition().closerThan(packet.portalPos, 128.0D)) {
                changede.LOGGER.warn("Ignoring latex painting portal preview request from {} for distant portal at {}", player.getGameProfile().getName(), packet.portalPos);
                return;
            }

            if (!(player.level() instanceof ServerLevel sourceLevel)) {
                changede.LOGGER.warn("Ignoring latex painting portal preview request at {} because player level is not a ServerLevel", packet.portalPos);
                return;
            }

            if (!hasPortalSource(sourceLevel, packet.portalPos, packet.portalEntityId)) {
                changede.LOGGER.warn(
                        "Ignoring latex painting portal preview request at {} because no portal block entity or portal entity exists on server (entityId={})",
                        packet.portalPos,
                        packet.portalEntityId
                );
                return;
            }

            PortalTarget target = findPortalTarget(sourceLevel, packet.portalPos, packet.portalEntityId);
            ServerLevel previewLevel = target == null ? LatexPaintingPortalBlock.getDestinationLevel(sourceLevel) : target.level();
            if (previewLevel == null) {
                changede.LOGGER.warn("Ignoring latex painting portal preview request at {} because destination dimension is missing", packet.portalPos);
                return;
            }

            BlockPos center = target == null
                    ? LatexPaintingPortalBlock.findPreviewTarget(previewLevel, packet.portalPos)
                    : LatexPaintingPortalBlock.findPortalPreviewCenter(previewLevel, target.pos(), target.sideFacing());
            List<LatexPaintingPortalPreviewPacket.Entry> entries = collectPreviewBlocks(previewLevel, center, target == null ? null : target.viewFacing());
            changede.LOGGER.warn(
                    "Sending latex painting portal preview to {} from {} at {}, targetPos={}, targetFacing={}, targetSide={}, center={}, blocks={}",
                    player.getGameProfile().getName(),
                    previewLevel.dimension().location(),
                    packet.portalPos,
                    target == null ? "auto" : target.pos(),
                    target == null ? "auto" : target.viewFacing(),
                    target == null ? "auto" : target.sideFacing(),
                    center,
                    entries.size()
            );
            CENetwork.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new LatexPaintingPortalPreviewPacket(sourceLevel.dimension().location(), packet.portalPos, skyColorFor(previewLevel, center), entries)
            );
        });
        context.setPacketHandled(true);
    }

    private static boolean hasPortalSource(ServerLevel level, BlockPos pos, int entityId) {
        if (entityId >= 0 && level.getEntity(entityId) instanceof LatexPaintingPortalEntity) {
            return true;
        }

        if (level.getBlockEntity(pos) instanceof LatexPaintingPortalBlockEntity) {
            return true;
        }

        AABB area = new AABB(pos).inflate(4.0D);
        return !level.getEntitiesOfClass(LatexPaintingPortalEntity.class, area).isEmpty();
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

    private static List<LatexPaintingPortalPreviewPacket.Entry> collectPreviewBlocks(ServerLevel level, BlockPos center, @Nullable Direction facing) {
        for (int chunkX = (center.getX() - RADIUS) >> 4; chunkX <= (center.getX() + RADIUS) >> 4; chunkX++) {
            for (int chunkZ = (center.getZ() - RADIUS) >> 4; chunkZ <= (center.getZ() + RADIUS) >> 4; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }

        List<LatexPaintingPortalPreviewPacket.Entry> entries = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (entries.size() >= MAX_BLOCKS) {
                    return entries;
                }

                addVisibleColumnBlocks(entries, level, center, dx, dz, facing);
            }
        }

        if (entries.isEmpty()) {
            addDebugFallbackPreview(entries);
        }
        return entries;
    }

    private static void addVisibleColumnBlocks(List<LatexPaintingPortalPreviewPacket.Entry> entries, ServerLevel level, BlockPos center, int dx, int dz,
                                               @Nullable Direction facing) {
        int encodedX = dx;
        int encodedZ = dz;
        if (facing != null) {
            int forward = dx * facing.getStepX() + dz * facing.getStepZ();
            if (forward < 1) {
                return;
            }

            Direction right = facing.getClockWise();
            encodedX = dx * right.getStepX() + dz * right.getStepZ();
            encodedZ = forward;
        }

        int x = center.getX() + dx;
        int z = center.getZ() + dz;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - VERTICAL_BELOW);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + VERTICAL_ABOVE);

        for (int y = maxY; y >= minY; y--) {
            if (entries.size() >= MAX_BLOCKS) {
                return;
            }

            BlockPos sample = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(sample);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            if (!isExposed(level, sample)) {
                continue;
            }

            int encodedY = sample.getY() - center.getY();
            if (facing != null && !isInsidePortalView(encodedX, encodedY, encodedZ)) {
                continue;
            }

            entries.add(new LatexPaintingPortalPreviewPacket.Entry(
                    (byte) encodedX,
                    (byte) encodedY,
                    (byte) encodedZ,
                    Block.getId(state)
            ));
        }
    }

    private static boolean isInsidePortalView(int localX, int localY, int depth) {
        double horizontalLimit = PORTAL_HALF_WIDTH + depth * HORIZONTAL_VIEW_SPREAD;
        double verticalLimit = PORTAL_HALF_HEIGHT + depth * VERTICAL_VIEW_SPREAD;
        return Math.abs(localX) <= horizontalLimit && Math.abs(localY) <= verticalLimit;
    }

    private static boolean isExposed(ServerLevel level, BlockPos pos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.isAir()
                    || neighbor.getRenderShape() == RenderShape.INVISIBLE
                    || !neighbor.getFluidState().isEmpty()
                    || !neighbor.isCollisionShapeFullBlock(level, neighborPos)) {
                return true;
            }
        }

        return false;
    }

    private static void addDebugFallbackPreview(List<LatexPaintingPortalPreviewPacket.Entry> entries) {
        BlockState dark = safeBlockState("dark_latex_block", Blocks.BLACK_CONCRETE);
        BlockState white = safeBlockState("white_latex_block", Blocks.WHITE_CONCRETE);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockState state = ((x + z) & 1) == 0 ? dark : white;
                entries.add(new LatexPaintingPortalPreviewPacket.Entry((byte) x, (byte) 0, (byte) z, Block.getId(state)));
            }
        }
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

    private static BlockState safeBlockState(String path, Block fallback) {
        Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("changed", path));
        return (block == null ? fallback : block).defaultBlockState();
    }

    private record PortalTarget(ServerLevel level, BlockPos pos, Direction viewFacing, Direction sideFacing) {
    }
}
