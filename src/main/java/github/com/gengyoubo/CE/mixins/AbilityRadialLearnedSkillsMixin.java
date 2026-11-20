package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.ability.YufengFlightAbility;
import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.client.gui.AbilityRadialScreen;
import net.ltxprogrammer.changed.world.inventory.AbilityRadialMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = AbilityRadialScreen.class, remap = false)
public abstract class AbilityRadialLearnedSkillsMixin {
    @Shadow @Final public List<AbstractAbility<?>> abilities;
    @Shadow @Final public AbilityRadialMenu menu;
    @Unique private List<AbstractAbility<?>> changed_extra$allAbilities;
    @Unique private int changed_extra$visibleMask = -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void changed_extra$captureCandidates(CallbackInfo ci) {
        changed_extra$allAbilities = List.copyOf(abilities);
        changed_extra$refreshLearned();
    }

    @Inject(method = "getCount", at = @At("HEAD"))
    private void changed_extra$refreshCount(CallbackInfoReturnable<Integer> ci) { changed_extra$refreshLearned(); }

    @Unique private void changed_extra$refreshLearned() {
        if (changed_extra$allAbilities == null) return;
        int mask = 0, index = 0;
        for (AbstractAbility<?> ability : changed_extra$allAbilities) if (ability instanceof YufengFlightAbility skill) {
            if (skill.unlocked(menu.player)) mask |= 1 << index;
            index++;
        }
        if (mask == changed_extra$visibleMask) return;
        changed_extra$visibleMask = mask;
        abilities.clear();
        for (AbstractAbility<?> ability : changed_extra$allAbilities)
            if (!(ability instanceof YufengFlightAbility skill) || skill.unlocked(menu.player)) abilities.add(ability);
        var paging = (AbilityRadialPagingAccessor) this;
        int offset = Math.min(paging.changed_extra$getViewOffset(), Math.max(0, abilities.size() - 8));
        paging.changed_extra$setViewOffset(offset);
        paging.changed_extra$setPreviousOffset(offset);
        paging.changed_extra$setTransitionTicks(0);
    }
}
