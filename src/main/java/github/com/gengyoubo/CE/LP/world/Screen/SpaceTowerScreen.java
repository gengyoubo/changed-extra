package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.SpaceTowerConfigPacket;
import github.com.gengyoubo.CE.LP.world.Menu.SpaceTowerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class SpaceTowerScreen extends AbstractContainerScreen<SpaceTowerMenu> {
    private final Map<SpaceTowerEnergyType, Button> modeButtons = new EnumMap<>(SpaceTowerEnergyType.class);
    private Button rpmMinus;
    private Button rpmPlus;
    private Button suMinus;
    private Button suPlus;

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
        guiGraphics.drawString(font, Component.literal("RPM: " + menu.getCeRpm()), 118, 60, 0x303030, false);
        guiGraphics.drawString(font, Component.literal("SU: " + menu.getCeSu()), 118, 82, 0x303030, false);
        guiGraphics.drawString(font, Component.translatable("screen.changede.space_tower.ce_cost", menu.getCeCostPerMinute()), 118, 104, 0x303030, false);
        guiGraphics.drawString(font, Component.translatable("screen.changede.space_tower.ce_storage", formatSeconds(menu.getCeStoredSeconds()), "05:00"), 118, 116, 0x303030, false);
        guiGraphics.drawString(font, Component.literal("J: " + menu.getJouleBuffer()), 118, 128, 0x303030, false);
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

        rpmMinus = addRenderableWidget(Button.builder(Component.literal("-2"), ignored -> CENetwork.sendToServer(SpaceTowerConfigPacket.adjustRpm(menu.getBlockPos(), -2)))
                .bounds(leftPos + 164, topPos + 56, 28, 16)
                .build());
        rpmPlus = addRenderableWidget(Button.builder(Component.literal("+2"), ignored -> CENetwork.sendToServer(SpaceTowerConfigPacket.adjustRpm(menu.getBlockPos(), 2)))
                .bounds(leftPos + 194, topPos + 56, 28, 16)
                .build());
        suMinus = addRenderableWidget(Button.builder(Component.literal("-1"), ignored -> CENetwork.sendToServer(SpaceTowerConfigPacket.adjustSu(menu.getBlockPos(), Screen.hasShiftDown() ? -4 : -1)))
                .bounds(leftPos + 164, topPos + 78, 28, 16)
                .build());
        suPlus = addRenderableWidget(Button.builder(Component.literal("+1"), ignored -> CENetwork.sendToServer(SpaceTowerConfigPacket.adjustSu(menu.getBlockPos(), Screen.hasShiftDown() ? 4 : 1)))
                .bounds(leftPos + 194, topPos + 78, 28, 16)
                .build());

        updateButtons();
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

        if (suMinus != null) {
            suMinus.setMessage(Component.literal(Screen.hasShiftDown() ? "-4" : "-1"));
        }
        if (suPlus != null) {
            suPlus.setMessage(Component.literal(Screen.hasShiftDown() ? "+4" : "+1"));
        }
    }

    private static String formatSeconds(int seconds) {
        int clamped = Math.max(0, seconds);
        return String.format("%02d:%02d", clamped / 60, clamped % 60);
    }
}
