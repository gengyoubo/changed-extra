package github.com.gengyoubo.CE.events;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class AdvancementChainEvents {
    private static final ChainStep[] ITEM_CHAIN = {
            new ChainStep("what_can_this_do", "burned_to_gray", "changede:latex_gray"),
            new ChainStep("burned_to_gray", "first_signs", "changede:unbaked_latex_ingot"),
            new ChainStep("first_signs", "hot_latex_ingot", "changede:latex_ingot"),
            new ChainStep("hot_latex_ingot", "empty_head", "changede:space_tower"),
            new ChainStep("empty_head", "wonderful_art", "changede:latex_painting_portal")
    };

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }

        for (ChainStep step : ITEM_CHAIN) {
            if (!isAdvancementDone(player, step.parentId()) || isAdvancementDone(player, step.advancementId())) {
                continue;
            }

            Item item = ForgeRegistries.ITEMS.getValue(step.itemId());
            if (item != null && player.getInventory().contains(new ItemStack(item))) {
                awardAdvancement(player, step.advancementId());
            }
        }
    }

    public static boolean isAdvancementDone(ServerPlayer player, ResourceLocation advancementId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static void awardAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private record ChainStep(ResourceLocation parentId, ResourceLocation advancementId, ResourceLocation itemId) {
        private ChainStep(String parentPath, String advancementPath, String itemId) {
            this(
                    ResourceLocation.fromNamespaceAndPath("changede", parentPath),
                    ResourceLocation.fromNamespaceAndPath("changede", advancementPath),
                    ResourceLocation.parse(itemId)
            );
        }
    }
}
