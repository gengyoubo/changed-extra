package github.com.gengyoubo.CE.JEI;

import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.recipe.AlloyFurnaceRecipe;
import github.com.gengyoubo.CE.LP.world.Screen.MachineGuiStyle;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class AlloyFurnaceCategory implements IRecipeCategory<AlloyFurnaceRecipe> {
    public static final RecipeType<AlloyFurnaceRecipe> TYPE = RecipeType.create("changede", "alloy_furnace", AlloyFurnaceRecipe.class);
    private static final ResourceLocation BACKGROUND = MachineGuiStyle.texture("jei_alloy_furnace");
    private final IDrawable icon;

    public AlloyFurnaceCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(CELPBlock.BASIC_ALLOY_FURNACE.get()));
    }
    @Override public @NotNull RecipeType<AlloyFurnaceRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("block.changede.basic_alloy_furnace"); }
    @Override public int getWidth() { return 150; }
    @Override public int getHeight() { return 72; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder, AlloyFurnaceRecipe recipe, @NotNull IFocusGroup focuses) {
        var ingredients = recipe.getIngredients();
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 8).addIngredients(ingredients.get(0));
        builder.addSlot(RecipeIngredientRole.INPUT, 32, 8).addIngredients(ingredients.get(1));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 108, 8).addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }

    @Override public void draw(AlloyFurnaceRecipe recipe, @NotNull IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        MachineGuiStyle.panel(graphics, BACKGROUND, 0, 0, getWidth(), getHeight());
        MachineGuiStyle.loopingBar(graphics, 62, 13, 34, 6, (int) Math.round(recipe.getSeconds() * 20), MachineGuiStyle.PROGRESS);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("gui.changede.alloy_furnace.time", String.format(Locale.ROOT, "%.1f", recipe.getSeconds())), 4, 34, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, Component.translatable("gui.changede.alloy_furnace.power", recipe.getLpPerSecond()), 4, 46, 0xFF007878, false);
        graphics.drawString(font, Component.translatable("jei.changede.alloy_furnace.swappable"), 4, 58, MachineGuiStyle.LABEL, false);
    }
}
