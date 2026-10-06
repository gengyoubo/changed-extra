package github.com.gengyoubo.CE.verification;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.GeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicLatexPurifierBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.ElectricFurnaceBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.OrangeProducerBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.energy.MachineEnergyPersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;

import java.util.UUID;
import java.util.ArrayList;

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
        var blocks = new ArrayList<Block>();
        CELPBlock.BLOCKS.getEntries().forEach(registered -> blocks.add(registered.get()));
        for (String id : new String[]{"changede:latex_skill_research_table", "changed:infuser", "changed:purifier",
                "changed_addon:unifuser", "changed_addon:advanced_unifuser", "changed_addon:catalyzer", "changed_addon:advanced_catalyzer"}) {
            Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id));
            check(block != null && block != Blocks.AIR, id + " exists");
            blocks.add(block);
        }
        for (var block : blocks) {
            var id = ForgeRegistries.BLOCKS.getKey(block);
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
            if (be != null) {
                CompoundTag tag = be.saveWithoutMetadata();
                for (String key : MachineEnergyPersistence.ENERGY_KEYS) {
                    if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) continue;
                    if (tag.get(key).getId() == Tag.TAG_DOUBLE) tag.putDouble(key, key.equals("JouleBuffer") ? 25.0D : 0.5D);
                    else tag.putInt(key, 123);
                }
                be.load(tag);
                be.setChanged();
            }
            CompoundTag expectedEnergy = MachineEnergyPersistence.capture(be);
            player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
            check(player.hasCorrectToolForDrops(block.defaultBlockState()), id + " accepts a pickaxe");
            check(player.gameMode.destroyBlock(pos), id + " can be mined in survival");
            var drops = level.getEntitiesOfClass(ItemEntity.class, area);
            int blockCount = drops.stream().filter(entity -> entity.getItem().is(block.asItem()))
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
            check(blockCount == 1, id + " drops exactly one block item");
            int coalCount = drops.stream().filter(entity -> entity.getItem().is(Items.COAL))
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
            check(coalCount == coal, id + " releases inventory exactly once");
            ItemStack droppedBlock = drops.stream().filter(entity -> entity.getItem().is(block.asItem())).findFirst().orElseThrow().getItem().copy();
            drops.forEach(ItemEntity::discard);
            if (!expectedEnergy.isEmpty()) {
                CompoundTag savedEnergy = BlockItem.getBlockEntityData(droppedBlock);
                check(savedEnergy != null && savedEnergy.equals(expectedEnergy), id + " drops carry only energy and capacity fields");
                player.setItemSlot(EquipmentSlot.MAINHAND, droppedBlock);
                var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                var context = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                check(((BlockItem)droppedBlock.getItem()).place(context).consumesAction(), id + " dropped item can be placed again");
                var restored = level.getBlockEntity(pos);
                check(MachineEnergyPersistence.capture(restored).equals(expectedEnergy), id + " retains exact energy after actual placement");
                restored.load(restored.saveWithoutMetadata());
                check(MachineEnergyPersistence.capture(restored).equals(expectedEnergy), id + " reloading cannot duplicate energy");
                check(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(), id + " placement does not duplicate inventory drops");
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                check(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(), id + " restored machine inventory is empty");
            }
        }
        LogManager.getLogger("changede-drop-check").info("ALL MACHINE DROP AND ENERGY CHECKS PASSED ({} blocks)", index);
    }
}
