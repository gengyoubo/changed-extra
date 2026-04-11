package github.com.gengyoubo.projectextended;

import github.com.gengyoubo.projectextended.items.CETotemOfUndying;
import github.com.gengyoubo.projectextended.items.EnumMatterTypeExtend;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.registration.impl.ItemDeferredRegister;
import moze_intel.projecte.gameObjs.registration.impl.ItemRegistryObject;

public class PTotemOfUndying {
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister("changede");
    public static final ItemRegistryObject<CETotemOfUndying>  DARK_MATTER_TOTEM_OF_UNDYING;
    public static final ItemRegistryObject<CETotemOfUndying>  RED_MATTER_TOTEM_OF_UNDYING;
    public static final ItemRegistryObject<CETotemOfUndying>  MATTER_TOTEM_OF_UNDYING_TRUE;
    public PTotemOfUndying(){

    }
    static {
        DARK_MATTER_TOTEM_OF_UNDYING=ITEMS.registerNoStackFireImmune("dark_matter_totem_of_undying",(properties -> new CETotemOfUndying(EnumMatterType.DARK_MATTER,properties)));
        RED_MATTER_TOTEM_OF_UNDYING=ITEMS.registerNoStackFireImmune("red_matter_totem_of_undying",(properties -> new CETotemOfUndying(EnumMatterType.RED_MATTER,properties)));
        MATTER_TOTEM_OF_UNDYING_TRUE=ITEMS.registerNoStackFireImmune("matter_totem_of_undying_true",(properties -> new CETotemOfUndying(EnumMatterTypeExtend.TRUE,properties)));
    }
}
