package github.com.gengyoubo.CE.compat.twilight;

import net.foxyas.changedaddon.entity.bosses.Experiment009BossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.registries.ForgeRegistries;
import twilightforest.entity.EnforcedHomePoint;

/** Seven 400-HP segments, using Addon's original forms in a 2/2/3 distribution. */
public final class TwilightFinalEncounter {
    public static final String STAGE = "changede_twilight_stage", LOCKED = "changede_twilight_locked";
    public static final String WAVE = "changede_twilight_guardians", GUARDIAN = "changede_twilight_guardian", OWNER = "changede_twilight_owner";
    private static final String PREVIOUS_AI = "changede_twilight_previous_no_ai";
    public static final int SEGMENT = 400;
    private record Group(String type, int count) {}
    private static final Group[][] WAVES = {
            {new Group("naga", 1)}, {new Group("lich", 1)},
            {new Group("minoshroom", 1), new Group("knight_phantom", 6)},
            {new Group("alpha_yeti", 1), new Group("snow_queen", 1)},
            {new Group("hydra", 1)}, {new Group("ur_ghast", 1)}
    };
    private TwilightFinalEncounter() {}
    public static boolean isFinal(Entity entity) {
        return entity instanceof Experiment009BossEntity && (entity.getPersistentData().getBoolean(TwilightFinalBossCompat.MARKER)
                || entity instanceof Twilight009StateAccess access && access.changede$stage() >= 0);
    }
    public static int stage(Experiment009BossEntity boss) {
        return boss.level().isClientSide ? Math.max(0, ((Twilight009StateAccess) boss).changede$stage())
                : Math.max(0, Math.min(6, boss.getPersistentData().getInt(STAGE)));
    }
    public static boolean locked(Experiment009BossEntity boss) {
        return boss.level().isClientSide ? ((Twilight009StateAccess) boss).changede$locked() : boss.getPersistentData().getBoolean(LOCKED);
    }
    public static int form(int stage) { return stage < 2 ? 1 : stage < 4 ? 2 : 3; }
    public static float floor(Experiment009BossEntity boss) { return TwilightFinalBossCompat.HEALTH - SEGMENT * (stage(boss) + 1); }
    public static void initialize(Experiment009BossEntity boss) {
        var data = boss.getPersistentData();
        if (!data.contains(STAGE)) {
            // Upgrade previously saved 500-heart encounters without healing or losing their progress.
            data.putInt(STAGE, Math.max(0, Math.min(6, (int) ((TwilightFinalBossCompat.HEALTH - boss.getHealth()) / SEGMENT))));
        }
        sync(boss);
    }
    private static void sync(Experiment009BossEntity boss) {
        var access = (Twilight009StateAccess) boss;
        access.changede$setStage(stage(boss)); access.changede$setLocked(locked(boss));
        boss.setPhase2(form(stage(boss)) >= 2); boss.setPhase3(form(stage(boss)) == 3);
        if (locked(boss)) boss.setNoAi(true);
    }
    public static float clamp(Experiment009BossEntity boss, float requested) {
        if (boss.level().isClientSide || !isFinal(boss) || !boss.getPersistentData().contains(STAGE)) return requested;
        if (!Float.isFinite(requested)) return boss.getHealth();
        float ceiling = TwilightFinalBossCompat.HEALTH - SEGMENT * stage(boss);
        return locked(boss) ? floor(boss) : Math.max(floor(boss), Math.min(ceiling, requested));
    }
    public static void healthChanged(Experiment009BossEntity boss) {
        if (!(boss.level() instanceof ServerLevel level) || !isFinal(boss) || !boss.getPersistentData().contains(STAGE)
                || locked(boss) || stage(boss) == 6 || boss.getHealth() > floor(boss)) return;
        var data = boss.getPersistentData();
        data.putBoolean(LOCKED, true); data.putBoolean(PREVIOUS_AI, boss.isNoAi()); boss.setNoAi(true);
        var list = new ListTag();
        for (Group group : WAVES[stage(boss)]) for (int i = 0; i < group.count; i++) {
            var entry = new CompoundTag(); entry.putString("type", "twilightforest:" + group.type); entry.putInt("number", i);
            entry.putBoolean("pending", true); list.add(entry);
        }
        data.put(WAVE, list); sync(boss); spawnPending(level, boss, list);
    }
    private static void spawnPending(ServerLevel level, Experiment009BossEntity owner, ListTag wave) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        BlockPos home = BlockPos.of(owner.getPersistentData().getLong(TwilightFinalBossCompat.HOME));
        for (int index = 0; index < wave.size(); index++) {
            var entry = wave.getCompound(index);
            if (!entry.getBoolean("pending")) continue;
            var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entry.getString("type")));
            if (type == null || !(type.create(level) instanceof Mob mob)) continue;
            double angle = Math.PI * 2 * index / Math.max(1, wave.size());
            int height = entry.getString("type").endsWith("ur_ghast") ? 10 : entry.getString("type").endsWith("snow_queen") ? 3 : 0;
            BlockPos pos = home.offset((int) (Math.cos(angle) * 10), height, (int) (Math.sin(angle) * 10));
            mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, owner.getRandom().nextFloat() * 360, 0);
            mob.getPersistentData().putBoolean(GUARDIAN, true); mob.getPersistentData().putUUID(OWNER, owner.getUUID());
            ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired(); mob.setHealth(mob.getMaxHealth());
            if (mob instanceof EnforcedHomePoint restricted) restricted.setRestrictionPoint(GlobalPos.of(level.dimension(), home));
            if (mob instanceof twilightforest.entity.boss.KnightPhantom knight) knight.setNumber(entry.getInt("number"));
            if (owner.getLastHurtByMob() instanceof net.minecraft.world.entity.player.Player player) mob.setTarget(player);
            if (level.addFreshEntity(mob)) { entry.putUUID("id", mob.getUUID()); entry.putBoolean("pending", false); }
        }
    }
    public static void tick(Experiment009BossEntity boss) {
        if (!(boss.level() instanceof ServerLevel level) || !isFinal(boss)) return;
        if (!boss.getPersistentData().contains(STAGE)) initialize(boss);
        if (!locked(boss)) { healthChanged(boss); return; }
        boss.setNoAi(true);
        var data = boss.getPersistentData(); var wave = data.getList(WAVE, Tag.TAG_COMPOUND); var deaths = TwilightGuardianDeaths.get(level);
        boolean complete = !wave.isEmpty();
        for (int i = 0; i < wave.size(); i++) {
            var entry = wave.getCompound(i);
            if (entry.getBoolean("pending") || !entry.hasUUID("id")) { complete = false; continue; }
            var id = entry.getUUID("id");
            if (!deaths.dead(id)) {
                complete = false;
                if (deaths.lost(id)) { deaths.forget(id); entry.putBoolean("pending", true); }
            }
        }
        if (!complete) { if (boss.tickCount % 40 == 0) spawnPending(level, boss, wave); return; }
        for (int i = 0; i < wave.size(); i++) deaths.forget(wave.getCompound(i).getUUID("id"));
        int before = form(stage(boss)); data.putInt(STAGE, stage(boss) + 1); data.putBoolean(LOCKED, false); data.remove(WAVE);
        boss.setNoAi(data.getBoolean(PREVIOUS_AI)); data.remove(PREVIOUS_AI); sync(boss);
        if (form(stage(boss)) != before) ((Twilight009StateAccess) boss).changede$phaseChanged(phase(boss));
    }
    public static Experiment009BossEntity.Exp9Phase phase(Experiment009BossEntity boss) {
        return switch (form(stage(boss))) {
            case 1 -> Experiment009BossEntity.Exp9Phase.PHASE1;
            case 2 -> Experiment009BossEntity.Exp9Phase.PHASE2;
            default -> Experiment009BossEntity.Exp9Phase.PHASE3;
        };
    }
    /** Feed Addon's existing stat/bleeding logic the corresponding native phase interval. */
    public static float addonRatio(Experiment009BossEntity boss) {
        return switch (form(stage(boss))) {
            case 1 -> .75f + .25f * (boss.getHealth() - 2000) / 800;
            case 2 -> .4f + .35f * (boss.getHealth() - 1200) / 800;
            default -> .4f * boss.getHealth() / 1200;
        };
    }
    public static void attacked(LivingAttackEvent event) {
        var entity = event.getEntity();
        if (entity instanceof Experiment009BossEntity boss && isFinal(boss) && locked(boss)) event.setCanceled(true);
        if (entity.getPersistentData().getBoolean(GUARDIAN) && event.getSource().getEntity() instanceof Experiment009BossEntity owner
                && entity.getPersistentData().hasUUID(OWNER) && entity.getPersistentData().getUUID(OWNER).equals(owner.getUUID())) event.setCanceled(true);
    }
    public static void died(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level && event.getEntity().getPersistentData().getBoolean(GUARDIAN)) {
            TwilightGuardianDeaths.get(level).markDead(event.getEntity().getUUID());
        }
    }
    public static void removed(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity().getPersistentData().getBoolean(GUARDIAN)
                && event.getEntity().getRemovalReason() == Entity.RemovalReason.DISCARDED) TwilightGuardianDeaths.get(level).markLost(event.getEntity().getUUID());
    }
    public static void drops(LivingDropsEvent event) { if (event.getEntity().getPersistentData().getBoolean(GUARDIAN)) event.setCanceled(true); }
    public static void experience(LivingExperienceDropEvent event) { if (event.getEntity().getPersistentData().getBoolean(GUARDIAN)) event.setDroppedExperience(0); }
}
