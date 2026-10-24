package github.com.gengyoubo.CE.JEI;

import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.recipe.IngotFillingRecipe;
import github.com.gengyoubo.CE.LP.world.Screen.MachineGuiStyle;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

public final class IngotFillingCategory implements IRecipeCategory<IngotFillingRecipe> {
    public static final RecipeType<IngotFillingRecipe> TYPE=RecipeType.create("changede","ingot_filling",IngotFillingRecipe.class);
    private static final ResourceLocation BACKGROUND=MachineGuiStyle.texture("jei_ingot_filling");
    private final IDrawable icon;
    public IngotFillingCategory(IGuiHelper helper) { icon=helper.createDrawableItemStack(new ItemStack(CELPBlock.INGOT_FILLER.get())); }
    @Override public RecipeType<IngotFillingRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.changede.ingot_filler"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 150; }
    @Override public int getHeight() { return 72; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder,IngotFillingRecipe recipe,IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT,8,8).addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(RecipeIngredientRole.INPUT,40,8).addIngredient(ForgeTypes.FLUID_STACK,recipe.getFluid())
                .setFluidRenderer(recipe.getFluidAmount(),false,16,16);
        builder.addSlot(RecipeIngredientRole.OUTPUT,112,8).addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }
    @Override public void draw(IngotFillingRecipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        MachineGuiStyle.panel(graphics,BACKGROUND,0,0,getWidth(),getHeight());
        MachineGuiStyle.loopingBar(graphics,70,13,32,6,(int)Math.round(recipe.getSeconds()*20),MachineGuiStyle.PROGRESS);
        var font=Minecraft.getInstance().font;
        graphics.drawString(font,Component.literal(recipe.getFluidAmount()+" mB · "+recipe.getFluidPerSecond()+" mB/s"),4,34,MachineGuiStyle.LABEL,false);
        graphics.drawString(font,Component.translatable("gui.changede.alloy_furnace.time",String.format(Locale.ROOT,"%.1f",recipe.getSeconds())),4,46,MachineGuiStyle.LABEL,false);
        graphics.drawString(font,Component.translatable("gui.changede.alloy_furnace.power",recipe.getLpPerSecond()),4,58,0xFF007878,false);
    }
}
