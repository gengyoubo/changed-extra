package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.E;

import github.com.gengyoubo.CE.LP.ILatexTypedEnergyHandler;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.TypedLatexEnergyStorage;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TypedEnergyPipeBlockEntity extends BasePipeBlockEntity implements ILatexTypedEnergyHandler {
    public static final int CAPACITY=1000,MAX_TRANSFER=100;
    private final TypedLatexEnergyStorage energy;
    private Direction lastInput;
    private long outputTick=Long.MIN_VALUE;
    private int extractedThisTick;

    public TypedEnergyPipeBlockEntity(BlockPos pos,BlockState state,LatexEnergyType type) {
        super(type==LatexEnergyType.WLP ? CELPBlockEntity.WLP_PIPE.get() : CELPBlockEntity.DLP_PIPE.get(),
                pos,state,TransportType.ENERGY);
        if(type==LatexEnergyType.LP)throw new IllegalArgumentException("Use the existing LP pipe for LP");
        energy=new TypedLatexEnergyStorage(type,CAPACITY);
    }
    @Override protected boolean canConnectToPipe(BasePipeBlockEntity other,Direction side) {
        return other instanceof TypedEnergyPipeBlockEntity pipe && pipe.getEnergyType()==getEnergyType();
    }
    @Override protected boolean canConnectToMachine(BlockEntity other,Direction side) {
        return other instanceof ILatexTypedEnergyHandler handler && handler.getEnergyType()==getEnergyType();
    }
    private BlockEntity neighbor(Direction side) {
        BlockPos pos=worldPosition.relative(side);
        return level!=null && level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4) ? level.getBlockEntity(pos) : null;
    }
    @Override protected void transfer() {
        if(level==null || level.isClientSide)return;
        int pullBudget=MAX_TRANSFER;
        for(Direction side:Direction.values()) {
            int space=CAPACITY-energy.getEnergyStored();
            if(space<=0 || pullBudget<=0)break;
            if(!getConnectionMode(side).canSource())continue;
            var target=neighbor(side);
            if(target instanceof TypedEnergyPipeBlockEntity)continue;
            if(target instanceof ILatexTypedEnergyHandler handler && handler.getEnergyType()==getEnergyType()) {
                int extracted=handler.extractTypedEnergy(getEnergyType(),Math.min(space,pullBudget),side.getOpposite());
                pullBudget-=receiveTypedEnergy(getEnergyType(),extracted,side);
            }
        }
        // Feed machines before forwarding to pipes; never immediately send back through the input port.
        for(boolean toPipe:new boolean[]{false,true}) {
            for(Direction side:Direction.values()) {
                if(side==lastInput || !getConnectionMode(side).canSink())continue;
                var target=neighbor(side);
                if((target instanceof TypedEnergyPipeBlockEntity)!=toPipe)continue;
                if(target instanceof ILatexTypedEnergyHandler handler && handler.getEnergyType()==getEnergyType()) {
                    int offered=Math.min(energy.getEnergyStored(),outputBudget());
                    if(offered<=0)return;
                    int accepted=Math.max(0,Math.min(offered,handler.receiveTypedEnergy(getEnergyType(),offered,side.getOpposite())));
                    extractTypedEnergy(getEnergyType(),accepted,side);
                }
            }
        }
    }
    private int outputBudget() {
        long tick=level==null ? 0 : level.getGameTime();
        if(tick!=outputTick) { outputTick=tick;extractedThisTick=0; }
        return MAX_TRANSFER-extractedThisTick;
    }
    @Override public LatexEnergyType getEnergyType() { return energy.getType(); }
    @Override public int receiveTypedEnergy(LatexEnergyType type,int amount) { return receiveTypedEnergy(type,amount,null); }
    @Override public int receiveTypedEnergy(LatexEnergyType type,int amount,Direction side) {
        if(type!=getEnergyType() || side!=null && !getConnectionMode(side).canSource())return 0;
        int accepted=energy.receive(amount);
        if(accepted>0) { lastInput=side;setChanged(); }
        return accepted;
    }
    @Override public int extractTypedEnergy(LatexEnergyType type,int amount) { return extractTypedEnergy(type,amount,null); }
    @Override public int extractTypedEnergy(LatexEnergyType type,int amount,Direction side) {
        if(type!=getEnergyType() || side!=null && !getConnectionMode(side).canSink())return 0;
        int extracted=energy.extract(Math.min(Math.max(amount,0),outputBudget()));
        if(extracted>0) {
            extractedThisTick+=extracted;
            if(energy.getEnergyStored()==0)lastInput=null;
            setChanged();
        }
        return extracted;
    }
    @Override public int getTypedEnergyStored() { return energy.getEnergyStored(); }
    @Override public int getTypedEnergyCapacity() { return CAPACITY; }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putInt("TypedEnergy",energy.getEnergyStored());
        if(lastInput!=null)tag.putString("PipeInputDirection",lastInput.getName());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);energy.setEnergy(tag.getInt("TypedEnergy"));
        lastInput=Direction.byName(tag.getString("PipeInputDirection"));
        outputTick=Long.MIN_VALUE;extractedThisTick=0;
    }
}
