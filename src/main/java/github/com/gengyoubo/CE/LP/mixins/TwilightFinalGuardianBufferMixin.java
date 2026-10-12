package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightforest.entity.boss.Naga;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.boss.UrGhast;

@Mixin(value = {Naga.class, Lich.class, UrGhast.class}, remap = false)
public abstract class TwilightFinalGuardianBufferMixin {
    @Redirect(method = "die", at = @At(value = "INVOKE", target = "Ltwilightforest/entity/boss/IBossLootBuffer;saveDropsIntoBoss(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/storage/loot/LootParams;Lnet/minecraft/server/level/ServerLevel;)V", remap = false), remap = true)
    private void changede$skipGuardianBuffer(LivingEntity boss, LootParams params, ServerLevel level) {
        if (!boss.getPersistentData().getBoolean(TwilightFinalEncounter.GUARDIAN)) IBossLootBuffer.saveDropsIntoBoss((LivingEntity & IBossLootBuffer) boss, params, level);
    }
    @Redirect(method = "remove", at = @At(value = "INVOKE", target = "Ltwilightforest/entity/boss/IBossLootBuffer;depositDropsIntoChest(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)V", remap = false), remap = true)
    private void changede$skipGuardianChest(LivingEntity boss, BlockState chest, BlockPos pos, ServerLevel level) {
        if (!boss.getPersistentData().getBoolean(TwilightFinalEncounter.GUARDIAN)) IBossLootBuffer.depositDropsIntoChest((LivingEntity & IBossLootBuffer) boss, chest, pos, level);
    }
}
