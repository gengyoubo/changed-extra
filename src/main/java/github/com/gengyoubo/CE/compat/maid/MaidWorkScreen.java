package github.com.gengyoubo.CE.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.MaidWorkSwitchPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class MaidWorkScreen extends AbstractContainerScreen<MaidWorkMenu> {
    private static final int VISIBLE_TASKS = 7;
    private final List<Button> taskButtons = new ArrayList<>();
    private int scroll;

    public MaidWorkScreen(MaidWorkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 230;
        imageHeight = 214;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        for (Button button : taskButtons) removeWidget(button);
        taskButtons.clear();
        List<IMaidTask> tasks = menu.tasks();
        int maxScroll = Math.max(0, tasks.size() - VISIBLE_TASKS);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int row = 0; row < VISIBLE_TASKS && scroll + row < tasks.size(); row++) {
            int taskIndex = scroll + row;
            IMaidTask task = tasks.get(taskIndex);
            Button button = Button.builder(task.getName(), ignored -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, taskIndex);
                        }
                    })
                    .bounds(leftPos + 12, topPos + 34 + row * 20, imageWidth - 24, 18)
                    .build();
            taskButtons.add(button);
            addRenderableWidget(button);
        }
        Button previous = Button.builder(Component.literal("▲"), ignored -> { scroll = Math.max(0, scroll - VISIBLE_TASKS); rebuildButtons(); })
                .bounds(leftPos + imageWidth - 20, topPos + 12, 12, 16).build();
        Button next = Button.builder(Component.literal("▼"), ignored -> { scroll += VISIBLE_TASKS; rebuildButtons(); })
                .bounds(leftPos + imageWidth - 20, topPos + 31 + VISIBLE_TASKS * 20, 12, 16).build();
        taskButtons.add(previous);
        taskButtons.add(next);
        addRenderableWidget(previous);
        addRenderableWidget(next);
        Button stop = Button.builder(Component.translatable("screen.changede.maid_work.stop"), ignored -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.tasks().size());
                    }
                })
                .bounds(leftPos + 12, topPos + 180, imageWidth - 24, 18).build();
        taskButtons.add(stop);
        addRenderableWidget(stop);
        Button back = Button.builder(Component.translatable("screen.changede.maid_work.back"), ignored ->
                        CENetwork.sendToServer(new MaidWorkSwitchPacket(menu.creatureId(), false)))
                .bounds(leftPos + 12, topPos + 8, 48, 18).build();
        taskButtons.add(back);
        addRenderableWidget(back);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF181522);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFFDED9E8);
        graphics.fill(leftPos + 8, topPos + 8, leftPos + imageWidth - 8, topPos + 28, 0xFF30283D);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.translatable("screen.changede.maid_work.title"), imageWidth / 2, 14, 0xFFFFFF);
        if (menu.tasks().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.changede.maid_work.no_tasks"), imageWidth / 2, 70, 0x303030);
        }
    }
}
