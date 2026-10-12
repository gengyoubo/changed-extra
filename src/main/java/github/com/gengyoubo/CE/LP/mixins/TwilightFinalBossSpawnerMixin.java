package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalBossCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.block.entity.spawner.FinalBossSpawnerBlockEntity;

@Mixin(value = FinalBossSpawnerBlockEntity.class, remap = false)
public abstract class TwilightFinalBossSpawnerMixin {
    @Inject(method = "spawnMyBoss", at = @At("HEAD"), cancellable = true)
    private void changede$spawnExperiment009(ServerLevelAccessor world, CallbackInfoReturnable<Boolean> cir) {
        BlockPos pos = ((FinalBossSpawnerBlockEntity) (Object) this).getBlockPos();
        cir.setReturnValue(TwilightFinalBossCompat.spawn(world, pos));
    }
}
