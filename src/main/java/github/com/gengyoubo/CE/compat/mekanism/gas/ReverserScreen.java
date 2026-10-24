package github.com.gengyoubo.CE.compat.mekanism.gas;

import github.com.gengyoubo.CE.LP.world.Screen.MachineGuiStyle;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import java.util.Locale;

public final class ReverserScreen extends AbstractContainerScreen<ReverserMenu> {
    private static final ResourceLocation TEXTURE = MachineGuiStyle.texture("reverser");
    public ReverserScreen(ReverserMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 204; inventoryLabelY = 110; }
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
        if (isHovering(61, 44, 54, 8, mouseX, mouseY)) graphics.renderTooltip(font, Component.translatable("gui.changede.reverser.progress",
                String.format(Locale.ROOT, "%.1f", menu.value(2) / 20.0), String.format(Locale.ROOT, "%.1f", menu.value(3) / 20.0)), mouseX, mouseY);
        if (isHovering(8, 84, 160, 5, mouseX, mouseY)) graphics.renderComponentTooltip(font, List.of(
                MachineGuiStyle.amount(menu.value(0), ReverserBlockEntity.ENERGY_CAPACITY, "WLP"), Component.literal(menu.value(4) + " WLP/s")), mouseX, mouseY);
        if (isHovering(8, 97, 160, 5, mouseX, mouseY)) graphics.renderComponentTooltip(font, List.of(
                MachineGuiStyle.amount(menu.value(1), ReverserBlockEntity.ENERGY_CAPACITY, "DLP"), Component.literal(menu.value(5) + " DLP/s")), mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineGuiStyle.panel(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight);
        for (boolean output : new boolean[]{false, true}) {
            int x = leftPos + (output ? 130 : 24), y = topPos + 31;
            GasStack gas = menu.gas(output);
            int height = (int) (44 * gas.getAmount() / ReverserBlockEntity.TANK_CAPACITY);
            if (height > 0) graphics.fill(x, y + 44 - height, x + 22, y + 44, 0xFF000000 | gas.getType().getTint());
        }
        MachineGuiStyle.bar(graphics, leftPos + 61, topPos + 44, 54, 8, menu.value(2), menu.value(3), 0xFF85B54D);
        MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 84, 160, 5, menu.value(0), ReverserBlockEntity.ENERGY_CAPACITY, MachineGuiStyle.WLP);
        MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 97, 160, 5, menu.value(1), ReverserBlockEntity.ENERGY_CAPACITY, MachineGuiStyle.DLP);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, MachineGuiStyle.TITLE, false);
        var inputLabel = Component.translatable("gui.changede.reverser.input");
        var outputLabel = Component.translatable("gui.changede.reverser.output");
        graphics.drawString(font, inputLabel, 35 - font.width(inputLabel) / 2, 19, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, outputLabel, 141 - font.width(outputLabel) / 2, 19, MachineGuiStyle.LABEL, false);
        int status = Math.max(0, Math.min(ReverserBlockEntity.Status.values().length - 1, menu.value(10)));
        var state = Component.translatable("state.changede.reverser." + ReverserBlockEntity.Status.values()[status].name().toLowerCase(Locale.ROOT));
        graphics.drawString(font, state, 88 - font.width(state) / 2, 60, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, MachineGuiStyle.LABEL, false);
    }
}
