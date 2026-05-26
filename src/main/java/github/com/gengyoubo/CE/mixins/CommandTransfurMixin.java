package github.com.gengyoubo.CE.mixins;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import github.com.gengyoubo.CE.fix.SpecialLatexFix.PatreonBenefitsFix;
import github.com.gengyoubo.CE.fix.SpecialLatexFix.SpecialLatex;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.command.CommandTransfur;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.ai.ImmediateTransfurDecision;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.extension.ChangedCompatibility;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(value = CommandTransfur.class,remap = false)
public class CommandTransfurMixin {
    @Unique
    private static final String CHANGED_EXTRA_TAG_HUMAN_LOCK = "latex_start_human_lock";
    @Unique
    private static final String CHANGED_EXTRA_TAG_LATEX_START_VARIANT = "latex_start_variant";
    @Unique
    private static final String CHANGED_E_SPECIAL_FORM_ID = "ChangedESpecialFormId";

    @Final
    @Shadow
    private static SimpleCommandExceptionType NOT_CAUSE;
    @Final
    @Shadow
    private static SimpleCommandExceptionType USED_BY_OTHER_MOD;
    @Final
    @Shadow
    private static SimpleCommandExceptionType NO_SPECIAL_FORM;

    @Unique
    private static TransfurVariant<?> changed_extra$resolveSpecialVariantForCommand(ResourceLocation form, ServerPlayer player) throws CommandSyntaxException {
        ResourceLocation key = form.equals(TransfurVariant.SPECIAL_LATEX)
                ? PatreonBenefitsFix.getSpecialFormId(player.getUUID())
                : form;

        if (PatreonBenefitsFix.isSpecialFormId(key)) {
            TransfurVariant<?> specialVariant = PatreonBenefitsFix.getSpecialVariant(key);
            if (specialVariant == null) {
                throw NO_SPECIAL_FORM.create();
            }
            return specialVariant;
        }

        TransfurVariant<?> variant = PatreonBenefitsFix.resolveVariant(key);
        if (variant == null) {
            throw NO_SPECIAL_FORM.create();
        }
        return variant;
    }

    @Inject(method = "transfurPlayer", at = @At("HEAD"), cancellable = true)
    private static void transfurPlayer(
            CommandSourceStack source,
            ServerPlayer player,
            ResourceLocation form,
            TransfurCause cause,
            CompoundTag tag,
            CallbackInfoReturnable<Integer> cir
    ) throws CommandSyntaxException {
        String stackSnippet = StackWalker.getInstance()
                .walk(stream -> stream
                        .skip(1)
                        .limit(8)
                        .map(frame -> frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber())
                        .collect(Collectors.joining(" <- ")));
        Changed.LOGGER.warn(
                "[Trace] CommandTransfur.transfurPlayer called player={} form={} cause={} stack={}",
                player.getGameProfile().getName(),
                form,
                cause,
                stackSnippet
        );

        if (!form.equals(TransfurVariant.SPECIAL_LATEX) && !PatreonBenefitsFix.isSpecialFormId(form))
            return;
        if (ChangedCompatibility.isPlayerUsedByOtherMod(player))
            throw USED_BY_OTHER_MOD.create();

        TransfurVariant<?> transfurVariant = changed_extra$resolveSpecialVariantForCommand(form, player);

        ProcessTransfur.transfur(player, ImmediateTransfurDecision.safe(transfurVariant, cause, (newEntity) -> {
            if (newEntity.getChangedEntity() instanceof SpecialLatex specialLatex) {
                specialLatex.setSpecialForm(player.getUUID());
            }
            if (tag != null) {
                newEntity.getChangedEntity().readPlayerVariantData(tag);
            }
        }));
        cir.setReturnValue(1);
    }

    @Inject(method = "progressPlayerTransfur", at = @At("HEAD"), cancellable = true)
    private static void progressPlayerTransfur(
            CommandSourceStack source,
            ServerPlayer player,
            ResourceLocation form,
            float progression,
            String cause,
            CallbackInfoReturnable<Integer> cir
    ) throws CommandSyntaxException {
        if (!form.equals(TransfurVariant.SPECIAL_LATEX) && !PatreonBenefitsFix.isSpecialFormId(form))
            return;
        if (ChangedCompatibility.isPlayerUsedByOtherMod(player))
            throw USED_BY_OTHER_MOD.create();

        TransfurCause transfurCause = TransfurCause.fromSerial(cause).result().orElse(null);
        if (transfurCause == null)
            throw NOT_CAUSE.create();

        TransfurVariant<?> transfurVariant = changed_extra$resolveSpecialVariantForCommand(form, player);

        ProcessTransfur.progressTransfur(
                player,
                LatexAssimilationDecision.strongReplication(
                        transfurVariant,
                        TransfurContext.hazard(transfurCause),
                        progression
                )
        );
        cir.setReturnValue(1);
    }

    @Inject(method = "untransfurPlayer", at = @At("TAIL"))
    private static void changed_extra$onUntransfurPlayer(
            CommandSourceStack source,
            ServerPlayer player,
            CallbackInfoReturnable<Integer> cir
    ) {
        // Respect explicit untf command across relog:
        // 1) keep human lock
        // 2) clear remembered latex/special form markers
        var data = player.getPersistentData();
        data.putBoolean(CHANGED_EXTRA_TAG_HUMAN_LOCK, true);
        data.remove(CHANGED_EXTRA_TAG_LATEX_START_VARIANT);
        data.remove(CHANGED_E_SPECIAL_FORM_ID);
        Changed.LOGGER.warn("[Trace] untransfurPlayer set human lock for {}", player.getGameProfile().getName());
    }

    @Inject(method = "untransfurPlayers", at = @At("TAIL"))
    private static void changed_extra$onUntransfurPlayers(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            CallbackInfoReturnable<Integer> cir
    ) {
        players.forEach(player -> {
            var data = player.getPersistentData();
            data.putBoolean(CHANGED_EXTRA_TAG_HUMAN_LOCK, true);
            data.remove(CHANGED_EXTRA_TAG_LATEX_START_VARIANT);
            data.remove(CHANGED_E_SPECIAL_FORM_ID);
            Changed.LOGGER.warn("[Trace] untransfurPlayers set human lock for {}", player.getGameProfile().getName());
        });
    }
}
