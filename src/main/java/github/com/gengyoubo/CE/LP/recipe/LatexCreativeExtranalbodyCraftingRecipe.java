package github.com.gengyoubo.CE.LP.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LatexCreativeExtranalbodyCraftingRecipe implements Recipe<Container> {
    private static final int GRID_SIZE = 3;

    private final ResourceLocation id;
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int energyCost;
    private final int processTime;

    public LatexCreativeExtranalbodyCraftingRecipe(
            ResourceLocation id,
            int width,
            int height,
            NonNullList<Ingredient> ingredients,
            ItemStack result,
            int energyCost,
            int processTime
    ) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.energyCost = energyCost;
        this.processTime = processTime;
    }

    @Override
    public boolean matches(Container container, @NotNull Level level) {
        if (container.getContainerSize() < 9) {
            return false;
        }
        for (int xOffset = 0; xOffset <= GRID_SIZE - width; xOffset++) {
            for (int yOffset = 0; yOffset <= GRID_SIZE - height; yOffset++) {
                if (matchesAt(container, xOffset, yOffset, false) || matchesAt(container, xOffset, yOffset, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAt(Container container, int xOffset, int yOffset, boolean mirrored) {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int recipeX = x - xOffset;
                int recipeY = y - yOffset;

                Ingredient ingredient = Ingredient.EMPTY;
                if (recipeX >= 0 && recipeY >= 0 && recipeX < width && recipeY < height) {
                    int idx = mirrored
                            ? (width - recipeX - 1) + recipeY * width
                            : recipeX + recipeY * width;
                    ingredient = ingredients.get(idx);
                }

                ItemStack stack = container.getItem(x + y * GRID_SIZE);
                if (ingredient == Ingredient.EMPTY) {
                    if (!stack.isEmpty()) {
                        return false;
                    }
                } else if (!ingredient.test(stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.width && height >= this.height;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return CELPRecipes.LATEX_CREATIVE_EXTRANALBODY_CRAFTING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return CELPRecipes.LATEX_CREATIVE_EXTRANALBODY_CRAFTING_TYPE;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public int getProcessTime() {
        return processTime;
    }

    public NonNullList<Ingredient> getIngredientsGrid() {
        return ingredients;
    }

    public int getRecipeWidth() {
        return width;
    }

    public int getRecipeHeight() {
        return height;
    }

    public static class Serializer implements RecipeSerializer<LatexCreativeExtranalbodyCraftingRecipe> {
        @Override
        public @NotNull LatexCreativeExtranalbodyCraftingRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            JsonArray patternJson = GsonHelper.getAsJsonArray(json, "pattern");
            String[] pattern = shrink(patternFromJson(patternJson));
            int width = pattern[0].length();
            int height = pattern.length;
            if (width > GRID_SIZE || height > GRID_SIZE) {
                throw new IllegalArgumentException("Pattern too large for 3x3 machine recipe: " + recipeId);
            }

            Map<String, Ingredient> key = keyFromJson(GsonHelper.getAsJsonObject(json, "key"));
            NonNullList<Ingredient> ingredients = dissolvePattern(pattern, key, width, height);

            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int energy = GsonHelper.getAsInt(json, "energy", 40);
            int time = GsonHelper.getAsInt(json, "time", 120);

            return new LatexCreativeExtranalbodyCraftingRecipe(recipeId, width, height, ingredients, result, energy, Math.max(1, time));
        }

        @Override
        public LatexCreativeExtranalbodyCraftingRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int width = buffer.readVarInt();
            int height = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));
            ItemStack result = buffer.readItem();
            int energy = buffer.readVarInt();
            int time = buffer.readVarInt();
            return new LatexCreativeExtranalbodyCraftingRecipe(recipeId, width, height, ingredients, result, energy, Math.max(1, time));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, LatexCreativeExtranalbodyCraftingRecipe recipe) {
            buffer.writeVarInt(recipe.width);
            buffer.writeVarInt(recipe.height);
            for (int i = 0; i < recipe.ingredients.size(); i++) {
                recipe.ingredients.get(i).toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.energyCost);
            buffer.writeVarInt(recipe.processTime);
        }

        private static String[] patternFromJson(JsonArray jsonArray) {
            String[] pattern = new String[jsonArray.size()];
            if (pattern.length == 0) {
                throw new IllegalArgumentException("Empty pattern not allowed");
            }
            if (pattern.length > GRID_SIZE) {
                throw new IllegalArgumentException("Pattern has too many rows, max is " + GRID_SIZE);
            }

            for (int i = 0; i < pattern.length; i++) {
                String row = GsonHelper.convertToString(jsonArray.get(i), "pattern[" + i + "]");
                if (row.length() > GRID_SIZE) {
                    throw new IllegalArgumentException("Pattern row too wide (max " + GRID_SIZE + ")");
                }
                if (i > 0 && row.length() != pattern[0].length()) {
                    throw new IllegalArgumentException("Pattern rows must all be the same width");
                }
                pattern[i] = row;
            }
            return pattern;
        }

        private static Map<String, Ingredient> keyFromJson(JsonObject json) {
            Map<String, Ingredient> key = new HashMap<>();
            for (String symbol : json.keySet()) {
                if (symbol.length() != 1) {
                    throw new IllegalArgumentException("Invalid key entry: '" + symbol + "' must be 1 character");
                }
                if (" ".equals(symbol)) {
                    throw new IllegalArgumentException("Invalid key entry: space is reserved");
                }
                key.put(symbol, Ingredient.fromJson(json.get(symbol)));
            }
            key.put(" ", Ingredient.EMPTY);
            return key;
        }

        private static NonNullList<Ingredient> dissolvePattern(String[] pattern, Map<String, Ingredient> key, int width, int height) {
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            Set<String> unused = new HashSet<>(key.keySet());
            unused.remove(" ");

            int idx = 0;
            for (String row : pattern) {
                for (int i = 0; i < row.length(); i++) {
                    String symbol = String.valueOf(row.charAt(i));
                    Ingredient ingredient = key.get(symbol);
                    if (ingredient == null) {
                        throw new IllegalArgumentException("Pattern references undefined key '" + symbol + "'");
                    }
                    unused.remove(symbol);
                    ingredients.set(idx++, ingredient);
                }
            }

            if (!unused.isEmpty()) {
                throw new IllegalArgumentException("Key defines symbols not used in pattern: " + unused);
            }
            return ingredients;
        }

        private static String[] shrink(String[] pattern) {
            int minCol = Integer.MAX_VALUE;
            int maxCol = 0;
            int top = 0;
            int bottom = 0;

            for (int row = 0; row < pattern.length; row++) {
                String s = pattern[row];
                int first = firstNonSpace(s);
                int last = lastNonSpace(s);
                minCol = Math.min(minCol, first);
                maxCol = Math.max(maxCol, last);

                if (last < 0) {
                    if (top == row) {
                        top++;
                    }
                    bottom++;
                } else {
                    bottom = 0;
                }
            }

            if (pattern.length == bottom) {
                throw new IllegalArgumentException("Pattern cannot be fully blank");
            }

            String[] out = new String[pattern.length - bottom - top];
            for (int row = 0; row < out.length; row++) {
                out[row] = pattern[row + top].substring(minCol, maxCol + 1);
            }
            return out;
        }

        private static int firstNonSpace(String s) {
            int len = s.length();
            int i = 0;

            while (i < len && s.charAt(i) == ' ') {
                i++;
            }

            return i;
        }

        private static int lastNonSpace(String s) {
            int len = s.length();
            int i = len - 1;

            while (i >= 0 && s.charAt(i) == ' ') {
                i--;
            }

            return i;
        }
    }
}
