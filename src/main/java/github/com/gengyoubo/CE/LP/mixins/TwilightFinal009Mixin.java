package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.Twilight009StateAccess;
import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
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
    @Unique private static final EntityDataAccessor<Integer> changede$STAGE = SynchedEntityData.defineId(Experiment009BossEntity.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Boolean> changede$LOCKED = SynchedEntityData.defineId(Experiment009BossEntity.class, EntityDataSerializers.BOOLEAN);
    @Unique private Experiment009BossEntity changede$self() { return (Experiment009BossEntity) (Object) this; }
    @Inject(method = "defineSynchedData", at = @At("RETURN"), remap = true)
    private void changede$defineStages(CallbackInfo ci) {
        changede$self().getEntityData().define(changede$STAGE, -1); changede$self().getEntityData().define(changede$LOCKED, false);
    }
    @Override public int changede$stage() { return changede$self().getEntityData().get(changede$STAGE); }
    @Override public void changede$setStage(int stage) { changede$self().getEntityData().set(changede$STAGE, stage); }
    @Override public boolean changede$locked() { return changede$self().getEntityData().get(changede$LOCKED); }
    @Override public void changede$setLocked(boolean locked) { changede$self().getEntityData().set(changede$LOCKED, locked); }
    @Invoker("onPhaseChange") public abstract void changede$phaseChanged(Experiment009BossEntity.Exp9Phase phase);
    @ModifyVariable(method = "setPhase2", at = @At("HEAD"), argsOnly = true)
    private boolean changede$phase2ByStage(boolean requested) {
        return TwilightFinalEncounter.isFinal(changede$self()) ? TwilightFinalEncounter.form(TwilightFinalEncounter.stage(changede$self())) >= 2 : requested;
    }
    @ModifyVariable(method = "setPhase3", at = @At("HEAD"), argsOnly = true)
    private boolean changede$phase3ByStage(boolean requested) {
        return TwilightFinalEncounter.isFinal(changede$self()) ? TwilightFinalEncounter.form(TwilightFinalEncounter.stage(changede$self())) == 3 : requested;
    }
    @Inject(method = "getPhase", at = @At("HEAD"), cancellable = true)
    private void changede$correctPhasePriority(CallbackInfoReturnable<Experiment009BossEntity.Exp9Phase> cir) {
        if (TwilightFinalEncounter.isFinal(changede$self())) cir.setReturnValue(TwilightFinalEncounter.phase(changede$self()));
    }
    @Redirect(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/foxyas/changedaddon/entity/bosses/Experiment009BossEntity;computeHealthRatio()F", remap = false), remap = true)
    private float changede$nativeRatio(Experiment009BossEntity boss) {
        return TwilightFinalEncounter.isFinal(boss) ? TwilightFinalEncounter.addonRatio(boss) : boss.computeHealthRatio();
    }
    @Inject(method = "baseTick", at = @At("RETURN"), remap = true)
    private void changede$tickEncounter(CallbackInfo ci) {
        TwilightFinalEncounter.tick(changede$self()); changede$updateBar();
    }
    @Inject(method = "customServerAiStep", at = @At("RETURN"), remap = true)
    private void changede$showFullEncounterBar(CallbackInfo ci) { changede$updateBar(); }
    @Unique private void changede$updateBar() {
        var boss = changede$self();
        if (boss.level().isClientSide || !TwilightFinalEncounter.isFinal(boss)) return;
        bossInfo.setProgress(boss.getHealth() / boss.getMaxHealth()); bossInfo.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        bossInfo.setColor(TwilightFinalEncounter.locked(boss) ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE);
        bossInfo.setName(Component.translatable(TwilightFinalEncounter.locked(boss) ? "boss.changede.twilight009.locked" : "boss.changede.twilight009.stage",
                boss.getDisplayName(), TwilightFinalEncounter.stage(boss) + 1));
    }
}
