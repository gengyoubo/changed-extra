package github.com.gengyoubo.CE.compat.mekanism.gas;

import github.com.gengyoubo.CE.LP.world.Screen.MachineGuiStyle;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public final class ReverserScreen extends AbstractContainerScreen<ReverserMenu> {
    public ReverserScreen(ReverserMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 219; inventoryLabelY = 123; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.changede.reverser.drain"), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 121, topPos + 4, 48, 16).tooltip(Tooltip.create(Component.translatable("gui.changede.reverser.drain_hint"))).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
        for (boolean output : new boolean[]{false, true}) {
            if (isHovering(output ? 130 : 24, 31, 22, 44, mouseX, mouseY)) {
                GasStack gas = menu.gas(output);
                graphics.renderComponentTooltip(font, List.of(gas.getTextComponent(), Component.literal(gas.getAmount() + " / 10000 mB")), mouseX, mouseY);
            }
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineGuiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight, false, 0xFFA1C66B);
        for (var slot : menu.slots) MachineGuiStyle.slot(graphics, leftPos + slot.x, topPos + slot.y);
        for (boolean output : new boolean[]{false, true}) {
            int x = leftPos + (output ? 130 : 24), y = topPos + 31;
            graphics.fill(x - 1, y - 1, x + 23, y + 45, 0xFF292D30);
            GasStack gas = menu.gas(output);
            int height = (int) (44 * gas.getAmount() / ReverserBlockEntity.TANK_CAPACITY);
            if (height > 0) graphics.fill(x, y + 44 - height, x + 22, y + 44, 0xFF000000 | gas.getType().getTint());
        }
        MachineGuiStyle.bar(graphics, leftPos + 61, topPos + 48, 54, 8, menu.value(2), menu.value(3), 0xFF85B54D);
        MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 96, 160, 5, menu.value(0), ReverserBlockEntity.ENERGY_CAPACITY, 0xFFF0F0E8);
        MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 114, 160, 5, menu.value(1), ReverserBlockEntity.ENERGY_CAPACITY, 0xFF685578);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, 0x303030, false);
        var inputLabel = Component.translatable("gui.changede.reverser.input");
        var outputLabel = Component.translatable("gui.changede.reverser.output");
        graphics.drawString(font, inputLabel, 35 - font.width(inputLabel) / 2, 20, 0x303030, false);
        graphics.drawString(font, outputLabel, 141 - font.width(outputLabel) / 2, 20, 0x303030, false);
        graphics.drawString(font, String.format(java.util.Locale.ROOT, "%.1f/%.1fs", menu.value(2) / 20.0, menu.value(3) / 20.0), 59, 62, 0x303030, false);
        int status = Math.max(0, Math.min(ReverserBlockEntity.Status.values().length - 1, menu.value(10)));
        graphics.drawString(font, Component.translatable("state.changede.reverser." + ReverserBlockEntity.Status.values()[status].name().toLowerCase(java.util.Locale.ROOT)), 8, 77, 0x303030, false);
        graphics.drawString(font, "WLP " + menu.value(0) + "/100000  " + menu.value(4) + "/s", 8, 87, 0x444444, false);
        graphics.drawString(font, "DLP " + menu.value(1) + "/100000  " + menu.value(5) + "/s", 8, 105, 0x444444, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x303030, false);
    }
}
