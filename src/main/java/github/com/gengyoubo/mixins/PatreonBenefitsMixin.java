package github.com.gengyoubo.mixins;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Mixin(value = PatreonBenefits.class, remap = false)
public abstract class PatreonBenefitsMixin {
    @Shadow private static String REPO_BASE;
    @Shadow private static String LINKS_DOCUMENT;
    @Shadow private static String VERSION_DOCUMENT;
    @Shadow private static String FORMS_DOCUMENT;
    @Shadow private static String FORMS_BASE;
    @Final @Shadow private static Map<UUID, PatreonBenefits.Tier> CACHED_LEVELS;
    @Final @Shadow private static int COMPATIBLE_VERSION;

/**
 * @author gengyoubo
 * @reason overwrite
 */
@Overwrite
    private static void updatePathStrings() throws Exception {
        REPO_BASE = "https://" + Changed.config.common.githubDomain.get() + "/gengyoubo/CEPB/main/";
        LINKS_DOCUMENT = REPO_BASE + "listing.json";
        VERSION_DOCUMENT = REPO_BASE + "version.txt";
        FORMS_DOCUMENT = REPO_BASE + "forms/index.json";
        FORMS_BASE = REPO_BASE + "forms/";
    }
    @ModifyArg(
            method = "loadBenefits",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/net/http/HttpRequest;newBuilder(Ljava/net/URI;)Ljava/net/http/HttpRequest$Builder;",
                    ordinal = 1
            ),
            index = 0
    )
    private static URI changede$logLoadBenefitsUri(URI uri) {
        Changed.LOGGER.debug("PatreonBenefits.loadBenefits request[1] uri={}, cachedLevels={}", uri, CACHED_LEVELS.size());
        return uri;
    }

}


