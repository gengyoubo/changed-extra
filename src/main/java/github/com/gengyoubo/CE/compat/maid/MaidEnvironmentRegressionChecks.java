package github.com.gengyoubo.CE.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.fluid.AbstractLatexFluid;
import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.util.EntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.entity.living.LivingEvent;

/** Exercises Changed's actual fluid callbacks using white, dark and aquatic bodies. */
final class MaidEnvironmentRegressionChecks {
    private MaidEnvironmentRegressionChecks() {}

    static void verify(ServerLevel level, ServerPlayer owner) {
        BlockPos pos = new BlockPos(8, 96, 8);
        BlockState previousFeet = level.getBlockState(pos);
        BlockState previousHead = level.getBlockState(pos.above());
        try {
            for (var type : java.util.List.of(ChangedEntities.WHITE_LATEX_WOLF_MALE.get(),
                    ChangedEntities.DARK_LATEX_WOLF_MALE.get(), ChangedEntities.LATEX_SHARK.get())) {
                ChangedEntity body = type.create(level);
                if (body == null) throw new AssertionError("Could not create environment test body");
                body.setPos(8.5, 96, 8.5);
                var proxy = new LatexMaidCompat.SyntheticMaid(level, body, owner);
                try {
                    check(EntityUtil.maybeGetOverlaying(proxy) == body && LatexType.getEntityLatexType(proxy) == body.getLatexType(),
                            "Work adapter retains the body's latex identity: " + type);
                    check(TransfurVariant.getEntityVariant(proxy) == body.getSelfVariant()
                                    && ProcessTransfur.getEntityVariant(proxy).equals(ProcessTransfur.getEntityVariant(body)),
                            "Both Changed variant queries resolve the real body: " + type);
                    check(proxy.canBreatheUnderwater() == body.canBreatheUnderwater()
                                    && proxy.isSensitiveToWater() == body.isSensitiveToWater()
                                    && proxy.fireImmune() == body.fireImmune(),
                            "Work mode retains breathing, water sensitivity and fire immunity: " + type);
                    check(proxy.getDimensions(Pose.STANDING).width == body.getDimensions(Pose.STANDING).width
                                    && proxy.getDimensions(Pose.STANDING).height == body.getDimensions(Pose.STANDING).height
                                    && Math.abs(proxy.getEyeHeight() - body.getEyeHeight()) < 0.001F
                                    && proxy.maxUpStep() == body.maxUpStep(),
                            "Work mode borrows body dimensions, eye height and step height: " + type);
                    double speed = body.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
                    body.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed + 0.125);
                    check(proxy.getAttributeValue(Attributes.MOVEMENT_SPEED) == body.getAttributeValue(Attributes.MOVEMENT_SPEED)
                                    && proxy.getMaxHealth() == body.getMaxHealth(),
                            "Work attributes read live body changes without a copied profile: " + type);
                    body.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
                    proxy.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
                    check(body.hasEffect(MobEffects.FIRE_RESISTANCE) && proxy.getEffect(MobEffects.FIRE_RESISTANCE) == body.getEffect(MobEffects.FIRE_RESISTANCE),
                            "Status effects are owned by the real body: " + type);
                    int duration = body.getEffect(MobEffects.FIRE_RESISTANCE).getDuration();
                    proxy.tickEffects();
                    check(body.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() == duration,
                            "The work adapter cannot tick a body's effects twice: " + type);
                    proxy.removeEffect(MobEffects.FIRE_RESISTANCE);
                    check(!body.hasEffect(MobEffects.FIRE_RESISTANCE), "Work effect removal reaches the real body: " + type);
                    for (BlockState fluid : java.util.List.of(ChangedBlocks.WHITE_LATEX_FLUID.get().defaultBlockState(),
                            ChangedBlocks.DARK_LATEX_FLUID.get().defaultBlockState())) {
                        level.setBlock(pos, fluid, 2);
                        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
                        check(proxy.canStandOnFluid(fluid.getFluidState()) == body.canStandOnFluid(fluid.getFluidState())
                                        && proxy.canSwimInFluidType(fluid.getFluidState().getFluidType()) == body.canSwimInFluidType(fluid.getFluidState().getFluidType())
                                        && proxy.canDrownInFluidType(fluid.getFluidState().getFluidType()) == body.canDrownInFluidType(fluid.getFluidState().getFluidType()),
                                "Fluid stance, swimming and drowning match the body: " + type + " / " + fluid.getBlock());
                        reset(body);
                        fluid.entityInside(level, pos, body);
                        float blockLoss = body.getMaxHealth() - body.getHealth();
                        reset(body);
                        fluid.entityInside(level, pos, proxy);
                        check(Math.abs(body.getMaxHealth() - body.getHealth() - blockLoss) < 0.001F,
                                "Fluid block contact preserves the body's resistance: " + type + " / " + fluid.getBlock());
                        reset(body);
                        AbstractLatexFluid.onEntityTick(new LivingEvent.LivingTickEvent(body));
                        float fluidLoss = body.getMaxHealth() - body.getHealth();
                        reset(body);
                        AbstractLatexFluid.onEntityTick(new LivingEvent.LivingTickEvent(proxy));
                        check(body.isAlive() && !proxy.isRemoved() && Math.abs(body.getMaxHealth() - body.getHealth() - fluidLoss) < 0.001F,
                                "Fluid tick preserves immunity or hostility without assimilating the work adapter: " + type + " / " + fluid.getBlock());
                    }
                    check(proxy.canDrownInFluidType(Fluids.WATER.getFluidType()) == body.canDrownInFluidType(Fluids.WATER.getFluidType()),
                            "Water breathing remains species-specific in work mode: " + type);
                } finally {
                    proxy.retire();
                    body.discard();
                }
            }
            // The bridge must not assign latex immunity to ordinary maids.
            var regular = new EntityMaid(level);
            try {
                regular.setPos(8.5, 96, 8.5);
                float health = regular.getHealth();
                var fluid = ChangedBlocks.WHITE_LATEX_FLUID.get().defaultBlockState();
                fluid.entityInside(level, pos, regular);
                check(LatexType.getEntityLatexType(regular) == null && regular.getHealth() < health,
                        "Ordinary maids remain vulnerable to white latex");
            } finally {
                regular.discard();
            }
            changede.LOGGER.info("MAID ENVIRONMENT REGRESSION CHECKS PASSED");
        } finally {
            level.setBlock(pos, previousFeet, 2);
            level.setBlock(pos.above(), previousHead, 2);
        }
    }

    private static void reset(ChangedEntity body) {
        body.setHealth(body.getMaxHealth());
        body.invulnerableTime = 0;
        body.hurtTime = 0;
        body.setDeltaMovement(0, 0, 0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        changede.LOGGER.info("Maid environment PASS: {}", message);
    }
}
