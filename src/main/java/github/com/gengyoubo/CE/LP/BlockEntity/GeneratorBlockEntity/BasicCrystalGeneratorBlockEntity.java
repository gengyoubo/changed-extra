package github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity;

import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class BasicCrystalGeneratorBlockEntity extends GeneratorBlockEntity {
    private static final int CAPACITY = 50_000;
    private static final ResourceLocation FRAGMENTS_TAG = ResourceLocation.parse("changede:crystals/fragments");
    private static final ResourceLocation SMALL_CRYSTALS_TAG = ResourceLocation.parse("changede:crystals/small");
    private static final ResourceLocation LARGE_CRYSTALS_TAG = ResourceLocation.parse("changede:crystals/large");

    public BasicCrystalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.BASIC_CRYSTAL_GENERATOR_BLOCK_ENTITY.get(), pos, state, CAPACITY);
    }

    @Override
    protected int generate(ItemStack fuel) {
        if (isInTag(fuel, FRAGMENTS_TAG)) return 100;
        if (isInTag(fuel, SMALL_CRYSTALS_TAG)) return 200;
        if (isInTag(fuel, LARGE_CRYSTALS_TAG)) return 500;
        return 0;
    }

    private static boolean isInTag(ItemStack stack, ResourceLocation tagId) {
        return stack.is(TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId));
    }
}
