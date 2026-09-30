package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.world.Menu.MachineStatusMenu;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import net.minecraft.client.gui.components.Button;

public class MachineStatusScreen extends AbstractContainerScreen<MachineStatusMenu> {
    private Button redstone;
    public MachineStatusScreen(MachineStatusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 196;
        inventoryLabelY = 101;
    }

    @Override protected void init() {
        super.init();
        redstone = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 8, topPos + 49, 108, 20).build());
        updateButton();
    }

    @Override protected void containerTick() { super.containerTick(); updateButton(); }

    private void updateButton() {
        if (redstone == null) return;
        redstone.visible = menu.getValue(9) == 1;
        var modes = github.com.gengyoubo.CE.LP.BlockEntity.RedstoneMode.values();
        int mode = Math.max(0, Math.min(modes.length - 1, menu.getValue(10)));
        redstone.setMessage(Component.translatable("gui.changede.redstone." + modes[mode].name().toLowerCase(java.util.Locale.ROOT)));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int kind = menu.getValue(9);
        MachineGuiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight, false,
                kind == 3 ? 0xFF73599C : kind == 4 ? 0xFFE79835 : 0xFF58AAC5);
        for (var slot : menu.slots) MachineGuiStyle.slot(graphics, leftPos + slot.x, topPos + slot.y);
        if (menu.getValue(1) > 0) MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 36, 108, 8, menu.getValue(0), menu.getValue(1), 0xFF56A8FF);
        if (menu.getValue(3) > 0) MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 62, 108, 8, menu.getValue(2), menu.getValue(3), kind == 2 ? 0xFFF5F5F5 : 0xFF100D18);
        if (menu.getValue(5) > 0) MachineGuiStyle.fluid(graphics, menu.getFluid(), menu.getValue(5), leftPos + 138, topPos + 27, 20, 56);
        if (kind == 4) MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 62, 108, 8, menu.getValue(7), menu.getValue(8), 0xFFFFA740);
        graphics.renderItem(menu.getMachineIcon(), leftPos + 151, topPos + 3);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFF27313C, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF27313C, false);
        if (menu.getValue(1) > 0) graphics.drawString(font, AmountFormat.format(menu.getValue(0), menu.getValue(1), "LP"), 8, 25, 0xFF285589, false);
        if (menu.getValue(3) > 0) graphics.drawString(font, AmountFormat.format(menu.getValue(2), menu.getValue(3), menu.getValue(9) == 2 ? "WLP" : "DLP"), 8, 51, 0xFF27313C, false);
        if (menu.getValue(9) == 4) graphics.drawString(font, Component.translatable("gui.changede.machine.production"), 8, 51, 0xFF27313C, false);
        if (menu.getValue(9) == 0) graphics.drawString(font, Component.translatable("gui.changede.machine.passive_pump"), 8, 27, 0xFF27313C, false);
        if (menu.getValue(5) > 0) graphics.drawString(font, AmountFormat.format(menu.getValue(4), menu.getValue(5), "mB"), 8, 85, 0xFF27313C, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.getValue(5) > 0 && isHovering(136, 25, 24, 60, mouseX, mouseY)) {
            var fluid = menu.getFluid();
            Component amount = Component.literal(AmountFormat.format(menu.getValue(4), menu.getValue(5), "mB"));
            graphics.renderComponentTooltip(font, fluid.isEmpty() ? List.of(amount) : List.of(fluid.getDisplayName(), amount), mouseX, mouseY);
        }
    }
}
