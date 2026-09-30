package github.com.gengyoubo.CE.LP.world.Screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.CE.LP.world.Menu.BasicLatexPurifierMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class BasicLatexPurifierScreen extends AbstractContainerScreen<BasicLatexPurifierMenu> {
    public BasicLatexPurifierScreen(BasicLatexPurifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 184;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(75, 28, 16, 54, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(menu.getFluidAmount() + " / " + menu.getTankCapacity() + " mB"), mouseX, mouseY);
        }
        if (isHovering(8, 79, 160, 4, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " LP"), mouseX, mouseY);
        }
    }

    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF252932);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 20, 0xFF3B414D);
        graphics.fill(leftPos + 1, topPos + 83, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF343A45);
        // slot and tank wells
        graphics.fill(leftPos + 26, topPos + 26, leftPos + 62, topPos + 62, 0xFF16191F);
        graphics.fill(leftPos + 116, topPos + 26, leftPos + 152, topPos + 62, 0xFF16191F);
        graphics.fill(leftPos + 75, topPos + 28, leftPos + 91, topPos + 82, 0xFF11151A);
        int fluidHeight = menu.getTankCapacity() > 0 ? menu.getFluidAmount() * 50 / menu.getTankCapacity() : 0;
        if (fluidHeight > 0) graphics.fill(leftPos + 77, topPos + 30 + 50 - fluidHeight, leftPos + 89, topPos + 80, 0xFFBDE6FF);
        graphics.fill(leftPos + 8, topPos + 79, leftPos + 168, topPos + 83, 0xFF15191E);
        int energyWidth = menu.getMaxEnergy() > 0 ? menu.getEnergy() * 160 / menu.getMaxEnergy() : 0;
        if (energyWidth > 0) graphics.fill(leftPos + 8, topPos + 79, leftPos + 8 + energyWidth, topPos + 83, 0xFF56A8FF);

        int max = Math.max(1, menu.getMaxProgress());
        int itemWidth = menu.getItemProgress() * 24 / max;
        int fluidWidth = menu.getFluidProgress() * 24 / max;
        graphics.fill(leftPos + 63, topPos + 35, leftPos + 63 + itemWidth, topPos + 40, 0xFFFFD16B);
        graphics.fill(leftPos + 93, topPos + 49, leftPos + 93 + fluidWidth, topPos + 54, 0xFF85D9FF);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.items"), 27, 15, 0xD8DDE5, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.fluid"), 71, 15, 0xD8DDE5, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.output"), 118, 15, 0xD8DDE5, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.energy", menu.getEnergy(), menu.getMaxEnergy()), 8, 65, 0x88BBFF, false);
        graphics.drawString(font, playerInventoryTitle, 8, 89, 0xFFFFFF, false);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256) {
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
