package github.com.gengyoubo.CE.JEI;

import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.recipe.IngotFillingRecipe;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

public final class IngotFillingCategory implements IRecipeCategory<IngotFillingRecipe> {
    public static final RecipeType<IngotFillingRecipe> TYPE=RecipeType.create("changede","ingot_filling",IngotFillingRecipe.class);
    private final IDrawable icon,arrow;
    public IngotFillingCategory(IGuiHelper helper) { icon=helper.createDrawableItemStack(new ItemStack(CELPBlock.INGOT_FILLER.get()));arrow=helper.getRecipeArrow(); }
    @Override public RecipeType<IngotFillingRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.changede.ingot_filler"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 150; }
    @Override public int getHeight() { return 66; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder,IngotFillingRecipe recipe,IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT,8,8).setStandardSlotBackground().addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(RecipeIngredientRole.INPUT,40,8).setStandardSlotBackground().addIngredient(ForgeTypes.FLUID_STACK,recipe.getFluid())
                .setFluidRenderer(recipe.getFluidAmount(),false,16,16);
        builder.addSlot(RecipeIngredientRole.OUTPUT,112,8).setOutputSlotBackground().addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }
    @Override public void draw(IngotFillingRecipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        arrow.draw(graphics,77,8);var font=Minecraft.getInstance().font;
        graphics.drawString(font,Component.literal(recipe.getFluidAmount()+" mB · "+recipe.getFluidPerSecond()+" mB/s"),4,34,0x808080,false);
        graphics.drawString(font,Component.translatable("gui.changede.alloy_furnace.time",String.format(Locale.ROOT,"%.1f",recipe.getSeconds())),4,46,0x808080,false);
        graphics.drawString(font,Component.translatable("gui.changede.alloy_furnace.power",recipe.getLpPerSecond()),4,58,0x56A8FF,false);
    }
}
