package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.world.Menu.BasicLatexPurifierMenu;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class BasicLatexPurifierScreen extends AbstractContainerScreen<BasicLatexPurifierMenu> {
    private static final ResourceLocation TEXTURE = MachineGuiStyle.texture("basic_latex_purifier");

    public BasicLatexPurifierScreen(BasicLatexPurifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 176;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(77, 30, 12, 30, mouseX, mouseY)) {
            FluidStack fluid = menu.getStoredFluid();
            Component amount = MachineGuiStyle.amount(menu.getFluidAmount(), menu.getTankCapacity(), "mB");
            graphics.renderComponentTooltip(font, fluid.isEmpty() ? List.of(amount)
                    : List.of(fluid.getDisplayName(), amount), mouseX, mouseY);
        }
        if (isHovering(8, 70, 160, 4, mouseX, mouseY)) {
            graphics.renderTooltip(font, MachineGuiStyle.amount(menu.getEnergy(), menu.getMaxEnergy(), "LP"), mouseX, mouseY);
        }
        if (isHovering(55, 38, 18, 5, mouseX, mouseY)) graphics.renderTooltip(font, time(menu.getItemProgress()), mouseX, mouseY);
        if (isHovering(93, 49, 24, 5, mouseX, mouseY)) graphics.renderTooltip(font, time(menu.getFluidProgress()), mouseX, mouseY);
    }

    private Component time(int progress) {
        return Component.translatable("gui.changede.alloy_furnace.time", AmountFormat.format(
                String.format(Locale.ROOT, "%.1f", progress / 20.0), String.format(Locale.ROOT, "%.1f", menu.getMaxProgress() / 20.0)));
    }

    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineGuiStyle.panel(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight);
        MachineGuiStyle.fluid(graphics, menu.getStoredFluid(), menu.getTankCapacity(), leftPos + 77, topPos + 30, 12, 30);
        MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 70, 160, 4, menu.getEnergy(), menu.getMaxEnergy(), MachineGuiStyle.LP);
        MachineGuiStyle.bar(graphics, leftPos + 55, topPos + 38, 18, 5, menu.getItemProgress(), menu.getMaxProgress(), MachineGuiStyle.PROGRESS);
        MachineGuiStyle.bar(graphics, leftPos + 93, topPos + 49, 24, 5, menu.getFluidProgress(), menu.getMaxProgress(), 0xFF85D9FF);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, MachineGuiStyle.TITLE, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.items"), 27, 17, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.fluid"), 71, 17, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.output"), 118, 17, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, playerInventoryTitle, 8, 80, MachineGuiStyle.LABEL, false);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256) {
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
