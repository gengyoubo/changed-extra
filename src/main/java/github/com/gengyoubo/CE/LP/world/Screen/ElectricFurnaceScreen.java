package github.com.gengyoubo.CE.LP.world.Screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.CE.LP.world.Menu.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final HashMap<String, Object> guistate = ElectricFurnaceMenu.guistate;
    private static final ResourceLocation texture = ResourceLocation.parse("changede:textures/screens/electric_furnace_block_entity.png");

    public ElectricFurnaceScreen(ElectricFurnaceMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private int getEnergyScaled() {
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();
        return maxEnergy > 0 && energy > 0 ? energy * 160 / maxEnergy : 0;
    }

    private Component getEnergyText() {
        return Component.literal(this.menu.getEnergyStored() + " / " + this.menu.getMaxEnergyStored() + " LP");
    }

    private int getCookProgressScaled() {
        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return progress * 24 / maxProgress;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (isHovering(8, 74, 160, 4, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, getEnergyText(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.blit(
                texture,
                this.leftPos,
                this.topPos,
                0, 0,
                this.imageWidth,
                this.imageHeight,
                this.imageWidth,
                this.imageHeight
        );

        int progressWidth = getCookProgressScaled();
        if (progressWidth > 0) {
            guiGraphics.fill(
                    this.leftPos + 76,
                    this.topPos + 35,
                    this.leftPos + 76 + progressWidth,
                    this.topPos + 49,
                    0xFFFFA500
            );
        }

        int energyWidth = getEnergyScaled();
        if (energyWidth > 0) {
            guiGraphics.fill(
                    this.leftPos + 8,
                    this.topPos + 74,
                    this.leftPos + 8 + energyWidth,
                    this.topPos + 78,
                    0xFF56A8FF
            );
        }

        RenderSystem.disableBlend();
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
        guiGraphics.drawString(this.font, getEnergyText(), 8, 62, 0x2D6FB7, false);
    }
}
