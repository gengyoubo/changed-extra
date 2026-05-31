package github.com.gengyoubo.CE.mixins;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ChangedEMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("changede-mixin-plugin");
    private static final String PROJECT_EXTENDED_PREFIX = "github.com.gengyoubo.CE.projectextended.mixins.";
    private static final Set<String> ADDON_CLIENT_MIXINS = Set.of(
            "github.com.gengyoubo.CE.mixins.AccessorySlotsSafetyMixin",
            "github.com.gengyoubo.CE.mixins.ClientEventMixin",
            "github.com.gengyoubo.CE.mixins.SignalCatcherItemMixin",
            "github.com.gengyoubo.CE.mixins.TransfurVariantsInfoMixin"
    );
    private static final Set<String> ADDON_COMMON_MIXINS = Set.of(
            "github.com.gengyoubo.CE.LP.mixins.UnifuserEnergyMixin",
            "github.com.gengyoubo.CE.LP.mixins.CatalyzerEnergyMixin"
    );
    private static final boolean ADDON_LIGHT_MODE = Boolean.parseBoolean(System.getProperty("changede.mixin.addonLight", "false"));

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info(
                "ChangedE mixin plugin loaded: addonLight={}, dist={}, changed_addon_loaded={}",
                ADDON_LIGHT_MODE,
                FMLEnvironment.dist,
                isModLoaded("changed_addon")
        );
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Replaced by event-based tooltip handlers, no longer needs bytecode transform.
        if ("github.com.gengyoubo.CE.mixins.GooCoreBlockMixin".equals(mixinClassName)) {
            return false;
        }

        // Client-only addon UI mixins.
        if (ADDON_CLIENT_MIXINS.contains(mixinClassName)) {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return false;
            }
            if (!isModLoaded("changed_addon")) {
                return false;
            }
            return !ADDON_LIGHT_MODE;
        }

        if (ADDON_COMMON_MIXINS.contains(mixinClassName)) {
            return isModLoaded("changed_addon");
        }

        // These two mixins only make sense when both optional dependencies exist.
        if (mixinClassName.startsWith(PROJECT_EXTENDED_PREFIX)) {
            return isModLoaded("projecte") && isModLoaded("projectextended");
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isModLoaded(String modId) {
        try {
            LoadingModList loadingModList = LoadingModList.get();
            if (loadingModList != null && loadingModList.getModFileById(modId) != null) {
                return true;
            }
        } catch (Throwable ignored) {
            // Fall through to game-scope ModList check.
        }

        try {
            return ModList.get().isLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
