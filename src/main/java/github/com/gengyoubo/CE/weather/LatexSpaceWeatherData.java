package github.com.gengyoubo.CE.weather;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.ServerLevelData;

/** Weather belongs to the latex dimension; the remaining world data stays shared. */
public final class LatexSpaceWeatherData extends SavedData {
    public static final String ID = "changede_latex_space_weather";
    private int clearTime;
    private int rainTime;
    private int thunderTime;
    private boolean raining;
    private boolean thundering;

    public LatexSpaceWeatherData(ServerLevelData initial) {
        clearTime = initial.getClearWeatherTime();
        rainTime = initial.getRainTime();
        thunderTime = initial.getThunderTime();
        raining = initial.isRaining();
        thundering = initial.isThundering();
        setDirty();
    }

    private LatexSpaceWeatherData(CompoundTag tag) {
        clearTime = tag.getInt("ClearTime");
        rainTime = tag.getInt("RainTime");
        thunderTime = tag.getInt("ThunderTime");
        raining = tag.getBoolean("Raining");
        thundering = tag.getBoolean("Thundering");
    }

    public static LatexSpaceWeatherData load(CompoundTag tag) {
        return new LatexSpaceWeatherData(tag);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("ClearTime", clearTime);
        tag.putInt("RainTime", rainTime);
        tag.putInt("ThunderTime", thunderTime);
        tag.putBoolean("Raining", raining);
        tag.putBoolean("Thundering", thundering);
        return tag;
    }

    public int getClearTime() { return clearTime; }
    public int getRainTime() { return rainTime; }
    public int getThunderTime() { return thunderTime; }
    public boolean isRaining() { return raining; }
    public boolean isThundering() { return thundering; }

    public void setClearTime(int value) {
        if (clearTime != value) { clearTime = value; setDirty(); }
    }
    public void setRainTime(int value) {
        if (rainTime != value) { rainTime = value; setDirty(); }
    }
    public void setThunderTime(int value) {
        if (thunderTime != value) { thunderTime = value; setDirty(); }
    }
    public void setRaining(boolean value) {
        if (raining != value) { raining = value; setDirty(); }
    }
    public void setThundering(boolean value) {
        if (thundering != value) { thundering = value; setDirty(); }
    }
}
