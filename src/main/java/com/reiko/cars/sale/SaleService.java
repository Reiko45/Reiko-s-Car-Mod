package com.reiko.cars.sale;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.key.CarKeys;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.api.TransactionResult;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Buying and selling cars. Any owner can put their car up for sale at a price (with a For Sale Sign, or on a
 * dealership's screen when it's parked on the lot); anyone else who right-clicks it can buy it, paying as at a shop
 * (card with PIN, or cash, plus sales tax) to the owner. The buyer gets the car and a key cut for new locks, so the
 * seller's keys stop fitting.
 *
 * <p>A dealership also buys cars: its owner sets what it pays for each kind of car, and a player who parks theirs on
 * the lot can trade it in, paid from the dealer's bank account, less for wear (down to {@link #WORN_OUT_SHARE} for a
 * worn-out car) and for dirt (see {@link com.reiko.cars.wash.Dirt#tradeInShare}).
 */
public final class SaleService {
    /** What a trade-in in the worst condition fetches, as a share of the dealer's offer for one in perfect condition. */
    public static final double WORN_OUT_SHARE = 0.4;

    private SaleService() {
    }

    public static double reach() {
        return CarsConfig.get(CarsConfig.DEALERSHIP_REACH);
    }

    /** The cars on {@code lot}: within reach of it, parked (not carried), nearest first. */
    public static List<CarEntity> carsOnLot(DealershipBlockEntity lot) {
        Level level = lot.getLevel();
        if (level == null) {
            return List.of();
        }
        double reach = reach();
        Vec3 center = lot.getBlockPos().getCenter();
        return level.getEntitiesOfClass(CarEntity.class, new AABB(lot.getBlockPos()).inflate(reach),
                        car -> car.isAlive() && !car.isPassenger() && flat(car.position(), center) <= reach)
                .stream()
                .sorted(Comparator.<CarEntity>comparingDouble(car -> flat(car.position(), center)).thenComparingInt(Entity::getId))
                .toList();
    }

    private static double flat(Vec3 a, Vec3 b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
    }

    /** The dealer's cars on the lot that are for sale. */
    public static List<CarEntity> forSale(DealershipBlockEntity lot) {
        return carsOnLot(lot).stream().filter(car -> lot.owner() != null && lot.owner().equals(car.owner()) && car.isForSale()).toList();
    }

    /** The dealer's cars on the lot, for sale or not. */
    public static List<CarEntity> stock(DealershipBlockEntity lot) {
        return carsOnLot(lot).stream().filter(car -> lot.owner() != null && lot.owner().equals(car.owner())).toList();
    }

    /** {@code player}'s own cars on the lot, which they could trade in. */
    public static List<CarEntity> tradeable(DealershipBlockEntity lot, Player player) {
        return carsOnLot(lot).stream().filter(car -> car.isOwner(player)).toList();
    }

    /** What {@code lot} pays for {@code car}: its offer for that kind, less for wear. 0 if it isn't buying them. */
    public static long offerFor(DealershipBlockEntity lot, CarEntity car) {
        long base = lot.offer(car.carType());
        if (base <= 0) {
            return 0;
        }
        double condition = car.condition().overall(car::hasPart);
        return Math.round(base * (WORN_OUT_SHARE + (1.0 - WORN_OUT_SHARE) * condition) * com.reiko.cars.wash.Dirt.tradeInShare(car.dirt()));
    }

    /** The dealership whose lot {@code car} is on, owned by {@code owner}, if any. */
    @Nullable
    public static DealershipBlockEntity lotOf(CarEntity car, @Nullable UUID owner) {
        if (owner == null) {
            return null;
        }
        int reach = (int) Math.ceil(reach());
        BlockPos at = car.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-reach, -3, -reach), at.offset(reach, 3, reach))) {
            if (car.level().getBlockEntity(pos) instanceof DealershipBlockEntity lot && owner.equals(lot.owner())
                    && flat(car.position(), pos.getCenter()) <= reach()) {
                return lot;
            }
        }
        return null;
    }

    private static OptionalLong parse(String text) {
        OptionalLong parsed = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (parsed.isEmpty() || parsed.getAsLong() < 0 || parsed.getAsLong() > Money.MAX_BALANCE / 100) {
            return OptionalLong.empty();
        }
        return parsed;
    }

    // ---- selling ----

    /** The owner puts {@code car} up for sale at the price typed. */
    public static ShopActionResult setPrice(ServerPlayer player, CarEntity car, String text) {
        if (!car.canManage(player) || car.owner() == null) {
            return ShopActionResult.fail("sale.reikocars.not_yours", car.ownerName());
        }
        ShopActionResult tied = tiedUp(car);
        if (tied != null) {
            return tied;
        }
        OptionalLong price = parse(text);
        if (price.isEmpty()) {
            return ShopActionResult.fail("sale.reikocars.bad_price");
        }
        car.setSalePrice(price.getAsLong());
        return ShopActionResult.ok(Component.translatable("sale.reikocars.for_sale", car.kindName(), EconomyApi.get().format(price.getAsLong())));
    }

    /** Why {@code car} can't change hands now (a loan on it, out on rent, held at an impound lot), or null if it can. */
    @Nullable
    static ShopActionResult tiedUp(CarEntity car) {
        if (car.loanId() != null) {
            return ShopActionResult.fail("loan.reikocars.pay_off_first");
        }
        if (car.rental() != null) {
            return ShopActionResult.fail("rental.reikocars.rented_out", car.kindName());
        }
        if (car.isImpounded()) {
            return ShopActionResult.fail("impound.reikocars.held");
        }
        return null;
    }

    public static ShopActionResult takeOffSale(ServerPlayer player, CarEntity car) {
        if (!car.canManage(player) || car.owner() == null) {
            return ShopActionResult.fail("sale.reikocars.not_yours", car.ownerName());
        }
        car.setSalePrice(-1);
        return ShopActionResult.ok(Component.translatable("sale.reikocars.off_sale", car.kindName()));
    }

    // ---- buying ----

    /**
     * {@code buyer} buys {@code car} at its price, by card or cash, plus sales tax, paid to its owner.
     *
     * @param expected the price the screen showed, before tax
     */
    public static ShopActionResult buy(ServerPlayer buyer, CarEntity car, long expected, Payment payment) {
        return buy(buyer, car, expected, payment, false);
    }

    /**
     * {@code buyer} buys {@code car}, outright or (with {@code finance}, at a dealership) on a car loan: the down payment
     * and the sales tax now, the rest a day at a time (see {@link com.reiko.cars.finance.LoanService}).
     */
    public static ShopActionResult buy(ServerPlayer buyer, CarEntity car, long expected, Payment payment, boolean finance) {
        UUID seller = car.owner();
        if (!car.isAlive() || !car.isForSale() || seller == null) {
            return ShopActionResult.fail("sale.reikocars.not_for_sale", car.kindName());
        }
        if (car.isOwner(buyer)) {
            return ShopActionResult.fail("sale.reikocars.already_yours");
        }
        if (!car.getPassengers().isEmpty() || car.isPassenger()) {
            return ShopActionResult.fail("sale.reikocars.occupied", car.kindName());
        }
        if (buyer.distanceToSqr(car) > 12.0 * 12.0) {
            return ShopActionResult.fail("sale.reikocars.too_far", car.kindName());
        }
        long price = car.salePrice();
        EconomyApi eco = EconomyApi.get();
        if (price != expected) {
            return ShopActionResult.fail("sale.reikocars.price_changed", eco.format(price + EconomyConfig.taxRates().salesTax(price)));
        }
        long tax = price > 0 ? EconomyConfig.taxRates().salesTax(price) : 0;
        if (price + tax > Money.MAX_BALANCE) {
            return ShopActionResult.fail("sale.reikocars.bad_price");
        }
        DealershipBlockEntity lot = lotOf(car, seller);
        com.reiko.cars.finance.LoanService.Terms terms = null;
        if (finance) {
            if (!com.reiko.cars.finance.LoanService.canFinance(car, lot)) {
                return ShopActionResult.fail("loan.reikocars.no_finance");
            }
            terms = com.reiko.cars.finance.LoanService.terms(price);
        }
        long now = terms != null ? terms.down() : price;
        String sellerName = car.ownerName();
        ShopPayments.Charge charge = null;
        if (now > 0 || (terms != null && tax > 0)) {
            Component description = Component.translatable(terms != null ? "loan.reikocars.bill_line" : "sale.reikocars.bill_line", car.kindName(), sellerName);
            charge = ShopPayments.charge(buyer, payment, now, tax, seller, "reikocars.car_sale", description);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }
        if (lot != null) {
            lot.recordSale(price);
        }
        handOver(car, buyer);
        com.reiko.cars.advancement.CarAdvancements.award(buyer, "buy_car");
        if (terms != null) {
            com.reiko.cars.finance.LoanService.start(buyer, car, lot, terms);
        }
        ItemStack key = CarKeys.keyFor(car);
        if (!buyer.getInventory().add(key)) {
            buyer.drop(key, false);
        }
        car.level().playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        ServerPlayer old = buyer.server.getPlayerList().getPlayer(seller);
        if (old != null) {
            old.sendSystemMessage(Component.translatable("sale.reikocars.seller_sold", buyer.getGameProfile().getName(), car.kindName(), eco.format(price)));
        }
        if (price <= 0) {
            return ShopActionResult.ok(Component.translatable("sale.reikocars.bought_free", car.kindName()));
        }
        if (terms != null) {
            Component line = Component.translatable("loan.reikocars.bought", car.kindName(), eco.format(terms.down() + tax), eco.format(terms.installment()),
                    terms.days(), sellerName);
            return ShopActionResult.ok(charge == null ? line : ShopPayments.receipt(line, payment, charge));
        }
        Component line = tax > 0
                ? Component.translatable("sale.reikocars.bought_tax", car.kindName(), eco.format(price + tax), eco.format(tax))
                : Component.translatable("sale.reikocars.bought", car.kindName(), eco.format(price));
        return ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** {@code car} becomes {@code buyer}'s: off sale, on new locks, locked. */
    private static void handOver(CarEntity car, Player buyer) {
        car.setOwner(buyer);
        rekey(car);
    }

    private static void rekey(CarEntity car) {
        car.setSalePrice(-1);
        car.setLockCode(CarKeys.newCode()); // the seller's keys stop fitting
        car.setLocked(true);
    }

    // ---- trade-ins ----

    /**
     * {@code seller} trades {@code car} in at {@code lot}, paid its offer from the dealer's bank account.
     *
     * @param expected the offer the screen showed
     */
    public static ShopActionResult tradeIn(ServerPlayer seller, DealershipBlockEntity lot, CarEntity car, long expected) {
        UUID dealer = lot.owner();
        if (dealer == null) {
            return ShopActionResult.fail("sale.reikocars.no_dealer");
        }
        if (lot.isOwner(seller)) {
            return ShopActionResult.fail("sale.reikocars.own_lot");
        }
        if (!car.isAlive() || !car.isOwner(seller) || !carsOnLot(lot).contains(car)) {
            return ShopActionResult.fail("sale.reikocars.not_on_lot", (int) reach());
        }
        ShopActionResult tied = tiedUp(car);
        if (tied != null) {
            return tied;
        }
        if (!car.getPassengers().isEmpty()) {
            return ShopActionResult.fail("sale.reikocars.occupied", car.kindName());
        }
        long offer = offerFor(lot, car);
        EconomyApi eco = EconomyApi.get();
        if (offer <= 0) {
            return ShopActionResult.fail("sale.reikocars.not_buying", lot.ownerName(), car.kindName());
        }
        if (offer != expected) {
            return ShopActionResult.fail("sale.reikocars.offer_changed", eco.format(offer));
        }
        MinecraftServer server = seller.server;
        if (eco.getBalance(server, dealer) < offer) {
            return ShopActionResult.fail("sale.reikocars.dealer_broke", lot.ownerName());
        }
        TransactionResult paid = eco.transfer(server, dealer, seller.getUUID(), offer, "reikocars.trade_in");
        if (!paid.isSuccess()) {
            return ShopActionResult.fail(Component.translatable(paid.translationKey()));
        }
        lot.recordTradeIn(offer);
        car.setOwner(dealer, lot.ownerName());
        rekey(car);
        car.level().playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.6F, 1.0F);
        ServerPlayer owner = server.getPlayerList().getPlayer(dealer);
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable("sale.reikocars.dealer_bought", seller.getGameProfile().getName(), car.kindName(), eco.format(offer)));
        }
        return ShopActionResult.ok(Component.translatable("sale.reikocars.traded_in", car.kindName(), lot.ownerName(), eco.format(offer)));
    }

    /** The dealer sets what they'll pay for a {@code type} in perfect condition (0 to stop buying them). */
    public static ShopActionResult setOffer(ServerPlayer player, DealershipBlockEntity lot, CarType type, String text) {
        if (!lot.canManage(player) || lot.owner() == null) {
            return ShopActionResult.fail("sale.reikocars.dealer_only", lot.ownerName());
        }
        OptionalLong offer = parse(text);
        if (offer.isEmpty()) {
            return ShopActionResult.fail("sale.reikocars.bad_price");
        }
        lot.setOffer(type, offer.getAsLong());
        Component name = Component.translatable("entity.reikocars." + type.id());
        return ShopActionResult.ok(offer.getAsLong() > 0
                ? Component.translatable("sale.reikocars.offer_set", name, EconomyApi.get().format(offer.getAsLong()))
                : Component.translatable("sale.reikocars.offer_off", name));
    }
}
