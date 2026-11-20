package github.com.gengyoubo.CE.LP.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A normal shaped crafting recipe, except that damageable ingredients (such as flint and steel) stay in
 * the grid with one durability used instead of being consumed. A tool on its last point breaks.
 */
public final class DamageToolShapedRecipe extends ShapedRecipe {
    private static final RandomSource RANDOM = RandomSource.create();

    private DamageToolShapedRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
    }

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(container);
        for (int slot = 0; slot < remaining.size(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isDamageableItem()) continue;
            ItemStack tool = stack.copyWithCount(1);
            // hurt applies Unbreaking and reports whether the tool broke.
            remaining.set(slot, tool.hurt(1, RANDOM, null) ? ItemStack.EMPTY : tool);
        }
        return remaining;
    }

    @Override public RecipeSerializer<?> getSerializer() { return CELPRecipes.DAMAGE_TOOL_SHAPED_SERIALIZER.get(); }

    public static final class Serializer implements RecipeSerializer<DamageToolShapedRecipe> {
        @Override public DamageToolShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new DamageToolShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }
        @Override public DamageToolShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer);
            return base == null ? null : new DamageToolShapedRecipe(base);
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, DamageToolShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
