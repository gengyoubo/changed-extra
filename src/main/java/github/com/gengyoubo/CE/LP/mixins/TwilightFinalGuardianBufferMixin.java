package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.IBossLootBuffer;

@Mixin(value = IBossLootBuffer.class, remap = false)
public interface TwilightFinalGuardianBufferMixin {
    @Inject(method = "saveDropsIntoBoss", at = @At("HEAD"), cancellable = true)
    private static void changede$skipGuardianBuffer(LivingEntity boss, LootParams params, ServerLevel level, CallbackInfo ci) {
        if (boss.getPersistentData().getBoolean(TwilightFinalEncounter.GUARDIAN)) ci.cancel();
    }
    @Inject(method = "depositDropsIntoChest", at = @At("HEAD"), cancellable = true)
    private static void changede$skipGuardianChest(LivingEntity boss, BlockState chest, BlockPos pos, ServerLevel level, CallbackInfo ci) {
        if (boss.getPersistentData().getBoolean(TwilightFinalEncounter.GUARDIAN)) ci.cancel();
    }
}
