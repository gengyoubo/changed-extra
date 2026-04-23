package github.com.gengyoubo.events;

import github.com.gengyoubo.CERegister;
import github.com.gengyoubo.changede;
import github.com.gengyoubo.fix.SpecialLatex.PatreonBenefitsFix;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber
public class latexStartEvents {
    public static final List<TransfurVariant<?>> FORM_VARIANTS = new ArrayList<>();
    private static final String TAG_VARIANT = "latex_start_variant";
    private static final String TAG_HUMAN_LOCK = "latex_start_human_lock";

    public static boolean isLatexStart(Level level) {
        return !level.getGameRules().getBoolean(CERegister.LATEX_START);
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (latexStartEvents.isLatexStart(player.level())) return;

        var rules = player.level().getGameRules();
        rules.getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(true, player.server);
        boolean keepForm = rules.getBoolean(ChangedGameRules.RULE_KEEP_FORM);

        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG_VARIANT) && ProcessTransfur.isPlayerTransfurred(player)) {
            ProcessTransfur.getPlayerTransfurVariantSafe(player)
                    .ifPresent(v -> data.putString(TAG_VARIANT, v.getFormId().toString()));
        }

        if (ProcessTransfur.isPlayerTransfurred(player)) {
            data.remove(TAG_HUMAN_LOCK);
            return;
        }

        // keepForm is on + human lock exists => keep human form, skip forced latex restore
        if (keepForm && data.getBoolean(TAG_HUMAN_LOCK)) {
            changede.LOGGER.debug("keepForm enabled; skip latex restore for {}", player.getGameProfile().getName());
            return;
        }

        TransfurVariant<?> variant;
        if (data.contains(TAG_VARIANT)) {
            ResourceLocation id = ResourceLocation.parse(data.getString(TAG_VARIANT));
            variant = PatreonBenefitsFix.resolveVariant(id);
            if (variant == null) {
                changede.LOGGER.warn("Saved latex_start_variant {} is unavailable, selecting fallback.", id);
                variant = latexStartEvents.getRandomForm(player.getRandom());
            }
        } else {
            variant = latexStartEvents.getRandomForm(player.getRandom());
        }
        if (variant == null) {
            variant = ChangedTransfurVariants.FALLBACK_VARIANT.get();
        }

        data.putString(TAG_VARIANT, variant.getFormId().toString());
        data.remove(TAG_HUMAN_LOCK);
        ProcessTransfur.setPlayerTransfurVariant(player, variant, (TransfurContext) null);
        changede.LOGGER.debug("Assigned player variant: {}", variant.getFormId());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        boolean keepForm = event.getEntity().level().getGameRules().getBoolean(ChangedGameRules.RULE_KEEP_FORM);
        boolean wasHuman = !ProcessTransfur.isPlayerTransfurred(event.getOriginal());

        // keepForm is on and player died as human -> preserve human state after respawn.
        if (keepForm && wasHuman) {
            newData.putBoolean(TAG_HUMAN_LOCK, true);
            return;
        }

        newData.remove(TAG_HUMAN_LOCK);
        if (oldData.contains(TAG_VARIANT)) {
            newData.putString(TAG_VARIANT, oldData.getString(TAG_VARIANT));
        }
    }

    public static void setup(final FMLCommonSetupEvent event) {
        changede.LOGGER.debug("start program!");
        event.enqueueWork(() -> {
            var registry = ChangedRegistry.TRANSFUR_VARIANT.get();
            FORM_VARIANTS.clear();
            for (TransfurVariant<?> variant : registry.getValues()) {
                var id = variant.getFormId();
                if (id == null) continue;
                String path = id.getPath();
                if (!path.startsWith("form_")) continue;
                if (BLACKLIST.contains(path)) {
                    changede.LOGGER.debug("Blacklist skip: {}", id);
                    continue;
                }
                FORM_VARIANTS.add(variant);
                changede.LOGGER.debug("Added variant: {}", id);
            }
            changede.LOGGER.debug("Final variant count: {}", FORM_VARIANTS.size());
        });
    }

    public static TransfurVariant<?> getRandomForm(RandomSource random) {
        if (FORM_VARIANTS.isEmpty()) return null;
        return FORM_VARIANTS.get(random.nextInt(FORM_VARIANTS.size()));
    }

    // Blacklist (id path only)
    public static final Set<String> BLACKLIST = Set.of(
            "form_special"
    );
}
