package github.com.gengyoubo.events;

import github.com.gengyoubo.CERegister;
import github.com.gengyoubo.changede;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.init.ChangedRegistry;
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

        CompoundTag data = player.getPersistentData();
        if (!data.contains("latex_start_variant") && ProcessTransfur.isPlayerTransfurred(player)) {
            ProcessTransfur.getPlayerTransfurVariantSafe(player)
                    .ifPresent(v -> data.putString("latex_start_variant", v.getFormId().toString()));
        }
        if (ProcessTransfur.isPlayerTransfurred(player)) return;

        TransfurVariant<?> variant;
        if (data.contains("latex_start_variant")) {
            ResourceLocation id = ResourceLocation.parse(data.getString("latex_start_variant"));
            variant = ChangedRegistry.TRANSFUR_VARIANT.get().getValue(id);
        } else {
            variant = latexStartEvents.getRandomForm(player.getRandom());
        }

        assert variant != null;
        player.getPersistentData().putString("latex_start_variant", variant.getFormId().toString());
        ProcessTransfur.setPlayerTransfurVariant(player, variant, (TransfurContext) null);
        changede.LOGGER.debug("已分配玩家变体: {}", variant.getFormId());
    }
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        newData.putString("latex_start_variant",
                oldData.getString("latex_start_variant"));
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
                // ✔ 只要 form_
                if (!path.startsWith("form_")) continue;
                // ❌ 黑名单过滤
                if (BLACKLIST.contains(path)) {
                    changede.LOGGER.debug("黑名单跳过: {}", id);
                    continue;
                }
                FORM_VARIANTS.add(variant);
                changede.LOGGER.debug("加入: {}", id);
            }
            changede.LOGGER.debug("最终数量: {}", FORM_VARIANTS.size());
        });
    }
    public static TransfurVariant<?> getRandomForm(RandomSource random) {
        if (FORM_VARIANTS.isEmpty()) return null;
        return FORM_VARIANTS.get(random.nextInt(FORM_VARIANTS.size()));
    }
    //黑名单
    public static final Set<String> BLACKLIST = Set.of(
            "form_special"
    );
}

