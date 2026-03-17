package github.com.gengyoubo.events;

import java.util.Optional;

import github.com.gengyoubo.CERegister;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede")
public class ScorchingHeatEvents {
@SubscribeEvent
public static void onBreak(BlockEvent.BreakEvent event) {

    Player player = event.getPlayer();
    Level level = (Level) event.getLevel();
    ItemStack tool = player.getMainHandItem();

    int ench = EnchantmentHelper.getTagEnchantmentLevel(
            CERegister.SCORCHINGHEAT.get(), tool);

    if (ench <= 0) return;

    // 与精准采集冲突
    if (EnchantmentHelper.getTagEnchantmentLevel(
            Enchantments.SILK_TOUCH, tool) > 0)
        return;

    BlockState state = level.getBlockState(event.getPos());

    ItemStack input = new ItemStack(state.getBlock().asItem());

    Optional<SmeltingRecipe> recipe =
            level.getRecipeManager().getRecipeFor(
                    RecipeType.SMELTING,
                    new SimpleContainer(input),
                    level
            );

    if (recipe.isEmpty()) return;

    ItemStack result =
            recipe.get().getResultItem(level.registryAccess()).copy();

    int count = 1;

    // 如果结果不是方块 → 时运生效
    if (!(result.getItem() instanceof BlockItem)) {

        int fortune =
                EnchantmentHelper.getTagEnchantmentLevel(
                        Enchantments.BLOCK_FORTUNE, tool);

        count += level.random.nextInt(fortune + 1);
    }

    result.setCount(count);

    Block.popResource(level, event.getPos(), result);

    // 取消原掉落
    event.setCanceled(true);
    level.destroyBlock(event.getPos(), false);
}
}
