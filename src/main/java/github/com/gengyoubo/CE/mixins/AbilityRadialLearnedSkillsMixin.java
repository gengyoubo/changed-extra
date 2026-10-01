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
    @Unique private List<AbstractAbility<?>> changede$allAbilities;
    @Unique private int changede$visibleMask = -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void changede$captureCandidates(CallbackInfo ci) {
        changede$allAbilities = List.copyOf(abilities);
        changede$refreshLearned();
    }

    @Inject(method = "getCount", at = @At("HEAD"))
    private void changede$refreshCount(CallbackInfoReturnable<Integer> ci) { changede$refreshLearned(); }

    @Unique private void changede$refreshLearned() {
        if (changede$allAbilities == null) return;
        int mask = 0, index = 0;
        for (AbstractAbility<?> ability : changede$allAbilities) if (ability instanceof YufengFlightAbility skill) {
            if (skill.unlocked(menu.player)) mask |= 1 << index;
            index++;
        }
        if (mask == changede$visibleMask) return;
        changede$visibleMask = mask;
        abilities.clear();
        for (AbstractAbility<?> ability : changede$allAbilities)
            if (!(ability instanceof YufengFlightAbility skill) || skill.unlocked(menu.player)) abilities.add(ability);
        var paging = (AbilityRadialPagingAccessor) this;
        int offset = Math.min(paging.changede$getViewOffset(), Math.max(0, abilities.size() - 8));
        paging.changede$setViewOffset(offset);
        paging.changede$setPreviousOffset(offset);
        paging.changede$setTransitionTicks(0);
    }
}
