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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class LatexPaintingPortalItem extends Item {
    private static final double LINK_SEARCH_RADIUS = 16.0D;

    public LatexPaintingPortalItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return InteractionResult.FAIL;
        }

        Direction viewFacing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        LatexPaintingPortalEntity portal = new LatexPaintingPortalEntity(CEEntity.LATEX_PAINTING_PORTAL.get(), level, pos, face);
        portal.setRenderReversed(false);

        if (level instanceof ServerLevel sourceLevel) {
            ServerLevel destination = LatexPaintingPortalBlock.getDestinationLevel(sourceLevel);
            if (destination != null) {
                BlockPos destinationPos = LatexPaintingPortalBlock.findSafeTarget(destination, pos);
                Direction destinationFacing = viewFacing;
                LatexPaintingPortalEntity linkedPortal = findNearestPortal(destination, destinationPos);
                if (linkedPortal == null) {
                    linkedPortal = new LatexPaintingPortalEntity(
                            CEEntity.LATEX_PAINTING_PORTAL.get(),
                            destination,
                            destinationPos,
                            destinationFacing
                    );
                    destination.addFreshEntity(linkedPortal);
                } else if (destination.dimension().equals(LatexPaintingPortalBlock.LATEX_SPACE)) {
                    linkedPortal.setFacing(destinationFacing);
                }

                linkedPortal.setRenderReversed(true);
                portal.setTarget(destination.dimension(), linkedPortal.blockPosition(), viewFacing);
                linkedPortal.setTarget(sourceLevel.dimension(), portal.blockPosition(), viewFacing);
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

    private static LatexPaintingPortalEntity findNearestPortal(ServerLevel level, BlockPos center) {
        AABB area = new AABB(
                center.getX() + 0.5D - LINK_SEARCH_RADIUS,
                level.getMinBuildHeight(),
                center.getZ() + 0.5D - LINK_SEARCH_RADIUS,
                center.getX() + 0.5D + LINK_SEARCH_RADIUS,
                level.getMaxBuildHeight(),
                center.getZ() + 0.5D + LINK_SEARCH_RADIUS
        );
        List<LatexPaintingPortalEntity> portals = level.getEntitiesOfClass(
                LatexPaintingPortalEntity.class,
                area,
                portal -> !portal.isRemoved() && horizontalDistanceSqr(portal, center) <= LINK_SEARCH_RADIUS * LINK_SEARCH_RADIUS
        );
        return portals.stream()
                .min(Comparator.comparingDouble(portal -> horizontalDistanceSqr(portal, center)))
                .orElse(null);
    }

    private static double horizontalDistanceSqr(LatexPaintingPortalEntity portal, BlockPos center) {
        double dx = portal.getX() - (center.getX() + 0.5D);
        double dz = portal.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }
}
