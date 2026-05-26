package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.LP.item.MimicItem;
import github.com.gengyoubo.CE.player.Perseverance;
import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LatexDeathHandlerEvents {
    private static final Set<UUID> SHOULD_TRANSFUR = new HashSet<>();

    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        MimicItem.resetDeathState(player);
        if (event.getSource().is(ChangedTags.DamageTypes.IS_TRANSFUR) && Perseverance.rollKeepForm(player)) {
            event.setCanceled(true);
            player.setHealth(Math.max(1.0F, player.getHealth()));
            player.invulnerableTime = Math.max(player.invulnerableTime, 20);
            return;
        }

        if (latexStartEvents.isLatexStart(player.level())) return;
        var rules = player.level().getGameRules();
        if (rules.getBoolean(ChangedGameRules.RULE_KEEP_FORM)) return;
        if (!ProcessTransfur.isPlayerTransfurred(player)) {
            SHOULD_TRANSFUR.add(player.getUUID());
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();
        MimicItem.resetDeathState(event.getEntity());

        if (oldData.contains("latex_start_variant")) {
            newData.putString("latex_start_variant", oldData.getString("latex_start_variant"));
        }
        if (SHOULD_TRANSFUR.contains(event.getOriginal().getUUID())) {
            SHOULD_TRANSFUR.add(event.getEntity().getUUID());
        }
    }
}
