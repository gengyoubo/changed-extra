package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.DimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerChannels;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.DimensionTowerConfigPacket;
import github.com.gengyoubo.CE.LP.world.Menu.DimensionSpaceTowerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DimensionSpaceTowerScreen extends AbstractContainerScreen<DimensionSpaceTowerMenu> {
    private static final ResourceLocation TEXTURE = MachineGuiStyle.texture("dimension_space_tower");
    private EditBox channelInput;
    private Button enabledButton;
    private Button typeButton;
    private Button modeButton;
    private Button redstoneButton;
    private int lastChannel = -1;

    public DimensionSpaceTowerScreen(DimensionSpaceTowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 260;
        imageHeight = 196;
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("screen.changede.dimension_tower." + key, args);
    }

    private void send(int action, int value) {
        CENetwork.sendToServer(new DimensionTowerConfigPacket(menu.getBlockPos(), action, value));
    }

    private Button button(int x, int y, int width, int action, String tooltip) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> send(action, 0))
                .bounds(leftPos + x, topPos + y, width, 20).build());
        if (tooltip != null) button.setTooltip(Tooltip.create(text(tooltip)));
        return button;
    }

    @Override protected void init() {
        super.init();
        channelInput = new EditBox(font, leftPos + 70, topPos + 29, 92, 18, text("channel"));
        channelInput.setMaxLength(4);
        channelInput.setFilter(value -> value.matches("[0-9]*"));
        channelInput.setTooltip(Tooltip.create(text("channel_help")));
        addRenderableWidget(channelInput);
        addRenderableWidget(Button.builder(Component.translatable("screen.changede.space_tower.apply"), ignored -> {
            String input = channelInput.getValue();
            int value = input.isEmpty() ? 0 : Integer.parseInt(input);
            if (value <= DimensionTowerChannels.MAX_CHANNEL) send(DimensionTowerConfigPacket.SET_CHANNEL, value);
        }).bounds(leftPos + 174, topPos + 28, 74, 20).build());
        typeButton = button(12, 56, 112, DimensionTowerConfigPacket.CYCLE_TYPE, "type_help");
        modeButton = button(136, 56, 112, DimensionTowerConfigPacket.CYCLE_MODE, "mode_help");
        enabledButton = button(12, 82, 112, DimensionTowerConfigPacket.TOGGLE_ENABLED, "anchor_help");
        redstoneButton = button(136, 82, 112, DimensionTowerConfigPacket.CYCLE_REDSTONE, "redstone_help");
        lastChannel = -1;
        updateControls();
    }

    @Override protected void containerTick() {
        super.containerTick();
        channelInput.tick();
        updateControls();
    }

    private void updateControls() {
        if (lastChannel != menu.getChannel()) {
            channelInput.setValue(Integer.toString(menu.getChannel()));
            lastChannel = menu.getChannel();
        }
        typeButton.setMessage(text("type", menu.getEnergyType().name()));
        modeButton.setMessage(text("mode." + menu.getMode().name().toLowerCase(Locale.ROOT)));
        enabledButton.setMessage(text(menu.isEnabled() ? "enabled" : "disabled"));
        redstoneButton.setMessage(Component.translatable("gui.changede.redstone." + menu.getRedstoneMode().name().toLowerCase(Locale.ROOT)));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineGuiStyle.panel(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight);
        MachineGuiStyle.bar(graphics, leftPos + 12, topPos + 158, 236, 8,
                menu.getStoredEnergy(menu.getEnergyType()), DimensionSpaceTowerBlockEntity.CAPACITY, 0xFF8B72CF);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 6, MachineGuiStyle.TITLE, false);
        graphics.drawString(font, text("channel"), 12, 34, MachineGuiStyle.LABEL, false);
        String status = menu.getChannel() == 0 ? "unassigned" : menu.isActive() ? "active" : "paused";
        graphics.drawString(font, text("status." + status), 12, 112, menu.isActive() ? 0xFF267E41 : 0xFF735D30, false);
        graphics.drawString(font, text("chunks", menu.getLoadedChunkCount()), 12, 126, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, text("peers", menu.getPeerCount()), 12, 140, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, text("range"), 12, 178, 0xFF65519E, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(12, 158, 236, 8, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(MachineGuiStyle.amount(menu.getStoredEnergy(menu.getEnergyType()), DimensionSpaceTowerBlockEntity.CAPACITY, menu.getEnergyType().name()));
            for (LatexEnergyType type : LatexEnergyType.values()) lines.add(text("buffer", type.name(), menu.getStoredEnergy(type)));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
