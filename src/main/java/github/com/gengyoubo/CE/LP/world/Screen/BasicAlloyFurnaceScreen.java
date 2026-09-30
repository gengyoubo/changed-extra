package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.world.Menu.BasicAlloyFurnaceMenu;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class BasicAlloyFurnaceScreen extends AbstractContainerScreen<BasicAlloyFurnaceMenu> {
    public BasicAlloyFurnaceScreen(BasicAlloyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 90;
    }

    private Component energyText() {
        return Component.literal(AmountFormat.format(menu.getEnergyStored(), menu.getMaxEnergyStored(), "LP"));
    }
    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(8, 80, 160, 5, mouseX, mouseY)) graphics.renderTooltip(font, energyText(), mouseX, mouseY);
    }
    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineGuiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight, false, 0xFFE79835);
        for (var slot : menu.slots) {
            MachineGuiStyle.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        graphics.fill(leftPos + 85, topPos + 38, leftPos + 109, topPos + 44, 0xFF626A75);
        if (menu.getProcessTicks() > 0) {
            int width = (int) Math.min(24, (long) menu.getProgress() * 24 / menu.getProcessTicks());
            graphics.fill(leftPos + 85, topPos + 38, leftPos + 85 + width, topPos + 44, 0xFFFFA740);
        }
        graphics.fill(leftPos + 8, topPos + 80, leftPos + 168, topPos + 85, 0xFF596271);
        if (menu.getMaxEnergyStored() > 0) {
            int width = (int) Math.min(160, (long) menu.getEnergyStored() * 160 / menu.getMaxEnergyStored());
            graphics.fill(leftPos + 8, topPos + 80, leftPos + 8 + width, topPos + 85, 0xFF56A8FF);
        }
    }
    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x303030, false);
        graphics.drawString(font, energyText(), 8, 60, 0x285589, false);
        if (menu.getProcessTicks() > 0) {
            String progress = AmountFormat.format(String.format(Locale.ROOT, "%.1f", menu.getProgress() / 20.0),
                    String.format(Locale.ROOT, "%.1f", menu.getProcessTicks() / 20.0));
            graphics.drawString(font, Component.translatable("gui.changede.alloy_furnace.time", progress), 8, 70, 0x303030, false);
            graphics.drawString(font, Component.translatable("gui.changede.alloy_furnace.power", menu.getLpPerSecond()), 100, 70, 0x285589, false);
        }
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x303030, false);
    }
}
