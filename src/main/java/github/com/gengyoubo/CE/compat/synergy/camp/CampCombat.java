package github.com.gengyoubo.CE.compat.synergy.camp;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.event.NpcDispositionEvents;

final class CampCombat {
    private CampCombat() {}
    static boolean allowed(ChangedEntity mob, LivingEntity target, LatexSettlementData.Settlement camp, boolean raider) {
        if (!target.isAlive() || target == mob || target.isSpectator() || target.isInvisible() || !mob.canAttack(target)
                || mob.isAlliedTo(target) || LatexCreatureCombatRules.mustRejectVillageTarget(mob, target)) return false;
        if (target instanceof ChangedEntity other && LatexCreatureCombatRules.mustRejectTarget(mob, other)) return false;
        if (raider) {
            if (!camp.residents.containsKey(target.getUUID()) && !target.getUUID().equals(camp.owner)) return false;
            return !(target instanceof ServerPlayer player) || !player.isCreative() && NpcDispositionEvents.hasHostileDisposition(mob, player);
        }
        if (camp.residents.containsKey(target.getUUID()) || target.getUUID().equals(camp.owner)
                || target.getPersistentData().hasUUID(LatexSettlementService.VISITOR)) return false;
        ServerPlayer owner = ((ServerLevel) mob.level()).getServer().getPlayerList().getPlayer(camp.owner);
        if (owner != null && target.isAlliedTo(owner)) return false;
        boolean hostile = camp.raiders.contains(target.getUUID()) || target.getType().getCategory() == MobCategory.MONSTER;
        if (target instanceof ChangedEntity other) {
            if (owner != null && PlayerOutpostService.authorized(other, owner)) return false;
            hostile |= LatexCreatureCombatRules.areRivals(mob, other);
        }
        if (target instanceof Mob enemy && enemy.getTarget() != null) {
            LivingEntity victim = enemy.getTarget();
            boolean attackingCamp = camp.residents.containsKey(victim.getUUID()) || victim.getUUID().equals(camp.owner);
            if (enemy instanceof ChangedEntity changed && victim instanceof ServerPlayer player)
                attackingCamp &= NpcDispositionEvents.hasHostileDisposition(changed, player);
            hostile |= attackingCamp;
        }
        if (target instanceof Player player) {
            if (player.isCreative() || owner != null && !owner.canHarmPlayer(player)) return false;
            hostile = recentAttacker(mob, target) || owner != null && recentAttacker(owner, target);
            if (!(target instanceof ServerPlayer serverPlayer) || !NpcDispositionEvents.hasHostileDisposition(mob, serverPlayer)) return false;
        }
        if (!hostile) return false;
        return BondedPetSettings.isValidConfiguredTarget(mob, owner, target)
                && (owner == null || BondedOwnerDefenseGoal.allowsConfiguredDefense(mob, owner, target));
    }
    private static boolean recentAttacker(LivingEntity victim, LivingEntity target) {
        return victim.getLastHurtByMob() == target && victim.tickCount - victim.getLastHurtByMobTimestamp() < 600;
    }
}
