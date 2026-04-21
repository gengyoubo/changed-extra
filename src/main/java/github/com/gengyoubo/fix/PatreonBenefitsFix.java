package github.com.gengyoubo.fix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import github.com.gengyoubo.changede;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.data.DeferredModelLayerLocation;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PatreonBenefitsFix extends PatreonBenefits {
    public static final DeferredRegister<AbstractAbility<?>> REGISTRY= ChangedRegistry.ABILITY.createDeferred("changed");
    private static final String SPECIAL_FORM_PATH_PREFIX = "special/form_";
    private static final HttpClient TEXTURE_PROBE_CLIENT = HttpClient.newBuilder().build();
    private static final Map<UUID, SpecialForm> CACHED_SPECIAL_FORMS = new HashMap<>();
    public static RegistryObject<SelectSpecialStateAbilityFix> SELECT_SPECIAL_STATE = REGISTRY.register("select_special_state", SelectSpecialStateAbilityFix::new);
    private static String REPO_BASE;
    private static String LINKS_DOCUMENT;
    private static String VERSION_DOCUMENT;
    private static String FORMS_DOCUMENT;
    private static String FORMS_BASE;
    private static Map<UUID, PatreonBenefits.Tier> CACHED_LEVELS;
    private static int COMPATIBLE_VERSION;
    private static int CURRENT_VERSION;

    @Nullable
    public static SpecialForm getPlayerSpecialForm(UUID player) {
        return CACHED_SPECIAL_FORMS.getOrDefault(player, null);
    }

    public static TransfurVariant<?> getPlayerSpecialVariant(UUID player) {
        SpecialForm form = getPlayerSpecialForm(player);
        if (form == null)
            return null;
        return form.variant;
    }

    public static ResourceLocation getSpecialFormId(UUID player) {
        return Changed.modResource(SPECIAL_FORM_PATH_PREFIX + player);
    }

    public static boolean isSpecialFormId(@Nullable ResourceLocation id) {
        return id != null
                && Changed.MODID.equals(id.getNamespace())
                && id.getPath().startsWith(SPECIAL_FORM_PATH_PREFIX);
    }

    @Nullable
    public static TransfurVariant<?> getSpecialVariant(ResourceLocation id) {
        if (!isSpecialFormId(id)) return null;

        String uuidPart = id.getPath().substring(SPECIAL_FORM_PATH_PREFIX.length());
        try {
            return getPlayerSpecialVariant(UUID.fromString(uuidPart));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Nullable
    public static TransfurVariant<?> resolveVariant(ResourceLocation id) {
        if (id == null) return null;

        TransfurVariant<?> specialVariant = getSpecialVariant(id);
        if (specialVariant != null) return specialVariant;

        // Keep special latex usable even when per-player Patreon form is missing.
        if (isSpecialFormId(id)) {
            TransfurVariant<?> baseSpecial = ChangedRegistry.TRANSFUR_VARIANT.get().getValue(TransfurVariant.SPECIAL_LATEX);
            if (baseSpecial != null) {
                return baseSpecial;
            }
        }

        return ChangedRegistry.TRANSFUR_VARIANT.get().getValue(id);
    }

    public static void loadSpecialForms(HttpClient client) throws Exception {
        if (!Changed.config.common.downloadPatreonContent.get()) return;
        readFields();
        HttpRequest request = HttpRequest.newBuilder(URI.create(FORMS_DOCUMENT)).GET().build();
        JsonElement json = JsonParser.parseString(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
        JsonArray formLocations = json.getAsJsonObject().get("forms").getAsJsonArray();

        AtomicInteger count = new AtomicInteger(0);

        ChangedRegistry.TRANSFUR_VARIANT.get();
        formLocations.forEach((element) -> {
            JsonObject object = element.getAsJsonObject();
            if (GsonHelper.getAsInt(object, "version", 1) > COMPATIBLE_VERSION)
                return;

            SpecialForm form = SpecialForm.fromJSON(
                    str -> {
                        try {
                            URI uri = resolveFromFormsBase(str);
                            if (uri == null) return new JsonObject();
                            return JsonParser.parseString(
                                    client.send(HttpRequest.newBuilder(uri).GET().build(),
                                            HttpResponse.BodyHandlers.ofString()).body()
                            ).getAsJsonObject();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return new JsonObject();
                        }
                    },
                    object
            );
            CACHED_SPECIAL_FORMS.put(form.playerUUID, form);
            count.getAndIncrement();
            ResourceLocation id = getSpecialFormId(form.playerUUID);
            changede.LOGGER.debug("Loaded special form {}", id);
        });
        changede.LOGGER.info("Updated {} patreon special forms", count.get());
    }

    public static void registerOnlineTexture(ResourceLocation location) {
        if (location == null) return;
        URI uri = resolveFromFormsBase(location.getPath());
        if (uri == null) {
            changede.LOGGER.warn("Skip online texture registration due to invalid FORMS_BASE. location={}", location);
            return;
        }
        if (!canLoadAsImage(uri)) {
            changede.LOGGER.warn("Skip online texture registration due to invalid image payload. location={} uri={}", location, uri);
            return;
        }
        DynamicClient.lateRegisterOnlineTexture(location, uri);
    }

    @Nullable
    private static URI resolveFromFormsBase(String relativePath) {
        if (relativePath == null) return null;

        String base = FORMS_BASE;
        if (base == null || base.isBlank()) {
            base = (REPO_BASE == null || REPO_BASE.isBlank()) ? null : REPO_BASE + "forms/";
        }
        if (base == null || base.isBlank()) return null;

        try {
            URI baseUri = URI.create(base.endsWith("/") ? base : (base + "/"));
            URI resolved = baseUri.resolve(relativePath);
            String scheme = resolved.getScheme();
            if (scheme == null || scheme.isBlank()) return null;
            return resolved;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean canLoadAsImage(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<InputStream> response = TEXTURE_PROBE_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                return false;
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.isBlank() && !contentType.toLowerCase(Locale.ROOT).contains("image")) {
                return false;
            }

            try (InputStream stream = response.body()) {
                byte[] signature = stream.readNBytes(8);
                if (signature.length < 8) return false;
                // PNG signature: 89 50 4E 47 0D 0A 1A 0A
                return (signature[0] & 0xFF) == 0x89
                        && signature[1] == 0x50
                        && signature[2] == 0x4E
                        && signature[3] == 0x47
                        && signature[4] == 0x0D
                        && signature[5] == 0x0A
                        && signature[6] == 0x1A
                        && signature[7] == 0x0A;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static void readFields() throws Exception {
        Class<?> cls = Class.forName("net.ltxprogrammer.changed.util.PatreonBenefits");

        Field repoBase = cls.getDeclaredField("REPO_BASE");
        repoBase.setAccessible(true);
        REPO_BASE = (String) repoBase.get(null);
        Field linksDocument =cls.getDeclaredField("LINKS_DOCUMENT");
        linksDocument.setAccessible(true);
        LINKS_DOCUMENT = (String) linksDocument.get(null);
        Field compatibleVersion = cls.getDeclaredField("COMPATIBLE_VERSION");
        compatibleVersion.setAccessible(true);
        COMPATIBLE_VERSION = compatibleVersion.getInt(null);

        Field cachedLevels = cls.getDeclaredField("CACHED_LEVELS");
        cachedLevels.setAccessible(true);
        CACHED_LEVELS = (Map<UUID, PatreonBenefits.Tier>) cachedLevels.get(null);

        Field currentVersion = cls.getDeclaredField("currentVersion");
        currentVersion.setAccessible(true);
        CURRENT_VERSION = currentVersion.getInt(null);

        Field formsDocument = cls.getDeclaredField("FORMS_DOCUMENT");
        formsDocument.setAccessible(true);
        FORMS_DOCUMENT = (String) formsDocument.get(null);
        Field formsBase = cls.getDeclaredField("FORMS_BASE");
        formsBase.setAccessible(true);
        FORMS_BASE = (String) formsBase.get(null);

        Field versionDocumnt=cls.getDeclaredField("VERSION_DOCUMENT");
        versionDocumnt.setAccessible(true);
        VERSION_DOCUMENT =(String) versionDocumnt.get(null);
    }

    public record SpecialForm(
            UUID playerUUID,
            String defaultState,
            Map<String, PatreonBenefits.EntityData> entityData,
            Map<String, PatreonBenefits.ModelData> modelData,
            OldTransfurVariant<?> variant
    ) {
        private static final Map<Dist, AtomicBoolean> UPDATE_FLAG = new HashMap<>();
        private static final AtomicBoolean UPDATE_PLAYER_JOIN_FLAG = new AtomicBoolean(false);

        public static void loadBenefits() throws Exception {
            if (!Changed.config.common.downloadPatreonContent.get()) return;
            // Load levels
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(LINKS_DOCUMENT)).GET().build();

            try {
                JsonElement json = JsonParser.parseString(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
                JsonArray links = json.getAsJsonObject().get("players").getAsJsonArray();

                links.forEach((element) -> {
                    JsonObject object = element.getAsJsonObject();
                    CACHED_LEVELS.put(UUID.fromString(object.get("uuid").getAsString()), PatreonBenefits.Tier.ofValue(object.get("tier").getAsInt()));
                });
            } catch (Exception ex) {
                changede.LOGGER.error("Encountered error while fetching patronage levels");
                throw ex;
            }

            // Load forms
            try {
                loadSpecialForms(client);
            } catch (Exception ex) {
                changede.LOGGER.error("Encountered error while loading special forms");
                throw ex;
            }

            // Load version
            try {
                request = HttpRequest.newBuilder(URI.create(VERSION_DOCUMENT)).GET().build();
                CURRENT_VERSION = Integer.parseInt(client.send(request, HttpResponse.BodyHandlers.ofString()).body().replace("\n", ""));
            } catch (Exception ex) {
                changede.LOGGER.error("Encountered error while fetching patron data version");
                throw ex;
            }
        }

        public static SpecialForm fromJSON(Function<String, JsonObject> jsonGetter, JsonObject object) {
            Map<String, PatreonBenefits.ModelData> models = new HashMap<>();
            Map<String, PatreonBenefits.EntityData> entities = new HashMap<>();
            String dState = "default";
            UUID uuid = UUID.fromString(GsonHelper.getAsString(object, "location"));
            List<AbstractAbility<?>> injectedAbilities = new ArrayList<>();

            if (object.has("entities")) {
                GsonHelper.getAsJsonArray(object, "entities").forEach(element -> {
                    JsonObject entityObject = element.getAsJsonObject();
                    String id = GsonHelper.getAsString(entityObject, "id");
                    entities.put(id, PatreonBenefits.EntityData.fromJSON(uuid, entityObject));
                });
                dState = GsonHelper.getAsString(object, "defaultState", dState);
            } else {
                entities.put(dState, PatreonBenefits.EntityData.fromJSON(uuid, object));
            }

            if (entities.size() > 1)
                injectedAbilities.add(SELECT_SPECIAL_STATE.get());

            for (var entry : entities.values()) {
                if (entry.hairStyles().size() > 1) {
                    injectedAbilities.add(ChangedAbilities.SELECT_HAIRSTYLE.get());
                    break;
                }
            }

            final String lockedDefault = dState;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (object.has("models")) {
                    GsonHelper.getAsJsonArray(object, "models").forEach(element -> {
                        JsonObject modelObject = element.getAsJsonObject();
                        String id = GsonHelper.getAsString(modelObject, "id");
                        models.put(id, PatreonBenefits.ModelData.fromJSON(jsonGetter, uuid + "/" + id, modelObject));
                    });
                } else {
                    models.put(lockedDefault, PatreonBenefits.ModelData.fromJSON(jsonGetter, uuid.toString(), object));
                }
            });

            return new SpecialForm(
                    uuid,
                    dState,
                    entities,
                    models,
                    OldTransfurVariant.fromJson(
                            Changed.modResource("special/form_" + uuid),
                            object.get("variant").getAsJsonObject(),
                            injectedAbilities
                    )
            );
        }

        public PatreonBenefits.EntityData getDefaultEntity() {
            return entityData.get(defaultState);
        }

        public PatreonBenefits.ModelData getDefaultModel() {
            PatreonBenefits.ModelData defaultModel = this.modelData.get(defaultState);
            if (defaultModel != null) return defaultModel;
            return this.modelData.values().stream().findFirst().orElse(null);
        }
    }
}
