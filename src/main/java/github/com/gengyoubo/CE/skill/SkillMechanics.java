package github.com.gengyoubo.CE.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Passive mechanics are granted per reward source; removing one source preserves the others. */
@Mod.EventBusSubscriber(modid="changede")
@SuppressWarnings("deprecation")
public final class SkillMechanics {
    private record Grant(String effect, double value) { }
    private static final Map<Player, Map<String, Grant>> GRANTS = new WeakHashMap<>();
    private static final Map<Player,ResourceLocation> GRANT_FORMS=new WeakHashMap<>();
    private static final Map<Player, Integer> RECOVERY = new WeakHashMap<>();
    private static final Set<Mob> FEAR_GOALS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final UUID HEALTH = UUID.nameUUIDFromBytes("changede.insect.health".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final String LIVES="changede_nine_lives", PROTECTED="changede_life_protection";
    private static final TagKey<Biome> FORESTS=TagKey.create(Registries.BIOME,ResourceLocation.tryParse("changede:skill_forests"));
    private static ResourceLocation clientForm;
    private static CompoundTag clientEffects=new CompoundTag();
    private static final Set<String> EFFECTS = Set.of("sea_core", "sea_guard", "sea_work", "sea_mining", "sea_thermal", "air_core",
            "air_endurance", "air_load", "feline_fear", "feline_damage", "nine_lives", "reptile_core", "blast_guard", "dragon_fortune",
            "dragon_looting", "dragon_ore", "dragon_hunt", "arthropod", "insect_core", "insect_recovery", "fish_diet",
            "dark_latex_diet", "white_latex_diet", "orange_diet", "meat_diet", "vegetarian_diet");
    private SkillMechanics() { }
    public static void validate(String effect, double value) {
        double maximum = effect.equals("feline_fear") ? 16 : effect.equals("insect_recovery") ? 4 : 1;
        if (!EFFECTS.contains(effect) || !Double.isFinite(value) || value <= 0 || value > maximum)
            throw new IllegalArgumentException("Invalid mechanic " + effect + ": " + value);
    }
    public static void grant(Player player, String source, String effect, double value) {
        GRANTS.computeIfAbsent(player, p -> new HashMap<>()).put(source, new Grant(effect,value));
        if (effect.equals("nine_lives") && !player.getPersistentData().contains(LIVES)) player.getPersistentData().putInt(LIVES,9);
    }
    public static void revoke(Player player, String source) {
        var grants=GRANTS.get(player); if (grants!=null) grants.remove(source);
    }
    public static double value(Player player, String effect) {
        if (LatexSkills.form(player)==null) return 0;
        if (player.level().isClientSide) return player.isLocalPlayer() && Objects.equals(clientForm,LatexSkills.form(player)) ? clientEffects.getDouble(effect) : 0;
        if (!Objects.equals(GRANT_FORMS.get(player),LatexSkills.form(player)))return 0;
        return GRANTS.getOrDefault(player,Map.of()).values().stream().filter(g->g.effect.equals(effect))
                .mapToDouble(Grant::value).reduce(effect.equals("insect_recovery") ? Double::min : Double::max).orElse(0);
    }
    public static void bindForm(Player player) {GRANT_FORMS.put(player,LatexSkills.form(player));}
    public static CompoundTag snapshot(Player player) {
        CompoundTag tag=new CompoundTag();for(String effect:EFFECTS){double value=value(player,effect);if(value>0)tag.putDouble(effect,value);}return tag;
    }
    public static void applyClient(ResourceLocation form,CompoundTag tag) {clientForm=form;clientEffects=tag.copy();}
    public static void refreshHealth(Player player) {
        var attribute=player.getAttribute(Attributes.MAX_HEALTH);
        if(attribute==null)return;
        boolean half=value(player,"insect_core")>0;
        if(half && attribute.getModifier(HEALTH)==null)attribute.addTransientModifier(new AttributeModifier(HEALTH,"changede.insect.health",-.5,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if(!half && attribute.getModifier(HEALTH)!=null)attribute.removeModifier(HEALTH);
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
    }
    public static int lives(Player player) { return player.getPersistentData().contains(LIVES) ? Math.max(0,Math.min(9,player.getPersistentData().getInt(LIVES))) : 9; }
    public static void migrate(ServerPlayer player) {
        CompoundTag data=player.getPersistentData();
        if (!data.getBoolean("changede_skill_branches_v2")) {
            CompoundTag root=data.getCompound("changede_latex_skills");
            if (root.getCompound("changede:yufeng").getBoolean("changede:yufeng_gate")) {
                CompoundTag air=root.getCompound("changede:air"); air.putBoolean("changede:air_core",true);
                root.put("changede:air",air); data.put("changede_latex_skills",root);
            }
            data.putBoolean("changede_skill_branches_v2",true);
        }
    }
    public static boolean feline(LivingEntity entity) {
        if (entity instanceof Cat || entity instanceof Ocelot) return true;
        return entity instanceof Player p && LatexSkills.form(p)!=null && LatexSkillTrees.hasForm("feline",LatexSkills.form(p));
    }
    private static int armorSlots(Player player) { int slots=0; for (ItemStack item:player.getArmorSlots()) if(!item.isEmpty())slots++; return slots; }
    public static double flightMultiplier(Player player) {
        return value(player,"air_core")>0 ? SkillMechanicRules.flightMultiplier(armorSlots(player),value(player,"air_load")) : 1;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (event.getAmount()<=0 || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        double amount=event.getAmount();
        if(event.getSource().getEntity() instanceof ServerPlayer attacker) {
            amount*=1+value(attacker,"feline_damage");
            if(value(attacker,"reptile_core")>0 && feline(event.getEntity()))amount*=.8;
        }
        if(event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().getLong(PROTECTED)>player.level().getGameTime()) { event.setCanceled(true); return; }
            boolean sea=value(player,"sea_core")>0;
            double thermal=1-value(player,"sea_thermal");
            amount=SkillMechanicRules.incoming(amount, sea && player.isInWaterOrBubble() ? Math.max(.2,value(player,"sea_guard")) : 0,
                    event.getSource().is(DamageTypeTags.IS_EXPLOSION) ? value(player,"blast_guard") : 0,
                    sea && event.getSource().is(DamageTypeTags.IS_FIRE) ? .25*thermal : 0,
                    sea && player.level().dimension().equals(Level.NETHER) ? .15*thermal : 0);
        }
        event.setAmount((float)amount);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || value(player,"nine_lives")==0 || lives(player)<=0
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        event.setCanceled(true);
        player.getPersistentData().putInt(LIVES,lives(player)-1);
        player.getPersistentData().putLong(PROTECTED,player.level().getGameTime()+20);
        player.setHealth(SkillMechanicRules.rescueHealth(player.getMaxHealth())); player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100,0));
        player.level().broadcastEntityEvent(player,(byte)35);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        var old=event.getOriginal().getPersistentData(); var next=event.getEntity().getPersistentData();
        if(old.contains(LIVES))next.putInt(LIVES,event.isWasDeath()?9:old.getInt(LIVES));
        if(old.contains("changede_skill_branches_v2"))next.putBoolean("changede_skill_branches_v2",old.getBoolean("changede_skill_branches_v2"));
    }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed event) {
        Player player=event.getEntity();
        if(player.isEyeInFluid(FluidTags.WATER)) {
            float speed=event.getNewSpeed();
            if(value(player,"sea_work")>0 && !net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(player))speed*=5;
            event.setNewSpeed(speed*(float)(1+value(player,"sea_mining")));
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))return;
        // Reconcile on a form transition immediately, before any periodic effects or incoming combat.
        String form=Objects.toString(LatexSkills.form(player),"");
        if(!form.equals(player.getPersistentData().getString("changede_skill_runtime_form"))) {
            player.getPersistentData().putString("changede_skill_runtime_form",form); LatexSkills.refresh(player); RECOVERY.remove(player);
        }
        refreshHealth(player);
        if(!player.isAlive())return;
        boolean forest=player.level().getBiome(player.blockPosition()).is(FORESTS);
        boolean rain=player.level().isRainingAt(player.blockPosition());
        if(value(player,"insect_core")>0 && (forest||rain) && player.getHealth()<player.getMaxHealth()) {
            int interval=(int)(20*(value(player,"insect_recovery")>0 ? value(player,"insect_recovery") : 4));
            int elapsed=RECOVERY.getOrDefault(player,0)+1;
            if(elapsed>=interval){player.heal(1);elapsed=0;} RECOVERY.put(player,elapsed);
        } else RECOVERY.remove(player);
        if(!player.isCreative() && !player.isSpectator() && value(player,"air_core")>0 && (player.isFallFlying()||player.getAbilities().flying)) {
            // Powered flight is charged at its original call site. Gliding uses the fallback baseline.
            if(!player.getAbilities().flying)player.causeFoodExhaustion((float)(.1*flightFoodMultiplier(player)/20));
            if(player.getFoodData().getFoodLevel()<=6 && player.getAbilities().flying) {
                player.getAbilities().flying=false;player.startFallFlying();player.onUpdateAbilities();
            }
        }
        if(player.tickCount%10==0 && value(player,"feline_fear")>0) {
            double radius=value(player,"feline_fear");
            for(Mob mob:player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(radius),m->m instanceof Creeper||m instanceof Phantom))
                if(FEAR_GOALS.add(mob))mob.goalSelector.addGoal(0,new FelineAvoidGoal(mob));
        }
    }
    public static double flightFoodMultiplier(Player player) { return value(player,"air_core")>0 ? SkillMechanicRules.flightExhaustion(1,value(player,"air_endurance")) : 1; }
    private static final class FelineAvoidGoal extends Goal {
        private final Mob mob; private Player threat;
        FelineAvoidGoal(Mob mob) { this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            threat=mob.level().getEntitiesOfClass(Player.class,mob.getBoundingBox().inflate(16),p->p.isAlive()&&!p.isSpectator()&&value(p,"feline_fear")>0&&mob.distanceToSqr(p)<=Math.pow(value(p,"feline_fear"),2))
                    .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            return threat!=null;
        }
        @Override public boolean canContinueToUse(){return threat!=null&&threat.isAlive()&&value(threat,"feline_fear")>0&&mob.distanceToSqr(threat)<Math.pow(value(threat,"feline_fear")+4,2);}
        @Override public void tick(){
            mob.setTarget(null); Vec3 away=mob.position().subtract(threat.position()).normalize();
            Vec3 target=mob.position().add(away.scale(8));
            if(mob instanceof Phantom) {
                mob.getMoveControl().setWantedPosition(target.x,target.y+2,target.z,1.2);
                // Phantom's custom move controller ignores vanilla navigation targets.
                mob.setDeltaMovement(away.scale(.35).add(0,.1,0));
            }
            else mob.getNavigation().moveTo(target.x,target.y,target.z,1.2);
        }
        @Override public void stop(){threat=null;mob.getNavigation().stop();}
    }
}
