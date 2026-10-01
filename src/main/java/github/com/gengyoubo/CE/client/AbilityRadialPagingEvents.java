package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.mixins.AbilityRadialPagingAccessor;
import net.ltxprogrammer.changed.client.gui.AbilityRadialScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Changed draws and handles its own radial menu, so these controls use screen events. */
@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class AbilityRadialPagingEvents {
    private static final int CAPACITY = 8, SIZE = 24;

    private static int y(AbilityRadialScreen screen) { return screen.height - 32; }
    private static int x(AbilityRadialScreen screen, boolean next) { return screen.width / 2 + (next ? 48 : -72); }
    private static boolean inside(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SIZE && my >= y && my < y + SIZE;
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbilityRadialScreen screen) || screen.getCount() <= CAPACITY) return;
        int offset = ((AbilityRadialPagingAccessor) screen).changede$getViewOffset();
        GuiGraphics graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        for (boolean next : new boolean[]{false, true}) {
            int x = x(screen, next), y = y(screen);
            boolean enabled = next ? offset + CAPACITY < screen.getCount() : offset > 0;
            boolean hovered = inside(event.getMouseX(), event.getMouseY(), x, y);
            graphics.fill(x, y, x + SIZE, y + SIZE, enabled && hovered ? 0xE0485C73 : 0xD0192535);
            int color = enabled ? 0xFFE2EDF8 : 0xFF566478;
            for (int dy = -7; dy <= 7; dy++) {
                int length = 8 - Math.abs(dy);
                int start = next ? x + 8 : x + 16 - length;
                graphics.fill(start, y + 12 + dy, start + length, y + 13 + dy, color);
            }
            if (hovered) graphics.renderTooltip(font, Component.translatable("screen.changede.abilities." +
                    (next ? "next" : "previous")), event.getMouseX(), event.getMouseY());
        }
        graphics.drawCenteredString(font, Component.literal((offset + 1) + "–" +
                Math.min(offset + CAPACITY, screen.getCount()) + " / " + screen.getCount()),
                screen.width / 2, y(screen) + 8, 0xDFE8F5);
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbilityRadialScreen screen) || screen.getCount() <= CAPACITY
                || event.getButton() != 0) return;
        for (boolean next : new boolean[]{false, true}) {
            if (!inside(event.getMouseX(), event.getMouseY(), x(screen, next), y(screen))) continue;
            // Consume even a disabled arrow: a click must never select an underlying ability.
            event.setCanceled(true);
            var accessor = (AbilityRadialPagingAccessor) screen;
            int offset = accessor.changede$getViewOffset();
            int target = next ? Math.min(offset + CAPACITY, screen.getCount() - CAPACITY)
                    : Math.max(0, ((offset - 1) / CAPACITY) * CAPACITY);
            if (target == offset) return;
            accessor.changede$setPreviousOffset(target);
            accessor.changede$setViewOffset(target);
            accessor.changede$setTransitionTicks(0);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return;
        }
    }
}
