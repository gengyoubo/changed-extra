package github.com.gengyoubo.mixins;

import github.com.gengyoubo.changede;
import github.com.gengyoubo.fix.PatreonBenefitsFix;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        REPO_BASE = "https://" + (String) Changed.config.common.githubDomain.get() + "/gengyoubo/CEPB/main/";
        LINKS_DOCUMENT = REPO_BASE + "listing.json";
        VERSION_DOCUMENT = REPO_BASE + "version.txt";
        FORMS_DOCUMENT = REPO_BASE + "forms/index.json";
        FORMS_BASE = REPO_BASE + "forms/";
    }
    @Inject(method = "loadBenefits",at= @At(value = "INVOKE", target = "Ljava/net/http/HttpRequest;newBuilder(Ljava/net/URI;)Ljava/net/http/HttpRequest$Builder;",shift = At.Shift.BY,by=-1,ordinal = 1))
    private static void InjectloadBenefits(CallbackInfo ci){
        System.out.println(CACHED_LEVELS);
    }

}


