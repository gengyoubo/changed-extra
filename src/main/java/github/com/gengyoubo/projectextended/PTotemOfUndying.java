package github.com.gengyoubo.projectextended;

import github.com.gengyoubo.projectextended.items.CETotemOfUndying;
import github.com.gengyoubo.projectextended.items.EnumMatterTypeExtend;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.registration.impl.ItemDeferredRegister;
import moze_intel.projecte.gameObjs.registration.impl.ItemRegistryObject;

public class PTotemOfUndying {
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister("changede");
    public static final ItemRegistryObject<CETotemOfUndying>  DARK_MATTER_TOTEM_OF_UNDYING=ITEMS.register("dark_matter_totem_of_undying",(properties -> new CETotemOfUndying(EnumMatterType.DARK_MATTER,properties)));
    public static final ItemRegistryObject<CETotemOfUndying>  RED_MATTER_TOTEM_OF_UNDYING=ITEMS.register("red_matter_totem_of_undying",(properties -> new CETotemOfUndying(EnumMatterType.RED_MATTER,properties)));
    public static final ItemRegistryObject<CETotemOfUndying>  MATTER_TOTEM_OF_UNDYING_TRUE=ITEMS.register("matter_totem_of_undying_true",(properties -> new CETotemOfUndying(EnumMatterTypeExtend.TRUE,properties)));
    public PTotemOfUndying(){

    }
}
