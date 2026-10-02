package github.com.gengyoubo.CE.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;

/** World-anchored texture layers crossfade using visual regions, independent of node state. */
public final class LatexSkillBackground {
    private static final int TILE = 32;
    private LatexSkillBackground() { }

    public static void render(GuiGraphics graphics, double panX, double panY, double zoom,
                              int left, int top, int right, int bottom, int columnSpacing, int rowSpacing, String viewedType,
                              java.util.List<SkillRegion> regions) {
        int x0 = (int) Math.floor((left - panX) / zoom / TILE);
        int x1 = (int) Math.ceil((right - panX) / zoom / TILE);
        int y0 = (int) Math.floor((top - panY) / zoom / TILE);
        int y1 = (int) Math.ceil((bottom - panY) / zoom / TILE);
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        for (int row = y0; row < y1; row++) for (int col = x0; col < x1; col++) {
            int x = col * TILE, y = row * TILE;
            var weights = SkillVisuals.weights(regions, (x + TILE / 2.0) / columnSpacing, (y + TILE / 2.0) / rowSpacing, viewedType);
            // Straight-alpha compositing must divide each layer by cumulative weight;
            // otherwise two 50% textures would leave 25% of the canvas visible underneath.
            double cumulative = 0;
            for (var layer : weights.entrySet()) {
                cumulative += layer.getValue();
                var theme = SkillVisuals.theme(layer.getKey());
                var sprite = atlas.apply(Math.floorMod(row, 3) == 0 ? theme.stripe() : theme.tile());
                int tint = theme.tint();
                graphics.blit(x, y, 0, TILE, TILE, sprite, ((tint >> 16) & 255) / 255F,
                        ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, (float) (layer.getValue() / cumulative));
            }
            double wingWeight = weights.entrySet().stream().filter(e -> SkillVisuals.theme(e.getKey()).motif().equals("wings"))
                    .mapToDouble(java.util.Map.Entry::getValue).sum();
            if (wingWeight > 0 && Math.floorMod(col + row, 4) == 0) {
                int color = ((int) (wingWeight * 62) << 24) | 0xDFC5F4;
                // Sparse feather chevrons form a distinct Yufeng canopy, stable while panning.
                for (int step = 0; step < 10; step++) {
                    graphics.fill(x + 16 - step, y + 11 + step / 2, x + 17 - step, y + 13 + step / 2, color);
                    graphics.fill(x + 16 + step, y + 11 + step / 2, x + 17 + step, y + 13 + step / 2, color);
                }
            }
        }
        graphics.fill((int) Math.floor((left - panX) / zoom), (int) Math.floor((top - panY) / zoom),
                (int) Math.ceil((right - panX) / zoom), (int) Math.ceil((bottom - panY) / zoom), 0x55101824);
    }
}
