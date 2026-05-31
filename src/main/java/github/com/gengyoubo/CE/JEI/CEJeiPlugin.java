package github.com.gengyoubo.CE.JEI;

import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.recipe.CELPRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.ltxprogrammer.changed.recipe.InfuserRecipe;
import net.ltxprogrammer.changed.recipe.PurifierRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class CEJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("changede", "jei_plugin");
    private static final RecipeType<InfuserRecipe> CHANGED_INFUSER_RECIPE = RecipeType.create(
            "changed",
            "infuser_recipe",
            InfuserRecipe.class
    );
    private static final RecipeType<PurifierRecipe> CHANGED_PURIFIER_RECIPE = RecipeType.create(
            "changed",
            "purifier_recipe",
            PurifierRecipe.class
    );

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new LatexCreativeExtranalbodyCraftingCategory(
                        registration.getJeiHelpers().getGuiHelper(),
                        new ItemStack(CELPBlock.LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK.get())
                )
        );
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        List<github.com.gengyoubo.CE.LP.recipe.LatexCreativeExtranalbodyCraftingRecipe> recipes = mc.level.getRecipeManager()
                .getAllRecipesFor(CELPRecipes.LATEX_CREATIVE_EXTRANALBODY_CRAFTING_TYPE)
                .stream()
                .filter(recipe -> recipe.getId().getPath().startsWith("lectb/"))
                .toList();

        registration.addRecipes(LatexCreativeExtranalbodyCraftingCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(CELPBlock.ELECTRIC_FURNACE.get()),
                RecipeTypes.SMELTING
        );
        registration.addRecipeCatalyst(
                new ItemStack(CELPBlock.LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK.get()),
                LatexCreativeExtranalbodyCraftingCategory.TYPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ChangedBlocks.INFUSER.get()),
                CHANGED_INFUSER_RECIPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ChangedBlocks.PURIFIER.get()),
                CHANGED_PURIFIER_RECIPE
        );
    }
}
