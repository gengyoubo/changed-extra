package github.com.gengyoubo.CE.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.TaskButton;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.MaidWorkSwitchPacket;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Reuses TLM's task widgets and GUI atlas without opening an EntityMaid menu. */
public final class MaidWorkScreen extends AbstractContainerScreen<MaidWorkMenu> {
    private static final ResourceLocation MAIN = ResourceLocation.fromNamespaceAndPath(
            "touhou_little_maid", "textures/gui/maid_gui_main.png");
    private static final ResourceLocation TASK = ResourceLocation.fromNamespaceAndPath(
            "touhou_little_maid", "textures/gui/maid_gui_task.png");
    private static final int TASKS_PER_PAGE = 12;
    private final List<TaskButton> taskButtons = new ArrayList<>();
    private int taskPage;

    public MaidWorkScreen(MaidWorkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 350;
        imageHeight = 256;
    }

    private int mainX() { return leftPos + 94; }
    private int taskX() { return leftPos + 2; }

    @Override
    protected void init() {
        super.init();
        rebuildTaskButtons();
    }

    private void rebuildTaskButtons() {
        clearWidgets();
        taskButtons.clear();
        List<IMaidTask> tasks = menu.tasks();
        int pages = Math.max(1, (tasks.size() + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE);
        taskPage = Math.min(taskPage, pages - 1);

        ImageButton previous = new ImageButton(mainX() - 89, topPos + 9, 16, 13,
                110, 0, 14, TASK, ignored -> {
                    if (taskPage > 0) { taskPage--; rebuildTaskButtons(); }
                });
        ImageButton next = new ImageButton(mainX() - 72, topPos + 9, 16, 13,
                93, 0, 14, TASK, ignored -> {
                    if (taskPage + 1 < pages) { taskPage++; rebuildTaskButtons(); }
                });
        ImageButton back = new ImageButton(mainX() - 19, topPos + 9, 13, 13,
                127, 0, 14, TASK, ignored ->
                CENetwork.sendToServer(new MaidWorkSwitchPacket(menu.creatureId(), false)));
        back.setTooltip(Tooltip.create(Component.translatable("screen.changede.maid_work.back")));
        addRenderableWidget(previous);
        addRenderableWidget(next);
        addRenderableWidget(back);

        for (int row = 0; row < TASKS_PER_PAGE; row++) {
            int index = taskPage * TASKS_PER_PAGE + row;
            if (index >= tasks.size()) break;
            IMaidTask task = tasks.get(index);
            TaskButton button = new TaskButton(task, true,
                    mainX() - 89, topPos + 23 + row * 19, 83, 19,
                    93, 28, 20, TASK, 256, 256,
                    ignored -> selectTask(index), List.of(task.getName()), Component.empty());
            taskButtons.add(button);
            addRenderableWidget(button);
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.changede.maid_work.stop"),
                        ignored -> selectTask(menu.tasks().size()))
                .bounds(mainX() + 90, topPos + 135, 156, 20).build());
    }

    private void selectTask(int index) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (minecraft != null) {
            for (TaskButton button : taskButtons) {
                if (button.isTooltipHovered()) {
                    button.renderTooltip(graphics, minecraft, mouseX, mouseY);
                    break;
                }
            }
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(MAIN, mainX(), topPos, 0, 0, 256, 256);
        graphics.blit(TASK, taskX(), topPos, 0, 0, 92, 256);

        ChangedEntity creature = minecraft != null && minecraft.level != null
                && minecraft.level.getEntity(menu.creatureId()) instanceof ChangedEntity changed ? changed : null;
        if (creature != null) {
            graphics.enableScissor(mainX() + 5, topPos + 6, mainX() + 75, topPos + 108);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    mainX() + 40, topPos + 101, 36,
                    mainX() + 40 - mouseX, topPos + 72 - mouseY, creature);
            graphics.disableScissor();
            float health = Math.max(0.0F, Math.min(1.0F, creature.getHealth() / creature.getMaxHealth()));
            graphics.fill(mainX() + 5, topPos + 114, mainX() + 72, topPos + 120, 0xFF333333);
            graphics.fill(mainX() + 6, topPos + 115, mainX() + 6 + Math.round(65 * health), topPos + 119, 0xFFE84C45);
            graphics.drawString(font, creature.getDisplayName(), mainX() + 5, topPos + 124, 0x303030, false);
        }

        List<IMaidTask> tasks = menu.tasks();
        int pages = Math.max(1, (tasks.size() + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE);
        graphics.drawCenteredString(font, (taskPage + 1) + "/" + pages, taskX() + 59, topPos + 11, 0x303030);

        int selected = menu.selectedTaskIndex();
        graphics.drawString(font, Component.translatable("screen.changede.maid_work.current"),
                mainX() + 90, topPos + 18, 0x303030, false);
        if (selected >= 0 && selected < tasks.size()) {
            IMaidTask task = tasks.get(selected);
            graphics.renderItem(task.getIcon(), mainX() + 90, topPos + 38);
            graphics.drawString(font, task.getName(), mainX() + 111, topPos + 42, 0x303030, false);
        } else {
            graphics.drawString(font, Component.translatable("screen.changede.maid_work.idle"),
                    mainX() + 90, topPos + 42, 0x303030, false);
        }
        graphics.drawString(font, Component.translatable("screen.changede.maid_work.select_hint"),
                mainX() + 90, topPos + 75, 0x555555, false);
        graphics.drawString(font, Component.translatable("screen.changede.maid_work.player_inventory"),
                mainX() + 88, topPos + 163, 0x303030, false);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Labels are placed in the TLM atlas panels by renderBg.
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= taskX() && mouseX < taskX() + 92 && mouseY >= topPos && mouseY < topPos + 256) {
            int pages = Math.max(1, (menu.tasks().size() + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE);
            int nextPage = Math.max(0, Math.min(pages - 1, taskPage + (delta < 0 ? 1 : -1)));
            if (nextPage != taskPage) { taskPage = nextPage; rebuildTaskButtons(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
