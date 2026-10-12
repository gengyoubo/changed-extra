package github.com.gengyoubo.CE.compat.twilight;

import net.foxyas.changedaddon.entity.bosses.Experiment009BossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Collision-safe destinations for the castle encounter's native teleport skill. */
public final class TwilightFinalBossMovement {
    private static final int[] HEIGHT_OFFSETS = {0, 1, -1, 2, -2};
    private TwilightFinalBossMovement() {}

    public static void teleportNear(Experiment009BossEntity boss, LivingEntity target) {
        if (!(boss.level() instanceof ServerLevel level) || !boss.isAlive() || target == null
                || target.level() != level) return;
        Vec3 destination = findDestination(level, boss, target.position());
        if (destination == null) return;
        move(boss, destination);
        boss.getLookControl().setLookAt(target, 180, 180);
        if (boss.distanceToSqr(target) <= 16) target.hurt(boss.getThunderDmg(), 2);
    }

    public static void tick(Experiment009BossEntity boss) {
        if (!(boss.level() instanceof ServerLevel level) || !TwilightFinalEncounter.isFinal(boss)
                || !boss.isAlive() || boss.tickCount % 10 != 0) return;
        // Entity overlap and ordinary contact with a wall are not an embedded body.
        if (!level.getBlockCollisions(boss, boss.getBoundingBox().deflate(1.0E-5)).iterator().hasNext()) return;
        Vec3 destination = findDestination(level, boss, boss.position());
        if (destination != null) move(boss, destination);
    }

    private static Vec3 findDestination(ServerLevel level, Experiment009BossEntity boss, Vec3 preferred) {
        BlockPos home = BlockPos.of(boss.getPersistentData().getLong(TwilightFinalBossCompat.HOME));
        BlockPos origin = new BlockPos(Math.max(home.getX() - 19, Math.min(home.getX() + 19, (int) Math.floor(preferred.x))),
                Math.max(home.getY(), Math.min(home.getY() + 21, (int) Math.floor(preferred.y))),
                Math.max(home.getZ() - 19, Math.min(home.getZ() + 19, (int) Math.floor(preferred.z))));
        Vec3 nearby = search(level, boss, home, origin, 6);
        return nearby != null ? nearby : search(level, boss, home, home, 12);
    }

    private static Vec3 search(ServerLevel level, Experiment009BossEntity boss, BlockPos home, BlockPos origin, int radius) {
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) for (int dz = -ring; dz <= ring; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                for (int dy : HEIGHT_OFFSETS) {
                    BlockPos feet = origin.offset(dx, dy, dz);
                    Vec3 destination = Vec3.atBottomCenterOf(feet);
                    AABB box = boss.getBoundingBox().move(destination.subtract(boss.position())).deflate(1.0E-5);
                    // Keep the complete body inside the roof arena, with room below its ceiling.
                    if (box.minX < home.getX() - 20 || box.maxX > home.getX() + 21
                            || box.minZ < home.getZ() - 20 || box.maxZ > home.getZ() + 21
                            || box.minY < home.getY() || box.maxY > home.getY() + 24) continue;
                    if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 1, box.minZ),
                            BlockPos.containing(box.maxX, box.maxY, box.maxZ)) || !level.getWorldBorder().isWithinBounds(box)) continue;
                    BlockPos support = feet.below();
                    if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
                            || level.containsAnyLiquid(box) || !level.noCollision(boss, box)) continue;
                    return destination;
                }
            }
        }
        return null;
    }

    private static void move(Experiment009BossEntity boss, Vec3 destination) {
        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
        boss.fallDistance = 0;
        boss.teleportTo(destination.x, destination.y, destination.z);
        boss.hasImpulse = true;
    }
}
