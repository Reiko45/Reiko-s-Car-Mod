package com.reiko.cars.wash;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.fire.Firefighting;
import com.reiko.cars.registry.ModItems;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The self-service Wash Bay. A player pays its price (card or cash, plus sales tax, to its owner) for {@code
 * carWash.baySeconds} of its pressure washer and is handed the lance, if they haven't got one; while they're within
 * {@code carWash.lanceReach} of the bay and their time lasts, holding right-click with the lance sprays a jet of water,
 * and a car it hits loses {@link #RATE} of its dirt a tick. The bay's owner sprays free. A lance away from any bay its
 * holder may use goes back on its holster after a while.
 */
public final class WashBayService {
    /** Dirt the jet washes off a tick: a filthy car's clean in about four seconds of spraying. */
    public static final float RATE = 0.015F;

    private WashBayService() {
    }

    /** Right-clicking a bay: its owner gets their screen (or, sneaking, a lance); anyone else pays, or is told their time. */
    public static void use(ServerPlayer player, WashBayBlockEntity bay) {
        long now = player.serverLevel().getGameTime();
        if (bay.canManage(player) && bay.owner() != null && !player.isSecondaryUseActive()) {
            WashOwnerMenu.open(player, bay, WashOwnerMenu.BAY);
            return;
        }
        if (bay.owner() == null) {
            player.displayClientMessage(Component.translatable("car_wash.reikocars.closed"), true);
            return;
        }
        if (bay.mayUse(player, now)) {
            boolean handed = giveLance(player);
            player.displayClientMessage(bay.isOwner(player) ? Component.translatable("wash_bay.reikocars.yours")
                    : Component.translatable(handed ? "wash_bay.reikocars.take_lance" : "wash_bay.reikocars.time_left",
                    seconds(bay.ticksLeft(player, now))), true);
            return;
        }
        if (bay.price() <= 0) {
            bay.addTime(player, now, CarsConfig.get(CarsConfig.BAY_SECONDS) * 20L);
            bay.recordSale(0);
            giveLance(player);
            player.displayClientMessage(Component.translatable("wash_bay.reikocars.free", CarsConfig.get(CarsConfig.BAY_SECONDS)), true);
            return;
        }
        WashPayMenu.open(player, bay, WashOwnerMenu.BAY);
    }

    /**
     * {@code player} pays {@code bay}'s price for its time, by card or cash plus sales tax.
     *
     * @param expected the price the screen showed, before tax
     */
    public static ShopActionResult pay(ServerPlayer player, BlockPos bayPos, long expected, Payment payment) {
        if (!(player.serverLevel().getBlockEntity(bayPos) instanceof WashBayBlockEntity bay) || bay.owner() == null) {
            return ShopActionResult.fail("car_wash.reikocars.gone");
        }
        long price = bay.price();
        EconomyApi eco = EconomyApi.get();
        long tax = EconomyConfig.taxRates().salesTax(price);
        if (price != expected) {
            return ShopActionResult.fail("car_wash.reikocars.price_changed", eco.format(price + tax));
        }
        if (price + tax > Money.MAX_BALANCE) {
            return ShopActionResult.fail("car_wash.reikocars.bad_price");
        }
        Component description = Component.translatable("wash_bay.reikocars.bill_line", bay.ownerName());
        ShopPayments.Charge charge = ShopPayments.charge(player, payment, price, tax, bay.owner(), "reikocars.wash_bay", description);
        if (!charge.success()) {
            return ShopActionResult.fail(charge.message());
        }
        int seconds = CarsConfig.get(CarsConfig.BAY_SECONDS);
        bay.recordSale(price);
        bay.addTime(player, player.serverLevel().getGameTime(), seconds * 20L);
        giveLance(player);
        Component line = tax > 0
                ? Component.translatable("wash_bay.reikocars.paid_tax", eco.format(price + tax), eco.format(tax), seconds)
                : Component.translatable("wash_bay.reikocars.paid", eco.format(price), seconds);
        return ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** Hands {@code player} a lance if they haven't one. Returns whether it did. */
    static boolean giveLance(Player player) {
        if (holdsOrCarries(player)) {
            return false;
        }
        ItemStack lance = new ItemStack(ModItems.PRESSURE_WASHER.get());
        if (!player.getInventory().add(lance)) {
            player.drop(lance, false);
        }
        return true;
    }

    private static boolean holdsOrCarries(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof PressureWasherItem) {
                return true;
            }
        }
        return player.getOffhandItem().getItem() instanceof PressureWasherItem;
    }

    private static long seconds(long ticks) {
        return (ticks + 19) / 20;
    }

    // ---- the bays a lance works at ----

    /** The bay {@code player}'s lance draws from: the nearest in reach they may use, or null. */
    @Nullable
    public static WashBayBlockEntity bayFor(Player player) {
        double reach = CarsConfig.get(CarsConfig.LANCE_REACH);
        long now = player.level().getGameTime();
        return baysNear(player, reach).stream().filter(bay -> bay.mayUse(player, now)).findFirst().orElse(null);
    }

    /** The bays within {@code reach} of {@code player}, nearest first. */
    public static List<WashBayBlockEntity> baysNear(Player player, double reach) {
        Level level = player.level();
        BlockPos at = player.blockPosition();
        int r = (int) Math.ceil(reach);
        List<WashBayBlockEntity> bays = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -4, -r), at.offset(r, 4, r))) {
            if (level.getBlockEntity(pos) instanceof WashBayBlockEntity bay && pos.getCenter().distanceTo(player.position()) <= reach + 0.5) {
                bays.add(bay);
            }
        }
        bays.sort(Comparator.comparingDouble(bay -> bay.getBlockPos().getCenter().distanceToSqr(player.position())));
        return bays;
    }

    /** Why the lance won't spray: no bay in reach, or no time left at the one that is. */
    public static Component whyNot(Player player) {
        List<WashBayBlockEntity> bays = baysNear(player, CarsConfig.get(CarsConfig.LANCE_REACH));
        return bays.isEmpty() ? Component.translatable("wash_bay.reikocars.no_bay", Math.round(CarsConfig.get(CarsConfig.LANCE_REACH)))
                : Component.translatable("wash_bay.reikocars.time_up");
    }

    // ---- spraying ----

    /**
     * One tick of spraying, on either side: false once there's no bay in reach they may use. On the server it washes
     * the cars the jet hits; on a client it draws the spray.
     */
    public static boolean spray(Player player) {
        WashBayBlockEntity bay = bayFor(player);
        if (bay == null) {
            return false;
        }
        Firefighting.Jet jet = Firefighting.trace(player.level(), player, 1.0F);
        if (player instanceof ServerPlayer server) {
            List<CarEntity> washed = hit(server, jet);
            long now = server.serverLevel().getGameTime();
            if (!washed.isEmpty() && now % 5 == 0) {
                CarEntity car = washed.getFirst();
                Component state = bay.isOwner(server) ? Dirt.describe(car.dirt())
                        : Component.translatable("wash_bay.reikocars.spraying", Dirt.describe(car.dirt()), seconds(bay.ticksLeft(server, now)));
                server.displayClientMessage(car.dirt() <= 0.0F ? Component.translatable("wash_bay.reikocars.car_clean") : state, true);
            } else if (washed.isEmpty() && now % 20 == 0 && !bay.isOwner(server)) {
                server.displayClientMessage(Component.translatable("wash_bay.reikocars.seconds_left", seconds(bay.ticksLeft(server, now))), true);
            }
        } else if (player.level().isClientSide) {
            Firefighting.drawSpray(player.level(), jet, player.getRandom());
        }
        return true;
    }

    /** Washes the cars along the jet and where it lands. Returns them, nearest the nozzle first. */
    public static List<CarEntity> hit(ServerPlayer player, Firefighting.Jet jet) {
        List<Vec3> points = jet.points();
        Set<CarEntity> cars = new HashSet<>();
        List<CarEntity> order = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            AABB along = new AABB(points.get(i - 1), points.get(i)).inflate(0.3);
            for (CarEntity car : player.level().getEntitiesOfClass(CarEntity.class, along, car -> car.isAlive() && !car.isPassenger())) {
                if (cars.add(car)) {
                    order.add(car);
                }
            }
        }
        if (jet.impact() != null) {
            for (CarEntity car : player.level().getEntitiesOfClass(CarEntity.class, new AABB(jet.impact(), jet.impact()).inflate(0.8),
                    car -> car.isAlive() && !car.isPassenger())) {
                if (cars.add(car)) {
                    order.add(car);
                }
            }
        }
        for (CarEntity car : order) {
            if (car.dirt() > 0.0F) {
                car.setDirt(car.dirt() - RATE);
            }
        }
        return order;
    }
}
