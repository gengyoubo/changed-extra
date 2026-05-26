package github.com.gengyoubo.CE.LP.world.Screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.CE.LP.world.Menu.LatexCreativeExtranalbodyCraftTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class LatexCreativeExtranalbodyCraftTableScreen extends AbstractContainerScreen<LatexCreativeExtranalbodyCraftTableMenu> {
    public LatexCreativeExtranalbodyCraftTableScreen(LatexCreativeExtranalbodyCraftTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF12151E);
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + 78, 0xFF1D2230);
        guiGraphics.fill(left + 4, top + 80, left + this.imageWidth - 4, top + this.imageHeight - 4, 0xFF161A24);

        // 3x3 machine grid + output slot
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = left + 16 + col * 18;
                int sy = top + 16 + row * 18;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF2B3550);
            }
        }
        guiGraphics.fill(left + 123, top + 34, left + 141, top + 52, 0xFF3A5B30);

        // Progress beam
        int progress = this.menu.getScaledProgress(52);
        if (progress > 0) {
            guiGraphics.fill(left + 66, top + 40, left + 66 + progress, top + 46, 0xFF5FE3B1);
        }

        // Energy bar (bottom)
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int w = Math.max(1, energy * 160 / maxEnergy);
            guiGraphics.fill(left + 8, top + 74, left + 8 + w, top + 77, 0xFF56A8FF);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xE8EDF8, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x9FAAC8, false);
        guiGraphics.drawString(this.font, "EXTRANALBODY SYNTH", 58, 27, 0x8AE8CB, false);
    }
}
