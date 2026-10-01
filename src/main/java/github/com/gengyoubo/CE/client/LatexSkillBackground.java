package github.com.gengyoubo.CE.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Tiles are anchored to graph coordinates, so walls move and scale with the canvas. */
public final class LatexSkillBackground {
    private static final int TILE = 32;
    private static final double TRANSITION = TILE * 3.0;
    private static final ResourceLocation WHITE_WALL = texture("wall_white");
    private static final ResourceLocation BLUE_WALL = texture("wall_blue_striped");
    private static final ResourceLocation DARK = texture("dark_latex_block_side");
    private static final ResourceLocation WHITE = texture("white_latex_block_side");
    private static final ResourceLocation DARK_SEEP = texture("dark_latex_wall_splotch");
    private static final ResourceLocation WHITE_SEEP = texture("white_latex_wall_splotch");

    private LatexSkillBackground() { }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("changed", "block/" + name);
    }

    /** Stable for negative coordinates too; unrelated salts keep tiles and stains uncorrelated. */
    private static double noise(int col, int row, long salt) {
        long value = (long) col * 0x9E3779B97F4A7C15L ^ (long) row * 0xC2B2AE3D27D4EB4FL ^ salt;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    public static void render(GuiGraphics graphics, List<CompoundTag> nodes, String species,
                              double panX, double panY, double zoom, int left, int top, int right, int bottom,
                              int columnSpacing, int rowSpacing) {
        ResourceLocation latex = species.equals("dark") ? DARK : species.equals("white") ? WHITE : null;
        List<CompoundTag> group = nodes.stream().filter(n -> n.getString("scope").equals("group")).toList();
        double regionTop = Double.POSITIVE_INFINITY, regionRight = Double.NEGATIVE_INFINITY;
        if (latex != null && !group.isEmpty()) {
            int firstRow = group.stream().mapToInt(n -> n.getInt("y")).min().orElse(0);
            regionTop = (firstRow - 0.5) * rowSpacing;
            // Keep the shared continuation on the right in its wall region. Individual branches
            // inherit their species region, rather than introducing a Yufeng-specific background.
            regionRight = nodes.stream().filter(n -> !n.getString("scope").equals("global"))
                    .mapToInt(n -> n.getInt("x")).max().orElse(0) * columnSpacing + columnSpacing / 2.0;
            double sharedLeft = nodes.stream().filter(n -> n.getString("scope").equals("global") && n.getInt("y") >= firstRow)
                    .mapToInt(n -> n.getInt("x")).min().orElse(Integer.MAX_VALUE);
            regionRight = Math.min(regionRight, sharedLeft * columnSpacing - columnSpacing / 2.0);
            regionRight = Math.floor(regionRight / TILE) * TILE;
        }
        int x0 = (int) Math.floor((left - panX) / zoom / TILE);
        int x1 = (int) Math.ceil((right - panX) / zoom / TILE);
        int y0 = (int) Math.floor((top - panY) / zoom / TILE);
        int y1 = (int) Math.ceil((bottom - panY) / zoom / TILE);
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var wallSprite = atlas.apply(WHITE_WALL);
        var stripeSprite = atlas.apply(BLUE_WALL);
        var latexSprite = latex == null ? wallSprite : atlas.apply(latex);
        var seepSprite = atlas.apply(species.equals("white") ? WHITE_SEEP : DARK_SEEP);
        for (int row = y0; row < y1; row++) {
            for (int col = x0; col < x1; col++) {
                int x = col * TILE, y = row * TILE;
                // Blend the entry edge and side boundary, leaving the shared continuation clean.
                double distance = Math.min(regionRight - (x + TILE / 2.0), y + TILE / 2.0 - regionTop);
                double factor = latex == null ? 0 : Math.max(0, Math.min(1, distance / TRANSITION));
                boolean covered = factor >= 1 || (factor > 0 && noise(col, row, 0x4C41544558L) < factor);
                var image = covered ? latexSprite : Math.floorMod(row, 3) == 0 ? stripeSprite : wallSprite;
                graphics.blit(x, y, 0, TILE, TILE, image);
                if (!covered && factor > 0 && factor < 1 && noise(col, row, 0x53454550414745L) < 0.35 + factor * 0.5) {
                    // Changed's transparent wall stains make remaining walls look coated, rather
                    // than introducing a separate UI theme or a Form-specific texture.
                    float alpha = (float) (0.2 + factor * 0.55);
                    graphics.blit(x, y, 0, TILE, TILE, seepSprite, 1.0F, 1.0F, 1.0F, alpha);
                }
            }
        }
        // A light shade keeps the wall texture recognizable while maintaining node contrast.
        graphics.fill((int) Math.floor((left - panX) / zoom), (int) Math.floor((top - panY) / zoom),
                (int) Math.ceil((right - panX) / zoom), (int) Math.ceil((bottom - panY) / zoom), 0x40101824);
    }
}
