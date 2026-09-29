package github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity;

import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.minecraft.world.level.block.state.BlockState;

public class BasicGeneratorBlockEntity extends GeneratorBlockEntity {
    private static final int CAPACITY = 10_000;

    public BasicGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.BASIC_GENERATOR_BLOCK_ENTITY.get(), pos, state, CAPACITY);
    }

    @Override
    protected int generate(ItemStack fuel) {
        return fuel.is(ChangedItems.WHITE_LATEX_GOO.get()) || fuel.is(ChangedItems.DARK_LATEX_GOO.get()) ? 200 : 0;
    }

}
