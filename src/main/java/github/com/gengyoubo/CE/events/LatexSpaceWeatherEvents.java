package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.player.Perseverance;
import github.com.gengyoubo.CE.weather.LatexSpaceWeather;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.ai.ImmediateTransfurDecision;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.extension.ChangedCompatibility;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LatexSpaceWeatherEvents {
    private static final double RAIN_CHANCE_PER_SECOND = 0.01;
    private static final double THUNDER_CHANCE_PER_SECOND = 0.03;

    private LatexSpaceWeatherEvents() { }

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
        if (!level.isRaining() || !level.canSeeSky(player.blockPosition())
                || level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, player.blockPosition()).getY() > player.getY()) return;
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
    }
}
