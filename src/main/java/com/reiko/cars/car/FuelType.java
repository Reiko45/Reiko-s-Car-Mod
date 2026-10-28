package com.reiko.cars.car;

import com.reiko.cars.ReikoCars;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * The two kinds of fuel: gasoline (BuildCraft's light fuels, and other mods' gasoline, ethanol and the like) and
 * diesel (BuildCraft's heavy fuels, Dense Fuel and the Mixed Heavy Fuels it's refined from, and other mods' diesel and
 * biodiesel). Each is a fluid tag, {@code reikocars:gasoline} and {@code reikocars:diesel}, so data packs can move
 * fluids between them. Each car runs on one kind ({@link CarType#fuel()}) and each pump sells one kind.
 */
public enum FuelType {
    GASOLINE("gas_pump", 0xFFC62828, 90_000),
    DIESEL("diesel_pump", 0xFF2E7D32, 360_000);

    private final String id = name().toLowerCase(Locale.ROOT);
    private final TagKey<Fluid> tag = FluidTags.create(ReikoCars.id(id));
    private final String pumpId;
    /** The colour of the fuel's name on a pump's sign. */
    public final int signColor;
    /**
     * MJ in a bucket of BuildCraft's own fuel of this kind (light fuel, dense fuel), the unit car tanks are measured in.
     */
    public final double bucketEnergy;

    FuelType(String pumpId, int signColor, double bucketEnergy) {
        this.pumpId = pumpId;
        this.signColor = signColor;
        this.bucketEnergy = bucketEnergy;
    }

    /** The pump that sells it: {@code gas_pump} or {@code diesel_pump}. */
    public String pumpId() {
        return pumpId;
    }

    public String id() {
        return id;
    }

    public TagKey<Fluid> tag() {
        return tag;
    }

    /** "Gasoline" or "Diesel". */
    public Component displayName() {
        return Component.translatable("fuel.reikocars." + id);
    }

    /**
     * Which kind of fuel {@code stack} is, or null if it isn't one (crude oil, water). Diesel wins when a fluid is in
     * both tags: some mods put every fuel they make in the common {@code c:fuel} tag, which counts as gasoline.
     */
    @Nullable
    public static FuelType of(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(DIESEL.tag)) {
            return DIESEL;
        }
        return stack.is(GASOLINE.tag) ? GASOLINE : null;
    }

    public boolean matches(FluidStack stack) {
        return of(stack) == this;
    }
}
