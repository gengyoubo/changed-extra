package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.loot.TFLootTables;

@Mixin(value = KnightPhantom.class, remap = false)
public abstract class TwilightFinalGuardianKnightMixin {
    @Redirect(method = "die", at = @At(value = "INVOKE", target = "Ltwilightforest/loot/TFLootTables;generateChest(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)V", remap = false), remap = true)
    private void changede$skipKnightReward(TFLootTables table, WorldGenLevel level, BlockPos pos, Direction direction, boolean trapped) {
        if (!((KnightPhantom) (Object) this).getPersistentData().getBoolean(TwilightFinalEncounter.GUARDIAN)) table.generateChest(level, pos, direction, trapped);
    }
}
