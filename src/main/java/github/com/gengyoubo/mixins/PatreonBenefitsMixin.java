package github.com.gengyoubo.mixins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

@Mixin(value = PatreonBenefits.class, remap = false)
public abstract class PatreonBenefitsMixin {
    @Shadow private static String REPO_BASE;
    @Shadow private static String LINKS_DOCUMENT;
    @Shadow private static String VERSION_DOCUMENT;
    @Shadow private static String FORMS_DOCUMENT;
    @Shadow private static String FORMS_BASE;
/**
 * @author gengyoubo
 * @reason overwrite
 */
@Overwrite
    private static void updatePathStrings() {
        REPO_BASE = "https://" + (String) Changed.config.common.githubDomain.get() + "/gengyoubo/CEPB/main/";
        LINKS_DOCUMENT = REPO_BASE + "listing.json";
        VERSION_DOCUMENT = REPO_BASE + "version.txt";
        FORMS_DOCUMENT = REPO_BASE + "forms/index.json";
        FORMS_BASE = REPO_BASE + "forms/";
    }
}
