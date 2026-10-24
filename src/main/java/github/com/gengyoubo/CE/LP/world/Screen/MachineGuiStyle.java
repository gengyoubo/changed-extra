package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/**
 * Dynamic parts drawn over the Changed-style panels baked by tools/generate_machine_gui.py:
 * the panels already hold slots, goo troughs and tank frames, so bars and fluids only fill them.
 */
public final class MachineGuiStyle {
    public static final int TITLE = 0xFFFFFFFF;
    public static final int LABEL = 0xFF373737;
    public static final int LP = 0xFF00A8A8;
    public static final int WLP = 0xFFF0F0E8;
    public static final int DLP = 0xFF3A3446;
    public static final int PROGRESS = 0xFFFFA740;
    private MachineGuiStyle() { }

    public static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("changede", "textures/gui/" + name + ".png");
    }

    /** Panels are exported at their exact size, so the texture is width x height. */
    public static void panel(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }

    /** Wet-looking fill for a trough: lit top row, shaded bottom row and a bright leading edge. */
    public static void bar(GuiGraphics graphics, int x, int y, int width, int height, long amount, long max, int color) {
        int fill = max <= 0 ? 0 : (int) Math.min(width, Math.max(0L, amount * width / max));
        if (fill <= 0) return;
        graphics.fill(x, y, x + fill, y + height, color);
        graphics.fill(x, y, x + fill, y + 1, shade(color, 1.25F));
        if (height > 2) graphics.fill(x, y + height - 1, x + fill, y + height, shade(color, 0.7F));
        graphics.fill(x + fill - 1, y, x + fill, y + height, shade(color, 1.35F));
    }

    public static void fluid(GuiGraphics graphics, FluidStack fluid, int capacity, int x, int y, int width, int height) {
        if (!fluid.isEmpty() && capacity > 0) {
            var properties = IClientFluidTypeExtensions.of(fluid.getFluid());
            var texture = properties.getStillTexture(fluid);
            if (texture != null) {
                var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
                int color = properties.getTintColor(fluid);
                int filled = (int) Math.min(height, Math.max(1L, (long) fluid.getAmount() * height / capacity));
                graphics.enableScissor(x, y + height - filled, x + width, y + height);
                graphics.setColor(((color >>> 16) & 255) / 255F, ((color >>> 8) & 255) / 255F, (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
                try {
                    for (int dy = 0; dy < filled; dy += 16) for (int dx = 0; dx < width; dx += 16)
                        graphics.blit(x + dx, y + height - dy - 16, 0, 16, 16, sprite);
                } finally { graphics.setColor(1, 1, 1, 1); graphics.disableScissor(); }
            }
        }
        for (int dy = 8; dy < height; dy += 8) graphics.fill(x + width - 3, y + dy, x + width, y + dy + 1, 0xA0FFFFFF);
    }

    public static Component amount(long current, long max, String unit) {
        return Component.literal(AmountFormat.format(current, max, unit));
    }

    private static int shade(int color, float factor) {
        int r = Math.min(255, (int) (((color >>> 16) & 255) * factor));
        int g = Math.min(255, (int) (((color >>> 8) & 255) * factor));
        int b = Math.min(255, (int) ((color & 255) * factor));
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }
}
