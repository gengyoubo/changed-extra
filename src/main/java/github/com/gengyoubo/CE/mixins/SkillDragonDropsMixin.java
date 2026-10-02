package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.skill.SkillDragonLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(Block.class)
public abstract class SkillDragonDropsMixin {
    @ModifyVariable(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",at=@At("HEAD"),argsOnly=true)
    private static ItemStack changede$fortune(ItemStack tool,BlockState state,ServerLevel level,BlockPos pos,BlockEntity blockEntity,Entity entity,ItemStack original) {
        return SkillDragonLoot.tool(tool,entity,level,pos,state);
    }
    @Inject(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",at=@At("RETURN"),cancellable=true)
    private static void changede$bonus(BlockState state,ServerLevel level,BlockPos pos,BlockEntity blockEntity,Entity entity,ItemStack tool,CallbackInfoReturnable<List<ItemStack>> ci) {
        ci.setReturnValue(SkillDragonLoot.extra(ci.getReturnValue(),tool,entity,level,pos,state));
    }
}
