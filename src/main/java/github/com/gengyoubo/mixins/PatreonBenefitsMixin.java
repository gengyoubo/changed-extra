package github.com.gengyoubo.mixins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;


@Mixin(value =PatreonBenefits.class,remap = false)
public class PatreonBenefitsMixin {
    @Shadow public static int currentVersion;
    @Final @Shadow private static Map<UUID, PatreonBenefits.Tier> CACHED_LEVELS;
    @Final @Shadow private static Logger LOGGER;

    @Inject(method = "loadBenefits", at = @At("HEAD"))
    private static void loadCustomBenefits(CallbackInfo ci) {
        String customBase = "https://raw.githubusercontent.com/gengyoubo/changed-extra/main/patreon-benefits/";
        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(customBase + "listing.json")).GET().build();
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

            JsonElement json = JsonParser.parseString(body);
            JsonArray links = json.getAsJsonObject().get("players").getAsJsonArray();
            links.forEach(element -> {
                JsonObject object = element.getAsJsonObject();
                CACHED_LEVELS.put(
                        UUID.fromString(object.get("uuid").getAsString()),
                        PatreonBenefits.Tier.ofValue(object.get("tier").getAsInt())
                );
            });
        } catch (Exception ex) {
            LOGGER.error("Failed to load custom patronage levels", ex);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(customBase + "version.txt")).GET().build();
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body().trim();
            int customVersion = Integer.parseInt(body);
            currentVersion = Math.max(currentVersion, customVersion);
        } catch (Exception ex) {
            LOGGER.error("Failed to load custom patronage version", ex);
        }
    }
}
