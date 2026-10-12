package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.tinkers.smeltery.MoltenLatexFluids;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;

@Mixin(value = EntityMeltingRecipe.class, remap = false)
public abstract class TinkersLatexEntityMeltingMixin {
    @Shadow @Final private ResourceLocation id;
    @Inject(method = "getOutput(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraftforge/fluids/FluidStack;", at = @At("HEAD"), cancellable = true)
    private void changede$latexOutput(LivingEntity entity, CallbackInfoReturnable<FluidStack> cir) {
        if (!id.equals(ResourceLocation.parse("changede:smeltery/entity_melting/latex"))) return;
        boolean dark;
        if (entity instanceof ChangedEntity latex && latex.getLatexType() == ChangedLatexTypes.DARK_LATEX.get()) dark = true;
        else if (entity instanceof ChangedEntity latex && latex.getLatexType() == ChangedLatexTypes.WHITE_LATEX.get()) dark = false;
        else dark = entity.getRandom().nextBoolean();
        cir.setReturnValue(new FluidStack(dark ? MoltenLatexFluids.DARK.source.get() : MoltenLatexFluids.WHITE.source.get(), 250));
    }
}
