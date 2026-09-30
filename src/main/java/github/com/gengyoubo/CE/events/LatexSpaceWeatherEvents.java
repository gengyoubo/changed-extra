package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.player.Perseverance;
import github.com.gengyoubo.CE.weather.LatexSpaceWeather;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.ai.ImmediateTransfurDecision;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.extension.ChangedCompatibility;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LatexSpaceWeatherEvents {
    private static final double RAIN_CHANCE_PER_SECOND = 0.01;
    private static final double THUNDER_CHANCE_PER_SECOND = 0.03;
    private static final float RAIN_DAMAGE_PER_SECOND = 1.0F;
    private static final float THUNDER_DAMAGE_PER_SECOND = 2.0F;
    private static final ResourceKey<DamageType> WEATHER_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("changede", "latex_weather"));
    private static final ResourceLocation WEATHER_TRANSFUR = ResourceLocation.fromNamespaceAndPath("changede", "weather_transfur");
    private static final ResourceLocation WEATHER_DEATH = ResourceLocation.fromNamespaceAndPath("changede", "weather_death");

    private LatexSpaceWeatherEvents() { }

    private static boolean exposedToWeather(LivingEntity entity) {
        var level = entity.level();
        return level.dimension().location().equals(LatexSpaceWeather.DIMENSION)
                && level.isRaining() && level.canSeeSky(entity.blockPosition())
                && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, entity.blockPosition()).getY() <= entity.getY();
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.isAlive() || entity.tickCount % 20 != 0) return;
        LatexType type;
        if (entity instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator() || ChangedCompatibility.isPlayerUsedByOtherMod(player)) return;
            var variant = ProcessTransfur.getPlayerTransfurVariant(player);
            type = variant == null ? ChangedLatexTypes.NONE.get() : variant.getLatexType();
        } else if (entity instanceof ChangedEntity latex) {
            // Player forms are already handled through their host player.
            if (latex.getUnderlyingPlayer() != null) return;
            type = latex.getLatexType();
        } else {
            return;
        }
        if (!exposedToWeather(entity)) return;
        var biome = entity.level().getBiome(entity.blockPosition());
        boolean incompatible = (LatexSpaceWeather.isDark(biome) && type != ChangedLatexTypes.DARK_LATEX.get())
                || (LatexSpaceWeather.isWhite(biome) && type != ChangedLatexTypes.WHITE_LATEX.get());
        if (incompatible) {
            DamageSource source = new DamageSource(entity.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(WEATHER_DAMAGE));
            entity.hurt(source, entity.level().isThundering()
                    ? THUNDER_DAMAGE_PER_SECOND : RAIN_DAMAGE_PER_SECOND);
            if (entity instanceof ServerPlayer player && !player.isAlive()) {
                AdvancementChainEvents.awardAdvancement(player, WEATHER_DEATH);
            }
        }
    }

    private static boolean susceptible(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && player.level().dimension().location().equals(LatexSpaceWeather.DIMENSION)
                && !ProcessTransfur.isPlayerTransfurred(player)
                && !ChangedCompatibility.isPlayerUsedByOtherMod(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0 || !susceptible(player)) return;
        var level = player.serverLevel();
        if (!exposedToWeather(player)) return;
        var biome = level.getBiome(player.blockPosition());
        boolean dark = LatexSpaceWeather.isDark(biome);
        if (!dark && !LatexSpaceWeather.isWhite(biome)) return;
        double baseChance = level.isThundering() ? THUNDER_CHANCE_PER_SECOND : RAIN_CHANCE_PER_SECOND;
        double chance = baseChance * level.getRainLevel(1) * (1 - Perseverance.getKeepFormChance(player));
        if (player.getRandom().nextDouble() < chance) transfur(player, dark);
    }

    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !susceptible(player)
                || !player.serverLevel().isThundering()
                || !LatexSpaceWeather.isDarkForest(player.level().getBiome(player.blockPosition()))) return;
        transfur(player, true);
    }

    private static void transfur(ServerPlayer player, boolean dark) {
        // Explicit ordinary wolf pool excludes all boss and special forms.
        boolean female = player.getRandom().nextBoolean();
        TransfurVariant<?> variant = dark
                ? (female ? ChangedTransfurVariants.DARK_LATEX_WOLF_FEMALE.get() : ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.get())
                : (female ? ChangedTransfurVariants.WHITE_LATEX_WOLF_FEMALE.get() : ChangedTransfurVariants.WHITE_LATEX_WOLF_MALE.get());
        ProcessTransfur.transfur(player, ImmediateTransfurDecision.safe(variant, TransfurCause.CEILING_HAZARD));
        if (ProcessTransfur.isPlayerTransfurred(player)) {
            AdvancementChainEvents.awardAdvancement(player, WEATHER_TRANSFUR);
        }
    }
}
