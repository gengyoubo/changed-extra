package github.com.gengyoubo.CE.compat.synergy;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.parkabird.changedsynergy.ai.CreatureMorphContinuity;

/** Repairs bonded-player continuity when a Changed creature is replaced by a new species. */
public final class ChangedSynergyMorphCompat {
    private static final String SOCIAL_TAG = "ChangedSynergySocial";
    private static final String PERSONALITY_TAG = "ChangedSynergyPersonality";

    private ChangedSynergyMorphCompat() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                TransfurEvents.ChangedEntityFusionWithMobEvent.class,
                event -> repairBond(event.getSourceEntity(), event.getFusionEntity().getEntity()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                TransfurEvents.ChangedEntityFusionWithChangedEntityEvent.class,
                event -> repairBond(event.getSourceEntity(), event.getFusionEntity().getEntity()));
    }

    private static void repairBond(LivingEntity source, LivingEntity outcome) {
        if (!(source instanceof ChangedEntity previous) || !(outcome instanceof ChangedEntity replacement)
                || previous == replacement || replacement.level().isClientSide()) return;

        if (hasBond(previous.getPersistentData()) && !hasBond(replacement.getPersistentData())) {
            CreatureMorphContinuity.transferForced(previous, replacement);
        }
    }

    private static boolean hasBond(CompoundTag data) {
        CompoundTag social = data.getCompound(SOCIAL_TAG);
        if (social.hasUUID("PetOwner") || !social.getCompound("BondedPlayers").isEmpty()) return true;
        CompoundTag memories = data.getCompound(PERSONALITY_TAG).getCompound("PlayerMemories");
        for (String playerId : memories.getAllKeys()) {
            if (memories.getCompound(playerId).getBoolean("Relationship")) return true;
        }
        return false;
    }
}
