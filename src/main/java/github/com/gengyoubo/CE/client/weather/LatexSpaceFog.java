package github.com.gengyoubo.CE.client.weather;

import github.com.gengyoubo.CE.weather.LatexSpaceWeather;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Weather-driven white fog, blended across white biome borders. */
@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LatexSpaceFog {
    private LatexSpaceFog() { }

    private static float strength(Camera camera, double partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(LatexSpaceWeather.DIMENSION)
                || camera.getFluidInCamera() != FogType.NONE) return 0;
        float rain = level.getRainLevel((float) partialTick);
        if (rain <= 0) return 0;

        // Bilinear biome samples blend continuously as the camera crosses a border.
        double gridX = camera.getPosition().x / 4;
        double gridZ = camera.getPosition().z / 4;
        int cellX = Mth.floor(gridX);
        int cellZ = Mth.floor(gridZ);
        float fractionX = (float) (gridX - cellX);
        float fractionZ = (float) (gridZ - cellZ);
        float whiteWeight = 0;
        int y = camera.getBlockPosition().getY();
        for (int x = 0; x <= 1; x++) for (int z = 0; z <= 1; z++) {
            float weight = (x == 0 ? 1 - fractionX : fractionX) * (z == 0 ? 1 - fractionZ : fractionZ);
            if (LatexSpaceWeather.isWhite(level.getBiome(new BlockPos((cellX + x) * 4, y, (cellZ + z) * 4)))) whiteWeight += weight;
        }
        return rain * whiteWeight;
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float strength = strength(event.getCamera(), event.getPartialTick());
        if (strength <= 0) return;
        event.setRed(Mth.lerp(strength, event.getRed(), 0.97F));
        event.setGreen(Mth.lerp(strength, event.getGreen(), 0.97F));
        event.setBlue(Mth.lerp(strength, event.getBlue(), 0.97F));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) return;
        float strength = strength(event.getCamera(), event.getPartialTick());
        if (strength <= 0) return;
        // Preserve any already-shorter fog distance (e.g. blindness or low render distance).
        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        float thunder = Minecraft.getInstance().level.getThunderLevel((float) event.getPartialTick());
        event.setFarPlaneDistance(Mth.lerp(strength, far, Math.min(far, Mth.lerp(thunder, 32.0F, 12.0F))));
        event.setNearPlaneDistance(Mth.lerp(strength, near, Math.min(near, Mth.lerp(thunder, 4.0F, 1.0F))));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
