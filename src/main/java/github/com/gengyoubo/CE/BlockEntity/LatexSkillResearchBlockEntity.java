package github.com.gengyoubo.CE.BlockEntity;

import github.com.gengyoubo.CE.LP.*;
import github.com.gengyoubo.CE.init.CEBlockEntity;
import github.com.gengyoubo.CE.skill.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

/** Only typed WLP enters this machine; ordinary LP and DLP cannot substitute for it. */
public final class LatexSkillResearchBlockEntity extends BlockEntity implements ILatexTypedEnergyHandler {
    public static final int CAPACITY=100_000;
    private final TypedLatexEnergyStorage energy=new TypedLatexEnergyStorage(LatexEnergyType.WLP,CAPACITY);
    private UUID owner;
    private ResourceLocation project;
    private int secondTicks;
    // Client-only presentation state; never persisted or used for paid research time.
    public int bookTime;
    public float bookRotation,previousBookRotation,bookOpen,previousBookOpen,bookFlip,previousBookFlip;
    private float targetBookRotation,targetBookFlip,bookFlipSpeed;
    public LatexSkillResearchBlockEntity(BlockPos pos,BlockState state) { super(CEBlockEntity.LATEX_SKILL_RESEARCH.get(),pos,state); }
    public boolean owns(Player player,ResourceLocation id) {
        return player.getUUID().equals(owner) && id.equals(project) && SkillResearchAccounts.bound(player,id,this);
    }
    public boolean canBind(Player player) {
        if(owner==null || player.getUUID().equals(owner))return true;
        var current=level==null || level.getServer()==null ? null : level.getServer().getPlayerList().getPlayer(owner);
        return current!=null && (project==null || !SkillResearchAccounts.bound(current,project,this));
    }
    public void bind(UUID player,ResourceLocation id) { owner=player;project=id;secondTicks=0;setChanged(); }
    public void consume(int amount) { energy.extract(amount);setChanged(); }
    public void tickBook() {
        if(level==null || !level.isClientSide)return;
        previousBookRotation=bookRotation;previousBookOpen=bookOpen;previousBookFlip=bookFlip;
        var player=level.getNearestPlayer(worldPosition.getX()+0.5,worldPosition.getY()+0.5,
                worldPosition.getZ()+0.5,3.0,false);
        if(player!=null) {
            targetBookRotation=(float)Mth.atan2(player.getZ()-worldPosition.getZ()-0.5,
                    player.getX()-worldPosition.getX()-0.5);
            bookOpen+=0.1F;
            if(bookOpen<0.5F || level.random.nextInt(40)==0) {
                targetBookFlip+=(level.random.nextBoolean() ? 1 : -1)*(1+level.random.nextInt(3));
            }
        } else { targetBookRotation+=0.02F;bookOpen-=0.1F; }
        // Keep interpolation continuous across the -pi / +pi boundary.
        float difference=Mth.wrapDegrees((targetBookRotation-bookRotation)*Mth.RAD_TO_DEG)*Mth.DEG_TO_RAD;
        bookRotation+=difference*0.4F;
        targetBookRotation=bookRotation+Mth.wrapDegrees((targetBookRotation-bookRotation)*Mth.RAD_TO_DEG)*Mth.DEG_TO_RAD;
        bookOpen=Mth.clamp(bookOpen,0,1);
        float desiredSpeed=Mth.clamp((targetBookFlip-bookFlip)*0.4F,-0.2F,0.2F);
        bookFlipSpeed+=(desiredSpeed-bookFlipSpeed)*0.9F;
        bookFlip+=bookFlipSpeed;bookTime++;
    }
    public void tick() {
        if(level==null || level.isClientSide)return;
        // Existing LP pipes feed the converter; this station draws its separate WLP output.
        for(Direction direction:Direction.values()) {
            int space=CAPACITY-energy.getEnergyStored();if(space<=0)break;
            if(level.getBlockEntity(worldPosition.relative(direction)) instanceof ILatexTypedEnergyHandler source
                    && source.getEnergyType()==LatexEnergyType.WLP) {
                int extracted=source.extractTypedEnergy(LatexEnergyType.WLP,Math.min(1000,space),direction.getOpposite());
                receiveTypedEnergy(LatexEnergyType.WLP,extracted);
            }
        }
        if(owner==null || project==null) { secondTicks=0;return; }
        var player=level.getServer().getPlayerList().getPlayer(owner);
        if(player==null) { secondTicks=0;return; } // No wall-clock or offline catch-up.
        if(!SkillResearchAccounts.bound(player,project,this)) { owner=null;project=null;secondTicks=0;setChanged();return; }
        if(++secondTicks>=20) { secondTicks=0;SkillResearchAccounts.paidSecond(player,project,this); }
    }
    @Override public LatexEnergyType getEnergyType() { return LatexEnergyType.WLP; }
    @Override public int receiveTypedEnergy(LatexEnergyType type,int amount) {
        if(type!=LatexEnergyType.WLP)return 0;int accepted=energy.receive(amount);if(accepted>0)setChanged();return accepted;
    }
    @Override public int extractTypedEnergy(LatexEnergyType type,int amount) { return 0; }
    @Override public int getTypedEnergyStored() { return energy.getEnergyStored(); }
    @Override public int getTypedEnergyCapacity() { return CAPACITY; }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putInt("WLP",energy.getEnergyStored());
        if(owner!=null && project!=null) { tag.putUUID("ResearchOwner",owner);tag.putString("Project",project.toString()); }
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);energy.setEnergy(tag.getInt("WLP"));
        owner=tag.hasUUID("ResearchOwner") ? tag.getUUID("ResearchOwner") : null;
        project=ResourceLocation.tryParse(tag.getString("Project"));secondTicks=0;
    }
}
