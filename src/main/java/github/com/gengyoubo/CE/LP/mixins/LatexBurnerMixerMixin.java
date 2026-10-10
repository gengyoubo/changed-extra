package github.com.gengyoubo.CE.LP.mixins;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import github.com.gengyoubo.CE.compat.create.burner.LatexBurnerBlockEntity;
import github.com.gengyoubo.CE.compat.create.burner.LatexBurnerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalMixerBlockEntity.class, remap = false)
public abstract class LatexBurnerMixerMixin extends BasinOperatingBlockEntity {
    @Unique private final LatexBurnerRules.Progress changede$burnerProgress = new LatexBurnerRules.Progress();
    @Unique private Object changede$previousRecipe;
    protected LatexBurnerMixerMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Inject(method = "tick", at = @At("HEAD"))
    private void changede$resetBurnerProgress(CallbackInfo callback) {
        MechanicalMixerBlockEntity mixer = (MechanicalMixerBlockEntity) (Object) this;
        if (currentRecipe != changede$previousRecipe || !mixer.running || mixer.processingTicks < 0) changede$burnerProgress.reset();
        changede$previousRecipe = currentRecipe;
    }

    @Redirect(method = "tick", at = @At(value = "FIELD",
            target = "Lcom/simibubi/create/content/kinetics/mixer/MechanicalMixerBlockEntity;processingTicks:I",
            opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void changede$advanceBurnerRecipe(MechanicalMixerBlockEntity mixer, int decremented) {
        if (level == null || level.isClientSide || !(currentRecipe instanceof BasinRecipe recipe)
                || recipe.getRequiredHeat() == HeatCondition.NONE) {
            changede$burnerProgress.reset(); mixer.processingTicks = decremented; return;
        }
        var basin = getBasin().orElse(null);
        if (basin == null || !(level.getBlockEntity(basin.getBlockPos().below()) instanceof LatexBurnerBlockEntity burner)) {
            changede$burnerProgress.reset(); mixer.processingTicks = decremented; return;
        }
        if (getSpeed() == 0 || !isSpeedRequirementFulfilled() || !recipe.getRequiredHeat().testBlazeBurner(burner.heat()) || !BasinRecipe.match(basin, recipe)) {
            changede$burnerProgress.reset(); mixer.processingTicks = decremented + 1; return;
        }
        mixer.processingTicks = changede$burnerProgress.advance(decremented + 1, burner.profile().speedUnits());
    }
}
