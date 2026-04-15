package github.com.gengyoubo.LP.world.Screen;

import github.com.gengyoubo.LP.world.Menu.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse("changede:textures/screens/basic_generator_block_entity.png");

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 230, 180);

        int progressWidth = menu.getScaledProgress(24);
        if (progressWidth > 0) {
            guiGraphics.fill(this.leftPos + 79, this.topPos + 34, this.leftPos + 79 + progressWidth, this.topPos + 50, 0xFFFFA500);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "Electric Furnace", 8, 6, 0x404040, false);
        guiGraphics.drawString(this.font, menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " LP", 8, 18, 0x404040, false);
    }
}
