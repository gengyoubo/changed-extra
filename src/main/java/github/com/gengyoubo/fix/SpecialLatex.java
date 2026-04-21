package github.com.gengyoubo.fix;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.HairStyle;
import net.ltxprogrammer.changed.entity.TransfurMode;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.util.Color3;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpecialLatex extends ChangedEntity {
    public String wantedState = "default";
    public List<String> possibleModels = new ArrayList<>();
    public PatreonBenefitsFix.SpecialForm specialLatexForm = null;
    private UUID assignedUUID = null;

    public SpecialLatex(EntityType<? extends ChangedEntity> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    public static void init() {}

    public PatreonBenefits.EntityData getCurrentData() {
        if (specialLatexForm == null)
            return null;
        return specialLatexForm.entityData().getOrDefault(wantedState, specialLatexForm.getDefaultEntity());
    }

    public void setSpecialForm(UUID uuid) {
        this.assignedUUID = uuid;
        this.specialLatexForm = PatreonBenefitsFix.getPlayerSpecialForm(uuid);
        if (this.specialLatexForm != null) {
            this.wantedState = this.specialLatexForm.defaultState();
            this.possibleModels.clear();
            this.possibleModels.addAll(this.specialLatexForm.modelData().keySet());
            Changed.LOGGER.info(
                    "SpecialLatex bound form uuid={} defaultState={} modelStates={}",
                    uuid,
                    this.wantedState,
                    this.possibleModels
            );
        } else {
            Changed.LOGGER.warn("SpecialLatex failed to bind form for uuid={}", uuid);
        }
    }

    public EntityDimensions getDimensions(Pose pose) {
        if (specialLatexForm == null)
            return super.getDimensions(pose);

        EntityDimensions core = getCurrentData().dimensions();

        if (this.isVisuallySwimming())
            return EntityDimensions.scalable(core.width, core.width);
        return switch (pose) {
            case STANDING -> core;
            case SLEEPING -> SLEEPING_DIMENSIONS;
            case FALL_FLYING, SWIMMING, SPIN_ATTACK -> EntityDimensions.scalable(core.width, core.width);
            case CROUCHING -> EntityDimensions.scalable(core.width, core.height - 0.3f);
            case DYING -> EntityDimensions.fixed(0.2f, 0.2f);
            default -> core;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public Color3 getHairColor(int layer) {
        if (specialLatexForm == null)
            return Color3.WHITE;
        try {
            return getCurrentData().hairColors().get(layer);
        } catch (Exception ignored) {
            if (!getCurrentData().hairColors().isEmpty())
                return getCurrentData().hairColors().get(0);
            else
                return Color3.WHITE;
        }
    }

    public @Nullable List<HairStyle> getValidHairStyles() {
        if (specialLatexForm == null)
            return HairStyle.Collection.EMPTY;
        return getCurrentData().hairStyles();
    }

    public UUID getAssignedUUID() {
        return assignedUUID;
    }

    @Override
    public LatexType getLatexType() {
        if (specialLatexForm == null)
            return ChangedLatexTypes.NONE.get();
        return specialLatexForm.variant().type;
    }

    @Override
    public TransfurMode getTransfurMode() {
        if (specialLatexForm == null)
            return TransfurMode.REPLICATION;
        return specialLatexForm.variant().transfurMode();
    }

    @Override
    @SuppressWarnings("deprecation")
    public TransfurVariant<?> getTransfurVariant() {
        if (specialLatexForm == null)
            return super.getTransfurVariant();
        return specialLatexForm.variant();
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Event {
        @SubscribeEvent
        public static void onVariantAssigned(ProcessTransfur.EntityVariantAssigned event) {
            if (event.variant == null || event.variant.getEntityType() != ChangedEntitiesFix.SPECIAL_LATEX.get())
                return;

            if (event.livingEntity instanceof Player player) {
                TransfurVariant<?> specialVariant = PatreonBenefitsFix.getPlayerSpecialVariant(player.getUUID());
                if (specialVariant != null) {
                    event.variant = specialVariant;
                } else {
                    Changed.LOGGER.warn("No special variant data for player {}. Keeping assigned variant {}.", player.getUUID(), event.variant.getFormId());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Player player = event.player;
            ProcessTransfur.getPlayerTransfurVariantSafe(player).ifPresent(instance -> {
                if (instance.getChangedEntity() instanceof SpecialLatex specialLatex && specialLatex.specialLatexForm == null) {
                    specialLatex.setSpecialForm(player.getUUID());
                    if (specialLatex.specialLatexForm == null) {
                        Changed.LOGGER.warn("SpecialLatex has no form data for player {}", player.getUUID());
                    }
                }
            });
        }
    }
}
