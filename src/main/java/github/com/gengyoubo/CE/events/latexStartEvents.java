package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.changede;
import github.com.gengyoubo.CE.fix.SpecialLatexFix.PatreonBenefitsFix;
import github.com.gengyoubo.CE.init.CEGameRules;
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
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class latexStartEvents {
    public static final List<TransfurVariant<?>> FORM_VARIANTS = new ArrayList<>();
    private static final String TAG_VARIANT = "latex_start_variant";
    private static final String TAG_HUMAN_LOCK = "latex_start_human_lock";

    public static boolean isLatexStart(Level level) {
        return !level.getGameRules().getBoolean(CEGameRules.LATEX_START);
    }

    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (latexStartEvents.isLatexStart(player.level())) return;

        var rules = player.level().getGameRules();
        rules.getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(true, player.server);
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG_VARIANT) && ProcessTransfur.isPlayerTransfurred(player)) {
            ProcessTransfur.getPlayerTransfurVariantSafe(player)
                    .ifPresent(v -> data.putString(TAG_VARIANT, v.getFormId().toString()));
        }

        if (ProcessTransfur.isPlayerTransfurred(player)) {
            changede.LOGGER.info(
                    "[LatexStart] join-skip already transfurred player={} variantTag={} humanLock={}",
                    player.getGameProfile().getName(),
                    data.contains(TAG_VARIANT) ? data.getString(TAG_VARIANT) : "<none>",
                    data.getBoolean(TAG_HUMAN_LOCK)
            );
            data.remove(TAG_HUMAN_LOCK);
            return;
        }

        // Human lock exists => keep human form, skip forced latex restore.
        // This lock is set when player explicitly untf, so command intent is respected across relog.
        if (data.getBoolean(TAG_HUMAN_LOCK)) {
            // Explicit untf state: also clear remembered variant so later flow won't reuse stale form.
            data.remove(TAG_VARIANT);
            changede.LOGGER.info(
                    "[LatexStart] join-skip human-lock player={} variantTag={}",
                    player.getGameProfile().getName(),
                    data.contains(TAG_VARIANT) ? data.getString(TAG_VARIANT) : "<none>"
            );
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

        boolean hadVariantTag = data.contains(TAG_VARIANT);
        boolean hadHumanLock = data.getBoolean(TAG_HUMAN_LOCK);
        data.putString(TAG_VARIANT, variant.getFormId().toString());
        data.remove(TAG_HUMAN_LOCK);
        changede.LOGGER.warn(
                "[LatexStart] forcing transfur on join player={} variant={} hadVariantTag={} humanLock={}",
                player.getGameProfile().getName(),
                variant.getFormId(),
                hadVariantTag,
                hadHumanLock
        );
        ProcessTransfur.setPlayerTransfurVariant(player, variant, (TransfurContext) null);
        changede.LOGGER.debug("Assigned player variant: {}", variant.getFormId());
    }

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
        event.enqueueWork(() -> {
            var registry = ChangedRegistry.TRANSFUR_VARIANT.get();
            FORM_VARIANTS.clear();
            int blacklistCount = 0;
            for (TransfurVariant<?> variant : registry.getValues()) {
                var id = variant.getFormId();
                if (id == null) continue;
                String path = id.getPath();
                if (!path.startsWith("form_")) continue;
                if (BLACKLIST.contains(path)) {
                    blacklistCount++;
                    continue;
                }
                FORM_VARIANTS.add(variant);
            }
            changede.LOGGER.info("Latex start variants prepared: usable={}, blacklisted={}", FORM_VARIANTS.size(), blacklistCount);
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
