package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.InfuserPowerBlockEntity;
import net.ltxprogrammer.changed.block.Infuser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Infuser.class, remap = false)
public abstract class InfuserEnergyBlockMixin implements EntityBlock {
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new InfuserPowerBlockEntity(pos, state);
    }
}
