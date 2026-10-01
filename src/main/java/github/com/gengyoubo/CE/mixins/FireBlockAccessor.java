package github.com.gengyoubo.CE.mixins;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireBlock.class)
public interface FireBlockAccessor {
    @Invoker("setFlammable")
    void changede$setFlammable(Block block, int encouragement, int flammability);
}
