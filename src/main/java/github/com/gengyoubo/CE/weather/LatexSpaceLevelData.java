package github.com.gengyoubo.CE.weather;

import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;

/** DerivedLevelData normally ignores all weather setters in custom dimensions. */
public final class LatexSpaceLevelData extends DerivedLevelData {
    private final LatexSpaceWeatherData weather;

    public LatexSpaceLevelData(WorldData world, ServerLevelData original, LatexSpaceWeatherData weather) {
        super(world, original);
        this.weather = weather;
    }

    @Override public int getClearWeatherTime() { return weather.getClearTime(); }
    @Override public void setClearWeatherTime(int value) { weather.setClearTime(value); }
    @Override public int getRainTime() { return weather.getRainTime(); }
    @Override public void setRainTime(int value) { weather.setRainTime(value); }
    @Override public int getThunderTime() { return weather.getThunderTime(); }
    @Override public void setThunderTime(int value) { weather.setThunderTime(value); }
    @Override public boolean isRaining() { return weather.isRaining(); }
    @Override public void setRaining(boolean value) { weather.setRaining(value); }
    @Override public boolean isThundering() { return weather.isThundering(); }
    @Override public void setThundering(boolean value) { weather.setThundering(value); }
}
