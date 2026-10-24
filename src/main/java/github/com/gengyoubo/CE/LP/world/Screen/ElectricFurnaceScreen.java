package github.com.gengyoubo.CE.LP.world.Screen;
import github.com.gengyoubo.CE.util.AmountFormat;

import github.com.gengyoubo.CE.LP.world.Menu.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;

public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final HashMap<String, Object> guistate = ElectricFurnaceMenu.guistate;
    private static final ResourceLocation texture = MachineGuiStyle.texture("electric_furnace");

    public ElectricFurnaceScreen(ElectricFurnaceMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (isHovering(8, 62, 160, 4, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, MachineGuiStyle.amount(this.menu.getEnergyStored(), this.menu.getMaxEnergyStored(), "LP"), mouseX, mouseY);
        }
        if (isHovering(70, 38, 36, 6, mouseX, mouseY)) {
            String time = AmountFormat.format(String.format(Locale.ROOT, "%.1f", this.menu.getProgress() / 20.0),
                    String.format(Locale.ROOT, "%.1f", this.menu.getMaxProgress() / 20.0));
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.changede.alloy_furnace.time", time), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        MachineGuiStyle.panel(guiGraphics, texture, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        MachineGuiStyle.bar(guiGraphics, this.leftPos + 70, this.topPos + 38, 36, 6,
                this.menu.getProgress(), this.menu.getMaxProgress(), MachineGuiStyle.PROGRESS);
        MachineGuiStyle.bar(guiGraphics, this.leftPos + 8, this.topPos + 62, 160, 4,
                this.menu.getEnergyStored(), this.menu.getMaxEnergyStored(), MachineGuiStyle.LP);
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
        guiGraphics.drawString(this.font, this.title, 8, 6, MachineGuiStyle.TITLE, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 72, MachineGuiStyle.LABEL, false);
    }
}
