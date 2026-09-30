package github.com.gengyoubo.CE.LP.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** Two interchangeable inputs, a duration in seconds, and an LP cost per second. */
public final class AlloyFurnaceRecipe implements Recipe<SimpleContainer> {
    public static final int DEFAULT_LP_PER_SECOND = 200;
    private final ResourceLocation id;
    private final Ingredient x;
    private final Ingredient y;
    private final ItemStack result;
    private final int processTicks;
    private final int lpPerSecond;

    public AlloyFurnaceRecipe(ResourceLocation id, Ingredient x, Ingredient y, ItemStack result, int processTicks, int lpPerSecond) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.result = result.copy();
        this.processTicks = processTicks;
        this.lpPerSecond = lpPerSecond;
    }

    @Override public boolean matches(SimpleContainer inventory, @NotNull Level level) {
        if (inventory.getContainerSize() < 2) return false;
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);
        return !first.isEmpty() && !second.isEmpty()
                && ((x.test(first) && y.test(second)) || (y.test(first) && x.test(second)));
    }

    public boolean acceptsInput(ItemStack stack) { return !stack.isEmpty() && (x.test(stack) || y.test(stack)); }
    public int getProcessTicks() { return processTicks; }
    public double getSeconds() { return processTicks / 20.0; }
    public int getLpPerSecond() { return lpPerSecond; }
    @Override public @NotNull NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, x, y); }
    @Override public @NotNull ItemStack assemble(@NotNull SimpleContainer inventory, @NotNull RegistryAccess access) { return result.copy(); }
    @Override public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return CELPRecipes.ALLOY_FURNACE_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return CELPRecipes.ALLOY_FURNACE_TYPE; }

    public static final class Serializer implements RecipeSerializer<AlloyFurnaceRecipe> {
        @Override public @NotNull AlloyFurnaceRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            Ingredient x = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "X"));
            Ingredient y = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "Y"));
            double seconds = GsonHelper.getAsDouble(json, "time");
            int lp = GsonHelper.getAsInt(json, "lp_per_second", DEFAULT_LP_PER_SECOND);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            if (!Double.isFinite(seconds) || seconds <= 0 || seconds * 20 > Integer.MAX_VALUE) {
                throw new JsonSyntaxException("Alloy furnace time must be positive seconds: " + id);
            }
            if (lp < 0 || result.isEmpty() || result.getCount() > result.getMaxStackSize()) {
                throw new JsonSyntaxException("Invalid alloy furnace LP cost or result: " + id);
            }
            return new AlloyFurnaceRecipe(id, x, y, result, (int) Math.ceil(seconds * 20), lp);
        }

        @Override public AlloyFurnaceRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf buffer) {
            return new AlloyFurnaceRecipe(id, Ingredient.fromNetwork(buffer), Ingredient.fromNetwork(buffer),
                    buffer.readItem(), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, AlloyFurnaceRecipe recipe) {
            recipe.x.toNetwork(buffer);
            recipe.y.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.processTicks);
            buffer.writeVarInt(recipe.lpPerSecond);
        }
    }
}
