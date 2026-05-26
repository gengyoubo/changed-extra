package github.com.gengyoubo.CE.JEI;

import github.com.gengyoubo.CE.LP.recipe.LatexCreativeExtranalbodyCraftingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LatexCreativeExtranalbodyCraftingCategory implements IRecipeCategory<LatexCreativeExtranalbodyCraftingRecipe> {
    public static final RecipeType<LatexCreativeExtranalbodyCraftingRecipe> TYPE = RecipeType.create(
            "changede",
            "latex_creative_extranalbody_crafting",
            LatexCreativeExtranalbodyCraftingRecipe.class
    );

    private final IDrawable icon;
    private final IDrawable arrow;
    private final Component title = Component.literal("Latex Creative Extranalbody Crafting");
    private static final int WIDTH = 150;
    private static final int HEIGHT = 84;

    public LatexCreativeExtranalbodyCraftingCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public @NotNull RecipeType<LatexCreativeExtranalbodyCraftingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, LatexCreativeExtranalbodyCraftingRecipe recipe, @NotNull IFocusGroup focuses) {
        int recipeWidth = recipe.getRecipeWidth();
        int recipeHeight = recipe.getRecipeHeight();
        int xOffset = (3 - recipeWidth) / 2;
        int yOffset = (3 - recipeHeight) / 2;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, 4 + x * 18, 4 + y * 18).setStandardSlotBackground();
                int localX = x - xOffset;
                int localY = y - yOffset;
                if (localX >= 0 && localY >= 0 && localX < recipeWidth && localY < recipeHeight) {
                    int index = localX + localY * recipeWidth;
                    slot.addIngredients(recipe.getIngredientsGrid().get(index));
                }
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 22)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY));
    }

    @Override
    public void draw(
            LatexCreativeExtranalbodyCraftingRecipe recipe,
            mezz.jei.api.gui.ingredient.@NotNull IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        guiGraphics.fill(0, 0, WIDTH, HEIGHT, 0xFF151A24);
        guiGraphics.fill(2, 2, WIDTH - 2, HEIGHT - 2, 0xFF1C2333);
        arrow.draw(guiGraphics, 74, 22);
        guiGraphics.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                "Energy [LP]: " + recipe.getEnergyCost(),
                4,
                63,
                0x56A8FF,
                false
        );
        double seconds = recipe.getProcessTime() / 20.0D;
        guiGraphics.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                String.format(java.util.Locale.ROOT, "Time [s]: %.1f", seconds),
                4,
                74,
                0x8AE8CB,
                false
        );
    }
}
