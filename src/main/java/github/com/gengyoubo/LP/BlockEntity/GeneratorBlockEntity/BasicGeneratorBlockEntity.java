package github.com.gengyoubo.LP.BlockEntity.GeneratorBlockEntity;

import github.com.gengyoubo.LP.CELPRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BasicGeneratorBlockEntity extends GeneratorBlockEntity {
    private static final int CAPACITY = 10_000;
    private static final int GENERATION_PER_TICK = 20;

    public BasicGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(CELPRegister.BASIC_GENERATOR_BLOCK_ENTITY.get(), pos, state, CAPACITY);
    }

    @Override
    protected int generate() {
        return GENERATION_PER_TICK;
    }
}
