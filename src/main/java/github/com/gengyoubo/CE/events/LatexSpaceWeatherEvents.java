package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.player.Perseverance;
import github.com.gengyoubo.CE.weather.LatexSpaceWeather;
import github.com.gengyoubo.CE.weather.WhiteFogExposure;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
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
    private static final double RAIN_MIN_CHANCE_PER_SECOND = 0.05;
    private static final double RAIN_MAX_CHANCE_PER_SECOND = 0.50;
    private static final double THUNDER_MIN_CHANCE_PER_SECOND = 0.15;
    private static final double THUNDER_MAX_CHANCE_PER_SECOND = 0.80;
    private static final double HEALTH_CHANCE_EXPONENT = 3.0;
    private static final float RAIN_DAMAGE_PER_SECOND = 1.0F;
    private static final float THUNDER_DAMAGE_PER_SECOND = 2.0F;
    private static final ResourceKey<DamageType> WEATHER_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("changede", "latex_weather"));
    private static final ResourceLocation WEATHER_TRANSFUR = ResourceLocation.fromNamespaceAndPath("changede", "weather_transfur");
    private static final ResourceLocation WEATHER_DEATH = ResourceLocation.fromNamespaceAndPath("changede", "weather_death");
    private static final TagKey<EntityType<?>> WHITE_FORMS = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("changede", "white_latex_weather_immune"));

    private LatexSpaceWeatherEvents() { }

    private static boolean exposedToWeather(LivingEntity entity) {
        var level = entity.level();
        if (!level.dimension().location().equals(LatexSpaceWeather.DIMENSION) || !level.isRaining()) return false;
        var biome = level.getBiome(entity.blockPosition());
        if (LatexSpaceWeather.isWhite(biome)) {
            return WhiteFogExposure.isExposed(level, BlockPos.containing(entity.getEyePosition()));
        }
        return LatexSpaceWeather.isDark(biome) && level.canSeeSky(entity.blockPosition())
                && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, entity.blockPosition()).getY() <= entity.getY();
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.isAlive() || entity.tickCount % 20 != 0) return;
        LatexType type;
        EntityType<?> formType;
        if (entity instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator() || ChangedCompatibility.isPlayerUsedByOtherMod(player)) return;
            var variant = ProcessTransfur.getPlayerTransfurVariant(player);
            type = variant == null ? ChangedLatexTypes.NONE.get() : variant.getLatexType();
            formType = variant == null ? player.getType() : variant.getChangedEntity().getType();
        } else if (entity instanceof ChangedEntity latex) {
            // Player forms are already handled through their host player.
            if (latex.getUnderlyingPlayer() != null) return;
            type = latex.getLatexType();
            formType = latex.getType();
        } else {
            return;
        }
        if (!exposedToWeather(entity)) return;
        var biome = entity.level().getBiome(entity.blockPosition());
        boolean incompatible = (LatexSpaceWeather.isDark(biome) && type != ChangedLatexTypes.DARK_LATEX.get())
                || (LatexSpaceWeather.isWhite(biome) && type != ChangedLatexTypes.WHITE_LATEX.get()
                    && !formType.is(WHITE_FORMS));
        if (incompatible) {
            DamageSource source = new DamageSource(entity.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(LatexSpaceWeather.isWhite(biome)
                            ? LatexSpaceWeather.WHITE_FOG_DAMAGE : WEATHER_DAMAGE));
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
        double baseChance = getWeatherBaseChance(player, level.isThundering());
        double chance = baseChance * level.getRainLevel(1) * (1 - Perseverance.getKeepFormChance(player));
        if (player.getRandom().nextDouble() < chance) transfur(player, dark);
    }

    private static double getWeatherBaseChance(ServerPlayer player, boolean thundering) {
        double maxHealth = player.getMaxHealth();
        double healthRatio = maxHealth > 0 ? player.getHealth() / maxHealth : 0;
        double missingHealthRatio = 1 - Math.max(0, Math.min(1, healthRatio));
        double minChance = thundering ? THUNDER_MIN_CHANCE_PER_SECOND : RAIN_MIN_CHANCE_PER_SECOND;
        double maxChance = thundering ? THUNDER_MAX_CHANCE_PER_SECOND : RAIN_MAX_CHANCE_PER_SECOND;
        // Use the player's actual maximum health, including attributes from other mods.
        // Normalize the exponential curve so full health is exactly the minimum,
        // and zero health approaches exactly the maximum.
        double growth = Math.expm1(HEALTH_CHANCE_EXPONENT * missingHealthRatio)
                / Math.expm1(HEALTH_CHANCE_EXPONENT);
        return minChance + (maxChance - minChance) * growth;
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
