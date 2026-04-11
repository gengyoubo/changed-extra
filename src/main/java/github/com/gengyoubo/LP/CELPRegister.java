package github.com.gengyoubo.LP;

import github.com.gengyoubo.LP.Block.BasicGeneratorBlock;
import github.com.gengyoubo.LP.BlockEntity.GeneratorBlockEntity.BasicGeneratorBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.EnergyPipeBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.WireBlock;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.WireType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CELPRegister {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "changede");

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "changede");

    // Keep the old names as aliases so existing registration code keeps compiling.
    public static final DeferredRegister<Block> WIRE_BLOCKS = BLOCKS;
    public static final DeferredRegister<BlockEntityType<?>> WIRE_BLOCK_ENTITIES = BLOCK_ENTITIES;

    public static final RegistryObject<Block> BASIC_WIRE =
            BLOCKS.register("basic_wire",
                    () -> new WireBlock(WireType.BASIC, BlockBehaviour.Properties.of()));

    public static final RegistryObject<Item> BASIC_WIRE_ITEM =
            ITEMS.register("basic_wire",
                    () -> new BlockItem(BASIC_WIRE.get(), new Item.Properties()));

    public static final RegistryObject<Block> BASIC_GENERATOR =
            BLOCKS.register("basic_generator", BasicGeneratorBlock::new);

    public static final RegistryObject<Item> BASIC_GENERATOR_ITEM =
            ITEMS.register("basic_generator",
                    () -> new BlockItem(BASIC_GENERATOR.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<EnergyPipeBlockEntity>> BASIC_WIRE_ENTITIES =
            BLOCK_ENTITIES.register("wire",
                    () -> BlockEntityType.Builder.of(
                            EnergyPipeBlockEntity::new,
                            BASIC_WIRE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<BasicGeneratorBlockEntity>> BASIC_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("basic_generator",
                    () -> BlockEntityType.Builder.of(
                            BasicGeneratorBlockEntity::new,
                            BASIC_GENERATOR.get()
                    ).build(null));
}
