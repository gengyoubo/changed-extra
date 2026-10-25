package github.com.gengyoubo.CE.compat.mekanism.gas;

import mekanism.api.chemical.gas.GasStack;
import mekanism.client.gui.GuiUtils;
import mekanism.client.jei.ChemicalStackRenderer;
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
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Laid out like Mekanism's own JEI pages: gauges for the gases, bars for WLP and DLP, its arrow between. */
public final class GasJei implements IRecipeCategory<ReversingRecipe> {
    public static final RecipeType<ReversingRecipe> TYPE = RecipeType.create("changede", "reversing", ReversingRecipe.class);
    private static final ResourceLocation BAR = mekanism("bar/base.png"), PROGRESS = mekanism("progress/large_right.png"),
            TICKS = mekanism("gauge/standard.png"), GAUGE_INPUT = mekanism("gauge/red.png"), GAUGE_OUTPUT = mekanism("gauge/blue.png");
    private static final int WIDTH = 150, HEIGHT = 60, INPUT_X = 10, OUTPUT_X = 122, WLP_X = 0, DLP_X = 144, ARROW_X = 51, ARROW_Y = 26;
    private final IDrawable background, icon, ticks;
    private GasJei(IGuiHelper helper) {
        background = helper.createBlankDrawable(WIDTH, HEIGHT);
        icon = helper.createDrawableItemStack(GasCompat.REVERSER.getItemStack());
        ticks = helper.drawableBuilder(TICKS, 0, 0, 16, HEIGHT - 2).setTextureSize(16, HEIGHT - 2).build();
    }
    private static ResourceLocation mekanism(String path) { return ResourceLocation.fromNamespaceAndPath("mekanism", "gui/" + path); }
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
        var inputs = recipe.getInput().getRepresentations();
        var outputs = recipe.getOutputDefinition();
        // Both gauges share one scale, as Mekanism does, so the larger amount fills its gauge.
        long capacity = Math.max(inputs.stream().mapToLong(stack -> stack.getAmount()).max().orElse(1),
                outputs.stream().mapToLong(stack -> stack.getAmount()).max().orElse(1));
        ChemicalStackRenderer<GasStack> renderer = new ChemicalStackRenderer<>(capacity, 16, HEIGHT - 2);
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X + 1, 1).addIngredients(MekanismJEI.TYPE_GAS, inputs)
                .setCustomRenderer(MekanismJEI.TYPE_GAS, renderer).setOverlay(ticks, 0, 0);
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, 1).addIngredients(MekanismJEI.TYPE_GAS, outputs)
                .setCustomRenderer(MekanismJEI.TYPE_GAS, renderer).setOverlay(ticks, 0, 0);
    }
    @Override public void draw(ReversingRecipe recipe, IRecipeSlotsView ignored, GuiGraphics graphics, double mouseX, double mouseY) {
        GuiUtils.renderExtendedTexture(graphics, GAUGE_INPUT, 2, 2, INPUT_X, 0, 18, HEIGHT);
        GuiUtils.renderExtendedTexture(graphics, GAUGE_OUTPUT, 2, 2, OUTPUT_X, 0, 18, HEIGHT);
        bar(graphics, WLP_X, 0xFFF0F0E8, 0xFFFFFFFF);
        bar(graphics, DLP_X, 0xFF3A3446, 0xFF6A6280);
        // The arrow fills once per recipe duration, like Mekanism's animated JEI progress.
        int ticks = Math.max(1, recipe.ticks());
        graphics.blit(PROGRESS, ARROW_X, ARROW_Y, 0, 0, 48, 8, 48, 24);
        graphics.blit(PROGRESS, ARROW_X, ARROW_Y, 0, 8, (int) (48 * (Util.getMillis() / 50 % ticks) / ticks), 8, 48, 24);
    }
    private static void bar(GuiGraphics graphics, int x, int color, int lit) {
        GuiUtils.renderExtendedTexture(graphics, BAR, 2, 2, x, 0, 6, HEIGHT);
        graphics.fill(x + 1, 1, x + 5, HEIGHT - 1, color);
        graphics.fill(x + 1, 1, x + 2, HEIGHT - 1, lit);
    }
    @Override public List<Component> getTooltipStrings(ReversingRecipe recipe, IRecipeSlotsView view, double mouseX, double mouseY) {
        if (mouseY < 0 || mouseY >= HEIGHT) return List.of();
        if (mouseX >= ARROW_X && mouseX < ARROW_X + 48 && mouseY >= ARROW_Y && mouseY < ARROW_Y + 8)
            return List.of(Component.translatable("gui.changede.reverser.progress", 0, recipe.ticks() / 20.0));
        boolean white = mouseX >= WLP_X && mouseX < WLP_X + 6, dark = mouseX >= DLP_X && mouseX < DLP_X + 6;
        if (!white && !dark) return List.of();
        long whiteTotal = ((long) recipe.whiteRate() * recipe.ticks() + 19) / 20;
        long darkTotal = ((long) recipe.darkRate() * recipe.ticks() + 19) / 20;
        return List.of(Component.literal(white ? recipe.whiteRate() + " WLP/s" : recipe.darkRate() + " DLP/s"),
                Component.translatable("jei.changede.gas.energy", whiteTotal, darkTotal));
    }
}
