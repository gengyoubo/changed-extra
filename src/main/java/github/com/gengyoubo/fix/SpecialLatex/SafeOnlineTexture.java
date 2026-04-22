package github.com.gengyoubo.fix.SpecialLatex;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.ltxprogrammer.changed.Changed;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

@OnlyIn(Dist.CLIENT)
public class SafeOnlineTexture extends AbstractTexture {
    private final URI uri;
    private final net.minecraft.resources.ResourceLocation debugLocation;

    public SafeOnlineTexture(URI uri, net.minecraft.resources.ResourceLocation debugLocation) {
        this.uri = uri;
        this.debugLocation = debugLocation;
    }

    @Override
    public void load(@NotNull ResourceManager resourceManager) {
        NativeImage image = null;
        try {
            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            try (InputStream stream = connection.getInputStream()) {
                image = NativeImage.read(stream);
            }
        } catch (Exception e) {
            Changed.LOGGER.warn("Failed to load online texture {} from {}", debugLocation, uri, e);
        }

        if (image == null) {
            // Graceful fallback: 1x1 opaque white pixel, never crash render thread.
            image = new NativeImage(1, 1, false);
            image.setPixelRGBA(0, 0, 0xFFFFFFFF);
        }

        final NativeImage finalImage = image;
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> uploadImage(finalImage));
        } else {
            uploadImage(finalImage);
        }
    }

    private void uploadImage(NativeImage image) {
        try (image) {
            TextureUtil.prepareImage(this.getId(), image.getWidth(), image.getHeight());
            image.upload(0, 0, 0, true);
        }
    }
}
