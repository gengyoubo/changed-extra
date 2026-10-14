package github.com.gengyoubo.CE.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Query the server's loaded biomes instead of rebuilding vanilla worldgen with mod references. */
@Pseudo
@Mixin(targets = "jeresources.api.util.BiomeHelper", remap = false)
public abstract class JerBiomeHelperMixin {
    @Redirect(method = {"getBiome", "getBiomes", "getAllBiomes"}, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/data/registries/VanillaRegistries;createLookup()Lnet/minecraft/core/HolderLookup$Provider;",
            remap = true), remap = false, require = 3)
    private static HolderLookup.Provider changede$useLoadedBiomes() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null ? connection.registryAccess() : VanillaRegistries.createLookup();
    }
}
