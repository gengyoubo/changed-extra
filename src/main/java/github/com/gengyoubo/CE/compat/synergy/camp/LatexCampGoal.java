package github.com.gengyoubo.CE.compat.synergy.camp;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;

import static github.com.gengyoubo.CE.compat.synergy.camp.LatexSettlementData.*;

/** Highest movement priority; every decision refers to the one canonical entity. */
final class LatexCampGoal extends Goal {
    private final ChangedEntity mob;
    LatexCampGoal(ChangedEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }
    private Settlement camp() {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        for (String key : java.util.List.of(LatexSettlementService.MEMBER, LatexSettlementService.VISITOR, LatexSettlementService.RAIDER)) {
            if (!mob.getPersistentData().hasUUID(key)) continue;
            Settlement camp = LatexSettlementData.get(level.getServer()).settlements.get(mob.getPersistentData().getUUID(key));
            if (camp != null && camp.active && camp.dimension.equals(level.dimension().location().toString())) return camp;
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
        boolean guard = resident != null && resident.role == Role.GUARD;
        LivingEntity target = null;
        if (raider || guard) {
            target = level.getEntitiesOfClass(LivingEntity.class, new AABB(camp.core).inflate(32), enemy -> {
                if (!enemy.isAlive() || enemy == mob || enemy.isInvisible() || enemy.isSpectator()) return false;
                if (raider) return camp.residents.containsKey(enemy.getUUID()) || enemy.getUUID().equals(camp.owner);
                return camp.raiders.contains(enemy.getUUID()) || enemy instanceof Monster;
            }).stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        }
        mob.setTarget(target);
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30, 30);
            if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(target, 1.15);
            if (mob.tickCount % 20 == 0 && mob.hasLineOfSight(target)
                    && mob.distanceToSqr(target) <= Math.pow(mob.getBbWidth() + target.getBbWidth() + 0.5, 2)) mob.doHurtTarget(target);
            return;
        }
        double distance = mob.blockPosition().distSqr(camp.core);
        int homeRadius = raider || camp.raid == Raid.ACTIVE || camp.raid == Raid.PREPARING ? 3 : guard ? 12 : 7;
        if (mob.tickCount % 40 == 0 && distance > homeRadius * homeRadius)
            mob.getNavigation().moveTo(camp.core.getX() + 0.5, camp.core.getY() + 0.5, camp.core.getZ() + 0.5, 1);
        else if (mob.tickCount % 160 == 0 && distance <= homeRadius * homeRadius) {
            double x = camp.core.getX() + mob.getRandom().nextInt(homeRadius * 2 + 1) - homeRadius;
            double z = camp.core.getZ() + mob.getRandom().nextInt(homeRadius * 2 + 1) - homeRadius;
            mob.getNavigation().moveTo(x, camp.core.getY(), z, 0.7);
        }
        if (raider && distance < 16 && mob.tickCount % 1200 == 0) {
            camp.prosperity = Math.max(0, camp.prosperity - 1);
            LatexSettlementData.get(level.getServer()).setDirty();
        }
    }
    @Override public void stop() { mob.getNavigation().stop(); mob.setTarget(null); }
}
