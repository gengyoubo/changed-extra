package github.com.gengyoubo.CE.skill;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Only an active opposite-latex diet permits eating raw goo; other material items remain materials. */
@Mod.EventBusSubscriber(modid = "changede")
public final class SkillGooDiet {
    private static final ResourceLocation DARK = ResourceLocation.parse("changed:dark_latex_goo");
    private static final ResourceLocation WHITE = ResourceLocation.parse("changed:white_latex_goo");
    private SkillGooDiet() { }
    private static SkillDiets.Diet diet(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return DARK.equals(id) ? SkillDiets.Diet.DARK_LATEX : WHITE.equals(id) ? SkillDiets.Diet.WHITE_LATEX : null;
    }
    public static boolean isGoo(ItemStack stack) { return !stack.isEmpty() && diet(stack) != null; }
    public static boolean canEat(Player player, ItemStack stack) {
        var diet = diet(stack);
        return !stack.isEmpty() && diet != null && player.isAlive() && !player.isSpectator()
                && SkillMechanics.value(player, diet.effect()) > 0
                && (player.level().isClientSide || SkillDiets.matches(diet, stack));
    }
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!canEat(player, event.getItemStack())) return;
        event.setCanceled(true);
        if (!player.canEat(false)) { event.setCancellationResult(InteractionResult.FAIL); return; }
        player.startUsingItem(event.getHand());
        event.setCancellationResult(InteractionResult.CONSUME);
    }
    public static void consume(ServerPlayer player, ItemStack stack) {
        if (!canEat(player, stack) || !player.canEat(false)) return;
        if (player.getFoodData() instanceof SkillFoodData data) data.changede$setOwner(player);
        var meal = SkillNutrition.dietMeal(2, 0.3F);
        player.getFoodData().eat(meal.nutrition(), meal.saturationModifier());
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
        player.gameEvent(GameEvent.EAT);
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }
}
