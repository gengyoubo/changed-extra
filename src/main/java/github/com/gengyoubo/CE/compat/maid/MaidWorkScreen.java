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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Reuses TLM's task widgets and GUI atlas without opening an EntityMaid menu. */
public final class MaidWorkScreen extends AbstractContainerScreen<MaidWorkMenu> {
    private static final ResourceLocation MAIN = ResourceLocation.fromNamespaceAndPath(
            "touhou_little_maid", "textures/gui/maid_gui_main.png");
    private static final ResourceLocation TASK = ResourceLocation.fromNamespaceAndPath(
            "touhou_little_maid", "textures/gui/maid_gui_task.png");
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(
            "touhou_little_maid", "textures/gui/maid_gui_side.png");
    private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(
            "touhou_little_maid", "textures/gui/maid_gui_button.png");
    private static final int TASKS_PER_PAGE = 12;
    private final List<TaskButton> taskButtons = new ArrayList<>();
    private int taskPage;

    public MaidWorkScreen(MaidWorkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 350;
        imageHeight = 256;
    }

    private int mainX() { return leftPos + 94; }
    private int taskX() { return mainX() - 93; }

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
                .bounds(mainX() + 5, topPos + 187, 70, 20).build());
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
        renderSidebarTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The TLM atlas also contains an upper-right maid equipment panel. Only
        // draw the creature sidebar and the player inventory portion here.
        graphics.blit(MAIN, mainX(), topPos, 0, 0, 80, 256);
        graphics.blit(MAIN, mainX() + 80, topPos + 166, 80, 166, 176, 90);
        graphics.blit(TASK, taskX(), topPos + 5, 0, 0, 92, 251);

        graphics.drawString(font, Component.translatable("screen.changede.maid_work.main_hand"),
                mainX() + 23, topPos + 211, 0x333333, false);
        graphics.fill(mainX() + 29, topPos + 223, mainX() + 51, topPos + 245, 0xFF373737);
        graphics.fill(mainX() + 30, topPos + 224, mainX() + 50, topPos + 244, 0xFFB8B8B8);

        ChangedEntity creature = minecraft != null && minecraft.level != null
                && minecraft.level.getEntity(menu.creatureId()) instanceof ChangedEntity changed ? changed : null;
        if (creature != null) {
            graphics.enableScissor(mainX() + 5, topPos + 6, mainX() + 75, topPos + 108);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    mainX() + 40, topPos + 101, 36,
                    mainX() + 40 - mouseX, topPos + 72 - mouseY, creature);
            graphics.disableScissor();
        }
        renderSidebar(graphics, creature);

        List<IMaidTask> tasks = menu.tasks();
        int pages = Math.max(1, (tasks.size() + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE);
        graphics.drawString(font, (taskPage + 1) + "/" + pages, mainX() - 48, topPos + 12, 0x333333, false);
    }

    private void renderSidebar(GuiGraphics graphics, ChangedEntity creature) {
        float health = creature == null ? 0.0F : creature.getHealth();
        float maxHealth = creature == null ? 1.0F : Math.max(1.0F, creature.getMaxHealth());
        int armor = creature == null ? 0 : creature.getArmorValue();
        boolean working = menu.selectedTaskIndex() >= 0;
        drawStatusBar(graphics, 0, health / maxHealth, Math.round(health));
        drawStatusBar(graphics, 1, armor / 20.0F, armor);
        drawStatusBar(graphics, 2, 1.0F, 60);
        drawStatusBar(graphics, 3, working ? 1.0F : 0.0F, working ? 1 : 0);

        graphics.blit(BUTTONS, mainX() + 4, topPos + 159, 0, 42, 71, 21);
        List<IMaidTask> tasks = menu.tasks();
        int selected = menu.selectedTaskIndex();
        ItemStack icon = selected >= 0 && selected < tasks.size()
                ? tasks.get(selected).getIcon() : new ItemStack(Items.FEATHER);
        Component name = selected >= 0 && selected < tasks.size()
                ? tasks.get(selected).getName() : Component.translatable("screen.changede.maid_work.idle");
        graphics.renderItem(icon, mainX() + 6, topPos + 161);
        var lines = font.split(name, 42);
        if (!lines.isEmpty()) {
            graphics.drawString(font, lines.get(0), mainX() + 28, topPos + 165, 0x333333, false);
        }
    }

    private void drawStatusBar(GuiGraphics graphics, int row, float fraction, int value) {
        int y = topPos + 113 + row * 11;
        graphics.blit(SIDE, mainX() + 5, y, 0, 9, 47, 9);
        int filled = (int) (43 * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            graphics.blit(SIDE, mainX() + 7, y + 2, 2, 18 + row * 5, filled, 5);
        }
        graphics.blit(SIDE, mainX() + 53, y, row * 9, 0, 9, 9);
        graphics.drawString(font, Integer.toString(value), mainX() + 63, y + 1, 0x333333, false);
    }

    private void renderSidebarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (mouseX >= mainX() + 5 && mouseX < mainX() + 75
                && mouseY >= topPos + 113 && mouseY < topPos + 155) {
            int row = (mouseY - topPos - 113) / 11;
            String key = switch (row) {
                case 0 -> "screen.changede.maid_work.health";
                case 1 -> "screen.changede.maid_work.armor";
                case 2 -> "screen.changede.maid_work.familiarity";
                default -> "screen.changede.maid_work.work_status";
            };
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
        }
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
