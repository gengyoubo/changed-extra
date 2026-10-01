package github.com.gengyoubo.CE.init;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** The two latex wood families share vanilla wood behavior and their own sign types. */
public final class CEWoodFamilies {
    public static final Family DARK = new Family("dark_latex", CEBlock.DARK_LATEX_LOG, CEBlock.DARK_LATEX_PLANKS);
    public static final Family WHITE = new Family("white_latex", CEBlock.WHITE_LATEX_LOG, CEBlock.WHITE_LATEX_PLANKS);
    public static final List<Family> ALL = List.of(DARK, WHITE);
    public static final RegistryObject<BlockEntityType<SignBlockEntity>> SIGN = CEBlockEntity.BLOCK_ENTITIES.register(
            "latex_sign", () -> BlockEntityType.Builder.of(CEWoodFamilies::newSign,
                    DARK.blocks.get("sign").get(), DARK.blocks.get("wall_sign").get(),
                    WHITE.blocks.get("sign").get(), WHITE.blocks.get("wall_sign").get()).build(null));
    public static final RegistryObject<BlockEntityType<HangingSignBlockEntity>> HANGING_SIGN = CEBlockEntity.BLOCK_ENTITIES.register(
            "latex_hanging_sign", () -> BlockEntityType.Builder.of(CEWoodFamilies::newHangingSign,
                    DARK.blocks.get("hanging_sign").get(), DARK.blocks.get("wall_hanging_sign").get(),
                    WHITE.blocks.get("hanging_sign").get(), WHITE.blocks.get("wall_hanging_sign").get()).build(null));

    private static SignBlockEntity newSign(BlockPos pos, BlockState state) { return new SignBlockEntity(SIGN.get(), pos, state); }
    private static HangingSignBlockEntity newHangingSign(BlockPos pos, BlockState state) { return new HangingSignBlockEntity(HANGING_SIGN.get(), pos, state); }
    private static <T extends BlockEntity> BlockEntityTicker<T> signTicker(BlockEntityType<T> type, boolean hanging) {
        return type == (hanging ? HANGING_SIGN.get() : SIGN.get())
                ? (level, pos, state, entity) -> SignBlockEntity.tick(level, pos, state, (SignBlockEntity) entity) : null;
    }
    public static void initialize() { /* Force registration before the deferred registers attach. */ }

    public static final class Family {
        public final String name;
        public final WoodType woodType;
        public final RegistryObject<Block> log;
        public final Map<String, RegistryObject<Block>> blocks = new LinkedHashMap<>();
        public final List<RegistryObject<Item>> items = new ArrayList<>();
        private Family(String name, RegistryObject<Block> log, RegistryObject<Block> planks) {
            this.name = name;
            this.log = log;
            woodType = WoodType.register(new WoodType("changede:" + name, BlockSetType.OAK));
            add("wood", () -> new RotatedPillarBlock(props(Blocks.OAK_WOOD)));
            add("stripped_log", () -> new RotatedPillarBlock(props(Blocks.STRIPPED_OAK_LOG)));
            add("stripped_wood", () -> new RotatedPillarBlock(props(Blocks.STRIPPED_OAK_WOOD)));
            add("stairs", () -> new StairBlock(() -> planks.get().defaultBlockState(), props(Blocks.OAK_STAIRS)));
            add("slab", () -> new SlabBlock(props(Blocks.OAK_SLAB)));
            add("fence", () -> new FenceBlock(props(Blocks.OAK_FENCE)));
            add("fence_gate", () -> new FenceGateBlock(props(Blocks.OAK_FENCE_GATE), woodType));
            add("door", () -> new DoorBlock(props(Blocks.OAK_DOOR), BlockSetType.OAK));
            add("trapdoor", () -> new TrapDoorBlock(props(Blocks.OAK_TRAPDOOR), BlockSetType.OAK));
            add("button", () -> new ButtonBlock(props(Blocks.OAK_BUTTON), BlockSetType.OAK, 30, true));
            add("pressure_plate", () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, props(Blocks.OAK_PRESSURE_PLATE), BlockSetType.OAK));
            blocks.put("sign", CEBlock.BLOCKS.register(name + "_sign", () -> new StandingSignBlock(props(Blocks.OAK_SIGN), woodType) {
                @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return newSign(pos, state); }
                @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return signTicker(type, false); }
            }));
            blocks.put("wall_sign", CEBlock.BLOCKS.register(name + "_wall_sign", () -> new WallSignBlock(props(Blocks.OAK_WALL_SIGN).dropsLike(blocks.get("sign").get()), woodType) {
                @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return newSign(pos, state); }
                @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return signTicker(type, false); }
            }));
            items.add(CEItem.ITEMS.register(name + "_sign", () -> new SignItem(new Item.Properties().stacksTo(16), blocks.get("sign").get(), blocks.get("wall_sign").get())));
            blocks.put("hanging_sign", CEBlock.BLOCKS.register(name + "_hanging_sign", () -> new CeilingHangingSignBlock(props(Blocks.OAK_HANGING_SIGN), woodType) {
                @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return newHangingSign(pos, state); }
                @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return signTicker(type, true); }
            }));
            blocks.put("wall_hanging_sign", CEBlock.BLOCKS.register(name + "_wall_hanging_sign", () -> new WallHangingSignBlock(props(Blocks.OAK_WALL_HANGING_SIGN).dropsLike(blocks.get("hanging_sign").get()), woodType) {
                @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return newHangingSign(pos, state); }
                @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return signTicker(type, true); }
            }));
            items.add(CEItem.ITEMS.register(name + "_hanging_sign", () -> new HangingSignItem(blocks.get("hanging_sign").get(), blocks.get("wall_hanging_sign").get(), new Item.Properties().stacksTo(16))));
        }
        private void add(String suffix, java.util.function.Supplier<Block> factory) {
            var block = CEBlock.BLOCKS.register(name + "_" + suffix, factory);
            blocks.put(suffix, block);
            items.add(CEItem.ITEMS.register(name + "_" + suffix, () -> new BlockItem(block.get(), new Item.Properties())));
        }
    }
    private static BlockBehaviour.Properties props(Block block) { return BlockBehaviour.Properties.copy(block); }
}
