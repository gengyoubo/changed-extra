package github.com.gengyoubo.mixins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import github.com.gengyoubo.fix.PatreonBenefitsFix;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import org.spongepowered.asm.mixin.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
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
    @Shadow public static int currentVersion;

    @Unique
    private static void changede$setRepoBase(String base) {
        REPO_BASE = base;
        LINKS_DOCUMENT = REPO_BASE + "listing.json";
        VERSION_DOCUMENT = REPO_BASE + "version.txt";
        FORMS_DOCUMENT = REPO_BASE + "forms/index.json";
        FORMS_BASE = REPO_BASE + "forms/";
    }

    @Unique
    private static void changede$loadLevels(HttpClient client) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(LINKS_DOCUMENT)).GET().build();
        JsonElement json = JsonParser.parseString(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
        JsonArray links = json.getAsJsonObject().get("players").getAsJsonArray();
        links.forEach(element -> {
            JsonObject object = element.getAsJsonObject();
            CACHED_LEVELS.put(UUID.fromString(object.get("uuid").getAsString()), PatreonBenefits.Tier.ofValue(object.get("tier").getAsInt()));
        });
    }

    @Unique
    private static int changede$loadVersion(HttpClient client) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(VERSION_DOCUMENT)).GET().build();
        return Integer.parseInt(client.send(request, HttpResponse.BodyHandlers.ofString()).body().replace("\n", ""));
    }

/**
 * @author gengyoubo
 * @reason Use custom repo as primary and official repo as fallback.
 */
@Overwrite
    private static void updatePathStrings() {
        changede$setRepoBase(PatreonBenefitsFix.getPrimaryRepositoryBase());
    }

    /**
     * @author gengyoubo
     * @reason Dual-link mode: load official and custom repositories, then merge.
     */
    @Overwrite
    public static void loadBenefits() throws IOException, InterruptedException {
        if (!Changed.config.common.downloadPatreonContent.get()) {
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        List<String> repos = PatreonBenefitsFix.getRepositoryBases();
        String primaryRepo = PatreonBenefitsFix.getPrimaryRepositoryBase();
        String defaultRepo = repos.isEmpty() ? primaryRepo : repos.get(0);

        CACHED_LEVELS.clear();
        int mergedVersion = -1;
        int successCount = 0;
        boolean primaryLoaded = false;
        Exception lastError = null;
        for (String repo : repos) {
            changede$setRepoBase(repo);
            try {
                changede$loadLevels(client);
                mergedVersion = Math.max(mergedVersion, changede$loadVersion(client));
                successCount++;
                if (repo.equals(primaryRepo)) {
                    primaryLoaded = true;
                }
                Changed.LOGGER.info("Patreon benefits merged source {}", repo);
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                lastError = ex;
                Changed.LOGGER.warn("Failed loading patreon benefits from {}", repo, ex);
            }
        }

        if (successCount > 0) {
            if (mergedVersion >= 0) {
                currentVersion = mergedVersion;
            }
            changede$setRepoBase(primaryLoaded ? primaryRepo : defaultRepo);
            Changed.LOGGER.info(
                    "Patreon benefits merged from {} source(s), players={}, activeRepo={}",
                    successCount,
                    CACHED_LEVELS.size(),
                    REPO_BASE
            );
            return;
        }

        if (lastError instanceof IOException ioEx) {
            throw ioEx;
        }
        if (lastError instanceof RuntimeException rtEx) {
            throw rtEx;
        }
        throw new IOException("Failed loading patreon benefits from all repositories: " + repos, lastError);
    }

}


