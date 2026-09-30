package github.com.gengyoubo.CE.LP.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CELPRecipes {
    private CELPRecipes() {
    }

    public static final String MOD_ID = "changede";
    public static final String LATEX_CREATIVE_EXTRANALBODY_CRAFTING_ID = "latex_creative_extranalbody_crafting";

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);

    public static final RegistryObject<RecipeSerializer<LatexCreativeExtranalbodyCraftingRecipe>> LATEX_CREATIVE_EXTRANALBODY_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    LATEX_CREATIVE_EXTRANALBODY_CRAFTING_ID,
                    LatexCreativeExtranalbodyCraftingRecipe.Serializer::new
            );

    public static final RecipeType<LatexCreativeExtranalbodyCraftingRecipe> LATEX_CREATIVE_EXTRANALBODY_CRAFTING_TYPE =
            RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MOD_ID, LATEX_CREATIVE_EXTRANALBODY_CRAFTING_ID));

    public static final RegistryObject<RecipeSerializer<AlloyFurnaceRecipe>> ALLOY_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("alloy_furnace", AlloyFurnaceRecipe.Serializer::new);
    public static final RecipeType<AlloyFurnaceRecipe> ALLOY_FURNACE_TYPE =
            RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MOD_ID, "alloy_furnace"));
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MOD_ID);
    public static final RegistryObject<RecipeType<AlloyFurnaceRecipe>> ALLOY_FURNACE_TYPE_REGISTRY =
            RECIPE_TYPES.register("alloy_furnace", () -> ALLOY_FURNACE_TYPE);
}
