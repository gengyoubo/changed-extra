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
    private static final ResourceLocation WHITE_WALL = texture("wall_white");
    private static final ResourceLocation BLUE_WALL = texture("wall_blue_striped");
    private static final ResourceLocation DARK = texture("dark_latex_block_side");
    private static final ResourceLocation WHITE = texture("white_latex_block_side");

    private LatexSkillBackground() { }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("changed", "block/" + name);
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
        for (int row = y0; row < y1; row++) {
            for (int col = x0; col < x1; col++) {
                int x = col * TILE, y = row * TILE;
                var image = latex != null && y >= regionTop && x < regionRight ? latexSprite
                        : Math.floorMod(row, 3) == 0 ? stripeSprite : wallSprite;
                graphics.blit(x, y, 0, TILE, TILE, image);
            }
        }
        // A light shade keeps the wall texture recognizable while maintaining node contrast.
        graphics.fill((int) Math.floor((left - panX) / zoom), (int) Math.floor((top - panY) / zoom),
                (int) Math.ceil((right - panX) / zoom), (int) Math.ceil((bottom - panY) / zoom), 0x40101824);
    }
}
