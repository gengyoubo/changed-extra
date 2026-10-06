package github.com.gengyoubo.CE.verification;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.GeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicLatexPurifierBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.ElectricFurnaceBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.OrangeProducerBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;

import java.util.UUID;

/** Verifies actual survival mining and inventories in an explicitly enabled disposable server. */
@Mod.EventBusSubscriber(modid = "changede")
public final class TechBlockDropRegressionChecks {
    private TechBlockDropRegressionChecks() { }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        LogManager.getLogger("changede-drop-check").info("PASS: {}", message);
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("changede.verifyTechBlockDrops")) return;
        ServerLevel level = event.getServer().overworld();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "TechDropTest"));
        player.setGameMode(GameType.SURVIVAL);
        var grid = new TransientCraftingContainer(player.inventoryMenu, 1, 1);
        grid.setItem(0, new ItemStack(CELPBlock.SPACE_TOWER.get()));
        var toDimension = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
        check(toDimension.assemble(grid, level.registryAccess()).is(CELPBlock.DIMENSION_SPACE_TOWER.get().asItem()),
                "Single-cell bridge tower converts to dimension tower");
        grid.setItem(0, new ItemStack(CELPBlock.DIMENSION_SPACE_TOWER.get()));
        var toBridge = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
        check(toBridge.assemble(grid, level.registryAccess()).is(CELPBlock.SPACE_TOWER.get().asItem()),
                "Single-cell dimension tower converts back to bridge tower");
        int index = 0;
        for (var registered : CELPBlock.BLOCKS.getEntries()) {
            var block = registered.get();
            BlockPos pos = new BlockPos(2208 + index++ * 4, 160, 2208);
            AABB area = new AABB(pos).inflate(2);
            level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var be = level.getBlockEntity(pos);
            int coal = 0;
            if (be instanceof GeneratorBlockEntity generator) {
                generator.getItemHandler().setStackInSlot(0, new ItemStack(Items.COAL, 3));
                coal = 3;
            } else if (be instanceof BasicLatexPurifierBlockEntity purifier) {
                purifier.getItemHandler().setStackInSlot(0, new ItemStack(Items.COAL, 3));
                purifier.getItemHandler().setStackInSlot(1, new ItemStack(Items.COAL, 2));
                coal = 5;
            } else if (be instanceof ElectricFurnaceBlockEntity furnace) {
                furnace.getItemHandler().setStackInSlot(0, new ItemStack(Items.COAL, 3));
                furnace.getItemHandler().setStackInSlot(1, new ItemStack(Items.COAL, 2));
                coal = 5;
            } else if (be instanceof OrangeProducerBlockEntity producer) {
                var output = new ItemStackHandler(1);
                output.setStackInSlot(0, new ItemStack(Items.COAL, 7));
                CompoundTag tag = producer.saveWithoutMetadata();
                tag.put("Output", output.serializeNBT());
                producer.load(tag);
                coal = 7;
            }
            player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
            check(player.hasCorrectToolForDrops(block.defaultBlockState()), registered.getId() + " accepts a pickaxe");
            check(player.gameMode.destroyBlock(pos), registered.getId() + " can be mined in survival");
            var drops = level.getEntitiesOfClass(ItemEntity.class, area);
            int blockCount = drops.stream().filter(entity -> entity.getItem().is(block.asItem()))
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
            check(blockCount == 1, registered.getId() + " drops exactly one block item");
            int coalCount = drops.stream().filter(entity -> entity.getItem().is(Items.COAL))
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
            check(coalCount == coal, registered.getId() + " releases inventory exactly once");
            drops.forEach(ItemEntity::discard);
        }
        LogManager.getLogger("changede-drop-check").info("ALL CE TECH BLOCK DROP CHECKS PASSED ({} blocks)", index);
    }
}
