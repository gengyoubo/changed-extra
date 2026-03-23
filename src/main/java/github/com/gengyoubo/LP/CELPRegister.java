package github.com.gengyoubo.LP;

import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
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
    //物品
    //导线
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "changede");

    public static final RegistryObject<Item> BASIC_WIRE_ITEM =
            ITEMS.register("baisc_wire",
                    () -> new BlockItem(CELPRegister.BASIC_WIRE.get(), new Item.Properties()));
    //机器

    //发电机

    //方块
    //导线
    public static final DeferredRegister<Block> WIRE_BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");

    public static final RegistryObject<Block> BASIC_WIRE =
            WIRE_BLOCKS.register("basic_wire",
                    () -> new WireBlock(WireType.BASIC, BlockBehaviour.Properties.of()));
    //机器

    //发电机

    //方块实体
    //导线
    public static final DeferredRegister<BlockEntityType<?>> WIRE_BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "changede");
    public static final RegistryObject<BlockEntityType<BasePipeBlockEntity>> BASIC_WIRE_ENTITIES =
            WIRE_BLOCK_ENTITIES.register("wire",
                    () -> BlockEntityType.Builder.of(
                            WireBlockEntity::new,
                            CELPRegister.BASIC_WIRE.get()
                    ).build(null));
    //机器

    //发电机

}
