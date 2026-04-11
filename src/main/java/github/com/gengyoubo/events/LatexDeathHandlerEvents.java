package github.com.gengyoubo.events;

import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class LatexDeathHandlerEvents {
    // 标记哪些玩家需要“死亡后变胶兽”
    private static final Set<UUID> SHOULD_TRANSFUR = new HashSet<>();

    // ① 死亡时判断
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (latexStartEvents.isLatexStart(player.level())) return;
        var rules = player.level().getGameRules();
        if (rules.getBoolean(ChangedGameRules.RULE_KEEP_FORM)) return;
        if (!ProcessTransfur.isPlayerTransfurred(player)) {
            SHOULD_TRANSFUR.add(player.getUUID());
        }
    }
    //死亡时为人类，将会退化至胶兽
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();
        // ✔ 复制variant
        if (oldData.contains("latex_start_variant")) {
            newData.putString("latex_start_variant",
                    oldData.getString("latex_start_variant"));
        }
        // ✔ 标记需要transfur（你的 SHOULD_TRANSFUR）
        if (SHOULD_TRANSFUR.contains(event.getOriginal().getUUID())) {
            SHOULD_TRANSFUR.add(event.getEntity().getUUID());
        }
    }
}
