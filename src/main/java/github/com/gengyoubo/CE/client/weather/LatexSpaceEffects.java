package github.com.gengyoubo.CE.client.weather;

import github.com.gengyoubo.CE.weather.LatexSpaceWeather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** White biomes use fog; only dark biome columns receive the black latex rain sheets. */
@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LatexSpaceEffects extends DimensionSpecialEffects.OverworldEffects {
    private static final ResourceLocation RAIN = ResourceLocation.fromNamespaceAndPath("changede", "textures/environment/dark_latex_rain.png");
    private static final ResourceLocation DARK_LATEX_BLOCK = ResourceLocation.fromNamespaceAndPath("changed", "dark_latex_block");
    private int rainSoundTime;

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(LatexSpaceWeather.DIMENSION, new LatexSpaceEffects());
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture light,
                                     double cameraX, double cameraY, double cameraZ) {
        float rain = level.getRainLevel(partialTick);
        if (rain <= 0) return true;
        int centerX = Mth.floor(cameraX);
        int centerY = Mth.floor(cameraY);
        int centerZ = Mth.floor(cameraZ);
        int radius = Minecraft.useFancyGraphics() ? 10 : 5;
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        boolean started = false;
        light.turnOnLightLayer();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, RAIN);
        try {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                for (int x = centerX - radius; x <= centerX + radius; x++) {
                    // The center column has no well-defined facing direction.
                    if (x == centerX && z == centerZ) continue;
                    int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    int bottom = Math.max(centerY - radius, surface);
                    int top = Math.max(centerY + radius, surface);
                    if (bottom >= top) continue;
                    sample.set(x, Math.max(surface, centerY), z);
                    if (!LatexSpaceWeather.isDark(level.getBiome(sample))) continue;

                    if (!started) {
                        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        started = true;
                    }
                    double distance = Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                    double offsetX = -(z - centerZ) / distance * 0.5;
                    double offsetZ = (x - centerX) / distance * 0.5;
                    RandomSource random = RandomSource.create((long) (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761));
                    int phase = (ticks + x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761) & 31;
                    float fall = -(phase + partialTick) / 32.0F * (3 + random.nextFloat());
                    double dx = x + 0.5 - cameraX;
                    double dz = z + 0.5 - cameraZ;
                    float fade = (float) Math.sqrt(dx * dx + dz * dz) / radius;
                    float alpha = Mth.clamp(((1 - fade * fade) * 0.5F + 0.5F) * rain, 0, 1);
                    int packedLight = LevelRenderer.getLightColor(level, sample);
                    float upperV = bottom * 0.25F + fall;
                    float lowerV = top * 0.25F + fall;
                    float thunder = LatexSpaceWeather.isDarkForest(level.getBiome(sample))
                            ? level.getThunderLevel(partialTick) : 0;
                    for (int layer = 0; layer < (thunder > 0 ? 2 : 1); layer++) {
                        float layerAlpha = layer == 0 ? alpha : alpha * thunder;
                        float shift = layer * 0.5F;
                        buffer.vertex(dx - offsetX, top - cameraY, dz - offsetZ).uv(shift, upperV + shift).color(1F, 1F, 1F, layerAlpha).uv2(packedLight).endVertex();
                        buffer.vertex(dx + offsetX, top - cameraY, dz + offsetZ).uv(1 + shift, upperV + shift).color(1F, 1F, 1F, layerAlpha).uv2(packedLight).endVertex();
                        buffer.vertex(dx + offsetX, bottom - cameraY, dz + offsetZ).uv(1 + shift, lowerV + shift).color(1F, 1F, 1F, layerAlpha).uv2(packedLight).endVertex();
                        buffer.vertex(dx - offsetX, bottom - cameraY, dz - offsetZ).uv(shift, lowerV + shift).color(1F, 1F, 1F, layerAlpha).uv2(packedLight).endVertex();
                    }
                }
            }
            if (started) tessellator.end();
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            light.turnOffLightLayer();
        }
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        float rain = level.getRainLevel(1) / (Minecraft.useFancyGraphics() ? 1 : 2);
        if (rain <= 0) { rainSoundTime = 0; return true; }
        RandomSource random = RandomSource.create((long) ticks * 312987231L);
        BlockPos origin = camera.getBlockPosition();
        BlockPos soundPos = null;
        ParticleStatus particles = minecraft.options.particles().get();
        int attempts = (int) (100 * rain * rain) / (particles == ParticleStatus.DECREASED ? 2 : 1);
        int stormAttempts = (int) (attempts * level.getThunderLevel(1));
        var latexBlock = ForgeRegistries.BLOCKS.getValue(DARK_LATEX_BLOCK);
        BlockState latex = latexBlock == null ? Blocks.BLACK_CONCRETE.defaultBlockState() : latexBlock.defaultBlockState();

        for (int i = 0; i < attempts + stormAttempts; i++) {
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                    origin.offset(random.nextInt(21) - 10, 0, random.nextInt(21) - 10));
            if (surface.getY() <= level.getMinBuildHeight() || Math.abs(surface.getY() - origin.getY()) > 10
                    || !LatexSpaceWeather.isDark(level.getBiome(surface))) continue;
            if (i >= attempts && !LatexSpaceWeather.isDarkForest(level.getBiome(surface))) continue;
            soundPos = surface.below();
            if (particles == ParticleStatus.MINIMAL) break;
            double x = random.nextDouble();
            double z = random.nextDouble();
            BlockState block = level.getBlockState(soundPos);
            FluidState fluid = level.getFluidState(soundPos);
            double height = Math.max(block.getCollisionShape(level, soundPos).max(Direction.Axis.Y, x, z), fluid.getHeight(level, soundPos));
            boolean hot = fluid.is(FluidTags.LAVA) || block.is(Blocks.MAGMA_BLOCK) || CampfireBlock.isLitCampfire(block);
            ParticleOptions splash = hot ? ParticleTypes.SMOKE : new BlockParticleOption(ParticleTypes.BLOCK, latex);
            level.addParticle(splash, soundPos.getX() + x, soundPos.getY() + height + 0.01,
                    soundPos.getZ() + z, 0, 0.02, 0);
        }
        if (soundPos != null && random.nextInt(3) < rainSoundTime++) {
            rainSoundTime = 0;
            boolean covered = soundPos.getY() > origin.getY() + 1
                    && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin).getY() > origin.getY();
            level.playLocalSound(soundPos, covered ? SoundEvents.WEATHER_RAIN_ABOVE : SoundEvents.WEATHER_RAIN,
                    SoundSource.WEATHER, covered ? 0.1F : 0.2F, covered ? 0.5F : 1.0F, false);
        }
        return true;
    }
}
