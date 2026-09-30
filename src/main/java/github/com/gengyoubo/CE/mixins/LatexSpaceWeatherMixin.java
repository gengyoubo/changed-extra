package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.weather.LatexSpaceLevelData;
import github.com.gengyoubo.CE.weather.LatexSpaceWeatherData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class LatexSpaceWeatherMixin {
    @Shadow public abstract ServerLevel overworld();
    @Shadow public abstract WorldData getWorldData();

    // Install before ServerLevel.prepareWeather(), so login and dimension changes
    // also receive the correct persisted state through vanilla weather packets.
    // A constructor redirect avoids ModifyArgs' synthetic Args classes, which
    // cannot be resolved by the module class loader in this Forge environment.
    @Redirect(method = "createLevels", at = @At(value = "NEW", target = "net/minecraft/server/level/ServerLevel"))
    private ServerLevel changede$installLatexWeather(MinecraftServer server, Executor executor,
            LevelStorageSource.LevelStorageAccess storage, ServerLevelData original,
            ResourceKey<Level> dimension, LevelStem stem, ChunkProgressListener progress,
            boolean debug, long seed, List<CustomSpawner> spawners, boolean tickTime,
            RandomSequences randomSequences) {
        ServerLevelData data = original;
        if (dimension.location().equals(ResourceLocation.fromNamespaceAndPath("changede", "latex_space"))) {
            LatexSpaceWeatherData weather = overworld().getDataStorage().computeIfAbsent(
                    LatexSpaceWeatherData::load, () -> new LatexSpaceWeatherData(original), LatexSpaceWeatherData.ID);
            data = new LatexSpaceLevelData(getWorldData(), original, weather);
        }
        return new ServerLevel(server, executor, storage, data, dimension, stem, progress,
                debug, seed, spawners, tickTime, randomSequences);
    }
}
