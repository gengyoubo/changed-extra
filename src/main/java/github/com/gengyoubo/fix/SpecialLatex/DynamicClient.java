package github.com.gengyoubo.fix.SpecialLatex;

import net.ltxprogrammer.changed.data.DeferredModelLayerLocation;
import net.ltxprogrammer.changed.init.ChangedTextures;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.DistExecutor;

import java.net.URI;
import java.util.function.Supplier;

public class DynamicClient {
    public static void lateRegisterLayerDefinition(DeferredModelLayerLocation layerLocation, Supplier<LayerDefinition> supplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Internal.lateRegisterLayerDefinition(layerLocation.get(), supplier));
    }

    public static void lateRegisterOnlineTexture(ResourceLocation location, URI uri) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChangedTextures.lateRegisterTexture(
                location,
                () -> new SafeOnlineTexture(uri, location)
        ));
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("UnstableApiUsage")
    private static class Internal {
        @SuppressWarnings("UnstableApiUsage")
        public static void lateRegisterLayerDefinition(ModelLayerLocation layerLocation, Supplier<LayerDefinition> supplier) {
            ForgeHooksClient.registerLayerDefinition(layerLocation, supplier);
        }
    }
}
