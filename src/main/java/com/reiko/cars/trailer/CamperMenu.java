package com.reiko.cars.trailer;

import com.reiko.cars.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/**
 * A camper, inside: its cupboards (rows of nine slots), its stove (food, fuel, and what's cooked), and a button to go
 * to bed.
 */
public final class CamperMenu extends AbstractContainerMenu {
    /** The button that goes to bed. */
    public static final int SLEEP = 0;
    public static final int LEFT = 8;
    public static final int TOP = 18;

    @Nullable
    private final TrailerEntity camper; // null on the client
    private final int rows;
    private final Container cupboards;
    private final Container stove;
    private final ContainerData data;
    private final int stoveTop;
    private final int inventoryTop;

    public CamperMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, buf.readVarInt(), new SimpleContainer(buf.readVarInt() * 9), new SimpleContainer(3),
                new SimpleContainerData(CamperStove.DATA_COUNT));
    }

    private CamperMenu(int containerId, Inventory inventory, @Nullable TrailerEntity camper, int entityId, Container cupboards, Container stove,
            ContainerData data) {
        super(ModMenus.CAMPER.get(), containerId);
        this.camper = camper;
        this.cupboards = cupboards;
        this.stove = stove;
        this.data = data;
        this.rows = cupboards.getContainerSize() / 9;
        this.entityId = entityId;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(cupboards, col + row * 9, LEFT + col * 18, TOP + row * 18));
            }
        }
        this.stoveTop = TOP + rows * 18 + 16;
        addSlot(new Slot(stove, CamperStove.FOOD, 44, stoveTop) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });
        addSlot(new Slot(stove, CamperStove.FUEL, 44, stoveTop + 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getBurnTime(RecipeType.SMOKING) > 0;
            }
        });
        addSlot(new Slot(stove, CamperStove.COOKED, 104, stoveTop + 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.inventoryTop = stoveTop + 66;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, LEFT + col * 18, inventoryTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, LEFT + col * 18, inventoryTop + 58));
        }
        addDataSlots(data);
    }

    private final int entityId;

    public static void open(ServerPlayer player, TrailerEntity camper) {
        int rows = camper.trailerType().cargoRows;
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CamperMenu(id, inventory, camper, camper.getId(), camper.cargo(),
                camper.stove().items(), camper.stove().data()), camper.getDisplayName()), buf -> {
                    buf.writeVarInt(camper.getId());
                    buf.writeVarInt(rows);
                });
    }

    /** The camper's entity id, so the screen can find it. */
    public int entityId() {
        return entityId;
    }

    public int rows() {
        return rows;
    }

    /** Where the stove's slots start, down the screen. */
    public int stoveTop() {
        return stoveTop;
    }

    public int inventoryTop() {
        return inventoryTop;
    }

    /** How much of its fuel the stove's flame has left, 0 to 1. */
    public float flame() {
        int total = data.get(1);
        return total <= 0 ? 0.0F : Math.min(1.0F, data.get(0) / (float) total);
    }

    /** How far along the cooking is, 0 to 1. */
    public float cooking() {
        int total = data.get(3);
        return total <= 0 ? 0.0F : Math.min(1.0F, data.get(2) / (float) total);
    }

    /** Whether the camper's parked, so the stove can cook. */
    public boolean parked() {
        return data.get(4) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == SLEEP && camper != null && player instanceof ServerPlayer serverPlayer) {
            CamperLife.sleep(serverPlayer, camper);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int cupboardEnd = rows * 9;
        int stoveEnd = cupboardEnd + 3;
        int end = slots.size();
        if (index < stoveEnd) {
            // out of the cupboards or the stove, into the player's inventory
            if (!moveItemStackTo(stack, stoveEnd, end, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // from the player: fuel to the stove's fire, food on the stove, anything else into the cupboards
            boolean moved = false;
            if (stack.getBurnTime(RecipeType.SMOKING) > 0) {
                moved = moveItemStackTo(stack, cupboardEnd + CamperStove.FUEL, cupboardEnd + CamperStove.FUEL + 1, false);
            }
            if (!moved && camper != null && camper.stove().recipeFor(stack) != null) {
                moved = moveItemStackTo(stack, cupboardEnd + CamperStove.FOOD, cupboardEnd + CamperStove.FOOD + 1, false);
            }
            if (!moved && !moveItemStackTo(stack, 0, cupboardEnd, false)) {
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
    public boolean stillValid(Player player) {
        return camper == null || (camper.isAlive() && player.distanceToSqr(camper) < 100.0);
    }
}
