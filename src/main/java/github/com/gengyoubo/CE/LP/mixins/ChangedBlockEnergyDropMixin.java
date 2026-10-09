package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.energy.MachineEnergyPersistence;
import net.ltxprogrammer.changed.block.ChangedBlock;
import net.ltxprogrammer.changed.block.Purifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** These Changed blocks return items directly, bypassing Forge's loot table modifiers. */
@Mixin(value = {ChangedBlock.class, Purifier.class}, remap = false)
public abstract class ChangedBlockEnergyDropMixin {
    @Inject(method = {"getDrops", "m_49635_"}, at = @At("RETURN"))
    private void changede$preserveEnergy(BlockState state, LootParams.Builder params,
                                        CallbackInfoReturnable<List<ItemStack>> cir) {
        MachineEnergyPersistence.preserveDrops(cir.getReturnValue(), params.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
    }
}
