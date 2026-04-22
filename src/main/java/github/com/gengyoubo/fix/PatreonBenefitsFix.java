package github.com.gengyoubo.fix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import github.com.gengyoubo.changede;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class PatreonBenefitsFix extends PatreonBenefits {
    public static final DeferredRegister<AbstractAbility<?>> REGISTRY= ChangedRegistry.ABILITY.createDeferred("changed");
    private static final String SPECIAL_FORM_PATH_PREFIX = "special/form_";
    private static final String OFFICIAL_REPO_BASE = "https://raw.githubusercontent.com/LtxProgrammer/patreon-benefits/main/";
    private static final HttpClient TEXTURE_PROBE_CLIENT = HttpClient.newBuilder().build();
    private static final LinkedHashSet<String> EXTRA_REPO_BASES = new LinkedHashSet<>();
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

    private static @Nullable String normalizeRepoBase(@Nullable String repoBase) {
        if (repoBase == null) return null;
        String trimmed = repoBase.trim();
        if (trimmed.isBlank()) return null;
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || scheme.isBlank()) return null;
            String normalized = uri.toString();
            return normalized.endsWith("/") ? normalized : (normalized + "/");
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getPrimaryRepositoryBase() {
        String primary = "https://" + Changed.config.common.githubDomain.get() + "/gengyoubo/CEPB/main/";
        String normalized = normalizeRepoBase(primary);
        return normalized != null ? normalized : primary;
    }

    /**
     * 追加一个新的 Patreon 数据仓库链接（repo 根目录）。
     * <p>
     * 传入示例: {@code https://example.com/my-benefits/main/}
     * <p>
     * 注意:
     * <p>
     * 1. 请在调用 {@link PatreonBenefits#loadBenefits()} 和 {@link SpecialForm#loadBenefits()} 之前添加。
     * <p>
     * 2. 会自动去重与标准化（补全末尾 '/'），无效链接会返回 false。
     * <p>
     * 3. 官方链接与主链接（githubDomain 指向）不需要重复添加。
     *
     * @param repoBase 新仓库根链接
     * @return true 表示成功加入列表，false 表示无效或已存在
     */
    public static synchronized boolean addRepositoryBase(String repoBase) {
        String normalized = normalizeRepoBase(repoBase);
        if (normalized == null) return false;
        if (normalized.equals(OFFICIAL_REPO_BASE) || normalized.equals(getPrimaryRepositoryBase())) {
            return false;
        }
        return EXTRA_REPO_BASES.add(normalized);
    }

    /**
     * 批量设置额外仓库链接。
     * <p>
     * 该方法会先清空旧的额外链接，再按输入重新添加（内部等价于多次调用 {@link #addRepositoryBase(String)}）。
     *
     * @param repoBases 额外仓库根链接集合
     */
    public static synchronized void setAdditionalRepositoryBases(Collection<String> repoBases) {
        EXTRA_REPO_BASES.clear();
        if (repoBases == null) return;
        repoBases.forEach(PatreonBenefitsFix::addRepositoryBase);
    }

    /**
     * 获取当前生效的仓库读取顺序。
     * <p>
     * 顺序固定为: 官方仓库 -> 主仓库 -> 额外仓库列表。
     * <p>
     * 加载时按此顺序合并，同 UUID/同 key 后加载会覆盖先加载数据。
     */
    public static synchronized List<String> getRepositoryBases() {
        LinkedHashSet<String> repos = new LinkedHashSet<>();
        repos.add(OFFICIAL_REPO_BASE);
        repos.add(getPrimaryRepositoryBase());
        repos.addAll(EXTRA_REPO_BASES);
        return new ArrayList<>(repos);
    }

    private static void applyRepoBase(String base) {
        REPO_BASE = base;
        LINKS_DOCUMENT = REPO_BASE + "listing.json";
        VERSION_DOCUMENT = REPO_BASE + "version.txt";
        FORMS_DOCUMENT = REPO_BASE + "forms/index.json";
        FORMS_BASE = REPO_BASE + "forms/";
    }

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
                            URI uri = resolveFromCurrentFormsBase(str);
                            if (uri == null) return new JsonObject();
                            return JsonParser.parseString(
                                    client.send(HttpRequest.newBuilder(uri).GET().build(),
                                            HttpResponse.BodyHandlers.ofString()).body()
                            ).getAsJsonObject();
                        } catch (Exception e) {
                            Changed.LOGGER.error(e);
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
        changede.LOGGER.info("Updated {} patreon special forms from {}", count.get(), REPO_BASE);
    }

    public static void registerOnlineTexture(ResourceLocation location) {
        if (location == null) return;
        for (String formsBase : candidateFormsBases()) {
            URI uri = resolveFromBase(formsBase, location.getPath());
            if (uri == null) continue;
            if (!canLoadAsImage(uri)) continue;
            DynamicClient.lateRegisterOnlineTexture(location, uri);
            return;
        }
        changede.LOGGER.warn("Skip online texture registration due to invalid image payload. location={} triedBases={}", location, candidateFormsBases());
    }

    @Nullable
    private static URI resolveFromCurrentFormsBase(String relativePath) {
        if (relativePath == null) return null;

        String base = FORMS_BASE;
        if (base == null || base.isBlank()) {
            base = (REPO_BASE == null || REPO_BASE.isBlank()) ? null : REPO_BASE + "forms/";
        }
        return resolveFromBase(base, relativePath);
    }

    @Nullable
    private static URI resolveFromBase(@Nullable String base, String relativePath) {
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

    private static List<String> candidateFormsBases() {
        LinkedHashSet<String> bases = new LinkedHashSet<>();
        if (FORMS_BASE != null && !FORMS_BASE.isBlank()) {
            bases.add(FORMS_BASE.endsWith("/") ? FORMS_BASE : (FORMS_BASE + "/"));
        }
        for (String repo : getRepositoryBases()) {
            String forms = repo.endsWith("/") ? (repo + "forms/") : (repo + "/forms/");
            bases.add(forms);
        }
        return new ArrayList<>(bases);
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
        public static void loadBenefits() throws Exception {
            if (!Changed.config.common.downloadPatreonContent.get()) return;
            readFields();

            HttpClient client = HttpClient.newHttpClient();
            List<String> repos = getRepositoryBases();
            String primary = getPrimaryRepositoryBase();
            boolean primaryLoaded = false;
            int successCount = 0;
            int mergedVersion = -1;
            Exception lastError = null;

            CACHED_LEVELS.clear();
            CACHED_SPECIAL_FORMS.clear();

            for (String repo : repos) {
                applyRepoBase(repo);
                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(LINKS_DOCUMENT)).GET().build();
                    JsonElement json = JsonParser.parseString(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
                    JsonArray links = json.getAsJsonObject().get("players").getAsJsonArray();
                    links.forEach(element -> {
                        JsonObject object = element.getAsJsonObject();
                        CACHED_LEVELS.put(UUID.fromString(object.get("uuid").getAsString()), PatreonBenefits.Tier.ofValue(object.get("tier").getAsInt()));
                    });

                    loadSpecialForms(client);

                    request = HttpRequest.newBuilder(URI.create(VERSION_DOCUMENT)).GET().build();
                    int version = Integer.parseInt(client.send(request, HttpResponse.BodyHandlers.ofString()).body().replace("\n", ""));
                    mergedVersion = Math.max(mergedVersion, version);

                    successCount++;
                    if (repo.equals(primary)) {
                        primaryLoaded = true;
                    }
                    changede.LOGGER.info("Patreon special data merged source {}", repo);
                } catch (InterruptedException ex) {
                    throw ex;
                } catch (Exception ex) {
                    lastError = ex;
                    changede.LOGGER.warn("Failed loading special data from {}", repo, ex);
                }
            }

            if (successCount > 0) {
                if (mergedVersion >= 0) {
                    CURRENT_VERSION = mergedVersion;
                }
                applyRepoBase(primaryLoaded ? primary : OFFICIAL_REPO_BASE);
                changede.LOGGER.info(
                        "Patreon special data merged from {} source(s), levels={}, forms={}, activeRepo={}",
                        successCount,
                        CACHED_LEVELS.size(),
                        CACHED_SPECIAL_FORMS.size(),
                        REPO_BASE
                );
                return;
            }

            if (lastError != null) throw lastError;
            throw new RuntimeException("Failed loading patreon special data from all repositories: " + repos);
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
