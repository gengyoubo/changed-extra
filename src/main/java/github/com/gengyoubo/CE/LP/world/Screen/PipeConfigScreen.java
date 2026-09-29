package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.PipeConnectionMode;
import github.com.gengyoubo.CE.LP.world.Menu.PipeConfigMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class PipeConfigScreen extends AbstractContainerScreen<PipeConfigMenu> {
    private final Map<Direction, Button> faceButtons = new EnumMap<>(Direction.class);

    public PipeConfigScreen(PipeConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 210;
        imageHeight = 190;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelY = imageHeight + 20;
    }

    @Override
    protected void init() {
        super.init();
        faceButtons.clear();
        int left = leftPos + 80;
        int top = topPos + 27;
        for (Direction direction : Direction.values()) {
            int row = direction.ordinal();
            Button button = Button.builder(modeLabel(menu.getMode(direction)), ignored -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction.ordinal());
                        }
                    })
                    .bounds(left, top + row * 22, 122, 20)
                    .build();
            addRenderableWidget(button);
            faceButtons.put(direction, button);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        faceButtons.forEach((direction, button) -> button.setMessage(modeLabel(menu.getMode(direction))));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0101010);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF777777);
        for (Direction direction : Direction.values()) {
            graphics.drawString(font, Component.translatable("direction." + direction.getName()), leftPos + 12,
                    topPos + 33 + direction.ordinal() * 22, 0xFFFFFF, false);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, 0xFFFFFF, false);
    }

    private static Component modeLabel(PipeConnectionMode mode) {
        return Component.translatable("message.changede.pipe_connection_mode." + mode.name().toLowerCase(java.util.Locale.ROOT));
    }
}
