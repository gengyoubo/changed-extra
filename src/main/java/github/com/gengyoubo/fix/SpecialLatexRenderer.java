package github.com.gengyoubo.fix;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.client.renderer.AdvancedHumanoidRenderer;
import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.layers.EmissiveBodyLayer;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.client.renderer.model.armor.ArmorModelPicker;
import net.ltxprogrammer.changed.client.renderer.model.armor.ArmorModel;
import net.ltxprogrammer.changed.client.renderer.model.armor.LatexHumanoidArmorModel;
import net.ltxprogrammer.changed.data.DelayLoadedModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Objects;

public class SpecialLatexRenderer extends AdvancedHumanoidRenderer<SpecialLatex, SpecialLatexModel> {
    private static final Map<Pair<UUID, String>, SpecialLatexRenderer> SPECIAL_RENDERERS = new HashMap<>();
    private static final Set<Pair<UUID, String>> BROKEN_RENDERERS = ConcurrentHashMap.newKeySet();
    private static final ResourceLocation DELAYED_TEXTURE = Changed.modResource("textures/delay_loaded_latex.png");
    private static final String SPECIAL_FORM_PREFIX = "special/form_";
    private static final Set<ResourceLocation> LOGGED_MODEL_LOADS = ConcurrentHashMap.newKeySet();
    private static final DummySpecialModel DUMMY_SPECIAL_MODEL = new DummySpecialModel();
    private final EntityRendererProvider.Context context;
    private final boolean isDelegate;

    public SpecialLatexRenderer(EntityRendererProvider.Context context) {
        super(context, null, (ArmorModelPicker) null, 0.0f);
        this.isDelegate = true;
        this.context = context;
    }

    public SpecialLatexRenderer(EntityRendererProvider.Context context, PatreonBenefits.ModelData modelData) {
        super(
                context,
                createSpecialModel(context, modelData),
                createNoopArmorPicker(),
                modelData.shadowSize()
        );
        modelData.emissive().ifPresent(emissive -> this.addLayer(new EmissiveBodyLayer<>(this, emissive)));
        this.isDelegate = false;
        this.context = context;
    }

    private static PatreonBenefits.ModelData ensureModelIsLoaded(PatreonBenefits.ModelData modelData) {
        ResourceLocation layerId = modelData.modelLayerLocation().model;
        if (layerId != null && LOGGED_MODEL_LOADS.add(layerId)) {
            Changed.LOGGER.debug(
                    "SpecialLatexRenderer loading model layer={} texture={} emissivePresent={}",
                    layerId,
                    modelData.texture(),
                    modelData.emissive().isPresent()
            );
        }
        if (!modelData.model().isResolved()) {
            modelData.registerLayerDefinitions(DynamicClient::lateRegisterLayerDefinition);
            modelData.registerTextures(PatreonBenefitsFix::registerOnlineTexture);
        }
        return modelData;
    }

    private static SpecialLatexModel createSpecialModel(EntityRendererProvider.Context context, PatreonBenefits.ModelData modelData) {
        PatreonBenefits.ModelData loaded = ensureModelIsLoaded(modelData);
        try {
            return new SpecialLatexModel(context.bakeLayer(loaded.modelLayerLocation().get()), loaded);
        } catch (IllegalArgumentException layerMissing) {
            Changed.LOGGER.debug("Layer bake failed for {}. Falling back to direct model bake", loaded.modelLayerLocation().model);
            try {
                DelayLoadedModel delayModel = loaded.model().get();
                return new SpecialLatexModel(
                        delayModel.createBodyLayer(DelayLoadedModel.HUMANOID_PART_FIXER, DelayLoadedModel.HUMANOID_GROUP_FIXER).bakeRoot(),
                        loaded
                );
            } catch (Exception directFail) {
                throw new IllegalArgumentException("Failed to create SpecialLatexModel via both layer and direct bake", directFail);
            }
        }
    }

    private static ArmorModelPicker<SpecialLatex, DummyArmorModel> createNoopArmorPicker() {
        return new NoopArmorModelPicker();
    }

    private PatreonBenefits.ModelData chooseModelData(SpecialLatex entity) {
        if (entity.specialLatexForm == null) {
            return null;
        }

        PatreonBenefits.ModelData modelData = entity.specialLatexForm.modelData().get(entity.wantedState);
        if (modelData == null) {
            modelData = entity.specialLatexForm.modelData().get(entity.specialLatexForm.defaultState());
        }
        if (modelData == null) {
            modelData = entity.specialLatexForm.getDefaultModel();
        }
        return modelData;
    }

    private SpecialLatexRenderer getAndCacheFor(SpecialLatex entity) {
        UUID uuid = entity.getAssignedUUID();
        PatreonBenefits.ModelData modelData = chooseModelData(entity);
        if (uuid == null || modelData == null) {
            return null;
        }

        String stateKey = entity.wantedState != null ? entity.wantedState : entity.specialLatexForm.defaultState();
        Pair<UUID, String> key = Pair.of(uuid, stateKey);
        if (BROKEN_RENDERERS.contains(key)) {
            return null;
        }

        SpecialLatexRenderer cached = SPECIAL_RENDERERS.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            SpecialLatexRenderer created = new SpecialLatexRenderer(this.context, modelData);
            SPECIAL_RENDERERS.put(key, created);
            return created;
        } catch (IllegalArgumentException ex) {
            BROKEN_RENDERERS.add(key);
            Changed.LOGGER.debug("Failed to bake special model layer for {} state={}, fallback to default renderer", uuid, stateKey);
            return null;
        }
    }

    private boolean runIfValidConsumer(SpecialLatex entity, Consumer<SpecialLatexRenderer> rendererConsumer) {
        if (!this.isDelegate) {
            return true;
        }
        if (entity.getAssignedUUID() == null && entity.getTransfurVariant() != null) {
            ResourceLocation formId = entity.getTransfurVariant().getFormId();
            if (PatreonBenefitsFix.isSpecialFormId(formId)) {
                try {
                    entity.setSpecialForm(UUID.fromString(formId.getPath().substring(SPECIAL_FORM_PREFIX.length())));
                } catch (Exception ignored) {
                    return false;
                }
            }
        }
        if (entity.getAssignedUUID() == null) {
            return false;
        }

        PatreonBenefitsFix.SpecialForm form = PatreonBenefitsFix.getPlayerSpecialForm(entity.getAssignedUUID());
        if (form == null) {
            return false;
        }
        if (entity.specialLatexForm == null) {
            entity.specialLatexForm = form;
        }

        SpecialLatexRenderer delegated = getAndCacheFor(entity);
        if (delegated == null) {
            return true;
        }

        rendererConsumer.accept(delegated);
        return false;
    }

    private <R> Optional<R> runIfValidFunction(SpecialLatex entity, Function<SpecialLatexRenderer, R> rendererConsumer) {
        if (!this.isDelegate) {
            return Optional.empty();
        }
        if (entity.getAssignedUUID() == null && entity.getTransfurVariant() != null) {
            ResourceLocation formId = entity.getTransfurVariant().getFormId();
            if (PatreonBenefitsFix.isSpecialFormId(formId)) {
                try {
                    entity.setSpecialForm(UUID.fromString(formId.getPath().substring(SPECIAL_FORM_PREFIX.length())));
                } catch (Exception ignored) {
                    return Optional.empty();
                }
            }
        }
        if (entity.getAssignedUUID() == null) {
            return Optional.empty();
        }

        PatreonBenefitsFix.SpecialForm form = PatreonBenefitsFix.getPlayerSpecialForm(entity.getAssignedUUID());
        if (form == null) {
            return Optional.empty();
        }
        if (entity.specialLatexForm == null) {
            entity.specialLatexForm = form;
        }

        SpecialLatexRenderer delegated = getAndCacheFor(entity);
        if (delegated == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(rendererConsumer.apply(delegated));
    }

    @Override
    @Nullable
    public RenderType getRenderType(SpecialLatex entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        Optional<RenderType> opt = runIfValidFunction(entity, renderer -> renderer.getRenderType(entity, bodyVisible, translucent, glowing));
        return opt.orElseGet(() -> super.getRenderType(entity, bodyVisible, translucent, glowing));
    }

    @Override
    public void render(SpecialLatex entity, float yRot, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        if (runIfValidConsumer(entity, renderer -> renderer.render(entity, yRot, partialTicks, pose, buffer, packedLight))) {
            super.render(entity, yRot, partialTicks, pose, buffer, packedLight);
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(SpecialLatex latex) {
        PatreonBenefits.ModelData modelData = chooseModelData(latex);
        return modelData != null ? modelData.texture() : DELAYED_TEXTURE;
    }

    @Override
    public AdvancedHumanoidModel<SpecialLatex> getModel(ChangedEntity entity) {
        if (entity instanceof SpecialLatex specialLatex) {
            Optional<AdvancedHumanoidModel<SpecialLatex>> delegated = runIfValidFunction(specialLatex, renderer -> renderer.getModel());
            if (delegated.isPresent()) {
                return delegated.get();
            }

            AdvancedHumanoidModel<SpecialLatex> localModel = super.getModel(entity);
            if (localModel != null) {
                return localModel;
            }

            return SPECIAL_RENDERERS.values().stream()
                    .map(renderer -> (AdvancedHumanoidModel<SpecialLatex>) renderer.getModel())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> {
                        try {
                            PatreonBenefits.ModelData modelData = chooseModelData(specialLatex);
                            if (modelData != null) {
                                DelayLoadedModel delayModel = ensureModelIsLoaded(modelData).model().get();
                                return new SpecialLatexModel(
                                        delayModel.createBodyLayer(DelayLoadedModel.HUMANOID_PART_FIXER, DelayLoadedModel.HUMANOID_GROUP_FIXER).bakeRoot(),
                                        modelData
                                );
                            }
                        } catch (Exception ex) {
                            Changed.LOGGER.debug("Failed to create emergency SpecialLatex model fallback");
                        }
                        return DUMMY_SPECIAL_MODEL;
                    });
        }
        return super.getModel(entity);
    }

    private static class DummyArmorModel extends LatexHumanoidArmorModel<SpecialLatex, DummyArmorModel> {
        private final ModelPart nullPart = AdvancedHumanoidModel.createNullPart("dummy");
        private final HumanoidAnimator<SpecialLatex, DummyArmorModel> animator = HumanoidAnimator.of(this);

        private DummyArmorModel() {
            super(AdvancedHumanoidModel.createNullPart("dummy_root"), ArmorModel.ARMOR_OUTER);
        }

        @Override
        public void renderForSlot(SpecialLatex entity, RenderLayerParent<? super SpecialLatex, ?> parent, ItemStack stack, EquipmentSlot slot, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            // No-op fallback armor render
        }

        @Override
        public HumanoidAnimator<SpecialLatex, DummyArmorModel> getAnimator(SpecialLatex entity) {
            return animator;
        }

        @Override
        public ModelPart getArm(HumanoidArm arm) {
            return nullPart;
        }

        @Override
        public ModelPart getLeg(HumanoidArm arm) {
            return nullPart;
        }

        @Override
        public ModelPart getHead() {
            return nullPart;
        }

        @Override
        public ModelPart getTorso() {
            return nullPart;
        }
    }

    private static class NoopArmorModelPicker extends ArmorModelPicker<SpecialLatex, DummyArmorModel> {
        private final DummyArmorModel dummy = new DummyArmorModel();
        private final Map<ArmorModel, DummyArmorModel> modelSet = Map.of(ArmorModel.ARMOR_OUTER, dummy);

        @Override
        public DummyArmorModel getModelForSlot(SpecialLatex entity, EquipmentSlot slot) {
            return dummy;
        }

        @Override
        public Map<ArmorModel, ? extends DummyArmorModel> getModelSetForSlot(SpecialLatex entity, EquipmentSlot slot) {
            return modelSet;
        }

        @Override
        public void forEach(SpecialLatex entity, java.util.function.Predicate<ArmorModel> predicate, java.util.function.BiConsumer<ArmorModel, ? super DummyArmorModel> consumer) {
            modelSet.forEach((armorModel, model) -> {
                if (predicate.test(armorModel)) {
                    consumer.accept(armorModel, model);
                }
            });
        }

        @Override
        public void prepareAndSetupModels(SpecialLatex entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            // Intentionally no-op: this picker only prevents crashes when dynamic armor layers are missing.
        }
    }

    private static class DummySpecialModel extends AdvancedHumanoidModel<SpecialLatex> {
        private final ModelPart nullPart = AdvancedHumanoidModel.createNullPart("dummy_special");
        private final HumanoidAnimator<SpecialLatex, DummySpecialModel> animator = HumanoidAnimator.of(this);

        private DummySpecialModel() {
            super(AdvancedHumanoidModel.createNullPart("dummy_special_root"));
        }

        @Override
        public HumanoidAnimator<SpecialLatex, DummySpecialModel> getAnimator(SpecialLatex entity) {
            return animator;
        }

        @Override
        public ModelPart getArm(HumanoidArm arm) {
            return nullPart;
        }

        @Override
        public ModelPart getLeg(HumanoidArm arm) {
            return nullPart;
        }

        @Override
        public ModelPart getHead() {
            return nullPart;
        }

        @Override
        public ModelPart getTorso() {
            return nullPart;
        }
    }


}
