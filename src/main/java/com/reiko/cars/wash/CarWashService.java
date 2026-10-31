package com.reiko.cars.wash;

import com.reiko.cars.car.CarEntity;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The drive-through Car Wash. A driver pulls up to it and presses the hitch key (G), or right-clicks it from the
 * driver's seat, and pays its price (by card or cash, plus sales tax) to its owner; then, for {@link #PASS_TICKS}, the car
 * is washed while it's in the lane under the arch, {@link #RATE} of its dirt a tick, so it comes out clean when it's
 * driven through slowly. The pass is used up once the car's left the lane. Its owner's cars, and anyone's when the price
 * is 0, are washed free.
 */
public final class CarWashService {
    /** How long a paid wash waits for the car to drive through, in ticks. */
    public static final int PASS_TICKS = 2400;
    /** Dirt washed off a tick in the lane: a filthy car's clean in a little over a second. */
    public static final float RATE = 0.04F;
    /** How far from a car the hitch key looks for a car wash, in blocks. */
    static final int SEARCH = 8;

    private CarWashService() {
    }

    // ---- paying ----

    /** The car wash {@code car} is at or pulling up to: its lane, or the road a few blocks either side of it. */
    @Nullable
    public static CarWashBlockEntity washFor(CarEntity car) {
        Level level = car.level();
        BlockPos at = car.blockPosition();
        CarWashBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        // Only the block entities in the chunks round the car: the dashboard asks every frame.
        java.util.List<CarWashBlockEntity> near = new java.util.ArrayList<>();
        for (int cx = (at.getX() - SEARCH) >> 4; cx <= (at.getX() + SEARCH) >> 4; cx++) {
            for (int cz = (at.getZ() - SEARCH) >> 4; cz <= (at.getZ() + SEARCH) >> 4; cz++) {
                if (level.hasChunk(cx, cz)) {
                    for (net.minecraft.world.level.block.entity.BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                        if (entity instanceof CarWashBlockEntity wash && Math.abs(entity.getBlockPos().getY() - at.getY()) <= 2) {
                            near.add(wash);
                        }
                    }
                }
            }
        }
        for (CarWashBlockEntity wash : near) {
            BlockState state = wash.getBlockState();
            boolean alongX = state.getValue(CarWashBlock.FACING).getAxis() == net.minecraft.core.Direction.Axis.X;
            AABB approach = CarWashBlock.lane(state, wash.getBlockPos()).inflate(alongX ? 4.0 : 0.0, 0.0, alongX ? 0.0 : 4.0);
            if (!approach.intersects(car.getBoundingBox())) {
                continue;
            }
            double distance = approach.getCenter().distanceToSqr(car.position());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = wash;
            }
        }
        return best;
    }

    /** The hitch key: if there's a car wash here, its offer (and true). */
    public static boolean use(ServerPlayer driver, CarEntity car) {
        CarWashBlockEntity wash = washFor(car);
        if (wash == null) {
            return false;
        }
        offer(driver, car, wash);
        return true;
    }

    /** {@code driver} asks {@code wash} to wash {@code car}: free, already paid, not needed, or the pay screen. */
    public static void offer(ServerPlayer driver, CarEntity car, CarWashBlockEntity wash) {
        long now = driver.serverLevel().getGameTime();
        if (wash.hasPass(car, now)) {
            driver.displayClientMessage(Component.translatable("car_wash.reikocars.paid_drive_through"), true);
            return;
        }
        if (car.dirt() <= 0.0F) {
            driver.displayClientMessage(Component.translatable("car_wash.reikocars.already_clean"), true);
            return;
        }
        if (wash.owner() == null) {
            driver.displayClientMessage(Component.translatable("car_wash.reikocars.closed"), true);
            return;
        }
        if (wash.isOwner(driver) || wash.owner().equals(car.owner()) || wash.price() <= 0) {
            wash.givePass(car, now + PASS_TICKS);
            if (!wash.isOwner(driver) && !wash.owner().equals(car.owner())) {
                wash.recordSale(0);
            }
            driver.displayClientMessage(Component.translatable("car_wash.reikocars.free"), true);
            return;
        }
        WashPayMenu.open(driver, wash, WashOwnerMenu.TUNNEL);
    }

    /**
     * {@code driver} pays {@code wash} for a wash of the car they're driving, by card or cash plus sales tax.
     *
     * @param expected the price the screen showed, before tax
     */
    public static ShopActionResult pay(ServerPlayer driver, BlockPos washPos, long expected, Payment payment) {
        if (!(driver.serverLevel().getBlockEntity(washPos) instanceof CarWashBlockEntity wash) || wash.owner() == null) {
            return ShopActionResult.fail("car_wash.reikocars.gone");
        }
        if (!(driver.getVehicle() instanceof CarEntity car) || car.getControllingPassenger() != driver || washFor(car) != wash) {
            return ShopActionResult.fail("car_wash.reikocars.drive_up");
        }
        long now = driver.serverLevel().getGameTime();
        if (wash.hasPass(car, now)) {
            return ShopActionResult.fail("car_wash.reikocars.paid_drive_through");
        }
        long price = wash.price();
        EconomyApi eco = EconomyApi.get();
        long tax = EconomyConfig.taxRates().salesTax(price);
        if (price != expected) {
            return ShopActionResult.fail("car_wash.reikocars.price_changed", eco.format(price + tax));
        }
        if (price + tax > Money.MAX_BALANCE) {
            return ShopActionResult.fail("car_wash.reikocars.bad_price");
        }
        Component description = Component.translatable("car_wash.reikocars.bill_line", wash.ownerName());
        ShopPayments.Charge charge = ShopPayments.charge(driver, payment, price, tax, wash.owner(), "reikocars.car_wash", description);
        if (!charge.success()) {
            return ShopActionResult.fail(charge.message());
        }
        wash.recordSale(price);
        wash.givePass(car, now + PASS_TICKS);
        Component line = tax > 0
                ? Component.translatable("car_wash.reikocars.paid_tax", eco.format(price + tax), eco.format(tax))
                : Component.translatable("car_wash.reikocars.paid", eco.format(price));
        return ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** "$15.00", or "free". */
    public static Component priceText(WashBusinessBlockEntity wash) {
        return wash.price() <= 0 ? Component.translatable("car_wash.reikocars.free_price") : Component.literal(EconomyApi.get().format(wash.price()));
    }

    /** The owner sets the price. */
    public static ShopActionResult setPrice(ServerPlayer player, WashBusinessBlockEntity wash, String text) {
        if (!wash.canManage(player) || wash.owner() == null) {
            return ShopActionResult.fail("car_wash.reikocars.owner_only", wash.ownerName());
        }
        OptionalLong price = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (price.isEmpty() || price.getAsLong() < 0 || price.getAsLong() > Money.MAX_BALANCE / 100) {
            return ShopActionResult.fail("car_wash.reikocars.bad_price");
        }
        wash.setPrice(price.getAsLong());
        return ShopActionResult.ok(Component.translatable("car_wash.reikocars.price_set", EconomyApi.get().format(price.getAsLong())));
    }

    // ---- washing ----

    /** A server tick: washes the cars in the lane that have a pass, and runs the brushes while it does. */
    public static void tick(CarWashBlockEntity wash) {
        if (!(wash.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        List<CarEntity> cars = wash.carsInLane();
        boolean running = false;
        Set<UUID> inLane = new HashSet<>();
        for (CarEntity car : cars) {
            inLane.add(car.getUUID());
            LivingEntity driver = car.getControllingPassenger();
            if (!wash.hasPass(car, now)) {
                if (driver instanceof Player player && car.dirt() > 0.0F && now % 60 == 0) {
                    player.displayClientMessage(Component.translatable("car_wash.reikocars.how_to_pay", priceText(wash)), true);
                }
                continue;
            }
            running = true;
            wash.washing.add(car.getUUID());
            car.setSnow(car.snow() - RATE * 2.0F);
            float before = car.dirt();
            if (before > 0.0F) {
                car.setDirt(before - RATE);
                if (car.dirt() <= 0.0F) {
                    if (driver instanceof Player player) {
                        player.displayClientMessage(Component.translatable("car_wash.reikocars.clean"), true);
                    }
                    if (driver instanceof net.minecraft.server.level.ServerPlayer player) {
                        com.reiko.cars.advancement.CarAdvancements.award(player, "wash");
                    }
                } else if (driver instanceof Player player && now % 5 == 0) {
                    player.displayClientMessage(Component.translatable("car_wash.reikocars.washing", Math.round(car.dirt() * 100)), true);
                }
            }
        }
        // Cars that have been through and left: their wash is used up.
        if (!wash.washing.isEmpty()) {
            for (UUID id : Set.copyOf(wash.washing)) {
                if (!inLane.contains(id)) {
                    wash.washing.remove(id);
                    wash.passes.remove(id);
                    wash.setChanged();
                }
            }
        }
        if (now % 100 == 0 && wash.passes.values().removeIf(until -> until <= now)) {
            wash.setChanged();
        }
        BlockState state = wash.getBlockState();
        if (state.getValue(CarWashBlock.RUNNING) != running) {
            level.setBlock(wash.getBlockPos(), state.setValue(CarWashBlock.RUNNING, running), Block.UPDATE_CLIENTS);
        }
    }
}
