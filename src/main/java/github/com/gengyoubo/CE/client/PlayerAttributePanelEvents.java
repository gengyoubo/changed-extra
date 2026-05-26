package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.changede;
import github.com.gengyoubo.CE.player.Perseverance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class PlayerAttributePanelEvents {
    private static boolean panelOpen = false;
    private static boolean warnedRenderError = false;
    private static boolean warnedPreviewError = false;

    private PlayerAttributePanelEvents() {
    }

    @SubscribeEvent
    public static void onRenderInventory(ScreenEvent.Render.Post event) {
        try {
            Screen screen = event.getScreen();
            if (isInventoryLike(screen)) {
                panelOpen = false;
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) {
                return;
            }

            GuiGraphics graphics = event.getGuiGraphics();
            Font font = minecraft.font;
            Layout layout = layout(screen);

            renderButton(graphics, font, layout.buttonX(), layout.buttonY());
            if (panelOpen) {
                renderPanel(graphics, font, player, layout.panelX(), layout.panelY(), event.getMouseX(), event.getMouseY());
            }
        } catch (RuntimeException ex) {
            panelOpen = false;
            if (!warnedRenderError) {
                warnedRenderError = true;
                changede.LOGGER.warn("Failed to render player attribute panel; panel closed to keep inventory usable.", ex);
            }
        }
    }

    @SubscribeEvent
    public static void onInventoryMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        try {
            Screen screen = event.getScreen();
            if (isInventoryLike(screen)) {
                panelOpen = false;
                return;
            }

            Layout layout = layout(screen);
            double mouseX = event.getMouseX();
            double mouseY = event.getMouseY();
            if (mouseX >= layout.buttonX() && mouseX < layout.buttonX() + Layout.BUTTON_WIDTH
                    && mouseY >= layout.buttonY() && mouseY < layout.buttonY() + Layout.BUTTON_HEIGHT) {
                panelOpen = !panelOpen;
                event.setCanceled(true);
            }
        } catch (RuntimeException ex) {
            panelOpen = false;
            event.setCanceled(true);
            changede.LOGGER.warn("Failed to handle player attribute button click; panel closed to keep inventory usable.", ex);
        }
    }

    private static boolean isInventoryLike(Screen screen) {
        return !(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen);
    }

    private static Layout layout(Screen screen) {
        int vanillaPanelWidth = screen instanceof CreativeModeInventoryScreen ? 195 : 176;
        int vanillaPanelHeight = screen instanceof CreativeModeInventoryScreen ? 136 : 166;
        int vanillaLeft = (screen.width - vanillaPanelWidth) / 2;
        int vanillaTop = (screen.height - vanillaPanelHeight) / 2;
        int buttonX = Math.max(4, vanillaLeft - Layout.BUTTON_WIDTH - 6);
        int buttonY = vanillaTop + 8;
        int panelX = Math.max(4, buttonX - Layout.PANEL_WIDTH - 6);
        int panelY = vanillaTop + 8;
        return new Layout(buttonX, buttonY, panelX, panelY);
    }

    private static void renderButton(GuiGraphics graphics, Font font, int x, int y) {
        int color = panelOpen ? 0xE044516B : 0xD8202430;
        graphics.fill(x, y, x + Layout.BUTTON_WIDTH, y + Layout.BUTTON_HEIGHT, color);
        graphics.fill(x + 1, y + 1, x + Layout.BUTTON_WIDTH - 1, y + 2, 0x80FFFFFF);
        graphics.drawString(font, Component.translatable("screen.changede.attributes.button"), x + 5, y + 6, 0xFFEFE6D0, false);
    }

    private static void renderPanel(GuiGraphics graphics, Font font, Player player, int x, int y, int mouseX, int mouseY) {
        int level = Perseverance.getLevel(player);
        graphics.fill(x, y, x + Layout.PANEL_WIDTH, y + Layout.PANEL_HEIGHT, 0xE0181C27);
        graphics.fill(x + 2, y + 2, x + Layout.PANEL_WIDTH - 2, y + 16, 0xE03A4056);
        graphics.drawString(font, Component.translatable("screen.changede.attributes.title"), x + 7, y + 6, 0xFFEFE6D0, false);

        renderPlayerPreview(graphics, player, x, y, mouseX, mouseY);

        int textY = y + 102;
        graphics.drawString(font, Component.translatable("screen.changede.attributes.perseverance", level), x + 8, textY, 0xFFBFE8FF, false);
        graphics.drawString(font, Component.translatable("screen.changede.attributes.keep_form", level * 10), x + 8, textY + 12, 0xFFC8F6C0, false);
        graphics.drawString(font, Component.translatable("screen.changede.attributes.mimic_tf", (int)(Perseverance.getMimicTransfurChance(player) * 100.0D)), x + 8, textY + 24, 0xFFFFD89A, false);
    }

    private static void renderPlayerPreview(GuiGraphics graphics, Player player, int x, int y, int mouseX, int mouseY) {
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 48, y + 76, 30, x + 48 - mouseX, y + 54 - mouseY, player);
        } catch (RuntimeException ex) {
            if (!warnedPreviewError) {
                warnedPreviewError = true;
                changede.LOGGER.warn("Failed to render player preview in attribute panel; continuing without preview.", ex);
            }
        }
    }

    private record Layout(int buttonX, int buttonY, int panelX, int panelY) {
        private static final int BUTTON_WIDTH = 34;
        private static final int BUTTON_HEIGHT = 22;
        private static final int PANEL_WIDTH = 98;
        private static final int PANEL_HEIGHT = 140;
    }
}
