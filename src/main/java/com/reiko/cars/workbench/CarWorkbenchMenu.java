package com.reiko.cars.workbench;

import com.reiko.cars.car.CarItem;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModMenus;
import com.reiko.cars.registry.ModRecipes;
import com.reiko.cars.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * The Car Workbench's menu: eight part slots laid out like a car seen from above (a wheel at each corner, the engine
 * at the front, the chassis in the middle, the body and interior beside it) and the finished car.
 *
 * <p>The screen lists every car the workbench can build. Picking one (a menu button click, so it works like the
 * stonecutter's list) shows its parts in the empty slots and moves any of them the player carries into place.
 * The car that comes out is whatever the parts make, whichever car is picked.
 */
public final class CarWorkbenchMenu extends AbstractContainerMenu {
    public static final int PARTS = CarAssemblyRecipe.SLOTS;
    public static final int RESULT = PARTS;
    private static final int INVENTORY_START = RESULT + 1;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int INVENTORY_END = HOTBAR_START + 9;

    /** Where each part slot's item sits on the screen, by slot: chassis, body, interior, engine, then the wheels. */
    public static final int[][] PART_POSITIONS = {
            {34, 68}, {12, 68}, {56, 68}, {34, 46},
            {12, 46}, {56, 46}, {12, 90}, {56, 90}};
    public static final int RESULT_X = 212;
    public static final int RESULT_Y = 70;
    public static final int INVENTORY_X = 38;
    public static final int INVENTORY_Y = 130;

    private final ContainerLevelAccess access;
    private final Level level;
    private final List<RecipeHolder<CarAssemblyRecipe>> recipes;
    private final DataSlot selected = DataSlot.standalone();
    private final ResultContainer result = new ResultContainer();
    private final SimpleContainer parts = new SimpleContainer(PARTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    /** The client's copy. */
    public CarWorkbenchMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public CarWorkbenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.CAR_WORKBENCH.get(), id);
        this.access = access;
        this.level = inventory.player.level();
        this.recipes = sortedRecipes(level);
        for (int slot = 0; slot < PARTS; slot++) {
            addSlot(new PartSlot(slot));
        }
        addSlot(new AssembledSlot(inventory.player));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
        addDataSlot(selected);
    }

    /**
     * Every car recipe, the mod's cars first in their usual order, then any others by name. The client and the
     * server sort the same list the same way, so a button number means the same car on both.
     */
    private static List<RecipeHolder<CarAssemblyRecipe>> sortedRecipes(Level level) {
        List<RecipeHolder<CarAssemblyRecipe>> list = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(ModRecipes.CAR_ASSEMBLY.get()));
        list.sort(Comparator.<RecipeHolder<CarAssemblyRecipe>>comparingInt(holder ->
                        holder.value().result().getItem() instanceof CarItem car ? car.carType().ordinal() : Integer.MAX_VALUE)
                .thenComparing(holder -> holder.id().toString()));
        return List.copyOf(list);
    }

    public List<RecipeHolder<CarAssemblyRecipe>> recipes() {
        return recipes;
    }

    /** The picked car's index in {@link #recipes()}. */
    public int selectedIndex() {
        return selected.get();
    }

    public Optional<CarAssemblyRecipe> selectedRecipe() {
        int index = selected.get();
        return index >= 0 && index < recipes.size() ? Optional.of(recipes.get(index).value()) : Optional.empty();
    }

    /** Whether any car takes {@code stack} in part slot {@code slot}. */
    public boolean fits(int slot, ItemStack stack) {
        for (RecipeHolder<CarAssemblyRecipe> holder : recipes) {
            if (holder.value().ingredient(slot).test(stack)) {
                return true;
            }
        }
        return false;
    }

    private CarAssemblyRecipe.Input input() {
        List<ItemStack> items = new ArrayList<>(PARTS);
        for (int slot = 0; slot < PARTS; slot++) {
            items.add(parts.getItem(slot));
        }
        return new CarAssemblyRecipe.Input(items);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == parts) {
            access.execute((level, pos) -> updateResult());
        }
    }

    /** Server only: shows the car the parts make, and picks it in the list. */
    private void updateResult() {
        CarAssemblyRecipe.Input input = input();
        Optional<RecipeHolder<CarAssemblyRecipe>> match = level.getRecipeManager().getRecipeFor(ModRecipes.CAR_ASSEMBLY.get(), input, level);
        result.setRecipeUsed(match.orElse(null));
        result.setItem(0, match.map(holder -> holder.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY));
        match.ifPresent(holder -> {
            for (int index = 0; index < recipes.size(); index++) {
                if (recipes.get(index).id().equals(holder.id())) {
                    selected.set(index);
                }
            }
        });
    }

    /** A click on car {@code id} in the list: picks it and, on the server, moves its parts into place. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= recipes.size()) {
            return false;
        }
        selected.set(id);
        if (!player.level().isClientSide) {
            loadParts(player, recipes.get(id).value());
        }
        return true;
    }

    /**
     * Hands back any part that doesn't belong to {@code recipe}, then fills each empty slot with a matching part from
     * the player's inventory, if they have one.
     */
    private void loadParts(Player player, CarAssemblyRecipe recipe) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < PARTS; slot++) {
            ItemStack stack = parts.getItem(slot);
            if (!stack.isEmpty() && !recipe.ingredient(slot).test(stack)) {
                parts.setItem(slot, ItemStack.EMPTY);
                inventory.placeItemBackInInventory(stack);
            }
        }
        for (int slot = 0; slot < PARTS; slot++) {
            if (!parts.getItem(slot).isEmpty()) {
                continue;
            }
            for (ItemStack carried : inventory.items) {
                if (!carried.isEmpty() && recipe.ingredient(slot).test(carried)) {
                    parts.setItem(slot, carried.split(1));
                    break;
                }
            }
        }
        inventory.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index == RESULT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, before);
        } else if (index < PARTS) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // One part per empty slot that takes it, so a stack of tires fills all four wheels at once.
            boolean placed = false;
            for (int part = 0; part < PARTS && !stack.isEmpty(); part++) {
                Slot partSlot = slots.get(part);
                if (!partSlot.hasItem() && partSlot.mayPlace(stack)) {
                    partSlot.setByPlayer(stack.split(1));
                    placed = true;
                }
            }
            if (placed) {
                if (stack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                return ItemStack.EMPTY; // done: don't let the shift-click loop carry on and shuffle the rest
            }
            int from = index < HOTBAR_START ? HOTBAR_START : INVENTORY_START;
            int to = index < HOTBAR_START ? INVENTORY_END : HOTBAR_START;
            if (!moveItemStackTo(stack, from, to, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == before.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CAR_WORKBENCH.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, parts));
    }

    /** One part, one at a time, and only parts that some car takes in this slot. */
    private final class PartSlot extends Slot {
        private final int part;

        PartSlot(int part) {
            super(parts, part, PART_POSITIONS[part][0], PART_POSITIONS[part][1]);
            this.part = part;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return fits(part, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** The finished car. Taking it uses up one of each part. */
    private final class AssembledSlot extends Slot {
        private final Player player;
        private int removeCount;

        AssembledSlot(Player player) {
            super(result, 0, RESULT_X, RESULT_Y);
            this.player = player;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            if (hasItem()) {
                removeCount += Math.min(amount, getItem().getCount());
            }
            return super.remove(amount);
        }

        @Override
        protected void onQuickCraft(ItemStack stack, int amount) {
            removeCount += amount;
            checkTakeAchievements(stack);
        }

        @Override
        protected void checkTakeAchievements(ItemStack stack) {
            if (removeCount > 0) {
                stack.onCraftedBy(player.level(), player, removeCount);
            }
            removeCount = 0;
        }

        @Override
        public void onTake(Player taker, ItemStack stack) {
            checkTakeAchievements(stack);
            for (int slot = 0; slot < PARTS; slot++) {
                parts.removeItem(slot, 1);
            }
            access.execute((level, pos) -> {
                level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.9F);
                level.playSound(null, pos, ModSounds.ENGINE_START.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
            });
            super.onTake(taker, stack);
        }
    }
}
