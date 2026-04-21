package github.com.gengyoubo.fix;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.entity.*;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.ltxprogrammer.changed.entity.latex.LatexType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OldTransfurVariant<T extends ChangedEntity>  extends TransfurVariant<T>{
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-70000000);
    // Variant properties
    public final Supplier<? extends EntityType<? extends ChangedEntity>> ctor;
    public final LatexType type;
    public final float jumpStrength;
    public final TransfurVariant.BreatheMode breatheMode;
    public final float stepSize;
    public final boolean canGlide;
    public final int extraJumpCharges;
    public final boolean reducedFall;
    public final boolean canClimb;
    public final VisionType visionType;
    public final MiningStrength miningStrength;
    public final UseItemMode itemUseMode;
    public final List<Class<? extends PathfinderMob>> scares;
    public final TransfurMode transfurMode;
    public final ImmutableList<Function<EntityType<?>, ? extends AbstractAbility<?>>> abilities;
    public final float cameraZOffset;
    public final ResourceLocation sound;
    private ResourceLocation formId;
    public OldTransfurVariant(Supplier<EntityType<T>> ctor,
                              LatexType type,
                           float jumpStrength, BreatheMode breatheMode, float stepSize, boolean canGlide, int extraJumpCharges,
                           boolean reducedFall, boolean canClimb,
                           VisionType visionType, MiningStrength miningStrength, UseItemMode itemUseMode, List<Class<? extends PathfinderMob>> scares, TransfurMode transfurMode,
                           List<Function<EntityType<?>, ? extends AbstractAbility<?>>> abilities, float cameraZOffset,ResourceLocation sound) {
        super(ctor,breatheMode,canGlide,extraJumpCharges,canClimb,visionType,miningStrength,itemUseMode,  (entity, mob) ->
                scares.stream().anyMatch(clazz -> clazz.isInstance(mob)),transfurMode,abilities,cameraZOffset,sound);
        this.ctor = ctor;
        this.type = type;
        this.jumpStrength = jumpStrength;
        this.breatheMode = breatheMode;
        this.stepSize = stepSize;
        this.miningStrength = miningStrength;
        this.visionType = visionType;
        this.canGlide = canGlide;
        this.extraJumpCharges = extraJumpCharges;
        this.itemUseMode = itemUseMode;
        this.abilities = ImmutableList.<Function<EntityType<?>, ? extends AbstractAbility<?>>>builder().addAll(abilities).build();
        this.reducedFall = reducedFall;
        this.canClimb = canClimb;
        this.scares = scares;
        this.transfurMode = transfurMode;
        this.cameraZOffset = cameraZOffset;
        this.sound = sound;
    }

    public static int getNextEntId() {
        return NEXT_ENTITY_ID.getAndDecrement();
    }

    // Parses variant from JSON, does not register variant
    public static Object fromJson(ResourceLocation id, JsonObject root) {
        return fromJson(id, root, List.of());
    }

    private static <E extends Enum<E>> E parseEnumOrDefault(
            Class<E> enumClass,
            String rawValue,
            E defaultValue,
            ResourceLocation variantId,
            String fieldName
    ) {
        if (rawValue == null || rawValue.isBlank()) return defaultValue;

        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException ignored) {
            // Upstream Patreon JSON occasionally ships invalid enum strings.
            // Keep startup resilient by logging and falling back.
            Changed.LOGGER.warn(
                    "Invalid {} '{}' for variant {}. Falling back to {}.",
                    fieldName, rawValue, variantId, defaultValue
            );
            return defaultValue;
        }
    }

    private static ResourceLocation parseSoundOrDefault(JsonObject root, ResourceLocation variantId) {
        ResourceLocation fallbackSound = ChangedSounds.SYRINGE_PRICK.getId();
        if (fallbackSound == null) {
            fallbackSound = Changed.modResource("syringe_prick");
        }

        String rawSound = GsonHelper.getAsString(root, "sound", fallbackSound.toString());
        ResourceLocation soundId = ResourceLocation.tryParse(rawSound);
        if (soundId != null && ForgeRegistries.SOUND_EVENTS.containsKey(soundId)) {
            return soundId;
        }

        Changed.LOGGER.warn(
                "Invalid or missing sound '{}' for variant {}. Falling back to {}.",
                rawSound, variantId, fallbackSound
        );
        return fallbackSound;
    }

    public static OldTransfurVariant<?> fromJson(ResourceLocation id, JsonObject root, List<AbstractAbility<?>> injectAbilities) {
        ResourceLocation entityType = ResourceLocation.tryParse(GsonHelper.getAsString(root, "entity", ChangedEntitiesFix.SPECIAL_LATEX.getId().toString()));
        EntityType<? extends ChangedEntity> resolvedEntityType = entityType != null ? (EntityType<? extends ChangedEntity>) ForgeRegistries.ENTITY_TYPES.getValue(entityType) : null;
        if (resolvedEntityType == null) {
            Changed.LOGGER.warn("Invalid entity type '{}' for variant {}. Falling back to {}.",
                    entityType, id, ChangedEntitiesFix.SPECIAL_LATEX.getId());
            resolvedEntityType = ChangedEntitiesFix.SPECIAL_LATEX.get();
        }
        final EntityType<? extends ChangedEntity> parsedEntityType = resolvedEntityType;

        List<Class<? extends PathfinderMob>> scares = new ArrayList<>(ImmutableList.of(AbstractVillager.class));
        GsonHelper.getAsJsonArray(root, "scares", new JsonArray()).forEach(element -> {
            try {
                scares.add((Class<? extends PathfinderMob>) Class.forName(element.getAsString()));
            } catch (Exception e) {
                Changed.LOGGER.error("Invalid class given: {}", element.getAsString());
            }
        });

        List<AbstractAbility<?>> abilities = new ArrayList<>(injectAbilities);
        GsonHelper.getAsJsonArray(root, "abilities", new JsonArray()).forEach(element -> {
            abilities.add(ChangedRegistry.ABILITY.get().getValue(ResourceLocation.tryParse(element.getAsString())));
        });

        List<Function<EntityType<?>, ? extends AbstractAbility<?>>> nAbilitiesList = abilities.stream().map(a -> (Function<EntityType<?>, AbstractAbility<?>>) type -> a).collect(Collectors.toList());

        boolean nightVision = GsonHelper.getAsBoolean(root, "nightVision", false);
        boolean weakMining = GsonHelper.getAsBoolean(root, "weakMining", false);
        ResourceLocation latexTypeId = ResourceLocation.tryParse(
                GsonHelper.getAsString(root, "latexType", "changed:none")
        );

        var latexType = latexTypeId != null
                ? ChangedRegistry.LATEX_TYPE.getValue(latexTypeId)
                : ChangedLatexTypes.NONE.get();

        if (latexType == null) {
            latexType = ChangedLatexTypes.NONE.get();
        }

        BreatheMode breatheMode = parseEnumOrDefault(
                BreatheMode.class,
                GsonHelper.getAsString(root, "breatheMode", BreatheMode.NORMAL.toString()),
                BreatheMode.NORMAL,
                id,
                "breatheMode"
        );
        UseItemMode useItemMode = parseEnumOrDefault(
                UseItemMode.class,
                GsonHelper.getAsString(root, "itemUseMode", UseItemMode.NORMAL.toString()),
                UseItemMode.NORMAL,
                id,
                "itemUseMode"
        );
        TransfurMode transfurMode = parseEnumOrDefault(
                TransfurMode.class,
                GsonHelper.getAsString(root, "transfurMode", TransfurMode.REPLICATION.toString()),
                TransfurMode.REPLICATION,
                id,
                "transfurMode"
        );
        ResourceLocation soundId = parseSoundOrDefault(root, id);

        return new OldTransfurVariant<>(
                () -> parsedEntityType,
                latexType,
                GsonHelper.getAsFloat(root, "jumpStrength", 1.0f),
                breatheMode,
                GsonHelper.getAsFloat(root, "stepSize", 0.6f),
                GsonHelper.getAsBoolean(root, "canGlide", false),
                GsonHelper.getAsInt(root, "extraJumpCharges", 0),
                GsonHelper.getAsBoolean(root, "reducedFall", false),
                GsonHelper.getAsBoolean(root, "canClimb", false),
                VisionType.fromSerial(GsonHelper.getAsString(root, "visionType", (nightVision ? VisionType.NIGHT_VISION : VisionType.NORMAL).getSerializedName()))
                        .result().orElse(VisionType.NORMAL),
                MiningStrength.fromSerial(GsonHelper.getAsString(root, "miningStrength", (weakMining ? MiningStrength.WEAK : MiningStrength.NORMAL).getSerializedName()))
                        .result().orElse(MiningStrength.NORMAL),
                useItemMode,
                scares,
                transfurMode,
                nAbilitiesList,
                GsonHelper.getAsFloat(root, "cameraZOffset", 0.0F),
                soundId).setFormId(id);
    }

    @Override
    public ResourceLocation getFormId() {
        return formId != null ? formId : super.getFormId();
    }

    public OldTransfurVariant<T> setFormId(ResourceLocation id) {
        this.formId = id;
        return this;
    }

    @Override
    public String toString() {
        ResourceLocation id = getFormId();
        return id != null ? id.toString() : "unregistered_old_transfur_variant";
    }

    public boolean isReducedFall() {
        return reducedFall;
    }

    public TransfurMode transfurMode() { return transfurMode; }

    public int getTicksRequiredToFreeze(Level level) {
        return ChangedEntities.getCachedEntity(level, ctor.get()).getTicksRequiredToFreeze();
    }

    public boolean is(@javax.annotation.Nullable OldTransfurVariant<?> variant) {
        if (variant == null)
            return false;
        return getEntityType() == variant.getEntityType();
    }

    public boolean is(@javax.annotation.Nullable Supplier<? extends TransfurVariant<?>> variant) {
        if (variant == null)
            return false;
        return getEntityType() == variant.get().getEntityType();
    }

    public EntityType<T> getEntityType() {
        return (EntityType<T>) ctor.get();
    }

    private T createChangedEntity(Level level) {
        T entity = (T)(((EntityType)this.ctor.get()).create(level));
        entity.setId(getNextEntId());
        entity.setSilent(true);
        entity.goalSelector.removeAllGoals((goal) -> true);
        return entity;
    }

    public static class UniversalAbilitiesEvent extends Event implements IModBusEvent {
        private final List<Function<EntityType<?>, ? extends AbstractAbility<?>>> abilities;

        public UniversalAbilitiesEvent(List<Function<EntityType<?>, ? extends AbstractAbility<?>>> abilities) {
            this.abilities = abilities;
        }

        public void addAbility(Supplier<? extends AbstractAbility<?>> ability) {
            abilities.add(type -> ability.get());
        }

        public void addAbility(Predicate<EntityType<?>> predicate, Supplier<? extends AbstractAbility<?>> ability) {
            abilities.add(type -> predicate.test(type) ? ability.get() : null);
        }

        public void addAbility(Function<EntityType<?>, ? extends AbstractAbility<?>> ability) {
            abilities.add(ability);
        }

        public Predicate<EntityType<?>> isOfTag(TagKey<EntityType<?>> tag) {
            return type -> type.is(tag);
        }

        public Predicate<EntityType<?>> isNotOfTag(TagKey<EntityType<?>> tag) {
            return type -> !type.is(tag);
        }
    }

    public static class Builder<T extends ChangedEntity> {
        final Supplier<EntityType<T>> entityType;
        float jumpStrength = 1.0F;
        OldTransfurVariant.BreatheMode breatheMode = OldTransfurVariant.BreatheMode.NORMAL;
        float stepSize = 0.6F;
        boolean canGlide = false;
        int extraJumpCharges = 0;
        boolean reducedFall = false;
        boolean canClimb = false;
        VisionType visionType = VisionType.NORMAL;
        MiningStrength miningStrength = MiningStrength.NORMAL;
        int legCount = 2;
        UseItemMode itemUseMode = UseItemMode.NORMAL;
        List<Class<? extends PathfinderMob>> scares = new ArrayList<>(ImmutableList.of(AbstractVillager.class));
        TransfurMode transfurMode = TransfurMode.REPLICATION;
        List<Function<EntityType<?>, ? extends AbstractAbility<?>>> abilities = new ArrayList<>();
        float cameraZOffset = 0.0F;

        public Builder(Supplier<EntityType<T>> entityType) {
            this.entityType = entityType;

            var event = new OldTransfurVariant.UniversalAbilitiesEvent(this.abilities);
            event.addAbility(event.isOfTag(ChangedTags.EntityTypes.LATEX)
                    .and(event.isNotOfTag(ChangedTags.EntityTypes.PARTIAL_LATEX)), ChangedAbilities.SWITCH_TRANSFUR_MODE);
            event.addAbility(event.isOfTag(ChangedTags.EntityTypes.LATEX)
                    .and(event.isNotOfTag(ChangedTags.EntityTypes.ARMLESS))
                    .and(event.isNotOfTag(ChangedTags.EntityTypes.PARTIAL_LATEX)), ChangedAbilities.GRAB_ENTITY_ABILITY);

            Changed.postModLoadingEvent(event);
        }

        public static <T extends ChangedEntity> OldTransfurVariant.Builder<T> of(Supplier<EntityType<T>> entityType) {
            return new OldTransfurVariant.Builder<T>(entityType);
        }

        public void ignored() {}

        public OldTransfurVariant.Builder<T> jumpStrength(float factor) {
            this.jumpStrength = factor; return this;
        }

        public OldTransfurVariant.Builder<T> gills() {
            return gills(false);
        }

        public OldTransfurVariant.Builder<T> gills(boolean suffocate_on_land) {
            this.breatheMode = suffocate_on_land ? OldTransfurVariant.BreatheMode.WATER : OldTransfurVariant.BreatheMode.ANY; return this;
        }

        public OldTransfurVariant.Builder<T> breatheMode(OldTransfurVariant.BreatheMode mode) {
            this.breatheMode = mode; return this;
        }

        public OldTransfurVariant.Builder<T> reducedFall() {
            this.reducedFall = true; return this;
        }

        public OldTransfurVariant.Builder<T> noVision() {
            this.visionType = VisionType.BLIND; return this;
        }

        public OldTransfurVariant.Builder<T> noVision(boolean v) {
            this.visionType = v ? VisionType.BLIND : visionType; return this;
        }

        public OldTransfurVariant.Builder<T> reducedFall(boolean v) {
            this.reducedFall = v; return this;
        }

        public OldTransfurVariant.Builder<T> canClimb() {
            this.canClimb = true; return this;
        }

        public OldTransfurVariant.Builder<T> canClimb(boolean v) {
            this.canClimb = v; return this;
        }

        public <E extends PathfinderMob> OldTransfurVariant.Builder<T> scares(Class<E> type) {
            scares.add(type); return this;
        }

        public OldTransfurVariant.Builder<T> scares(List<Class<? extends PathfinderMob>> v) {
            scares = v; return this;
        }

        public OldTransfurVariant.Builder<T> stepSize(float factor) {
            this.stepSize = factor; return this;
        }

        public OldTransfurVariant.Builder<T> glide() {
            return glide(true);
        }

        public OldTransfurVariant.Builder<T> glide(boolean enable) {
            this.canGlide = enable; return this;
        }

        public OldTransfurVariant.Builder<T> doubleJump() {
            return extraJumps(1);
        }

        public OldTransfurVariant.Builder<T> extraJumps(int count) {
            this.extraJumpCharges = count; return this;
        }

        public OldTransfurVariant.Builder<T> addAbility(Function<EntityType<?>, ? extends AbstractAbility<?>> ability) {
            if (ability != null)
                this.abilities.add(ability);
            return this;
        }

        public OldTransfurVariant.Builder<T> addAbility(Supplier<? extends AbstractAbility<?>> ability) {
            if (ability != null)
                this.abilities.add(type -> ability.get());
            return this;
        }

        public OldTransfurVariant.Builder<T> abilities(List<Function<EntityType<?>, ? extends AbstractAbility<?>>> abilities) {
            this.abilities = new ArrayList<>(abilities); return this;
        }

        public OldTransfurVariant.Builder<T> extraHands() {
            return addAbility(ChangedAbilities.SWITCH_HANDS);
        }

        public OldTransfurVariant.Builder<T> rideable() {
            return addAbility(ChangedAbilities.ACCESS_CHEST);
        }

        public OldTransfurVariant.Builder<T> absorbing() {
            return transfurMode(TransfurMode.ABSORPTION);
        }

        public OldTransfurVariant.Builder<T> replicating() {
            return transfurMode(TransfurMode.REPLICATION);
        }

        public OldTransfurVariant.Builder<T> nightVision() {
            this.visionType = VisionType.NIGHT_VISION; return this;
        }

        public OldTransfurVariant.Builder<T> nightVision(boolean v) {
            this.visionType = v ? VisionType.NIGHT_VISION : visionType; return this;
        }

        public OldTransfurVariant.Builder<T> visionType(VisionType type) {
            this.visionType = type; return this;
        }

        public OldTransfurVariant.Builder<T> weakMining() {
            this.miningStrength = MiningStrength.WEAK; return this;
        }

        public OldTransfurVariant.Builder<T> weakMining(boolean v) {
            this.miningStrength = v ? MiningStrength.WEAK : miningStrength; return this;
        }

        public OldTransfurVariant.Builder<T> miningStrength(MiningStrength strength) {
            this.miningStrength = strength; return this;
        }

        public OldTransfurVariant.Builder<T> transfurMode(TransfurMode mode) {
            this.transfurMode = mode; return this;
        }

        public OldTransfurVariant.Builder<T> noLegs() {
            this.legCount = 0;
            return this;
        }

        public OldTransfurVariant.Builder<T> hasLegs(boolean v) {
            this.legCount = 2;
            return this;
        }

        public OldTransfurVariant.Builder<T> quadrupedal() {
            this.legCount = 4;
            return this;
        }

        public OldTransfurVariant.Builder<T> disableItems() {
            this.itemUseMode = UseItemMode.NONE;
            return this;
        }

        public OldTransfurVariant.Builder<T> holdItemsInMouth() {
            this.itemUseMode = UseItemMode.MOUTH;
            return this;
        }

        public OldTransfurVariant.Builder<T> itemUseMode(UseItemMode v) {
            this.itemUseMode = v;
            return this;
        }

        public OldTransfurVariant.Builder<T> cameraZOffset(float v) {
            this.cameraZOffset = v; return this;
        }
    }
}
