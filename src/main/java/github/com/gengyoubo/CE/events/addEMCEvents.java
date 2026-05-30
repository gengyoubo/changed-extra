package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.changede;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.imc.CustomEMCRegistration;
import moze_intel.projecte.api.imc.IMCMethods;
import moze_intel.projecte.api.nss.NSSItem;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

public class addEMCEvents {
    private static final Object[][] EMC_ENTRIES = {
            // changed
            {"changed:white_latex_goo", 64},
            {"changed:dark_latex_goo", 64},
            {"changed:orange", 128},
            {"changed:blood_syringe", 29},
            {"changed:dark_latex_crystal_fragment", 128},
            {"changed:beifeng_crystal_fragment", 128},
            {"changed:wolf_crystal_fragment", 128},
            {"changed:dark_dragon_crystal_fragment", 128},
            {"changed:latex_inkball", 128},
            {"changed:bar_stool", 64},
            {"changed:bar_top", 64},
            {"changed:floor_sign_wet", 64},
            {"changed:floor_sign_exit", 64},
            {"changed:floor_sign_electrical", 64},
            {"changed:generator", 64},
            {"changed:latex_crystal", 64},
            {"changed:latex_pup_crystal", 64},
            {"changed:beifeng_crystal", 64},
            {"changed:beifeng_crystal_small", 64},
            {"changed:dark_dragon_crystal", 64},
            {"changed:wolf_crystal", 64},
            {"changed:wolf_crystal_small", 64},
            {"changed:dark_latex_crystal_large", 64},
            {"changed:petri_dish", 1},
            {"changed:wall_sign_cat", 64},
            {"changed:wall_sign_do_not_speak_with_dark_latexes", 64},
            {"changed:wall_sign_do_not_touch", 64},
            {"changed:wall_sign_do_not_touch_latexes", 64},
            {"changed:wall_sign_prototype", 64},
            {"changed:wall_sign_squid", 64},
            // changed addon
            {"changed_addon:ammonia_particle", 233},
            {"changed_addon:catalyzed_dna", 29},
            {"changed_addon:iridium", 128},
            {"changed_addon:painite", 8192},
            {"changed_addon:red_latex_goo", 64},
            {"changed_addon:dormant_dark_latex", 256},
            {"changed_addon:dormant_white_latex", 256},
            {"changed_addon:signal_block", 16},
            {"changed_addon:translator", 16},
            {"changed_addon:signal_block", 16},
            {"changed_addon:translator", 16},
            {"changed_addon:luminar_crystal_shard", 128},
            {"changed_addon:yellow_wolf_crystal_fragment", 128},
            {"changed_addon:orange_wolf_crystal_fragment", 128},
            {"changed_addon:white_wolf_crystal_fragment", 128},
            {"changed_addon:blue_wolf_crystal_fragment", 128},
            {"changed_addon:luminar_crystal_small", 64},
            {"changed_addon:blue_wolf_crystal_small", 64},
            {"changed_addon:yellow_wolf_crystal_small", 64},
            {"changed_addon:orange_wolf_crystal_small", 64},
            {"changed_addon:ammonia", 28},
            {"changed_addon:litix_camonia",395},
            {"changed_addon:syringe_with_litix_cammonia",488},
            {"changed_addon:laethin",1346},
            {"changed_addon:laethin_syringe",1504},
            {"changed_addon:luminar_crystal_shard_hearted",160}
            // changed extra

    };

    public static void registerCustomEMC(InterModEnqueueEvent event) {
        if (!changede.PROJECTE) {
            return;
        }

        for (Object[] entry : EMC_ENTRIES) {
            registerItemEMC((String)entry[0], ((Number)entry[1]).longValue());
        }
    }

    private static void registerItemEMC(String itemId, long emc) {
        changede.getItem(itemId).filter(item -> item != Items.AIR).ifPresentOrElse(item ->
                InterModComms.sendTo(
                        ProjectEAPI.PROJECTE_MODID,
                        IMCMethods.REGISTER_CUSTOM_EMC,
                        () -> new CustomEMCRegistration(NSSItem.createItem(item), emc)
                ),
                () -> changede.LOGGER.warn("Skipping ProjectE EMC registration for missing item {}", itemId)
        );
    }
}
