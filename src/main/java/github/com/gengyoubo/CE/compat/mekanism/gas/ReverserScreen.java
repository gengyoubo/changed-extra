package github.com.gengyoubo.CE.compat.mekanism.gas;

import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.world.Screen.MachineGuiStyle;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiBar;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.window.GuiSideConfigurationTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import java.util.Locale;

/**
 * A Mekanism machine screen: Mekanism's gauges, arrow, inner screen and buttons, plus its side tabs
 * (redstone mode, security, side configuration and an energy tab for WLP/DLP use).
 */
public final class ReverserScreen extends GuiMekanismTile<ReverserBlockEntity, ReverserContainer> {
    private static final int TOP = 18, BAR_HEIGHT = 58;
    private GuiSideConfigurationTab<ReverserBlockEntity> sideConfigTab;

    public ReverserScreen(ReverserContainer container, Inventory inventory, Component title) {
        super(container, inventory, title);
        imageHeight += 12;
        inventoryLabelY = imageHeight - 94;
        dynamicSlots = true;
    }
    @Override protected void addGuiElements() {
        super.addGuiElements();
        sideConfigTab = addRenderableWidget(new GuiSideConfigurationTab<>(this, tile, () -> sideConfigTab));
        addRenderableWidget(new GuiEnergyTab(this, () -> List.of(Component.literal(tile.whiteRate() + " WLP/s"), Component.literal(tile.darkRate() + " DLP/s"))));
        addRenderableWidget(new LatexBar(this, () -> tile.getTypedEnergyStored(LatexEnergyType.WLP), "WLP", 8, TOP + 1, 0xFFF0F0E8, 0xFFFFFFFF));
        addRenderableWidget(new GuiGasGauge(() -> tile.inputTank, () -> tile.getGasTanks(null), GaugeType.STANDARD, this, 22, TOP));
        addRenderableWidget(new GuiProgress((IProgressInfoHandler) () -> tile.processTicks() == 0 ? 0 : tile.progress() / (double) tile.processTicks(),
                ProgressType.LARGE_RIGHT, this, 64, 30));
        addRenderableWidget(new GuiInnerScreen(this, 46, 44, 84, 16, () -> List.of(Component.translatable(
                "state.changede.reverser." + tile.status().name().toLowerCase(Locale.ROOT)))).centerY());
        addRenderableWidget(new TranslationButton(this, 64, 64, 48, 16, () -> "gui.changede.reverser.drain",
                () -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ReverserContainer.DRAIN_BUTTON),
                (element, graphics, mouseX, mouseY) -> displayTooltips(graphics, mouseX, mouseY, Component.translatable("gui.changede.reverser.drain_hint")), null));
        addRenderableWidget(new GuiGasGauge(() -> tile.outputTank, () -> tile.getGasTanks(null), GaugeType.STANDARD, this, 136, TOP));
        addRenderableWidget(new LatexBar(this, () -> tile.getTypedEnergyStored(LatexEnergyType.DLP), "DLP", 162, TOP + 1, 0xFF3A3446, 0xFF6A6280));
    }
    @Override protected void drawForegroundText(GuiGraphics graphics, int mouseX, int mouseY) {
        renderTitleText(graphics);
        drawString(graphics, playerInventoryTitle, inventoryLabelX, inventoryLabelY, titleTextColor());
        super.drawForegroundText(graphics, mouseX, mouseY);
    }

    /** A Mekanism vertical bar holding WLP or DLP: its frame, filled from the bottom in that latex's colour. */
    private static final class LatexBar extends GuiBar<GuiBar.IBarInfoHandler> {
        private final int color, lit;
        LatexBar(IGuiWrapper gui, java.util.function.IntSupplier stored, String unit, int x, int y, int color, int lit) {
            super(BAR, gui, new IBarInfoHandler() {
                @Override public double getLevel() { return stored.getAsInt() / (double) ReverserBlockEntity.ENERGY_CAPACITY; }
                @Override public Component getTooltip() { return MachineGuiStyle.amount(stored.getAsInt(), ReverserBlockEntity.ENERGY_CAPACITY, unit); }
            }, x, y, 4, BAR_HEIGHT, false);
            this.color = color;
            this.lit = lit;
        }
        @Override protected void renderBarOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, double level) {
            int height = calculateScaled(level, BAR_HEIGHT);
            if (height <= 0) return;
            int x = getX() + 1, bottom = getY() + 1 + BAR_HEIGHT;
            graphics.fill(x, bottom - height, x + 4, bottom, color);
            graphics.fill(x, bottom - height, x + 1, bottom, lit);
        }
    }
}
