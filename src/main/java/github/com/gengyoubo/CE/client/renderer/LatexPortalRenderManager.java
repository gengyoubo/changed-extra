package github.com.gengyoubo.CE.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import github.com.gengyoubo.CE.client.LatexPaintingPortalPreviewCache;
import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LatexPortalRenderManager {
    private static final Map<Integer, TextureTarget> TARGETS = new HashMap<>();

    private LatexPortalRenderManager() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || LatexPaintingPortalFramebufferRenderer.isRenderingPortalFrame()) {
            return;
        }

        Set<Integer> livePortals = new HashSet<>();
        ResourceLocation dimension = minecraft.level.dimension().location();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LatexPaintingPortalEntity portal) || portal.isRemoved()) {
                continue;
            }

            livePortals.add(portal.getId());
            TextureTarget target = TARGETS.get(portal.getId());
            target = LatexPaintingPortalFramebufferRenderer.ensureTarget(target);
            TARGETS.put(portal.getId(), target);

            LatexPaintingPortalPreviewCache.Snapshot snapshot =
                    LatexPaintingPortalPreviewCache.get(dimension, portal.blockPosition());
            LatexPaintingPortalFramebufferRenderer.renderToTarget(target, portal, event.getPartialTick(), snapshot);
        }

        TARGETS.entrySet().removeIf(entry -> {
            if (livePortals.contains(entry.getKey())) {
                return false;
            }

            entry.getValue().destroyBuffers();
            return true;
        });
    }

    public static RenderTarget getTarget(LatexPaintingPortalEntity entity) {
        return TARGETS.get(entity.getId());
    }
}
