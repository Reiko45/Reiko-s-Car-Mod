package com.reiko.cars.sale;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One car's sale screen. For its owner (with a For Sale Sign): the price to ask, or take it off sale. For anyone else
 * (right-clicking a car that's for sale): what it is, its condition and fuel, and the price, paid by card or cash.
 */
public final class CarSaleMenu extends AbstractContainerMenu {
    @Nullable
    private final CarEntity car; // null on the client
    private final boolean selling;
    private final String nameKey;
    private final String ownerName;
    private final long price;
    private final int condition;
    private final int fuel;
    private final int color;

    public CarSaleMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBoolean(), buf.readUtf(128), buf.readUtf(64), buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readInt());
    }

    private CarSaleMenu(int containerId, @Nullable CarEntity car, boolean selling, String nameKey, String ownerName, long price,
                        int condition, int fuel, int color) {
        super(ModMenus.CAR_SALE.get(), containerId);
        this.car = car;
        this.selling = selling;
        this.nameKey = nameKey;
        this.ownerName = ownerName;
        this.price = price;
        this.condition = condition;
        this.fuel = fuel;
        this.color = color;
    }

    /** Opens the screen: the selling side for the car's owner, the buying side for anyone else (admins included). */
    public static void open(ServerPlayer player, CarEntity car) {
        boolean selling = car.isOwner(player);
        Component title = Component.translatable(selling ? "container.reikocars.sell_car" : "container.reikocars.buy_car", car.kindName());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, car), title), buf -> {
            buf.writeBoolean(selling);
            buf.writeUtf(car.nameKey(), 128);
            buf.writeUtf(car.ownerName(), 64);
            buf.writeLong(car.salePrice());
            buf.writeVarInt(Math.round(car.condition().overall(car::hasPart) * 1000));
            buf.writeVarInt(Math.round(car.fuelFraction() * 1000));
            buf.writeInt(car.color());
        });
    }

    public static CarSaleMenu create(int containerId, ServerPlayer player, CarEntity car) {
        return new CarSaleMenu(containerId, car, car.isOwner(player), car.nameKey(), car.ownerName(), car.salePrice(),
                Math.round(car.condition().overall(car::hasPart) * 1000), Math.round(car.fuelFraction() * 1000), car.color());
    }

    @Nullable
    public CarEntity car() {
        return car;
    }

    /** The owner's side: setting the price. */
    public boolean isSelling() {
        return selling;
    }

    public Component carName() {
        return Component.translatable(nameKey);
    }

    public String ownerName() {
        return ownerName;
    }

    /** The asking price, in cents, when the screen opened; -1 if it wasn't for sale. */
    public long price() {
        return price;
    }

    /** Its overall condition, in thousandths. */
    public int condition() {
        return condition;
    }

    /** How full its tank is, in thousandths. */
    public int fuel() {
        return fuel;
    }

    public int color() {
        return color;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return car == null || (car.isAlive() && player.distanceToSqr(car) < 12.0 * 12.0);
    }
}
