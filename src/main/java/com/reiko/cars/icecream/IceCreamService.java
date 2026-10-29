package com.reiko.cars.icecream;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The ice cream truck's shop. Its owner stocks the freezer (sneak + right-click) and prices each kind of item on the
 * shop screen; anyone else right-clicks the truck (the owner, its serving hatch, on the left) to buy one at a time,
 * by card or cash plus sales tax, paid to the owner. Unpriced items aren't for sale.
 */
public final class IceCreamService {
    private IceCreamService() {
    }

    /** What's in the freezer, by kind, with how many of each, in the order they're stored. */
    public static Map<Item, Integer> stock(CarEntity truck) {
        Map<Item, Integer> stock = new LinkedHashMap<>();
        SimpleContainer freezer = truck.bed();
        for (int slot = 0; slot < freezer.getContainerSize(); slot++) {
            ItemStack stack = freezer.getItem(slot);
            if (!stack.isEmpty()) {
                stock.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return stock;
    }

    /** What's on the menu: the kinds in stock that have a price (the owner sees every kind in stock). */
    public static List<Item> menu(CarEntity truck, boolean owner) {
        List<Item> items = new ArrayList<>();
        for (Item item : stock(truck).keySet()) {
            if (owner || truck.shopPrices().containsKey(item)) {
                items.add(item);
            }
        }
        return items;
    }

    /** The price of one {@code item}, in cents, or -1 if it isn't for sale. */
    public static long price(CarEntity truck, Item item) {
        return truck.shopPrices().getOrDefault(item, -1L);
    }

    /**
     * {@code buyer} buys one {@code item} from the truck.
     *
     * @param expected the price the screen showed, before tax
     */
    public static ShopActionResult buy(ServerPlayer buyer, CarEntity truck, Item item, long expected, Payment payment) {
        if (truck.carType() != CarType.ICE_CREAM_TRUCK || truck.owner() == null) {
            return ShopActionResult.fail("icecream.reikocars.closed");
        }
        if (truck.isOwner(buyer)) {
            return ShopActionResult.fail("icecream.reikocars.yours");
        }
        long price = price(truck, item);
        int left = stock(truck).getOrDefault(item, 0);
        if (price < 0 || left <= 0) {
            return ShopActionResult.fail("icecream.reikocars.sold_out", new ItemStack(item).getHoverName());
        }
        EconomyApi eco = EconomyApi.get();
        if (price != expected) {
            return ShopActionResult.fail("icecream.reikocars.price_changed", eco.format(price + EconomyConfig.taxRates().salesTax(price)));
        }
        long tax = price > 0 ? EconomyConfig.taxRates().salesTax(price) : 0;
        ShopPayments.Charge charge = null;
        if (price > 0) {
            Component description = Component.translatable("icecream.reikocars.bill_line", new ItemStack(item).getHoverName(), truck.ownerName());
            charge = ShopPayments.charge(buyer, payment, price, tax, truck.owner(), "reikocars.ice_cream", description);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }
        ItemStack sold = takeOne(truck, item);
        Component name = sold.getHoverName(); // (before the stack goes into the buyer's inventory, leaving it empty)
        if (!buyer.getInventory().add(sold)) {
            buyer.drop(sold, false);
        }
        buyer.inventoryMenu.broadcastChanges();
        truck.level().playSound(null, truck.getX(), truck.getY(), truck.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.6F, 1.4F);
        Component line = tax > 0
                ? Component.translatable("icecream.reikocars.bought_tax", name, eco.format(price + tax), eco.format(tax))
                : Component.translatable("icecream.reikocars.bought", name, eco.format(price));
        return charge == null ? ShopActionResult.ok(line) : ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    private static ItemStack takeOne(CarEntity truck, Item item) {
        SimpleContainer freezer = truck.bed();
        for (int slot = freezer.getContainerSize() - 1; slot >= 0; slot--) {
            ItemStack stack = freezer.getItem(slot);
            if (stack.is(item)) {
                ItemStack one = stack.split(1);
                freezer.setChanged();
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The owner prices {@code item}: an empty price takes it off the menu. */
    public static ShopActionResult setPrice(ServerPlayer player, CarEntity truck, Item item, String text) {
        if (!truck.isOwner(player) && !player.hasPermissions(2)) {
            return ShopActionResult.fail("icecream.reikocars.owner_only", truck.ownerName());
        }
        Component name = new ItemStack(item).getHoverName();
        if (text.isBlank()) {
            truck.shopPrices().remove(item);
            return ShopActionResult.ok(Component.translatable("icecream.reikocars.off_menu", name));
        }
        OptionalLong price = EconomyApi.get().parse(text.trim());
        if (price.isEmpty() || price.getAsLong() < 0 || price.getAsLong() > Money.MAX_BALANCE / 100) {
            return ShopActionResult.fail("icecream.reikocars.bad_price");
        }
        truck.shopPrices().put(item, price.getAsLong());
        return ShopActionResult.ok(Component.translatable("icecream.reikocars.priced", name, EconomyApi.get().format(price.getAsLong())));
    }
}
