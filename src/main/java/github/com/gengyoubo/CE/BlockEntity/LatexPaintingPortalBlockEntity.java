package github.com.gengyoubo.CE.BlockEntity;

import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.RequestLatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.changede;
import github.com.gengyoubo.CE.init.CEBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LatexPaintingPortalBlockEntity extends BlockEntity {
    private static final int CLIENT_REQUEST_INTERVAL_TICKS = 80;

    private ResourceKey<Level> previewDimension = LatexPaintingPortalBlock.LATEX_SPACE;
    private BlockPos previewOrigin = BlockPos.ZERO;
    private int clientRequestCooldown;

    public LatexPaintingPortalBlockEntity(BlockPos pos, BlockState state) {
        super(CEBlockEntity.LATEX_PAINTING_PORTAL.get(), pos, state);
    }

    public ResourceKey<Level> getPreviewDimension() {
        return previewDimension;
    }

    public BlockPos getPreviewOrigin() {
        return previewOrigin;
    }

    public void clientTick() {
        if (level == null || !level.isClientSide) {
            return;
        }

        if (clientRequestCooldown > 0) {
            clientRequestCooldown--;
            return;
        }

        clientRequestCooldown = CLIENT_REQUEST_INTERVAL_TICKS;
        changede.LOGGER.warn("Client tick requesting latex painting portal preview at {}", worldPosition);
        CENetwork.sendToServer(new RequestLatexPaintingPortalPreviewPacket(worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("PreviewDimension", previewDimension.location().toString());
        tag.putLong("PreviewOrigin", previewOrigin.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("PreviewDimension")) {
            previewDimension = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString("PreviewDimension"))
            );
        }
        if (tag.contains("PreviewOrigin")) {
            previewOrigin = BlockPos.of(tag.getLong("PreviewOrigin"));
        }
    }
}
