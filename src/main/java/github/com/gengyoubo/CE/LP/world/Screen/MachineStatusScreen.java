package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.world.Menu.MachineStatusMenu;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import net.minecraft.client.gui.components.Button;

public class MachineStatusScreen extends AbstractContainerScreen<MachineStatusMenu> {
    /** Indexed by menu value 9: pump, fluid generator, WLP converter, DLP converter, producer. */
    private static final ResourceLocation[] TEXTURES = {
            MachineGuiStyle.texture("machine_status_pump"), MachineGuiStyle.texture("machine_status_generator"),
            MachineGuiStyle.texture("machine_status_converter"), MachineGuiStyle.texture("machine_status_converter"),
            MachineGuiStyle.texture("machine_status_producer")};
    private Button redstone;
    public MachineStatusScreen(MachineStatusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 92;
    }

    @Override protected void init() {
        super.init();
        redstone = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 8, topPos + 44, 108, 20).build());
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
        MachineGuiStyle.panel(graphics, TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, kind))], leftPos, topPos, imageWidth, imageHeight);
        if (menu.getValue(1) > 0) MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 28, 108, 8, menu.getValue(0), menu.getValue(1), MachineGuiStyle.LP);
        if (menu.getValue(3) > 0) MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 48, 108, 8, menu.getValue(2), menu.getValue(3), kind == 2 ? MachineGuiStyle.WLP : MachineGuiStyle.DLP);
        if (menu.getValue(5) > 0) MachineGuiStyle.fluid(graphics, menu.getFluid(), menu.getValue(5), leftPos + 138, topPos + 27, 20, 56);
        if (kind == 4) MachineGuiStyle.bar(graphics, leftPos + 8, topPos + 48, 108, 8, menu.getValue(7), menu.getValue(8), MachineGuiStyle.PROGRESS);
        graphics.renderItem(menu.getMachineIcon(), leftPos + 151, topPos + 3);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, MachineGuiStyle.TITLE, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, MachineGuiStyle.LABEL, false);
        if (menu.getValue(9) == 0) graphics.drawString(font, Component.translatable("gui.changede.machine.passive_pump"), 8, 27, MachineGuiStyle.LABEL, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int kind = menu.getValue(9);
        if (menu.getValue(1) > 0 && isHovering(8, 28, 108, 8, mouseX, mouseY))
            graphics.renderTooltip(font, MachineGuiStyle.amount(menu.getValue(0), menu.getValue(1), "LP"), mouseX, mouseY);
        if (isHovering(8, 48, 108, 8, mouseX, mouseY)) {
            if (menu.getValue(3) > 0) graphics.renderTooltip(font, MachineGuiStyle.amount(menu.getValue(2), menu.getValue(3), kind == 2 ? "WLP" : "DLP"), mouseX, mouseY);
            if (kind == 4) graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.changede.machine.production"),
                    Component.literal(AmountFormat.format(menu.getValue(7), menu.getValue(8)))), mouseX, mouseY);
        }
        if (menu.getValue(5) > 0 && isHovering(138, 27, 20, 56, mouseX, mouseY)) {
            var fluid = menu.getFluid();
            Component amount = MachineGuiStyle.amount(menu.getValue(4), menu.getValue(5), "mB");
            graphics.renderComponentTooltip(font, fluid.isEmpty() ? List.of(amount) : List.of(fluid.getDisplayName(), amount), mouseX, mouseY);
        }
    }
}
