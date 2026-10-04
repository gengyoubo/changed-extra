package github.com.gengyoubo.CE.compat.synergy.camp;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import java.util.Comparator;
import java.util.EnumSet;
import static github.com.gengyoubo.CE.compat.synergy.camp.LatexSettlementData.*;

/** The resident's body owns every action, item and biological state. */
@SuppressWarnings("deprecation")
final class LatexCampGoal extends Goal {
    private final ChangedEntity mob;
    private final CampResidentWork work;
    private long nextCombatScan;
    private boolean suspended;
    LatexCampGoal(ChangedEntity mob) {
        this.mob = mob; work = new CampResidentWork(mob);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }
    void resetWork() { work.reset(); }
    private Settlement camp() {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        for (String key : java.util.List.of(LatexSettlementService.MEMBER, LatexSettlementService.VISITOR, LatexSettlementService.RAIDER)) {
            if (!mob.getPersistentData().hasUUID(key)) continue;
            Settlement camp = LatexSettlementData.get(level.getServer()).settlements.get(mob.getPersistentData().getUUID(key));
            if (camp != null && camp.active && camp.dimension.equals(level.dimension().location().toString())
                    && level.hasChunkAt(camp.core)) return camp;
        }
        return null;
    }
    @Override public boolean canUse() { return camp() != null && !mob.isNoAi(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        Settlement camp = camp();
        if (camp == null || !(mob.level() instanceof ServerLevel level)) return;
        boolean raider = mob.getPersistentData().hasUUID(LatexSettlementService.RAIDER);
        Resident resident = camp.residents.get(mob.getUUID());
        boolean guard = resident != null && resident.workEnabled && resident.role == CampRole.GUARD;
        LivingEntity target = mob.getTarget();
        if (!raider && !guard || target != null && (!CampCombat.allowed(mob, target, camp, raider)
                || target.blockPosition().distSqr(camp.core) > 32 * 32)) target = null;
        if ((raider || guard) && level.getGameTime() >= nextCombatScan) {
            nextCombatScan = level.getGameTime() + 20;
            target = level.getEntitiesOfClass(LivingEntity.class, new AABB(camp.core).inflate(32),
                    enemy -> enemy.blockPosition().distSqr(camp.core) <= 32 * 32 && CampCombat.allowed(mob, enemy, camp, raider))
                    .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        }
        if (mob.getTarget() != target) mob.setTarget(target);
        // CS can reject setTarget. Only its accepted target may receive an attack.
        if (target != null && mob.getTarget() == target) {
            if (resident != null) { resident.workState = WorkState.FIGHTING; resident.workTarget = target.blockPosition(); }
            mob.getLookControl().setLookAt(target, 30, 30);
            if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(target, 1.15);
            if (mob.tickCount % 20 == 0 && mob.hasLineOfSight(target)
                    && mob.distanceToSqr(target) <= Math.pow(mob.getBbWidth() + target.getBbWidth() + 0.5, 2)) {
                mob.swing(InteractionHand.MAIN_HAND); mob.doHurtTarget(target);
            }
            return;
        }
        double distance = mob.blockPosition().distSqr(camp.core);
        boolean retreat = resident != null && !guard && (camp.raid == Raid.ACTIVE || camp.raid == Raid.PREPARING
                || mob.getHealth() < mob.getMaxHealth() / 2);
        if (resident != null && !raider && resident.workEnabled && !retreat
                && (!guard && resident.role != CampRole.NONE || CampWorkBuffer.hasCargo(mob))) {
            suspended = false;
            if (distance > 32 * 32) {
                resident.workState = WorkState.RETURNING; resident.workTarget = camp.core; home(camp, 7); return;
            }
            work.tick(level, camp, resident); return;
        }
        if (resident != null) {
            if ((retreat || !resident.workEnabled) && !suspended) { CampResidentWork.cancel(mob); suspended = true; }
            if (!retreat && resident.workEnabled) suspended = false;
            resident.workState = retreat ? WorkState.RETURNING : !resident.workEnabled ? WorkState.PAUSED
                    : guard ? WorkState.PATROLLING : WorkState.IDLE;
            resident.workTarget = retreat ? camp.core : null;
        }
        home(camp, raider || retreat || camp.raid == Raid.ACTIVE || camp.raid == Raid.PREPARING ? 3 : guard ? 12 : 7);
        if (raider && distance < 16 && mob.tickCount % 1200 == 0) {
            camp.prosperity = Math.max(0, camp.prosperity - 1); LatexSettlementData.get(level.getServer()).setDirty();
        }
    }
    private void home(Settlement camp, int radius) {
        double distance = mob.blockPosition().distSqr(camp.core);
        if (mob.tickCount % 40 == 0 && distance > radius * radius)
            mob.getNavigation().moveTo(camp.core.getX() + 0.5, camp.core.getY(), camp.core.getZ() + 0.5, 1);
        else if (mob.tickCount % 160 == 0 && distance <= radius * radius)
            mob.getNavigation().moveTo(camp.core.getX() + mob.getRandom().nextInt(radius * 2 + 1) - radius,
                    camp.core.getY(), camp.core.getZ() + mob.getRandom().nextInt(radius * 2 + 1) - radius, 0.7);
    }
    @Override public void stop() { CampResidentWork.cancel(mob); mob.getNavigation().stop(); mob.setTarget(null); }
}
