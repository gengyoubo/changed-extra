package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.Twilight009StateAccess;
import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
import github.com.gengyoubo.CE.compat.twilight.TwilightFinalBossMovement;
import net.foxyas.changedaddon.entity.bosses.Experiment009BossEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Experiment009BossEntity.class, remap = false)
public abstract class TwilightFinal009Mixin implements Twilight009StateAccess {
    @Shadow @Final private ServerBossEvent bossInfo;
    @Unique private static final EntityDataAccessor<Integer> changed_extra$STAGE = SynchedEntityData.defineId(Experiment009BossEntity.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Boolean> changed_extra$LOCKED = SynchedEntityData.defineId(Experiment009BossEntity.class, EntityDataSerializers.BOOLEAN);
    // defineSynchedData runs from Entity's constructor, before subclass field initializers.
    @Unique private boolean changed_extra$stageDataReady;
    @Unique private Experiment009BossEntity changed_extra$self() { return (Experiment009BossEntity) (Object) this; }
    @Inject(method = "defineSynchedData", at = @At("HEAD"), remap = true)
    private void changed_extra$defineStages(CallbackInfo ci) {
        changed_extra$self().getEntityData().define(changed_extra$STAGE, -1); changed_extra$self().getEntityData().define(changed_extra$LOCKED, false);
        changed_extra$stageDataReady = true;
    }
    @Override public int changed_extra$stage() { return changed_extra$stageDataReady ? changed_extra$self().getEntityData().get(changed_extra$STAGE) : -1; }
    @Override public void changed_extra$setStage(int stage) { if (changed_extra$stageDataReady) changed_extra$self().getEntityData().set(changed_extra$STAGE, stage); }
    @Override public boolean changed_extra$locked() { return changed_extra$stageDataReady && changed_extra$self().getEntityData().get(changed_extra$LOCKED); }
    @Override public void changed_extra$setLocked(boolean locked) { if (changed_extra$stageDataReady) changed_extra$self().getEntityData().set(changed_extra$LOCKED, locked); }
    @Invoker("onPhaseChange") public abstract void changed_extra$phaseChanged(Experiment009BossEntity.Exp9Phase phase);
    @ModifyVariable(method = "setPhase2", at = @At("HEAD"), argsOnly = true)
    private boolean changed_extra$phase2ByStage(boolean requested) {
        return TwilightFinalEncounter.isFinal(changed_extra$self()) ? TwilightFinalEncounter.form(TwilightFinalEncounter.stage(changed_extra$self())) >= 2 : requested;
    }
    @ModifyVariable(method = "setPhase3", at = @At("HEAD"), argsOnly = true)
    private boolean changed_extra$phase3ByStage(boolean requested) {
        return TwilightFinalEncounter.isFinal(changed_extra$self()) ? TwilightFinalEncounter.form(TwilightFinalEncounter.stage(changed_extra$self())) == 3 : requested;
    }
    @Inject(method = "getPhase", at = @At("HEAD"), cancellable = true)
    private void changed_extra$correctPhasePriority(CallbackInfoReturnable<Experiment009BossEntity.Exp9Phase> cir) {
        if (TwilightFinalEncounter.isFinal(changed_extra$self())) cir.setReturnValue(TwilightFinalEncounter.phase(changed_extra$self()));
    }
    @Inject(method = {"teleport", "teleportToTarget"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void changed_extra$safeCastleTeleport(net.minecraft.world.entity.LivingEntity target, CallbackInfo ci) {
        if (!TwilightFinalEncounter.isFinal(changed_extra$self())) return;
        TwilightFinalBossMovement.teleportNear(changed_extra$self(), target);
        ci.cancel();
    }
    @Redirect(method = {"spawnThunderBolt", "spawnVisualThunderBolt"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean changed_extra$tagThunder(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity attack) {
        TwilightFinalEncounter.markAttack(attack, changed_extra$self());
        return level.addFreshEntity(attack);
    }
    @Redirect(method = "spawnThunderCircle(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true), require = 0)
    private static boolean changed_extra$tagThunderCircle(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity attack,
            net.minecraft.world.entity.LivingEntity owner, net.minecraft.server.level.ServerLevel world, net.minecraft.core.BlockPos center, float radius, int count) {
        TwilightFinalEncounter.markAttack(attack, owner);
        return level.addFreshEntity(attack);
    }
    @Inject(method = "shouldDespawnInPeaceful", at = @At("HEAD"), cancellable = true, remap = true)
    private void changed_extra$keepEncounterInPeaceful(CallbackInfoReturnable<Boolean> cir) {
        if (TwilightFinalEncounter.isFinal(changed_extra$self())) cir.setReturnValue(false);
    }
    @Redirect(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/foxyas/changedaddon/entity/bosses/Experiment009BossEntity;computeHealthRatio()F", remap = false), remap = true)
    private float changed_extra$nativeRatio(Experiment009BossEntity boss) {
        return TwilightFinalEncounter.isFinal(boss) ? TwilightFinalEncounter.addonRatio(boss) : boss.computeHealthRatio();
    }
    @Inject(method = "baseTick", at = @At("RETURN"), remap = true)
    private void changed_extra$tickEncounter(CallbackInfo ci) {
        TwilightFinalBossMovement.tick(changed_extra$self());
        TwilightFinalEncounter.tick(changed_extra$self()); changed_extra$updateBar();
    }
    @Inject(method = "customServerAiStep", at = @At("RETURN"), remap = true)
    private void changed_extra$showFullEncounterBar(CallbackInfo ci) { changed_extra$updateBar(); }
    @Unique private void changed_extra$updateBar() {
        var boss = changed_extra$self();
        if (boss.level().isClientSide || !TwilightFinalEncounter.isFinal(boss)) return;
        bossInfo.setProgress(boss.getHealth() / boss.getMaxHealth()); bossInfo.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        bossInfo.setColor(TwilightFinalEncounter.locked(boss) ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE);
        bossInfo.setName(Component.translatable(TwilightFinalEncounter.locked(boss) ? "boss.changed_extra.twilight009.locked" : "boss.changed_extra.twilight009.stage",
                boss.getDisplayName(), TwilightFinalEncounter.stage(boss) + 1));
    }
}
