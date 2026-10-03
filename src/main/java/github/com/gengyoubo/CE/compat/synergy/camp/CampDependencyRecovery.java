package github.com.gengyoubo.CE.compat.synergy.camp;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Common-only recovery path: this class and the save model never reference CS classes or registries. */
public final class CampDependencyRecovery {
    private static final String FLAGS = "changede_camp_expedition_flags";
    public static void initialize() { MinecraftForge.EVENT_BUS.register(CampDependencyRecovery.class); }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        LatexSettlementData data = LatexSettlementData.get(event.getServer());
        for (var camp : data.settlements.values()) {
            camp.active = false; camp.raid = LatexSettlementData.Raid.COOLDOWN; camp.raiders.clear(); camp.visitor = null;
            for (var resident : camp.residents.values()) if (resident.legacyExpedition != null) resident.legacyExpedition.putBoolean("Canceled", true);
        }
        if (!data.settlements.isEmpty()) data.setDirty();
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity mob) || !(event.getLevel() instanceof ServerLevel)) return;
        if (mob.getPersistentData().hasUUID("changede_camp_visitor") || mob.getPersistentData().hasUUID("changede_camp_raider")) {
            event.setCanceled(true); return;
        }
        restore(mob);
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ChangedEntity mob && mob.level() instanceof ServerLevel) restore(mob);
    }
    private static void restore(ChangedEntity mob) {
        CampWorkBuffer.restoreTool(mob, null); CampWorkBuffer.cancelKitchen(mob);
        for (var stack : CampWorkBuffer.takeCargo(mob)) if (mob.spawnAtLocation(stack) == null) CampWorkBuffer.addCargo(mob, java.util.List.of(stack));
        if (!mob.getPersistentData().contains(FLAGS, Tag.TAG_COMPOUND)) return;
        CompoundTag flags = mob.getPersistentData().getCompound(FLAGS);
        mob.setNoAi(flags.getBoolean("NoAI")); mob.setInvisible(flags.getBoolean("Invisible"));
        mob.setInvulnerable(flags.getBoolean("Invulnerable")); mob.setNoGravity(flags.getBoolean("NoGravity"));
        mob.getPersistentData().remove(FLAGS);
    }
}
