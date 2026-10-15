package github.com.gengyoubo.CE.compat.mekanism.gas;

import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class GasJei implements IRecipeCategory<ReversingRecipe> {
    public static final RecipeType<ReversingRecipe> TYPE = RecipeType.create("changede", "reversing", ReversingRecipe.class);
    private final IDrawable background, icon;
    private GasJei(IGuiHelper helper) { background = helper.createBlankDrawable(170, 85); icon = helper.createDrawableItemStack(GasCompat.REVERSER.getItemStack()); }
    public static void categories(IRecipeCategoryRegistration registration) { registration.addRecipeCategories(new GasJei(registration.getJeiHelpers().getGuiHelper())); }
    public static void recipes(IRecipeRegistration registration) {
        var world = Minecraft.getInstance().level;
        if (world != null) registration.addRecipes(TYPE, world.getRecipeManager().getAllRecipesFor(GasCompat.REVERSING_TYPE.get()).stream().filter(recipe -> !recipe.isIncomplete()).toList());
        registration.addItemStackInfo(GasCompat.GAS_PIPE.getItemStack(), Component.translatable("jei.changede.gas.pipe"));
        registration.addItemStackInfo(GasCompat.REVERSER.getItemStack(), Component.translatable("tooltip.changede.reverser"), Component.translatable("tooltip.changede.reverser.ports"));
    }
    public static void catalysts(IRecipeCatalystRegistration registration) { registration.addRecipeCatalyst(GasCompat.REVERSER.getItemStack(), TYPE); }
    @Override public RecipeType<ReversingRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.changede.reverser"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, ReversingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 8).addIngredients(MekanismJEI.TYPE_GAS, recipe.getInput().getRepresentations());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 136, 8).addIngredients(MekanismJEI.TYPE_GAS, recipe.getOutputDefinition());
    }
    @Override public void draw(ReversingRecipe recipe, IRecipeSlotsView ignored, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, "→", 80, 12, 0x666666, false);
        var input = recipe.getInput().getRepresentations().get(0);
        graphics.drawString(font, input.getAmount() + " mB", 6, 29, 0x444444, false);
        graphics.drawString(font, recipe.getOutputDefinition().get(0).getAmount() + " mB", 130, 29, 0x444444, false);
        graphics.drawString(font, Component.translatable("gui.changede.reverser.progress", 0, recipe.ticks() / 20.0), 6, 44, 0x444444, false);
        graphics.drawString(font, "WLP " + recipe.whiteRate() + "/s   DLP " + recipe.darkRate() + "/s", 6, 57, 0x444444, false);
        long whiteTotal = ((long) recipe.whiteRate() * recipe.ticks() + 19) / 20;
        long darkTotal = ((long) recipe.darkRate() * recipe.ticks() + 19) / 20;
        graphics.drawString(font, Component.translatable("jei.changede.gas.energy", whiteTotal, darkTotal), 6, 70, 0x444444, false);
    }
}
