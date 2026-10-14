package github.com.gengyoubo.CE.compat.jer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Server-confirmed ore generation. No client/JER classes: also safe on dedicated servers. */
public record OreGenerationData(ResourceLocation block, ResourceLocation dimension,
                                float[] distribution, List<Drop> drops) {
    public static final int MIN_Y = -64;
    public static final int HEIGHTS = 320; // JER 1.20.1's chart coordinate system.

    public OreGenerationData {
        distribution = distribution.clone();
        drops = List.copyOf(drops);
        if (distribution.length != HEIGHTS) throw new IllegalArgumentException("Invalid ore distribution length");
        for (float value : distribution) {
            if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid ore density");
        }
    }

    public record Drop(ItemStack stack, int min, int max) {
        public Drop {
            stack = stack.copyWithCount(1);
            if (min < 0 || max < min) throw new IllegalArgumentException("Invalid ore drop range");
        }
    }
}
