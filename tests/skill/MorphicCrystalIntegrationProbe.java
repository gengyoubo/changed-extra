package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.changede;
import github.com.gengyoubo.CE.init.*;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicAlloyFurnaceBlockEntity;
import github.com.gengyoubo.CE.LP.recipe.CELPRecipes;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import java.util.*;

/** Server integration and fixed-seed generation sample, never included in release builds. */
public final class MorphicCrystalIntegrationProbe {
    private static void check(boolean result,String message) { if(!result)throw new AssertionError(message); }
    public static void run(MinecraftServer server) throws Exception {
        var level=server.overworld();var random=RandomSource.create(321);
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("13ece8c0-7b57-4c13-a451-d33e4c13cce2"),"MorphicProbe"));
        var iron=new ItemStack(Items.IRON_PICKAXE);var stone=new ItemStack(Items.STONE_PICKAXE);
        var fortune=new ItemStack(Items.DIAMOND_PICKAXE);fortune.enchant(Enchantments.BLOCK_FORTUNE,3);
        var silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(Enchantments.SILK_TOUCH,1);silk.enchant(Enchantments.BLOCK_FORTUNE,3);
        var pos=new BlockPos(10,100,10);int fortuneMax=0;
        for(var block:List.of(CEBlock.DARK_LATEX_MORPHIC_CRYSTAL_ORE.get(),CEBlock.WHITE_LATEX_MORPHIC_CRYSTAL_ORE.get())) {
            var state=block.defaultBlockState();
            check(state.requiresCorrectToolForDrops() && iron.isCorrectToolForDrops(state) && !stone.isCorrectToolForDrops(state),"iron tool gate");
            for(int i=0;i<128;i++) {
                var drops=Block.getDrops(state,level,pos,null,player,iron);
                check(drops.size()==1 && drops.get(0).is(CEItem.MORPHIC_CRYSTAL.get()) && drops.get(0).getCount()>=2 && drops.get(0).getCount()<=5,"plain crystal drops");
                var bonus=Block.getDrops(state,level,pos,null,player,fortune);
                int amount=bonus.get(0).getCount();check(amount>=2 && amount<=20,"fortune copper-style multiplier");fortuneMax=Math.max(fortuneMax,amount);
                var preserved=Block.getDrops(state,level,pos,null,player,silk);
                check(preserved.size()==1 && preserved.get(0).is(block.asItem()) && preserved.get(0).getCount()==1,"Silk Touch wins over Fortune");
                int xp=block.getExpDrop(state,level,random,pos,0,0);check(xp>=3 && xp<=7,"ore XP range");
                check(block.getExpDrop(state,level,random,pos,3,1)==0,"Silk Touch has no XP");
            }
            check(SkillDragonLoot.oreProduct(state)==null,"custom ore excluded from extra-product whitelist");
        }
        check(fortuneMax>5,"Fortune actually increases crystal output");
        for(boolean reversed:List.of(false,true)) {
            var a=new ItemStack(CEItem.MORPHIC_CRYSTAL.get());var b=new ItemStack(CEItem.LATEX_INGOT.get());
            var recipe=level.getRecipeManager().getRecipeFor(CELPRecipes.ALLOY_FURNACE_TYPE,new SimpleContainer(reversed ? b : a,reversed ? a : b),level).orElseThrow();
            check(recipe.getProcessTicks()==200 && recipe.getLpPerSecond()==200 && recipe.getResultItem(level.registryAccess()).is(CEItem.MORPHIC_CRYSTAL_ALLOY.get()),"alloy recipe and both input orders");
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,CELPBlock.BASIC_ALLOY_FURNACE.get().defaultBlockState());
            var furnace=(BasicAlloyFurnaceBlockEntity)level.getBlockEntity(pos);
            furnace.getItemHandler().setStackInSlot(0,reversed ? b : a);furnace.getItemHandler().setStackInSlot(1,reversed ? a : b);
            furnace.receiveEnergy(2000,null);for(int i=0;i<200;i++)furnace.tick();
            check(furnace.getEnergyStored()==0 && furnace.getItemHandler().getStackInSlot(2).is(CEItem.MORPHIC_CRYSTAL_ALLOY.get())
                    && furnace.getItemHandler().getStackInSlot(0).isEmpty() && furnace.getItemHandler().getStackInSlot(1).isEmpty(),"actual LP-powered alloy machine processing");
        }
        var grid=new TransientCraftingContainer(player.inventoryMenu,3,3);
        for(int slot:new int[]{1,3,5,7})grid.setItem(slot,new ItemStack(CEItem.MORPHIC_CRYSTAL_ALLOY.get()));
        grid.setItem(4,new ItemStack(CEItem.PAINITE_INGOT.get()));
        var crafted=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow();
        check(crafted.assemble(grid,level.registryAccess()).is(CEItem.MORPHIC_CRYSTAL_CORE.get()),"cross-shaped core recipe");
        grid.setItem(0,new ItemStack(CEItem.MORPHIC_CRYSTAL_ALLOY.get()));
        check(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).isEmpty(),"empty corners required");
        var feature=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(ResourceLocation.parse("changede:ore_morphic_crystal_small"));
        check(feature!=null && !feature.place(level,level.getChunkSource().getGenerator(),random,pos),"dimension guard rejects overworld");
        var latex=server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("changede:latex_space")));
        check(latex!=null,"latex dimension loaded");
        // A contiguous climate sample can contain only one substrate. Exercise both targets directly.
        for(boolean dark:List.of(false,true)) {
            var center=new BlockPos(dark ? 40 : 80,100,40);
            var substrate=(dark ? CEBlock.DARK_LATEX_STONE : CEBlock.WHITE_LATEX_STONE).get().defaultBlockState();
            var ore=(dark ? CEBlock.DARK_LATEX_MORPHIC_CRYSTAL_ORE : CEBlock.WHITE_LATEX_MORPHIC_CRYSTAL_ORE).get();
            for(var target:BlockPos.betweenClosed(center.offset(-8,-8,-8),center.offset(8,8,8)))latex.setBlock(target,substrate,2);
            check(feature.place(latex,latex.getChunkSource().getGenerator(),RandomSource.create(42),center),"feature places into "+(dark ? "dark" : "white")+" substrate");
            int generated=0;for(var target:BlockPos.betweenClosed(center.offset(-8,-8,-8),center.offset(8,8,8)))if(latex.getBlockState(target).is(ore))generated++;
            check(generated>0,"correct variant generated for each substrate");
        }
        Map<String,Object> report=new LinkedHashMap<>();report.put("seed",level.getSeed());report.put("chunks_per_dimension",512);
        var overworld=sample(level,false);var morphic=sample(latex,true);
        check(overworld.get("morphic")==0 && overworld.get("diamond")>0,"no overworld morphic generation");
        check(morphic.get("morphic")>0,"morphic ore generates naturally");
        double ratio=(double)morphic.get("morphic")/overworld.get("diamond");
        report.put("overworld",overworld);report.put("latex_space",morphic);report.put("morphic_to_diamond_ratio",ratio);
        java.nio.file.Files.writeString(java.nio.file.Path.of("morphic-worldgen-sample.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        check(ratio>.5 && ratio<1.75,"morphic block density should remain comparable to diamonds: "+ratio);
        changede.LOGGER.info("MORPHIC_INTEGRATION_PASS: drops, iron gate, Silk/Fortune, XP, LP processing, crafting, strict dimension and 512-chunk density ratio={}",ratio);
    }
    private static Map<String,Integer> sample(ServerLevel level,boolean latex) {
        int morphic=0,diamond=0,dark=0,white=0,hosts=0;
        for(int i=0;i<512;i++) {
            var chunk=level.getChunk(96+i%32,96+i/32);
            for(int y=-64;y<=16;y++) {
                var section=chunk.getSection(level.getSectionIndex(y));
                if(section.hasOnlyAir())continue;
                for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
                    var state=section.getBlockState(x,y&15,z);
                    if(state.is(CEBlock.DARK_LATEX_MORPHIC_CRYSTAL_ORE.get())) { dark++;morphic++; }
                    else if(state.is(CEBlock.WHITE_LATEX_MORPHIC_CRYSTAL_ORE.get())) { white++;morphic++; }
                    else if(state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE))diamond++;
                    if(state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(CEBlock.DARK_LATEX_STONE.get()) || state.is(CEBlock.WHITE_LATEX_STONE.get()))hosts++;
                }
            }
            if((i+1)%32==0)changede.LOGGER.info("MORPHIC_SAMPLE {} {}/512 chunks; morphic={}, diamond={}",latex ? "latex" : "overworld",i+1,morphic,diamond);
        }
        return Map.of("morphic",morphic,"diamond",diamond,"dark",dark,"white",white,"remaining_host_blocks",hosts);
    }
}
