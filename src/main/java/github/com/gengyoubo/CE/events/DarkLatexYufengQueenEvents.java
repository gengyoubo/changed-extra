package github.com.gengyoubo.CE.events;

import net.foxyas.changedaddon.entity.simple.DarkLatexYufengQueenEntity;
import net.ltxprogrammer.changed.entity.beast.AbstractDarkLatexEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class DarkLatexYufengQueenEvents {
    private static final String QUEEN_SUMMONED_TAG = "changede_dark_latex_yufeng_queen_summoned";
    private static final String SUMMON_COOLDOWN_TAG = "changede_dark_latex_yufeng_queen_summon_cooldown";
    private static final String WAS_IN_COMBAT_TAG = "changede_dark_latex_yufeng_queen_was_in_combat";
    private static final int SUMMON_INTERVAL_TICKS = 200;
    private static final double SUMMON_LIMIT_RADIUS = 16.0D;
    private static final int MAX_SUMMONED_NEARBY = 5;
    private static final List<RegistryObject<? extends EntityType<? extends AbstractDarkLatexEntity>>> SUMMON_POOL = List.of(
            ChangedEntities.DARK_LATEX_WOLF_MALE,
            ChangedEntities.DARK_LATEX_WOLF_FEMALE,
            ChangedEntities.DARK_LATEX_WOLF_PUP,
            ChangedEntities.DARK_LATEX_YUFENG,
            ChangedEntities.DARK_LATEX_DOUBLE_YUFENG
    );

    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof DarkLatexYufengQueenEntity queen)) {
            return;
        }
        if (!(queen.level() instanceof ServerLevel level)) {
            return;
        }

        CompoundTag data = queen.getPersistentData();
        boolean inCombat = queen.getTarget() != null && queen.getTarget().isAlive();
        if (!inCombat) {
            data.putBoolean(WAS_IN_COMBAT_TAG, false);
            data.putInt(SUMMON_COOLDOWN_TAG, 0);
            return;
        }

        if (!data.getBoolean(WAS_IN_COMBAT_TAG)) {
            trySummonSupport(queen, level);
            data.putInt(SUMMON_COOLDOWN_TAG, SUMMON_INTERVAL_TICKS);
            data.putBoolean(WAS_IN_COMBAT_TAG, true);
            return;
        }

        int cooldown = data.getInt(SUMMON_COOLDOWN_TAG);
        if (cooldown > 0) {
            data.putInt(SUMMON_COOLDOWN_TAG, cooldown - 1);
            return;
        }

        trySummonSupport(queen, level);
        data.putInt(SUMMON_COOLDOWN_TAG, SUMMON_INTERVAL_TICKS);
    }
    @SuppressWarnings("deprecation")
    private static void trySummonSupport(DarkLatexYufengQueenEntity queen, ServerLevel level) {
        if (countQueenSummonsNearby(queen) >= MAX_SUMMONED_NEARBY) {
            return;
        }

        EntityType<? extends AbstractDarkLatexEntity> type = SUMMON_POOL
                .get(level.random.nextInt(SUMMON_POOL.size()))
                .get();
        AbstractDarkLatexEntity summoned = type.create(level);
        if (summoned == null) {
            return;
        }

        BlockPos spawnPos = findSummonPosition(queen, level);
        summoned.moveTo(spawnPos, level.random.nextFloat() * 360.0F, 0.0F);
        summoned.getPersistentData().putBoolean(QUEEN_SUMMONED_TAG, true);

        DifficultyInstance difficulty = level.getCurrentDifficultyAt(spawnPos);
        summoned.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null, null);
        summoned.setTarget(queen.getTarget());

        level.addFreshEntity(summoned);
    }

    private static int countQueenSummonsNearby(DarkLatexYufengQueenEntity queen) {
        return queen.level().getEntitiesOfClass(AbstractDarkLatexEntity.class,
                queen.getBoundingBox().inflate(SUMMON_LIMIT_RADIUS),
                entity -> entity != queen
                        && entity.isAlive()
                        && entity.getPersistentData().getBoolean(QUEEN_SUMMONED_TAG)).size();
    }

    private static BlockPos findSummonPosition(DarkLatexYufengQueenEntity queen, ServerLevel level) {
        BlockPos origin = queen.blockPosition();
        for (int attempt = 0; attempt < 10; ++attempt) {
            int x = origin.getX() + level.random.nextInt(9) - 4;
            int z = origin.getZ() + level.random.nextInt(9) - 4;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, origin.getY(), z);

            while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos).isAir()) {
                pos.move(0, -1, 0);
            }

            BlockPos spawnPos = pos.above();
            if (level.getBlockState(spawnPos).isAir() && level.getBlockState(spawnPos.above()).isAir()) {
                return spawnPos;
            }
        }

        return origin;
    }
}
