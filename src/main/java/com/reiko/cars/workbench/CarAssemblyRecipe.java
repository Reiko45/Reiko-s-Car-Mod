package com.reiko.cars.workbench;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.registry.ModRecipes;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A car put together at a Car Workbench, from a chassis, a body, an interior, an engine and four wheels. Recipes are
 * data files ({@code data/reikocars/recipe/sedan.json} and the rest), so a data pack can change the parts or add more
 * vehicles:
 *
 * <pre>{@code
 * {
 *   "type": "reikocars:car_assembly",
 *   "chassis": {"item": "reikocars:sedan_chassis"},
 *   "body": {"item": "reikocars:sedan_body"},
 *   "interior": {"item": "reikocars:sedan_interior"},
 *   "engine": {"item": "reikocars:car_engine"},
 *   "wheel": {"item": "reikocars:tire"},
 *   "result": {"id": "reikocars:sedan"}
 * }
 * }</pre>
 */
public final class CarAssemblyRecipe implements Recipe<CarAssemblyRecipe.Input> {
    /** The workbench's part slots, in order. */
    public static final int CHASSIS = 0;
    public static final int BODY = 1;
    public static final int INTERIOR = 2;
    public static final int ENGINE = 3;
    /** The first of the four wheel slots: front left, front right, rear left, rear right. */
    public static final int FIRST_WHEEL = 4;
    public static final int SLOTS = 8;

    private final Ingredient chassis;
    private final Ingredient body;
    private final Ingredient interior;
    private final Ingredient engine;
    private final Ingredient wheel;
    private final ItemStack result;

    public CarAssemblyRecipe(Ingredient chassis, Ingredient body, Ingredient interior, Ingredient engine, Ingredient wheel, ItemStack result) {
        this.chassis = chassis;
        this.body = body;
        this.interior = interior;
        this.engine = engine;
        this.wheel = wheel;
        this.result = result;
    }

    /** What goes in part slot {@code slot}. */
    public Ingredient ingredient(int slot) {
        return switch (slot) {
            case CHASSIS -> chassis;
            case BODY -> body;
            case INTERIOR -> interior;
            case ENGINE -> engine;
            default -> wheel;
        };
    }

    public ItemStack result() {
        return result;
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input.size() != SLOTS) {
            return false;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!ingredient(slot).test(input.getItem(slot))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, chassis, body, interior, engine, wheel, wheel, wheel, wheel);
    }

    /** Keeps these out of the vanilla recipe book, which has no tab for them: the workbench shows its own. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.CAR_WORKBENCH.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CAR_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CAR_ASSEMBLY.get();
    }

    /** The eight part slots, in slot order. */
    public record Input(List<ItemStack> parts) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return parts.get(index);
        }

        @Override
        public int size() {
            return parts.size();
        }
    }

    public static final class Serializer implements RecipeSerializer<CarAssemblyRecipe> {
        private static final MapCodec<CarAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("chassis").forGetter(recipe -> recipe.chassis),
                Ingredient.CODEC_NONEMPTY.fieldOf("body").forGetter(recipe -> recipe.body),
                Ingredient.CODEC_NONEMPTY.fieldOf("interior").forGetter(recipe -> recipe.interior),
                Ingredient.CODEC_NONEMPTY.fieldOf("engine").forGetter(recipe -> recipe.engine),
                Ingredient.CODEC_NONEMPTY.fieldOf("wheel").forGetter(recipe -> recipe.wheel),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, CarAssemblyRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CarAssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.chassis,
                Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.body,
                Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.interior,
                Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.engine,
                Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.wheel,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                CarAssemblyRecipe::new);

        @Override
        public MapCodec<CarAssemblyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CarAssemblyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
