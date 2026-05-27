package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.SpaceTowerConfigPacket;
import github.com.gengyoubo.CE.LP.world.Menu.SpaceTowerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class SpaceTowerScreen extends AbstractContainerScreen<SpaceTowerMenu> {
    private final Map<SpaceTowerEnergyType, Button> modeButtons = new EnumMap<>(SpaceTowerEnergyType.class);
    private EditBox rpmInput;
    private EditBox suInput;
    private Button applyButton;
    private int lastSyncedRpm = Integer.MIN_VALUE;
    private int lastSyncedSu = Integer.MIN_VALUE;

    public SpaceTowerScreen(SpaceTowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 230;
        this.imageHeight = 200;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202734);
        guiGraphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFFDEE7F0);
        guiGraphics.fill(leftPos + 10, topPos + 22, leftPos + 220, topPos + 34, 0xFF293342);

        int max = Math.max(1, menu.getMaxEnergyStored());
        int barWidth = Math.min(208, Math.round(menu.getEnergyStored() * 208.0F / max));
        guiGraphics.fill(leftPos + 11, topPos + 23, leftPos + 11 + barWidth, topPos + 33, 0xFF60B25F);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("screen.changede.space_tower.title"), 10, 8, 0x202020, false);
        guiGraphics.drawString(font, Component.literal(menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " LP"), 12, 24, 0xFFFFFF, false);

        int rowY = 43;
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            guiGraphics.drawString(font, Component.translatable("screen.changede.space_tower.energy." + type.name().toLowerCase()), 16, rowY, 0x303030, false);
            rowY += 18;
        }

        guiGraphics.drawString(font, Component.translatable("screen.changede.space_tower.ce_settings"), 118, 43, 0x303030, false);
        guiGraphics.drawString(font, Component.literal("RPM:"), 118, 60, 0x303030, false);
        guiGraphics.drawString(font, Component.literal("SU:"), 118, 82, 0x303030, false);
        guiGraphics.drawString(font, Component.translatable("screen.changede.space_tower.ce_cost", menu.getCeCostPerMinute()), 118, 123, 0x303030, false);
        guiGraphics.drawString(font, Component.translatable("screen.changede.space_tower.ce_storage", formatSeconds(menu.getCeStoredSeconds()), "05:00"), 118, 135, 0x303030, false);
    }

    @Override
    protected void init() {
        super.init();
        modeButtons.clear();

        int rowY = topPos + 38;
        for (SpaceTowerEnergyType type : SpaceTowerEnergyType.values()) {
            Button button = Button.builder(Component.empty(), ignored -> CENetwork.sendToServer(SpaceTowerConfigPacket.toggleMode(menu.getBlockPos(), type)))
                    .bounds(leftPos + 48, rowY, 58, 16)
                    .build();
            modeButtons.put(type, button);
            addRenderableWidget(button);
            rowY += 18;
        }

        rpmInput = new EditBox(font, leftPos + 150, topPos + 55, 70, 18, Component.translatable("screen.changede.space_tower.rpm"));
        rpmInput.setFilter(SpaceTowerScreen::isNumericText);
        rpmInput.setMaxLength(9);
        addRenderableWidget(rpmInput);

        suInput = new EditBox(font, leftPos + 150, topPos + 77, 70, 18, Component.translatable("screen.changede.space_tower.su"));
        suInput.setFilter(SpaceTowerScreen::isNumericText);
        suInput.setMaxLength(9);
        addRenderableWidget(suInput);

        applyButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.changede.space_tower.apply"),
                        ignored -> submitCeSettings()
                )
                .bounds(leftPos + 164, topPos + 99, 56, 18)
                .build());

        updateButtons();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if ((rpmInput != null && rpmInput.isFocused()) || (suInput != null && suInput.isFocused())) {
            if (key == 257 || key == 335) {
                submitCeSettings();
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        for (Map.Entry<SpaceTowerEnergyType, Button> entry : modeButtons.entrySet()) {
            IOType mode = menu.getMode(entry.getKey());
            entry.getValue().setMessage(Component.translatable("screen.changede.space_tower.mode." + mode.name().toLowerCase()));
        }

        if (rpmInput != null && !rpmInput.isFocused() && lastSyncedRpm != menu.getCeRpm()) {
            lastSyncedRpm = menu.getCeRpm();
            rpmInput.setValue(Integer.toString(lastSyncedRpm));
        }
        if (suInput != null && !suInput.isFocused() && lastSyncedSu != menu.getCeSu()) {
            lastSyncedSu = menu.getCeSu();
            suInput.setValue(Integer.toString(lastSyncedSu));
        }
    }

    private void submitCeSettings() {
        int rpm = parseInputValue(rpmInput, menu.getCeRpm());
        int su = parseInputValue(suInput, menu.getCeSu());
        CENetwork.sendToServer(SpaceTowerConfigPacket.setRpm(menu.getBlockPos(), rpm));
        CENetwork.sendToServer(SpaceTowerConfigPacket.setSu(menu.getBlockPos(), su));
    }

    private static int parseInputValue(EditBox input, int fallback) {
        if (input == null || input.getValue().isBlank()) {
            return fallback;
        }
        try {
            return (int)Math.min(Integer.MAX_VALUE, Math.max(0L, Long.parseLong(input.getValue())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean isNumericText(String value) {
        return value.isEmpty() || value.chars().allMatch(Character::isDigit);
    }

    private static String formatSeconds(int seconds) {
        int clamped = Math.max(0, seconds);
        return String.format("%02d:%02d", clamped / 60, clamped % 60);
    }
}
