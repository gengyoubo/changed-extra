package github.com.gengyoubo.CE.LP.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.math.BigDecimal;

/** One item plus a specified fluid volume; durations and power are expressed in seconds. */
public final class IngotFillingRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final Fluid fluid;
    private final int fluidAmount,ticks,lpPerSecond;
    private final ItemStack result;
    public IngotFillingRecipe(ResourceLocation id,Ingredient ingredient,Fluid fluid,int amount,
                              ItemStack result,int ticks,int lpPerSecond) {
        this.id=id;this.ingredient=ingredient;this.fluid=fluid;fluidAmount=amount;
        this.result=result.copy();this.ticks=ticks;this.lpPerSecond=lpPerSecond;
    }
    public boolean acceptsInput(ItemStack stack) { return !stack.isEmpty() && ingredient.test(stack); }
    public boolean acceptsFluid(FluidStack stack) { return !stack.isEmpty() && stack.getFluid()==fluid; }
    public FluidStack getFluid() { return new FluidStack(fluid,fluidAmount); }
    public int getFluidAmount() { return fluidAmount; }
    public int getFluidPerSecond() { return (int)((long)fluidAmount*20/ticks); }
    public int getProcessTicks() { return ticks; }
    public double getSeconds() { return ticks/20.0; }
    public int getLpPerSecond() { return lpPerSecond; }
    @Override public boolean matches(SimpleContainer input,Level level) { return input.getContainerSize()>0 && acceptsInput(input.getItem(0)); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY,ingredient); }
    @Override public ItemStack assemble(SimpleContainer input,RegistryAccess access) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return CELPRecipes.INGOT_FILLING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return CELPRecipes.INGOT_FILLING_TYPE; }
    public static final class Serializer implements RecipeSerializer<IngotFillingRecipe> {
        @Override public IngotFillingRecipe fromJson(ResourceLocation id,JsonObject json) {
            var ingredient=Ingredient.fromJson(json.get("ingredient"));
            var fluidJson=GsonHelper.getAsJsonObject(json,"fluid");
            Fluid fluid=ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(GsonHelper.getAsString(fluidJson,"fluid")));
            int amount=GsonHelper.getAsInt(fluidJson,"amount");
            double seconds=GsonHelper.getAsDouble(json,"time");
            int lp=GsonHelper.getAsInt(json,"lp_per_second",200);
            var result=ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json,"result"));
            if(ingredient.isEmpty() || fluid==null || fluid==Fluids.EMPTY || amount<=0 || !Double.isFinite(seconds)
                    || seconds<=0 || seconds*20>Integer.MAX_VALUE || lp<0 || result.isEmpty() || result.getCount()>result.getMaxStackSize())
                throw new JsonSyntaxException("Invalid ingot filling recipe: "+id);
            BigDecimal duration=new BigDecimal(json.get("time").getAsString());
            int ticks;
            try {
                int rate=BigDecimal.valueOf(amount).divide(duration).intValueExact();
                if(rate<=0)throw new ArithmeticException();
            } catch(ArithmeticException error) {
                throw new JsonSyntaxException(id+": "+amount+" mB / "+duration.toPlainString()+" s must be a positive integer mB/s (within integer range)");
            }
            try { ticks=duration.multiply(BigDecimal.valueOf(20)).intValueExact(); }
            catch(ArithmeticException error) { throw new JsonSyntaxException(id+": time must be an exact multiple of 0.05 s"); }
            return new IngotFillingRecipe(id,ingredient,fluid,amount,result,ticks,lp);
        }
        @Override public IngotFillingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) {
            var ingredient=Ingredient.fromNetwork(buffer);var fluid=buffer.readFluidStack();
            return new IngotFillingRecipe(id,ingredient,fluid.getFluid(),fluid.getAmount(),buffer.readItem(),buffer.readVarInt(),buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,IngotFillingRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);buffer.writeFluidStack(recipe.getFluid());buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.ticks);buffer.writeVarInt(recipe.lpPerSecond);
        }
    }
}
