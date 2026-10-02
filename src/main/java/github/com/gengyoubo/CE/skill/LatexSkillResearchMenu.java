package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.init.CEBlock;
import github.com.gengyoubo.CE.BlockEntity.LatexSkillResearchBlockEntity;
import github.com.gengyoubo.CE.LP.world.Menu.CEMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Materials come from the player's backpack; the station holds WLP, the account holds progress. */
public final class LatexSkillResearchMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Player player;
    private final BlockPos position;
    public LatexSkillResearchMenu(int id,Inventory inventory,FriendlyByteBuf buffer) { this(id,inventory,buffer.readBlockPos()); }
    public LatexSkillResearchMenu(int id,Inventory inventory,BlockPos pos) {
        super(CEMenus.LATEX_SKILL_RESEARCH.get(),id);
        access=ContainerLevelAccess.create(inventory.player.level(),pos);
        player=inventory.player;position=pos;
    }
    public LatexSkillResearchBlockEntity station() {
        return player.level().getBlockEntity(position) instanceof LatexSkillResearchBlockEntity station ? station : null;
    }
    @Override public boolean stillValid(Player player) { return stillValid(access,player,CEBlock.LATEX_SKILL_RESEARCH_TABLE.get()); }
    @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
    public static boolean isResearching(Player player) {
        return player.containerMenu instanceof LatexSkillResearchMenu menu && menu.stillValid(player);
    }
}
