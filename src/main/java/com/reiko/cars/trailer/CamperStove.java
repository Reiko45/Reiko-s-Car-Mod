package com.reiko.cars.trailer;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import org.jetbrains.annotations.Nullable;

/**
 * A camper's stove: it cooks food as a smoker does (anything a smoker cooks, as fast), burning any furnace fuel, but
 * only while the camper is parked. Hitched up and rolling, the flame's out and the cooking waits.
 */
public final class CamperStove {
    public static final int FOOD = 0;
    public static final int FUEL = 1;
    public static final int COOKED = 2;
    /** The menu's numbers: the flame's ticks left and its fuel's total, the cooking's progress and total, and parked. */
    public static final int DATA_COUNT = 5;

    private final TrailerEntity camper;
    private final SimpleContainer items;
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmokingRecipe> recipes = RecipeManager.createCheck(RecipeType.SMOKING);
    private int litTime;
    private int litDuration;
    private int cookTime;
    private int cookTotal;

    CamperStove(TrailerEntity camper) {
        this.camper = camper;
        this.items = new SimpleContainer(3) {
            @Override
            public boolean stillValid(Player player) {
                return camper.isAlive() && player.distanceToSqr(camper) < 100.0;
            }
        };
    }

    public SimpleContainer items() {
        return items;
    }

    /** The flame's alight. */
    public boolean lit() {
        return litTime > 0;
    }

    /** The menu's view of the stove's numbers (server side). */
    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> litTime;
                    case 1 -> litDuration;
                    case 2 -> cookTime;
                    case 3 -> cookTotal;
                    default -> camper.isPassenger() ? 0 : 1;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    /** What {@code food} cooks into on the stove, or null if it doesn't. */
    @Nullable
    public RecipeHolder<SmokingRecipe> recipeFor(ItemStack food) {
        return food.isEmpty() ? null : recipes.getRecipeFor(new SingleRecipeInput(food), camper.level()).orElse(null);
    }

    /** One tick. {@code parked}: whether it can cook at all. Server side. */
    void tick(boolean parked) {
        if (!parked) {
            return; // hitched up: the flame's out until it's parked again, and nothing burns
        }
        boolean changed = false;
        if (litTime > 0) {
            litTime--;
        }
        ItemStack food = items.getItem(FOOD);
        RecipeHolder<SmokingRecipe> recipe = recipeFor(food);
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(new SingleRecipeInput(food), camper.level().registryAccess());
        boolean canCook = !result.isEmpty() && fits(result);
        if (litTime <= 0 && canCook) {
            ItemStack fuel = items.getItem(FUEL);
            int burn = fuel.getBurnTime(RecipeType.SMOKING);
            if (burn > 0) {
                litTime = litDuration = burn;
                ItemStack remainder = fuel.getCraftingRemainingItem();
                fuel.shrink(1);
                if (fuel.isEmpty() && !remainder.isEmpty()) {
                    items.setItem(FUEL, remainder); // a lava bucket leaves its bucket
                }
                changed = true;
            }
        }
        if (litTime > 0 && canCook) {
            cookTotal = recipe.value().getCookingTime();
            if (++cookTime >= cookTotal) {
                cookTime = 0;
                ItemStack cooked = items.getItem(COOKED);
                if (cooked.isEmpty()) {
                    items.setItem(COOKED, result.copy());
                } else {
                    cooked.grow(result.getCount());
                }
                food.shrink(1);
                changed = true;
            }
        } else if (!canCook) {
            cookTime = 0;
        } else {
            cookTime = Math.max(0, cookTime - 2); // out of fuel: it cools off
        }
        if (changed) {
            items.setChanged();
        }
    }

    private boolean fits(ItemStack result) {
        ItemStack cooked = items.getItem(COOKED);
        return cooked.isEmpty() || (ItemStack.isSameItemSameComponents(cooked, result)
                && cooked.getCount() + result.getCount() <= Math.min(cooked.getMaxStackSize(), items.getMaxStackSize()));
    }

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int slot = 0; slot < items.getContainerSize(); slot++) {
            ItemStack stack = items.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) slot);
                list.add(stack.save(registries, entry));
            }
        }
        tag.put("Items", list);
        tag.putInt("Lit", litTime);
        tag.putInt("LitDuration", litDuration);
        tag.putInt("Cook", cookTime);
        tag.putInt("CookTotal", cookTotal);
        return tag;
    }

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        items.clearContent();
        for (Tag t : tag.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            int slot = entry.getByte("Slot");
            if (slot >= 0 && slot < items.getContainerSize()) {
                ItemStack.parse(registries, entry).ifPresent(stack -> items.setItem(slot, stack));
            }
        }
        litTime = tag.getInt("Lit");
        litDuration = tag.getInt("LitDuration");
        cookTime = tag.getInt("Cook");
        cookTotal = tag.getInt("CookTotal");
    }
}
