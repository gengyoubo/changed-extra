package github.com.gengyoubo.projectextended;

import github.com.gengyoubo.projectextended.items.CEShield;
import github.com.gengyoubo.projectextended.items.CETrident;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.registration.impl.ItemDeferredRegister;
import moze_intel.projecte.gameObjs.registration.impl.ItemRegistryObject;

public class PERegister {
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister("projectextended");
    public static final ItemRegistryObject<CETrident> DARK_MATTER_TRIDENT=ITEMS.register("dark_matter_trident", (properties) -> new CETrident(EnumMatterType.DARK_MATTER, 2, 11.0F, properties));
    public static final ItemRegistryObject<CEShield> DARK_MATTER_SHIELD= ITEMS.register("dark_matter_shield", (properties) -> new CEShield(EnumMatterType.DARK_MATTER, properties));
    public static final ItemRegistryObject<CETrident> RED_MATTER_TRIDENT= ITEMS.register("red_matter_trident", (properties) -> new CETrident(EnumMatterType.RED_MATTER, 3, 14.0F, properties));
    public static final ItemRegistryObject<CEShield> RED_MATTER_SHIELD= ITEMS.register("red_matter_shield", (properties) -> new CEShield(EnumMatterType.RED_MATTER, properties));
}
