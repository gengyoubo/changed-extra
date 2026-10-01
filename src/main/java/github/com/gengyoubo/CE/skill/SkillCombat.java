package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class SkillCombat {
    public static final TagKey<Item> LATEX_WEAPONS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("changede", "latex_weapons"));
    public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("changede", "skill_weapons"));
    private static final String EQUIPMENT_SOURCE = "changede.equipment.armor_adaptation";
    private static final UUID EQUIPMENT_ID = UUID.nameUUIDFromBytes(EQUIPMENT_SOURCE.getBytes(StandardCharsets.UTF_8));
    private SkillCombat() { }
    public static LatexType latexType(Entity entity) {
        if (entity instanceof Player player) {
            var variant = ProcessTransfur.getPlayerTransfurVariant(player);
            return variant == null ? null : variant.getLatexType();
        }
        return entity instanceof ChangedEntity latex ? latex.getLatexType() : null;
    }
    public static boolean weapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return stack.is(WEAPONS) || stack.is(LATEX_WEAPONS) || item instanceof SwordItem || item instanceof AxeItem
                || item instanceof ProjectileWeaponItem || item instanceof TridentItem
                || stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
                        .anyMatch(modifier -> modifier.getAmount() > 0);
    }
    public static ItemStack attackWeapon(Player player, LivingEntity target) {
        ItemStack spear = LatexSpearItem.weaponForHit(player, target);
        return spear.isEmpty() ? player.getMainHandItem() : spear;
    }
    public static void refreshEquipment(Player player) {
        if (player.level().isClientSide) return;
        boolean armor = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && (stack.getItem() instanceof ArmorItem
                    || stack.getAttributeModifiers(slot).get(Attributes.ARMOR).stream().anyMatch(modifier -> modifier.getAmount() > 0))) {
                armor = true;
                break;
            }
        }
        double bonus = SkillCombatRules.equipmentArmor(armor, weapon(player.getMainHandItem()) || weapon(player.getOffhandItem()),
                player.getAttributeValue(SkillAttributes.ARMOR_ADAPTATION.get()), player.getAttributeValue(SkillAttributes.WEAPON_ADAPTATION.get()));
        AttributeInstance target = player.getAttribute(Attributes.ARMOR);
        if (target == null) return;
        var previous = target.getModifier(EQUIPMENT_ID);
        if (previous != null && previous.getAmount() != bonus) target.removeModifier(EQUIPMENT_ID);
        if (bonus > 0 && target.getModifier(EQUIPMENT_ID) == null)
            target.addTransientModifier(new AttributeModifier(EQUIPMENT_ID, EQUIPMENT_SOURCE, bonus, AttributeModifier.Operation.ADDITION));
    }
}
