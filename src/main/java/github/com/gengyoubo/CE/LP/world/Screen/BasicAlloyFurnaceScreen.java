package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.world.Menu.BasicAlloyFurnaceMenu;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class BasicAlloyFurnaceScreen extends AbstractContainerScreen<BasicAlloyFurnaceMenu> {
    private static final ResourceLocation TEXTURE = MachineGuiStyle.texture("basic_alloy_furnace");

    public BasicAlloyFurnaceScreen(BasicAlloyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(8, 62, 160, 4, mouseX, mouseY)) graphics.renderComponentTooltip(font, List.of(
                MachineGuiStyle.amount(menu.getEnergyStored(), menu.getMaxEnergyStored(), "LP"),
                Component.translatable("gui.changede.alloy_furnace.power", menu.getLpPerSecond())), mouseX, mouseY);
        if (isHovering(85, 38, 24, 6, mouseX, mouseY)) {
            String progress = AmountFormat.format(String.format(Locale.ROOT, "%.1f", menu.getProgress() / 20.0),
                    String.format(Locale.ROOT, "%.1f", menu.getProcessTicks() / 20.0));
            graphics.renderTooltip(font, Component.translatable("gui.changede.alloy_furnace.time", progress), mouseX, mouseY);
        }
    }
    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineGuiStyle.panel(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight);
        MachineGuiStyle.bar(graphics, leftPos + 85, topPos + 38, 24, 6, menu.getProgress(), menu.getProcessTicks(), MachineGuiStyle.PROGRESS);
        MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 62, 160, 4, menu.getEnergyStored(), menu.getMaxEnergyStored(), MachineGuiStyle.LP);
    }
    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, MachineGuiStyle.TITLE, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, MachineGuiStyle.LABEL, false);
    }
}
