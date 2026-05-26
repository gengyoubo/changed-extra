package github.com.gengyoubo.CE;

import com.mojang.logging.LogUtils;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPItem;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.recipe.CELPRecipes;
import github.com.gengyoubo.CE.LP.world.Menu.CEMenus;
import github.com.gengyoubo.CE.commands.CheckSpecialFormCommand;
import github.com.gengyoubo.CE.commands.ItemInfoCommand;
import github.com.gengyoubo.CE.commands.ReloadEMCCommand;
import github.com.gengyoubo.CE.events.GooCoreTooltipEvents;
import github.com.gengyoubo.CE.events.LatexDeathHandlerEvents;
import github.com.gengyoubo.CE.events.MimicYufengWingsFlightEvents;
import github.com.gengyoubo.CE.events.SWEvents;
import github.com.gengyoubo.CE.events.SalvageEvents;
import github.com.gengyoubo.CE.events.ScorchingHeatEvents;
import github.com.gengyoubo.CE.events.SignalCatcherTooltipEvents;
import github.com.gengyoubo.CE.events.XPBoostEvents;
import github.com.gengyoubo.CE.events.addEMCEvents;
import github.com.gengyoubo.CE.events.latexStartEvents;
import github.com.gengyoubo.CE.fix.SpecialLatexFix.CEChangedSounds;
import github.com.gengyoubo.CE.fix.SpecialLatexFix.ChangedEntitiesFix;
import github.com.gengyoubo.CE.fix.SpecialLatexFix.PatreonBenefitsFix;
import github.com.gengyoubo.CE.projectextended.PERegister;
import github.com.gengyoubo.CE.projectextended.PTotemOfUndying;
import github.com.gengyoubo.CE.projectextended.events.CEShieldEvents;
import github.com.gengyoubo.CE.init.CECreativeModeTab;
import github.com.gengyoubo.CE.init.CEEnchantment;
import github.com.gengyoubo.CE.init.CEGameRules;
import github.com.gengyoubo.CE.init.CEItem;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod("changede")
public class changede {
    public static final boolean PROJECTE = ModList.get().isLoaded("projecte");
    public static final boolean PE = ModList.get().isLoaded("projectextended");
    public static final boolean CHANGED_ADDON = ModList.get().isLoaded("changed_addon");
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean PATREON_SYNC_STARTED = new AtomicBoolean(false);

    public changede(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        bus.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent.class, this::commonSetup);
        CEEnchantment.ENCHANTMENTS.register(bus);
        CECreativeModeTab.CREATIVE_MODE_TABS.register(bus);
        CEItem.ITEMS.register(bus);
        CELPItem.ITEMS.register(bus);
        CELPBlock.WIRE_BLOCKS.register(bus);
        CELPBlockEntity.BLOCK_ENTITIES.register(bus);
        ChangedEntitiesFix.REGISTRY.register(bus);
        CEChangedSounds.REGISTRY.register(bus);
        CEMenus.REGISTRY.register(bus);
        CELPRecipes.RECIPE_SERIALIZERS.register(bus);
        PatreonBenefitsFix.REGISTRY.register(bus);
        CENetwork.register();
        CEGameRules.register();
        bus.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent.class, latexStartEvents::setup);

        registerForgeEventListeners();

        // 联动等价交换
        if (PROJECTE) {
            new ReloadEMCCommand();
            PTotemOfUndying.ITEMS.register(bus);
            if (PE) {
                PERegister.ITEMS.register(bus);
            }
        }
    }

    private void registerForgeEventListeners() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RegisterCommandsEvent.class, ItemInfoCommand::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RegisterCommandsEvent.class, CheckSpecialFormCommand::onRegisterCommands);

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerDestroyItemEvent.class, SalvageEvents::onBreak);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, SalvageEvents::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, ScorchingHeatEvents::onBreak);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, XPBoostEvents::onBlockXP);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingHurtEvent.class, SWEvents::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, MimicYufengWingsFlightEvents::onPlayerTick);

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, latexStartEvents::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.Clone.class, latexStartEvents::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDeathEvent.class, LatexDeathHandlerEvents::onPlayerDeath);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.Clone.class, LatexDeathHandlerEvents::onPlayerClone);

        if (PE) {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ShieldBlockEvent.class, CEShieldEvents::onShieldBlock);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingAttackEvent.class, CEShieldEvents::onLivingAttack);
        }

        if (CHANGED_ADDON && FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ItemTooltipEvent.class, GooCoreTooltipEvents::onItemTooltip);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ItemTooltipEvent.class, SignalCatcherTooltipEvents::onItemTooltip);
        }

        if (PROJECTE) {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityItemPickupEvent.class, addEMCEvents::onItemPickup);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        PatreonBenefitsFix.logRepositoryMode();
        event.enqueueWork(this::startPatreonSyncAsync);
    }

    private void startPatreonSyncAsync() {
        if (!PATREON_SYNC_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                PatreonBenefits.loadBenefits();
                PatreonBenefitsFix.readFields();
                PatreonBenefitsFix.SpecialForm.loadBenefits();
            } catch (Exception e) {
                LOGGER.warn("Patreon content sync failed in background task", e);
            }
        }, "changede-patreon-sync");
        worker.setDaemon(true);
        worker.start();
    }
}
