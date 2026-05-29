package github.com.gengyoubo.CE.Block;

import github.com.gengyoubo.CE.BlockEntity.LatexPaintingPortalBlockEntity;
import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import github.com.gengyoubo.CE.init.CEBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class LatexPaintingPortalBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final ResourceKey<Level> LATEX_SPACE = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("changede", "latex_space")
    );

    private static final String COOLDOWN_TAG = "changede_latex_painting_portal_cooldown";
    private static final int PORTAL_COOLDOWN_TICKS = 60;
    private static final double RETURN_PORTAL_SEARCH_RADIUS = 16.0D;
    private static final int SAFE_TARGET_SEARCH_RADIUS = 8;
    private static final int SURFACE_SEARCH_UP = 3;
    private static final int SURFACE_SEARCH_DOWN = 2;
    private static final VoxelShape NORTH_SOUTH_SHAPE = box(0.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);
    private static final VoxelShape EAST_WEST_SHAPE = box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 16.0D);

    public LatexPaintingPortalBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.5F, 6.0F).noCollission().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel sourceLevel)) {
            return;
        }

        long now = sourceLevel.getGameTime();
        if (player.getPersistentData().getLong(COOLDOWN_TAG) > now) {
            return;
        }

        teleportPlayer(player, sourceLevel, player.blockPosition());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LatexPaintingPortalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            @NotNull Level level,
            @NotNull BlockState state,
            @NotNull net.minecraft.world.level.block.entity.BlockEntityType<T> type
    ) {
        if (!level.isClientSide || type != CEBlockEntity.LATEX_PAINTING_PORTAL.get()) {
            return null;
        }

        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof LatexPaintingPortalBlockEntity portal) {
                portal.clientTick();
            }
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    public static @Nullable ServerLevel getDestinationLevel(ServerLevel sourceLevel) {
        if (sourceLevel.dimension() == LATEX_SPACE) {
            return sourceLevel.getServer().overworld();
        }

        return sourceLevel.getServer().getLevel(LATEX_SPACE);
    }

    public static boolean teleportPlayer(ServerPlayer player, ServerLevel sourceLevel, BlockPos origin) {
        long now = sourceLevel.getGameTime();
        if (player.getPersistentData().getLong(COOLDOWN_TAG) > now) {
            return false;
        }

        ServerLevel destination = getDestinationLevel(sourceLevel);
        if (destination == null) {
            return false;
        }

        BlockPos target = findSafeTarget(destination, origin);
        player.getPersistentData().putLong(COOLDOWN_TAG, destination.getGameTime() + PORTAL_COOLDOWN_TICKS);
        player.teleportTo(destination, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, player.getYRot(), player.getXRot());
        return true;
    }

    public static boolean teleportPlayerToPortal(ServerPlayer player, ServerLevel sourceLevel, ResourceKey<Level> targetDimension,
                                                 BlockPos targetPortalPos, Direction targetFacing) {
        long now = sourceLevel.getGameTime();
        if (player.getPersistentData().getLong(COOLDOWN_TAG) > now) {
            return false;
        }

        ServerLevel destination = sourceLevel.getServer().getLevel(targetDimension);
        if (destination == null) {
            return false;
        }

        BlockPos exitPortalPos = targetPortalPos;
        Direction exitSideFacing = targetFacing;
        LatexPaintingPortalEntity nearestPortal = findNearestPortal(destination, targetPortalPos);
        if (nearestPortal != null) {
            exitPortalPos = nearestPortal.blockPosition();
            exitSideFacing = nearestPortal.getFacing();
        }

        BlockPos target = findExitNearPortal(destination, exitPortalPos, exitSideFacing);
        player.getPersistentData().putLong(COOLDOWN_TAG, destination.getGameTime() + PORTAL_COOLDOWN_TICKS);
        player.teleportTo(
                destination,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                normalizeYaw(player.getYRot() + 180.0F),
                player.getXRot()
        );
        return true;
    }

    public static BlockPos findSafeTarget(ServerLevel destination, BlockPos origin) {
        return findTarget(destination, origin, true);
    }

    public static BlockPos findPreviewTarget(ServerLevel destination, BlockPos origin) {
        return findTarget(destination, origin, false);
    }

    public static BlockPos findPortalPreviewCenter(ServerLevel destination, BlockPos portalPos, Direction facing) {
        return findExitNearPortal(destination, portalPos, facing, false);
    }

    private static BlockPos findExitNearPortal(ServerLevel destination, BlockPos portalPos, Direction facing) {
        return findExitNearPortal(destination, portalPos, facing, true);
    }

    private static BlockPos findExitNearPortal(ServerLevel destination, BlockPos portalPos, Direction facing, boolean makeFallbackSafe) {
        Direction horizontal = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        BlockPos base = portalPos.relative(horizontal, 2);
        for (int forward = 2; forward <= 4; forward++) {
            for (int side = -1; side <= 1; side++) {
                for (int dy = 1; dy >= -3; dy--) {
                    BlockPos candidate = portalPos
                            .relative(horizontal, forward)
                            .relative(horizontal.getClockWise(), side)
                            .offset(0, dy, 0);
                    if (isSafeTarget(destination, candidate)) {
                        return candidate;
                    }
                }
            }
        }

        if (makeFallbackSafe && !isSafeTarget(destination, base)) {
            makeEmergencyLandingSpot(destination, base);
        }
        return base;
    }

    private static float normalizeYaw(float yaw) {
        while (yaw <= -180.0F) {
            yaw += 360.0F;
        }
        while (yaw > 180.0F) {
            yaw -= 360.0F;
        }
        return yaw;
    }

    private static @Nullable LatexPaintingPortalEntity findNearestPortal(ServerLevel level, BlockPos center) {
        List<LatexPaintingPortalEntity> portals = level.getEntitiesOfClass(
                LatexPaintingPortalEntity.class,
                horizontalSearchArea(level, center, RETURN_PORTAL_SEARCH_RADIUS),
                portal -> !portal.isRemoved() && isWithinHorizontalRadius(portal, center, RETURN_PORTAL_SEARCH_RADIUS)
        );
        return portals.stream()
                .min(Comparator.comparingDouble(portal -> horizontalDistanceSqr(portal, center)))
                .orElse(null);
    }

    private static net.minecraft.world.phys.AABB horizontalSearchArea(ServerLevel level, BlockPos center, double radius) {
        return new net.minecraft.world.phys.AABB(
                center.getX() + 0.5D - radius,
                level.getMinBuildHeight(),
                center.getZ() + 0.5D - radius,
                center.getX() + 0.5D + radius,
                level.getMaxBuildHeight(),
                center.getZ() + 0.5D + radius
        );
    }

    private static boolean isWithinHorizontalRadius(LatexPaintingPortalEntity portal, BlockPos center, double radius) {
        return horizontalDistanceSqr(portal, center) <= radius * radius;
    }

    private static double horizontalDistanceSqr(LatexPaintingPortalEntity portal, BlockPos center) {
        double dx = portal.getX() - (center.getX() + 0.5D);
        double dz = portal.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static BlockPos findTarget(ServerLevel destination, BlockPos origin, boolean makeFallbackSafe) {
        destination.getChunk(origin);
        for (int radius = 0; radius <= SAFE_TARGET_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    destination.getChunk(x >> 4, z >> 4);
                    int surfaceY = destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    for (int dy = SURFACE_SEARCH_UP; dy >= -SURFACE_SEARCH_DOWN; dy--) {
                        int y = Math.max(destination.getMinBuildHeight() + 1, Math.min(surfaceY + dy, destination.getMaxBuildHeight() - 2));
                        BlockPos candidate = new BlockPos(x, y, z);
                        if (isSafeTarget(destination, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        int y = destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        BlockPos fallback = new BlockPos(origin.getX(), Math.max(destination.getMinBuildHeight() + 2, y), origin.getZ());
        if (makeFallbackSafe) {
            makeEmergencyLandingSpot(destination, fallback);
        }
        return fallback;
    }

    private static boolean isSafeTarget(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        return below.isFaceSturdy(level, pos.below(), Direction.UP)
                && feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && feet.getFluidState().isEmpty()
                && head.getFluidState().isEmpty();
    }

    private static void makeEmergencyLandingSpot(ServerLevel level, BlockPos pos) {
        level.setBlock(pos.below(), Blocks.OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
    }
}
