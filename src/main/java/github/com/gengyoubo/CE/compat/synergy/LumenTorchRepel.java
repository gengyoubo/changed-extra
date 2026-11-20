package github.com.gengyoubo.CE.compat.synergy;

import github.com.gengyoubo.CE.init.CEBlock;
import github.com.gengyoubo.CE.init.CEItem;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Lumen torches drive off Changed: Synergy faction pursuit squads. A pursuer within {@link #RADIUS}
 * blocks of a placed lumen torch, or of a player holding one, has its pursuit expired, so Synergy's own
 * stand-down runs: the leader withdraws with its line, the squad calms and leaves.
 * Loaded only when Changed: Synergy is present.
 */
public final class LumenTorchRepel {
    /** Synergy's pursuit squad tag on the mob's persistent data, with the pursuit end time in "Until". */
    private static final String SQUAD = "SynergyPursuitSquad";
    static final int RADIUS = 12;
    private static final int VERTICAL = 4;

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(LumenTorchRepel::tick);
    }

    private static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity mob) || !(mob.level() instanceof ServerLevel level)
                || mob.tickCount % 20 != 7 || !mob.isAlive()) return;
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(SQUAD, Tag.TAG_COMPOUND)) return;
        CompoundTag squad = data.getCompound(SQUAD);
        long now = level.getGameTime();
        if (squad.getLong("Until") <= now || !nearLumen(level, mob)) return;
        squad.putLong("Until", now);
        data.put(SQUAD, squad);
        mob.setTarget(null);
        level.sendParticles(ParticleTypes.GLOW, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(),
                12, mob.getBbWidth() / 2, mob.getBbHeight() / 3, mob.getBbWidth() / 2, 0.02);
        level.playSound(null, mob.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8F, 1.4F);
    }

    private static boolean nearLumen(ServerLevel level, ChangedEntity mob) {
        for (Player player : level.players()) {
            if (player.distanceToSqr(mob) <= RADIUS * RADIUS && !player.isSpectator()
                    && (player.getMainHandItem().is(CEItem.LUMEN_TORCH.get()) || player.getOffhandItem().is(CEItem.LUMEN_TORCH.get())))
                return true;
        }
        BlockPos center = mob.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -VERTICAL, -RADIUS), center.offset(RADIUS, VERTICAL, RADIUS))) {
            if (!level.isLoaded(pos)) continue;
            var state = level.getBlockState(pos);
            if ((state.is(CEBlock.LUMEN_TORCH.get()) || state.is(CEBlock.LUMEN_WALL_TORCH.get())) && center.distSqr(pos) <= RADIUS * RADIUS)
                return true;
        }
        return false;
    }

    private LumenTorchRepel() {}
}
