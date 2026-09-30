package github.com.gengyoubo.CE.LP.world.Screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Shared pixel panel, recessed slots and fluid rendering; does not depend on Jade. */
public final class MachineGuiStyle {
    private static final ResourceLocation LIGHT = ResourceLocation.fromNamespaceAndPath("changede", "textures/gui/machine_panel_light.png");
    private static final ResourceLocation DARK = ResourceLocation.fromNamespaceAndPath("changede", "textures/gui/machine_panel_dark.png");
    private MachineGuiStyle() { }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, boolean dark, int accent) {
        graphics.blit(dark ? DARK : LIGHT, x, y, 0, 0, width, height, width, height);
        graphics.fill(x + 4, y + 19, x + width - 4, y + 21, accent);
        graphics.renderOutline(x, y, width, height, 0xFF404955);
    }

    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF4B5563);
        graphics.fill(x, y, x + 17, y + 17, 0xFFF4F6F8);
        graphics.fill(x, y, x + 16, y + 16, 0xFF929DA9);
    }

    public static void bar(GuiGraphics graphics, int x, int y, int width, int height, int amount, int max, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF697585);
        graphics.fill(x, y, x + width, y + height, 0xFF202833);
        int fill = max <= 0 ? 0 : (int) Math.min(width, Math.max(0L, (long) amount * width / max));
        if (fill > 0) graphics.fill(x, y, x + fill, y + height, color);
    }

    public static void fluid(GuiGraphics graphics, FluidStack fluid, int capacity, int x, int y, int width, int height) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xFF697585);
        graphics.fill(x, y, x + width, y + height, 0xFF161E27);
        if (fluid.isEmpty() || capacity <= 0) return;
        var properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture = properties.getStillTexture(fluid);
        if (texture == null) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int color = properties.getTintColor(fluid);
        int filled = (int) Math.min(height, Math.max(1L, (long) fluid.getAmount() * height / capacity));
        graphics.enableScissor(x, y + height - filled, x + width, y + height);
        graphics.setColor(((color >>> 16) & 255) / 255F, ((color >>> 8) & 255) / 255F, (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
        try {
            for (int dy = 0; dy < filled; dy += 16) for (int dx = 0; dx < width; dx += 16)
                graphics.blit(x + dx, y + height - dy - 16, 0, 16, 16, sprite);
        } finally { graphics.setColor(1, 1, 1, 1); graphics.disableScissor(); }
        for (int dy = 8; dy < height; dy += 8) graphics.fill(x + width - 3, y + dy, x + width, y + dy + 1, 0xA0FFFFFF);
    }
}
