package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.loot.TFLootTables;

@Mixin(value = TFLootTables.class, remap = false)
public abstract class TwilightFinalGuardianContainerMixin {
    @Inject(method = "entityDropsIntoContainer", at = @At("HEAD"), cancellable = true)
    private static void changede$skipGuardianContainer(LivingEntity boss, DamageSource source, BlockState chest, BlockPos pos, CallbackInfo ci) {
        if (boss.getPersistentData().getBoolean(TwilightFinalEncounter.GUARDIAN)) ci.cancel();
    }
}
