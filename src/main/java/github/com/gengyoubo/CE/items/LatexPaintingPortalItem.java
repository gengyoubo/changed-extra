package github.com.gengyoubo.CE.items;

import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import github.com.gengyoubo.CE.init.CEEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class LatexPaintingPortalItem extends Item {
    public LatexPaintingPortalItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        LatexPaintingPortalEntity portal = new LatexPaintingPortalEntity(CEEntity.LATEX_PAINTING_PORTAL.get(), level, pos, face);

        if (level instanceof ServerLevel sourceLevel) {
            ServerLevel destination = LatexPaintingPortalBlock.getDestinationLevel(sourceLevel);
            if (destination != null) {
                BlockPos destinationPos = LatexPaintingPortalBlock.findSafeTarget(destination, pos);
                Direction destinationFacing = face.getOpposite();
                LatexPaintingPortalEntity linkedPortal = new LatexPaintingPortalEntity(
                        CEEntity.LATEX_PAINTING_PORTAL.get(),
                        destination,
                        destinationPos,
                        destinationFacing
                );

                portal.setTarget(destination.dimension(), linkedPortal.blockPosition(), destinationFacing);
                linkedPortal.setTarget(sourceLevel.dimension(), portal.blockPosition(), face);
                destination.addFreshEntity(linkedPortal);
            }
            level.addFreshEntity(portal);
            consumeItem(context);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void consumeItem(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
