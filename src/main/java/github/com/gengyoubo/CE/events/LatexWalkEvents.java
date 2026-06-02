package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.init.CEEnchantment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public final class LatexWalkEvents {
    private static final String LATEX_WALK_GRACE_TAG = "changede_latex_walk_grace";
    private static final int LATEX_WALK_JUMP_GRACE_TICKS = 20;
    private static final double LATEX_LIQUID_MAX_SINK_SPEED = -0.01D;
    private static final double LATEX_LIQUID_SURFACE_CHECK_OFFSET = 0.05D;
    private static final double LATEX_LIQUID_SURFACE_SNAP_LIMIT = 0.2D;
    private static final UUID LATEX_WALK_SPEED_ID = UUID.fromString("2cc05392-5afd-4971-9f77-27f607b2fa94");
    private static final AttributeModifier LATEX_WALK_SPEED = new AttributeModifier(
            LATEX_WALK_SPEED_ID,
            "Latex walk speed",
            0.5D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
    );

    private LatexWalkEvents() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (hasLatexWalk(player)) {
            applyLatexWalking(player);
        } else {
            player.getPersistentData().remove(LATEX_WALK_GRACE_TAG);
            removeSpeedModifier(player);
        }
    }

    private static boolean hasLatexWalk(Player player) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                CEEnchantment.LATEX_WALK.get(),
                player.getItemBySlot(EquipmentSlot.FEET)
        ) > 0;
    }

    private static void applyLatexWalking(Player player) {
        boolean onLatexBlock = isLatexBlock(player.level().getBlockState(player.blockPosition().below()));
        BlockPos latexSurfacePos = BlockPos.containing(player.getX(), player.getY() - LATEX_LIQUID_SURFACE_CHECK_OFFSET, player.getZ());
        boolean onLatexLiquidSurface = isLatexLiquid(player.level().getBlockState(latexSurfacePos));
        boolean inLatexLiquid = isLatexLiquid(player.level().getBlockState(player.blockPosition()))
                || isLatexLiquid(player.level().getBlockState(BlockPos.containing(player.position().add(0.0D, 0.25D, 0.0D))));
        boolean touchingLatex = onLatexBlock || onLatexLiquidSurface || inLatexLiquid;
        boolean keepJumpSpeed = updateJumpGrace(player, touchingLatex);

        if (touchingLatex || keepJumpSpeed) {
            addSpeedModifier(player);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        } else {
            removeSpeedModifier(player);
        }

        if (onLatexLiquidSurface) {
            stabilizeOnLatexLiquidSurface(player, latexSurfacePos);
        } else if (inLatexLiquid) {
            slowLatexLiquidSink(player);
        }
    }

    private static void stabilizeOnLatexLiquidSurface(Player player, BlockPos latexSurfacePos) {
        double surfaceY = latexSurfacePos.getY() + 1.0D;
        Vec3 movement = player.getDeltaMovement();
        double verticalMovement = movement.y < 0.0D ? 0.0D : movement.y;

        player.setDeltaMovement(movement.x, verticalMovement, movement.z);
        if (player.getY() > surfaceY - LATEX_LIQUID_SURFACE_SNAP_LIMIT && player.getY() < surfaceY) {
            player.setPos(player.getX(), surfaceY, player.getZ());
        }

        player.fallDistance = 0.0F;
        player.setSwimming(false);
        player.setOnGround(true);
    }

    private static void slowLatexLiquidSink(Player player) {
        Vec3 movement = player.getDeltaMovement();
        if (movement.y < LATEX_LIQUID_MAX_SINK_SPEED) {
            player.setDeltaMovement(movement.x, LATEX_LIQUID_MAX_SINK_SPEED, movement.z);
        }
        player.fallDistance = 0.0F;
    }

    private static boolean updateJumpGrace(Player player, boolean touchingLatex) {
        CompoundTag data = player.getPersistentData();
        if (touchingLatex) {
            data.putInt(LATEX_WALK_GRACE_TAG, LATEX_WALK_JUMP_GRACE_TICKS);
            return true;
        }

        int grace = data.getInt(LATEX_WALK_GRACE_TAG);
        if (grace <= 0 || player.onGround()) {
            data.remove(LATEX_WALK_GRACE_TAG);
            return false;
        }

        data.putInt(LATEX_WALK_GRACE_TAG, grace - 1);
        return true;
    }

    private static void addSpeedModifier(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && movementSpeed.getModifier(LATEX_WALK_SPEED_ID) == null) {
            movementSpeed.addTransientModifier(LATEX_WALK_SPEED);
        }
    }

    private static void removeSpeedModifier(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && movementSpeed.getModifier(LATEX_WALK_SPEED_ID) != null) {
            movementSpeed.removeModifier(LATEX_WALK_SPEED_ID);
        }
    }

    private static boolean isLatexBlock(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        return path.contains("latex_block") && !isLatexLiquid(state);
    }

    private static boolean isLatexLiquid(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        return path.contains("latex")
                && (path.contains("fluid") || path.contains("liquid") || path.contains("puddle") || !state.getFluidState().isEmpty());
    }
}
