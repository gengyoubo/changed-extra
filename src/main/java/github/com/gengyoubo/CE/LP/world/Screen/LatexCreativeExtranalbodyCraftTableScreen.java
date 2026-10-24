package github.com.gengyoubo.CE.LP.world.Screen;
import github.com.gengyoubo.CE.util.AmountFormat;

import github.com.gengyoubo.CE.LP.world.Menu.LatexCreativeExtranalbodyCraftTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class LatexCreativeExtranalbodyCraftTableScreen extends AbstractContainerScreen<LatexCreativeExtranalbodyCraftTableMenu> {
    private static final ResourceLocation TEXTURE = MachineGuiStyle.texture("latex_creative_extranalbody_craft_table");

    public LatexCreativeExtranalbodyCraftTableScreen(LatexCreativeExtranalbodyCraftTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 178;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (isHovering(8, 76, 160, 4, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, MachineGuiStyle.amount(this.menu.getEnergyStored(), this.menu.getMaxEnergyStored(), "LP"), mouseX, mouseY);
        }
        if (isHovering(76, 40, 40, 6, mouseX, mouseY)) {
            String time = AmountFormat.format(String.format(Locale.ROOT, "%.1f", this.menu.getProgress() / 20.0),
                    String.format(Locale.ROOT, "%.1f", this.menu.getMaxProgress() / 20.0));
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.changede.alloy_furnace.time", time), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        MachineGuiStyle.panel(guiGraphics, TEXTURE, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        MachineGuiStyle.bar(guiGraphics, this.leftPos + 76, this.topPos + 40, 40, 6,
                this.menu.getProgress(), this.menu.getMaxProgress(), 0xFF5FE3B1);
        MachineGuiStyle.bar(guiGraphics, this.leftPos + 8, this.topPos + 76, 160, 4,
                this.menu.getEnergyStored(), this.menu.getMaxEnergyStored(), MachineGuiStyle.LP);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 5, MachineGuiStyle.TITLE, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 85, MachineGuiStyle.LABEL, false);
    }
}
