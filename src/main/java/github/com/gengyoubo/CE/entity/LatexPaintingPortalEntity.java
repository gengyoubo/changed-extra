package github.com.gengyoubo.CE.entity;

import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
import github.com.gengyoubo.CE.init.CEItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LatexPaintingPortalEntity extends Entity {
    public static final double PORTAL_WIDTH = 3.0D;
    public static final double PORTAL_HEIGHT = 3.0D;
    private static final double WALL_OFFSET = 0.51D;
    private static final double PORTAL_THICKNESS = 0.10D;

    private static final EntityDataAccessor<Integer> FACING =
            SynchedEntityData.defineId(LatexPaintingPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TARGET_DIMENSION =
            SynchedEntityData.defineId(LatexPaintingPortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<BlockPos> TARGET_POS =
            SynchedEntityData.defineId(LatexPaintingPortalEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> TARGET_FACING =
            SynchedEntityData.defineId(LatexPaintingPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RENDER_REVERSED =
            SynchedEntityData.defineId(LatexPaintingPortalEntity.class, EntityDataSerializers.BOOLEAN);

    public LatexPaintingPortalEntity(EntityType<? extends LatexPaintingPortalEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public LatexPaintingPortalEntity(EntityType<? extends LatexPaintingPortalEntity> type, Level level, BlockPos pos, Direction facing) {
        this(type, level);
        setFacing(facing);
        setPos(
                pos.getX() + 0.5D + facing.getStepX() * WALL_OFFSET,
                pos.getY() + PORTAL_HEIGHT / 2.0D,
                pos.getZ() + 0.5D + facing.getStepZ() * WALL_OFFSET
        );
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FACING, Direction.NORTH.get2DDataValue());
        entityData.define(TARGET_DIMENSION, LatexPaintingPortalBlock.LATEX_SPACE.location().toString());
        entityData.define(TARGET_POS, BlockPos.ZERO);
        entityData.define(TARGET_FACING, Direction.SOUTH.get2DDataValue());
        entityData.define(RENDER_REVERSED, false);
    }

    public Direction getFacing() {
        return Direction.from2DDataValue(entityData.get(FACING));
    }

    public void setFacing(Direction facing) {
        Direction horizontal = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        entityData.set(FACING, horizontal.get2DDataValue());
        setYRot(horizontal.toYRot());
        yRotO = getYRot();
    }

    public ResourceKey<Level> getTargetDimension() {
        ResourceLocation location = ResourceLocation.tryParse(entityData.get(TARGET_DIMENSION));
        if (location == null) {
            return LatexPaintingPortalBlock.LATEX_SPACE;
        }

        return ResourceKey.create(Registries.DIMENSION, location);
    }

    public BlockPos getTargetPos() {
        BlockPos target = entityData.get(TARGET_POS);
        return target.equals(BlockPos.ZERO) ? blockPosition() : target;
    }

    public Direction getTargetFacing() {
        return Direction.from2DDataValue(entityData.get(TARGET_FACING));
    }

    public void setTarget(ResourceKey<Level> dimension, BlockPos pos, Direction facing) {
        entityData.set(TARGET_DIMENSION, dimension.location().toString());
        entityData.set(TARGET_POS, pos.immutable());
        entityData.set(TARGET_FACING, (facing.getAxis().isHorizontal() ? facing : Direction.SOUTH).get2DDataValue());
    }

    public boolean isRenderReversed() {
        return entityData.get(RENDER_REVERSED);
    }

    public void setRenderReversed(boolean renderReversed) {
        entityData.set(RENDER_REVERSED, renderReversed);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);

        if (!(level() instanceof ServerLevel sourceLevel)) {
            return;
        }

        AABB portalArea = getPortalArea();
        List<ServerPlayer> players = sourceLevel.getEntitiesOfClass(ServerPlayer.class, portalArea);
        for (ServerPlayer player : players) {
            LatexPaintingPortalBlock.teleportPlayerToPortal(player, sourceLevel, getTargetDimension(), getTargetPos(), getTargetFacing());
        }
    }

    public AABB getPortalArea() {
        Direction facing = getFacing();
        double halfWidth = PORTAL_WIDTH / 2.0D;
        double halfHeight = PORTAL_HEIGHT / 2.0D;
        double halfThickness = PORTAL_THICKNESS / 2.0D;
        if (facing.getAxis() == Direction.Axis.X) {
            return new AABB(
                    getX() - halfThickness,
                    getY() - halfHeight,
                    getZ() - halfWidth,
                    getX() + halfThickness,
                    getY() + halfHeight,
                    getZ() + halfWidth
            );
        }

        return new AABB(
                getX() - halfWidth,
                getY() - halfHeight,
                getZ() - halfThickness,
                getX() + halfWidth,
                getY() + halfHeight,
                getZ() + halfThickness
        );
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }

        Entity attacker = source.getEntity();
        if (!(attacker instanceof net.minecraft.world.entity.player.Player player) || !player.getAbilities().instabuild) {
            level().addFreshEntity(new ItemEntity(
                    level(),
                    getX(),
                    getY(),
                    getZ(),
                    new ItemStack(CEItem.LATEX_PAINTING_PORTAL.get())
            ));
        }
        discardLinkedPortal();
        discard();
        return true;
    }

    private void discardLinkedPortal() {
        if (!(level() instanceof ServerLevel sourceLevel)) {
            return;
        }

        ServerLevel targetLevel = sourceLevel.getServer().getLevel(getTargetDimension());
        if (targetLevel == null) {
            return;
        }

        AABB searchArea = new AABB(getTargetPos()).inflate(4.0D);
        List<LatexPaintingPortalEntity> linked = targetLevel.getEntitiesOfClass(LatexPaintingPortalEntity.class, searchArea);
        for (LatexPaintingPortalEntity portal : linked) {
            if (portal == this) {
                continue;
            }
            if (portal.getTargetDimension().equals(sourceLevel.dimension()) && portal.getTargetPos().closerToCenterThan(position(), 5.0D)) {
                portal.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        setFacing(Direction.from2DDataValue(tag.getInt("Facing")));
        if (tag.contains("TargetDimension")) {
            entityData.set(TARGET_DIMENSION, tag.getString("TargetDimension"));
        }
        if (tag.contains("TargetX") && tag.contains("TargetY") && tag.contains("TargetZ")) {
            entityData.set(TARGET_POS, new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ")));
        }
        if (tag.contains("TargetFacing")) {
            entityData.set(TARGET_FACING, Direction.from2DDataValue(tag.getInt("TargetFacing")).get2DDataValue());
        }
        setRenderReversed(tag.getBoolean("RenderReversed"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Facing", getFacing().get2DDataValue());
        tag.putString("TargetDimension", entityData.get(TARGET_DIMENSION));
        BlockPos target = getTargetPos();
        tag.putInt("TargetX", target.getX());
        tag.putInt("TargetY", target.getY());
        tag.putInt("TargetZ", target.getZ());
        tag.putInt("TargetFacing", getTargetFacing().get2DDataValue());
        tag.putBoolean("RenderReversed", isRenderReversed());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
