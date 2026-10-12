package github.com.gengyoubo.CE.compat.twilight;

import net.foxyas.changedaddon.init.ChangedAddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import twilightforest.init.TFStructures;
import twilightforest.util.LandmarkUtil;

/** Only castle-spawned Experiment 009 bosses receive the final encounter health. */
public final class TwilightFinalBossCompat {
    public static final String MARKER = "changede_twilight_final_boss";
    public static final String HOME = "changede_twilight_final_home";
    public static final float HEALTH = 2800;
    private TwilightFinalBossCompat() {}
    public static void initialize(net.minecraftforge.eventbus.api.IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener(TwilightFinalBossCompat::joined);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, TwilightFinalBossCompat::died);
        MinecraftForge.EVENT_BUS.addListener(TwilightCastleUpgrade::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, TwilightFinalBossRewards::prepare);
        MinecraftForge.EVENT_BUS.addListener(TwilightFinalBossRewards::removed);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TwilightFinalEncounter::attacked);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, TwilightFinalEncounter::died);
        MinecraftForge.EVENT_BUS.addListener(TwilightFinalEncounter::removed);
        MinecraftForge.EVENT_BUS.addListener(TwilightFinalEncounter::drops);
        MinecraftForge.EVENT_BUS.addListener(TwilightFinalEncounter::experience);
        bus.addListener((net.minecraftforge.event.AddPackFindersEvent event) -> {
            if (event.getPackType() != net.minecraft.server.packs.PackType.SERVER_DATA) return;
            var path = net.minecraftforge.fml.ModList.get().getModFileById("changede").getFile().findResource("resourcepacks", "twilight_final");
            var pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate("changede:twilight_final",
                    net.minecraft.network.chat.Component.literal("Changed Extra Final Castle"), true,
                    id -> new net.minecraftforge.resource.PathPackResources(id, true, path), net.minecraft.server.packs.PackType.SERVER_DATA,
                    net.minecraft.server.packs.repository.Pack.Position.BOTTOM, net.minecraft.server.packs.repository.PackSource.BUILT_IN);
            if (pack != null) event.addRepositorySource(output -> output.accept(pack));
        });
    }
    public static boolean spawn(ServerLevelAccessor world, BlockPos pos) {
        var boss = ChangedAddonEntities.EXPERIMENT_009_BOSS.get().create(world.getLevel());
        if (boss == null) return false;
        boss.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, world.getRandom().nextFloat() * 360, 0);
        ForgeEventFactory.onFinalizeSpawn(boss, world, world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null, null);
        boss.getPersistentData().putBoolean(MARKER, true);
        boss.getPersistentData().putLong(HOME, pos.asLong());
        boss.getPersistentData().putInt(TwilightFinalEncounter.STAGE, 0);
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
        boss.setHealth(HEALTH);
        boss.setPersistenceRequired();
        boss.restrictTo(pos, 32);
        TwilightFinalEncounter.initialize(boss);
        return world.addFreshEntity(boss);
    }
    private static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof net.foxyas.changedaddon.entity.bosses.Experiment009BossEntity boss)
                || !boss.getPersistentData().getBoolean(MARKER)) return;
        // NBT already preserves current health. Never heal the boss when its chunk reloads.
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
        boss.restrictTo(BlockPos.of(boss.getPersistentData().getLong(HOME)), 32);
        TwilightFinalEncounter.initialize(boss);
    }
    private static void died(LivingDeathEvent event) {
        var boss = event.getEntity();
        if (!(boss.level() instanceof ServerLevel level) || event.isCanceled() || !boss.getPersistentData().getBoolean(MARKER)) return;
        BlockPos home = BlockPos.of(boss.getPersistentData().getLong(HOME));
        // Conquest belongs to the encounter's castle even if the boss was lured away.
        level.getChunkAt(home);
        LandmarkUtil.markStructureConquered(level, GlobalPos.of(level.dimension(), home), TFStructures.FINAL_CASTLE, true);
        if (event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            var advancement = level.getServer().getAdvancements().getAdvancement(net.minecraft.resources.ResourceLocation.parse("changede:twilight_final_boss"));
            if (advancement != null) player.getAdvancements().award(advancement, "defeated");
        }
    }
}
