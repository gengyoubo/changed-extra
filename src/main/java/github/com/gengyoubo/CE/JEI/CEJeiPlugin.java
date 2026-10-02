package github.com.gengyoubo.CE.JEI;

import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.recipe.CELPRecipes;
import github.com.gengyoubo.CE.init.CEItem;
import github.com.gengyoubo.CE.items.LatexDrinkItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.ltxprogrammer.changed.recipe.InfuserRecipe;
import net.ltxprogrammer.changed.recipe.PurifierRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                ),
                new AlloyFurnaceCategory(registration.getJeiHelpers().getGuiHelper()),
                new IngotFillingCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        registerSynergyFoodInfo(registration);

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
        registration.addRecipes(AlloyFurnaceCategory.TYPE,
                mc.level.getRecipeManager().getAllRecipesFor(CELPRecipes.ALLOY_FURNACE_TYPE));
        registration.addRecipes(IngotFillingCategory.TYPE,mc.level.getRecipeManager().getAllRecipesFor(CELPRecipes.INGOT_FILLING_TYPE));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(CELPBlock.BASIC_ALLOY_FURNACE.get()), AlloyFurnaceCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(CELPBlock.INGOT_FILLER.get()),IngotFillingCategory.TYPE);
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

    private static void registerSynergyFoodInfo(IRecipeRegistration registration) {
        if (!ModList.get().isLoaded("changed_synergy")) {
            return;
        }

        Map<Item, EnumSet<LatexDiet>> foods = new LinkedHashMap<>();
        for (LatexDiet diet : LatexDiet.values()) {
            TagKey<Item> dietTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("changed_addon", diet.itemTag)
            );
            for (ItemStack food : Ingredient.of(dietTag).getItems()) {
                if (!food.isEmpty()) {
                    foods.computeIfAbsent(food.getItem(), ignored -> EnumSet.noneOf(LatexDiet.class)).add(diet);
                }
            }
        }

        Set<Item> oranges = new HashSet<>();
        addIfPresent(oranges, "changed", "orange");
        addIfPresent(oranges, "changed_addon", "golden_orange");
        addIfPresent(oranges, "changed_additions", "golden_orange");

        for (Item orange : oranges) {
            foods.computeIfAbsent(orange, ignored -> EnumSet.noneOf(LatexDiet.class));
        }

        Item enchantedOrange = CEItem.ENCHANTED_GOLDEN_ORANGE.get();
        foods.computeIfAbsent(enchantedOrange, ignored -> EnumSet.noneOf(LatexDiet.class));

        Set<Item> latexDrinks = new HashSet<>();
        CEItem.LATEX_DRINKS.stream()
                .map(registryObject -> registryObject.get())
                .filter(item -> item instanceof LatexDrinkItem)
                .forEach(latexDrinks::add);
        for (Item drink : latexDrinks) {
            foods.computeIfAbsent(drink, ignored -> EnumSet.noneOf(LatexDiet.class));
        }

        foods.forEach((item, diets) -> {
            List<Component> description = new ArrayList<>();
            if (oranges.contains(item)) {
                description.add(Component.translatable("jei.changede.latex_food.orange"));
            }
            if (item == enchantedOrange) {
                description.add(Component.translatable("jei.changede.latex_food.enchanted_orange"));
            }
            if (latexDrinks.contains(item)) {
                description.add(Component.translatable("jei.changede.latex_food.latex_drink"));
            }
            if (!diets.isEmpty()) {
                MutableComponent dietNames = Component.empty();
                boolean first = true;
                for (LatexDiet diet : diets) {
                    if (!first) {
                        dietNames.append(Component.literal("、"));
                    }
                    dietNames.append(Component.translatable(diet.translationKey));
                    first = false;
                }
                description.add(Component.translatable("jei.changede.latex_food.accepted_by", dietNames));
            }

            registration.addItemStackInfo(new ItemStack(item), description.toArray(Component[]::new));
        });
    }

    private static void addIfPresent(Set<Item> items, String namespace, String path) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            items.add(item);
        }
    }

    private enum LatexDiet {
        AQUATIC("aquatic_diet_list", "jei.changede.latex_food.diet.aquatic"),
        SHARK("shark_diet_list", "jei.changede.latex_food.diet.shark"),
        CAT("cat_diet_list", "jei.changede.latex_food.diet.cat"),
        DRAGON("dragon_diet_list", "jei.changede.latex_food.diet.dragon"),
        FOX("fox_diet_list", "jei.changede.latex_food.diet.fox"),
        SWEET_TOOTH("sweet_tooth_list", "jei.changede.latex_food.diet.sweet_tooth"),
        WOLF("wolf_diet_list", "jei.changede.latex_food.diet.wolf"),
        SPECIAL("special_diet_list", "jei.changede.latex_food.diet.special");

        private final String itemTag;
        private final String translationKey;

        LatexDiet(String itemTag, String translationKey) {
            this.itemTag = itemTag;
            this.translationKey = translationKey;
        }
    }
}
