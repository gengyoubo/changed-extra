package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.init.CEEnchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
public class SWEvents {
    public static void onLivingHurt(LivingHurtEvent event) {

        Entity source = event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        float damage = event.getAmount();

        // ===== 攻击加成 =====
        if (source instanceof LivingEntity attacker) {

            int strong = 0;

            for (ItemStack stack : attacker.getAllSlots()) {

                strong += stack.getEnchantmentLevel(CEEnchantment.SOSTRONG.get()) * 2;

                strong += stack.getEnchantmentLevel(CEEnchantment.PREETTYSTRONG.get());
            }

            damage += strong;
        }

        // ===== 防御减伤 =====
        int weak = 0;

        for (ItemStack stack : target.getAllSlots()) {

            weak += stack.getEnchantmentLevel(CEEnchantment.SOWEEK.get()) * 2;

            weak += stack.getEnchantmentLevel(CEEnchantment.PRETTYWEEK.get());
        }

        damage -= weak;

        if (damage < 1F) {
            damage = 1F;
        }

        event.setAmount(damage);
    }
}
