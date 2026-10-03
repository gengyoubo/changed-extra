package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class LatexCampfireBlockEntity extends BlockEntity {
    UUID settlementId;
    String faction = "white";
    boolean active;

    public LatexCampfireBlockEntity(BlockPos pos, BlockState state) { super(LatexCampfireCompat.CORE.get(), pos, state); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (settlementId != null) tag.putUUID("SettlementId", settlementId);
        tag.putString("Faction", faction); tag.putBoolean("Active", active);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        settlementId = tag.hasUUID("SettlementId") ? tag.getUUID("SettlementId") : null;
        faction = tag.getString("Faction"); active = tag.getBoolean("Active");
    }
    void bind(LatexSettlementData.Settlement camp) {
        settlementId = camp.id; faction = camp.faction; active = camp.active; setChanged();
        if (level != null && getBlockState().getValue(LatexCampfireBlock.LIT) != active)
            level.setBlock(worldPosition, getBlockState().setValue(LatexCampfireBlock.LIT, active), 3);
    }
}
