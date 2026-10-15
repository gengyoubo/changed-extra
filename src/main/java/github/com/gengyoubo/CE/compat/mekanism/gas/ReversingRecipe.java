package github.com.gengyoubo.CE.compat.mekanism.gas;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.math.BigDecimal;
import mekanism.api.SerializerHelper;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.GasToGasRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.GasStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ReversingRecipe extends GasToGasRecipe {
    private final int ticks, whiteRate, darkRate;
    private final String signature;
    public ReversingRecipe(ResourceLocation id, GasStackIngredient input, GasStack output, int ticks, int whiteRate, int darkRate) {
        super(id, input, output);
        if (ticks <= 0 || whiteRate <= 0 || darkRate <= 0 || whiteRate > 2_000_000 || darkRate > 2_000_000
                || output.getAmount() > ReverserBlockEntity.TANK_CAPACITY
                || input.getRepresentations().stream().anyMatch(stack -> stack.getAmount() <= 0 || stack.getAmount() > ReverserBlockEntity.TANK_CAPACITY))
            throw new JsonSyntaxException("Invalid reversing quantity, duration or dual energy rate: " + id);
        this.ticks = ticks; this.whiteRate = whiteRate; this.darkRate = darkRate;
        signature = id + "|" + input.serialize() + "|" + output.write(new net.minecraft.nbt.CompoundTag()) + "|" + ticks + "|" + whiteRate + "|" + darkRate;
    }
    public int ticks() { return ticks; }
    public int whiteRate() { return whiteRate; }
    public int darkRate() { return darkRate; }
    public String signature() { return signature; }
    @Override public RecipeType<ReversingRecipe> getType() { return GasCompat.REVERSING_TYPE.get(); }
    @Override public RecipeSerializer<ReversingRecipe> getSerializer() { return GasCompat.REVERSING_SERIALIZER.get(); }
    @Override public String getGroup() { return "changede:reversing"; }
    @Override public ItemStack getToastSymbol() { return GasCompat.REVERSER.getItemStack(); }
    @Override public void write(FriendlyByteBuf buffer) {
        super.write(buffer); buffer.writeVarInt(ticks); buffer.writeVarInt(whiteRate); buffer.writeVarInt(darkRate);
    }
    public static final class Serializer implements RecipeSerializer<ReversingRecipe> {
        @Override public ReversingRecipe fromJson(ResourceLocation id, JsonObject json) {
            var input = json.get("input");
            if (input == null || (!input.isJsonObject() && !input.isJsonArray()))
                throw new JsonSyntaxException("Reversing input must be a gas ingredient: " + id);
            int ticks;
            try { ticks = new BigDecimal(GsonHelper.getAsString(json, "time")).multiply(BigDecimal.valueOf(20)).intValueExact(); }
            catch (ArithmeticException | NumberFormatException error) { throw new JsonSyntaxException("Reversing time must be positive seconds in exact 0.05 s steps: " + id, error); }
            return new ReversingRecipe(id, IngredientCreatorAccess.gas().deserialize(input), SerializerHelper.getGasStack(json, "output"),
                    ticks, GsonHelper.getAsInt(json, "wlp_per_second"), GsonHelper.getAsInt(json, "dlp_per_second"));
        }
        @Override public ReversingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ReversingRecipe(id, IngredientCreatorAccess.gas().read(buffer), GasStack.readFromPacket(buffer), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ReversingRecipe recipe) { recipe.write(buffer); }
    }
}
