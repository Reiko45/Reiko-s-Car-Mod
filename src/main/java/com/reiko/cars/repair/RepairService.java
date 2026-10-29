package com.reiko.cars.repair;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.job.CarJobs;
import com.reiko.cars.registry.ModSounds;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerType;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Repairs at a repair bench. A player can fit parts they carry to a car or trailer beside the bench for nothing, or
 * pay for the bench to fit parts from its stock, at its owner's prices. Paying works as at a shop or pump in Reiko's
 * Economy Mod (a debit card with PIN, a credit card, or cash with change, plus sales tax), and the owner is paid into
 * their bank account. The owner repairs from their own stock for free.
 *
 * <p>Any vehicle beside the bench can be worked on, locked or not: a repair only ever helps its owner, and whoever
 * asks for it pays.
 */
public final class RepairService {
    private RepairService() {
    }

    /** Every car and trailer within reach of the bench, nearest first (then oldest first, so the order holds still). */
    public static List<Repairable> vehiclesInReach(RepairBenchBlockEntity bench) {
        Level level = bench.getLevel();
        if (level == null) {
            return List.of();
        }
        double reach = CarsConfig.get(CarsConfig.BENCH_REACH);
        Vec3 center = bench.getBlockPos().getCenter();
        // A trailer is far longer than its hitbox, so look further out for those.
        List<Entity> found = level.getEntities((Entity) null, new AABB(bench.getBlockPos()).inflate(reach + TrailerType.LENGTH / 2.0),
                entity -> entity instanceof Repairable && inReach(bench, (Repairable) entity));
        return found.stream()
                .sorted(Comparator.<Entity>comparingDouble(entity -> distance(center, (Repairable) entity)).thenComparingInt(Entity::getId))
                .map(Repairable.class::cast)
                .toList();
    }

    /**
     * What {@code player} would repair here, until they pick another: the vehicle they're in, else the nearest they
     * may use (their own, or an unlocked one), else the nearest of all.
     */
    @Nullable
    public static Repairable defaultTarget(RepairBenchBlockEntity bench, Player player, List<Repairable> vehicles) {
        for (Repairable vehicle : vehicles) {
            if (player.getVehicle() == vehicle.entity()) {
                return vehicle;
            }
        }
        for (Repairable vehicle : vehicles) {
            if (vehicle instanceof CarEntity car ? car.canBeUsedBy(player) : ((TrailerEntity) vehicle).canBeUsedBy(player)) {
                return vehicle;
            }
        }
        return vehicles.isEmpty() ? null : vehicles.getFirst();
    }

    /** What {@code player} would repair here, if they don't pick. */
    @Nullable
    public static Repairable defaultTarget(RepairBenchBlockEntity bench, Player player) {
        return defaultTarget(bench, player, vehiclesInReach(bench));
    }

    /** A repair bench that has {@code vehicle} in reach, if there's one. */
    @Nullable
    public static RepairBenchBlockEntity benchNear(Repairable vehicle) {
        Entity entity = vehicle.entity();
        int reach = (int) Math.ceil(CarsConfig.get(CarsConfig.BENCH_REACH) + entity.getBbWidth()) + 4;
        net.minecraft.core.BlockPos at = entity.blockPosition();
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(at.offset(-reach, -2, -reach), at.offset(reach, 2, reach))) {
            if (entity.level().getBlockEntity(pos) instanceof RepairBenchBlockEntity bench && inReach(bench, vehicle)) {
                return bench;
            }
        }
        return null;
    }

    public static boolean inReach(RepairBenchBlockEntity bench, Repairable vehicle) {
        Entity entity = vehicle.entity();
        return entity.isAlive() && entity.level() == bench.getLevel()
                && distance(bench.getBlockPos().getCenter(), vehicle) <= CarsConfig.get(CarsConfig.BENCH_REACH);
    }

    /** From the bench to the nearest point of the vehicle (all along a trailer, not just its hitbox). */
    private static double distance(Vec3 from, Repairable vehicle) {
        if (vehicle instanceof TrailerEntity trailer) {
            double nearest = boxDistance(from, trailer.getBoundingBox());
            for (var part : trailer.getParts()) {
                nearest = Math.min(nearest, boxDistance(from, part.getBoundingBox()));
            }
            return nearest;
        }
        return boxDistance(from, vehicle.entity().getBoundingBox());
    }

    private static double boxDistance(Vec3 from, AABB box) {
        double dx = Math.max(Math.max(box.minX - from.x, 0.0), from.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - from.y, 0.0), from.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - from.z, 0.0), from.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * How many items a job on {@code part} uses, given that it needs {@code needed} to be as new and {@code available}
     * are to hand: an oil change, tires and brakes need the whole set, engine and body work can be done in part. 0
     * means the job can't be done (or doesn't need doing).
     */
    public static int jobSize(Part part, int needed, int available) {
        if (needed <= 0 || available <= 0) {
            return 0;
        }
        if (RepairJobs.allAtOnce(part)) {
            return available >= needed ? needed : 0;
        }
        return Math.min(needed, available);
    }

    /** Whether bit {@code part} is set in {@code mask}. */
    public static boolean has(int mask, Part part) {
        return (mask & (1 << part.ordinal())) != 0;
    }

    /** How many of {@code part}'s items {@code player} carries. */
    public static int carried(Player player, Part part) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (RepairJobs.partFor(stack) == part) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void takeCarried(Player player, Part part, int count) {
        for (ItemStack stack : player.getInventory().items) {
            if (count <= 0) {
                break;
            }
            if (RepairJobs.partFor(stack) == part) {
                int taken = Math.min(count, stack.getCount());
                stack.shrink(taken);
                count -= taken;
            }
        }
        player.getInventory().setChanged();
    }

    /** Fits the parts {@code player} carries, for the parts in {@code mask}, to {@code car}. */
    public static ShopActionResult fitOwn(ServerPlayer player, RepairBenchBlockEntity bench, @Nullable Repairable car, int mask) {
        if (car == null || !inReach(bench, car)) {
            return ShopActionResult.fail("repair.reikocars.no_car", (int) CarsConfig.get(CarsConfig.BENCH_REACH));
        }
        CarCondition condition = car.condition();
        List<Component> done = new ArrayList<>();
        int fitted = 0;
        ShopActionResult problem = null;
        for (Part part : Part.values()) {
            if (!has(mask, part)) {
                continue;
            }
            if (!car.hasPart(part)) {
                problem = ShopActionResult.fail("repair.reikocars.no_such_part", car.kindName(), part.displayName());
                continue;
            }
            int needed = RepairJobs.needed(part, car, condition);
            int have = carried(player, part);
            int count = jobSize(part, needed, have);
            if (count <= 0) {
                problem = needed <= 0 ? ShopActionResult.fail("repair.reikocars.fine", part.displayName())
                        : have <= 0 ? ShopActionResult.fail("repair.reikocars.none_carried", itemName(part))
                        : ShopActionResult.fail("repair.reikocars.need_set", needed, itemName(part), have);
                continue;
            }
            takeCarried(player, part, count);
            condition = RepairJobs.fit(part, car, condition, count);
            done.add(jobName(part, count));
            fitted += count;
        }
        if (done.isEmpty()) {
            return problem != null ? problem : ShopActionResult.fail("repair.reikocars.nothing_selected");
        }
        car.setCondition(condition);
        player.inventoryMenu.broadcastChanges();
        finished(bench, car);
        if (!ownedBy(car, player.getUUID())) {
            CarJobs.work(player, CarJobs.REPAIR_PART, fitted); // fitting your own parts to someone else's car is a mechanic's work
        }
        return ShopActionResult.ok(Component.translatable("repair.reikocars.fitted", list(done), car.entity().getDisplayName()));
    }

    /**
     * Has the bench fit parts from its stock, for the parts in {@code mask}, and charges {@code player} for them.
     * Nothing is fitted unless the payment goes through.
     *
     * @param expectedTotal the bill the player saw, before tax, so a price or stock change in the meantime is caught
     */
    public static ShopActionResult buy(ServerPlayer player, RepairBenchBlockEntity bench, @Nullable Repairable car, int mask,
                                       long expectedTotal, Payment payment) {
        if (car == null || !inReach(bench, car)) {
            return ShopActionResult.fail("repair.reikocars.no_car", (int) CarsConfig.get(CarsConfig.BENCH_REACH));
        }
        CarCondition condition = car.condition();
        int[] counts = new int[Part.values().length];
        long total = 0;
        boolean any = false;
        for (Part part : Part.values()) {
            if (!has(mask, part)) {
                continue;
            }
            if (!car.hasPart(part)) {
                return ShopActionResult.fail("repair.reikocars.no_such_part", car.kindName(), part.displayName());
            }
            int needed = RepairJobs.needed(part, car, condition);
            int count = jobSize(part, needed, bench.count(part));
            if (count <= 0) {
                return needed <= 0 ? ShopActionResult.fail("repair.reikocars.fine", part.displayName())
                        : ShopActionResult.fail("repair.reikocars.out_of_stock", itemName(part));
            }
            counts[part.ordinal()] = count;
            total += count * bench.price(part);
            any = true;
        }
        if (!any) {
            return ShopActionResult.fail("repair.reikocars.nothing_selected");
        }
        if (total != expectedTotal) {
            return ShopActionResult.fail("repair.reikocars.total_changed", EconomyApi.get().format(total + EconomyConfig.taxRates().salesTax(total)));
        }

        boolean free = bench.isOwner(player) || total == 0;
        long tax = free ? 0 : EconomyConfig.taxRates().salesTax(total);
        ShopPayments.Charge charge = null;
        List<Component> jobs = new ArrayList<>();
        for (Part part : Part.values()) {
            if (counts[part.ordinal()] > 0) {
                jobs.add(jobName(part, counts[part.ordinal()]));
            }
        }
        if (!free) {
            if (total + tax > Money.MAX_BALANCE) {
                return ShopActionResult.fail("repair.reikocars.too_expensive");
            }
            Component description = Component.translatable("repair.reikocars.bill_line", list(jobs), bench.ownerName());
            charge = ShopPayments.charge(player, payment, total, tax, bench.owner(), "reikocars.repair", description);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }

        int fitted = 0;
        int bodyWork = 0;
        for (Part part : Part.values()) {
            int count = counts[part.ordinal()];
            if (count > 0) {
                int taken = bench.take(part, count);
                condition = RepairJobs.fit(part, car, condition, taken);
                fitted += taken;
                if (part == Part.BODY) {
                    bodyWork = taken;
                }
            }
        }
        car.setCondition(condition);
        // The bench's stock, fitted to someone else's vehicle: its owner's work as a mechanic, if they're on.
        if (bench.owner() != null && !ownedBy(car, bench.owner())) {
            CarJobs.work(player.server.getPlayerList().getPlayer(bench.owner()), CarJobs.REPAIR_PART, fitted);
        }
        if (!free) {
            bench.recordJob(total);
            if (bodyWork > 0 && car instanceof CarEntity insured) {
                // Crash damage: an insurance claim, paid back to whoever paid the bill.
                com.reiko.cars.insurance.InsuranceService.claim(player, insured, bodyWork * bench.price(Part.BODY));
            }
        }
        finished(bench, car);
        Component jobList = list(jobs);
        if (free) {
            return ShopActionResult.ok(Component.translatable("repair.reikocars.repaired_free", jobList));
        }
        Component line = tax > 0
                ? Component.translatable("repair.reikocars.repaired_tax", jobList, EconomyApi.get().format(total + tax), EconomyApi.get().format(tax))
                : Component.translatable("repair.reikocars.repaired", jobList, EconomyApi.get().format(total));
        return ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** Whether {@code owner} owns {@code vehicle}. */
    private static boolean ownedBy(Repairable vehicle, java.util.UUID owner) {
        return vehicle.entity() instanceof com.reiko.cars.key.Keyed keyed && owner.equals(keyed.owner());
    }

    /** The clank of a job done, and a puff of sparkle over the car. */
    private static void finished(RepairBenchBlockEntity bench, Repairable vehicle) {
        Entity car = vehicle.entity();
        Level level = bench.getLevel();
        if (level == null) {
            return;
        }
        level.playSound(null, bench.getBlockPos(), ModSounds.REPAIR.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, car.getX(), car.getY() + car.getBbHeight() * 0.7, car.getZ(), 12,
                    car.getBbWidth() * 0.4, 0.3, car.getBbWidth() * 0.4, 0.0);
        }
    }

    /** "an oil change", "4 Tires", "2 Engine Parts". */
    private static Component jobName(Part part, int count) {
        return part == Part.OIL ? Component.translatable("repair.reikocars.job_oil")
                : Component.translatable("repair.reikocars.job", count, itemName(part));
    }

    private static Component itemName(Part part) {
        return new ItemStack(RepairJobs.item(part)).getHoverName();
    }

    /** "a, b and c". */
    private static Component list(List<Component> parts) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                out.append(i == parts.size() - 1 ? Component.translatable("repair.reikocars.and") : Component.literal(", "));
            }
            out.append(parts.get(i));
        }
        return out;
    }

    /** Sets the price of one of {@code part}'s items from typed text like {@code 12.50}. Owner or admin only. */
    public static ShopActionResult setPrice(ServerPlayer player, RepairBenchBlockEntity bench, int mask, String text) {
        if (!bench.canManage(player)) {
            return ShopActionResult.fail("repair.reikocars.owner_only_price", bench.ownerName());
        }
        Part part = null;
        for (Part candidate : Part.values()) {
            if (has(mask, candidate)) {
                part = candidate;
                break;
            }
        }
        if (part == null) {
            return ShopActionResult.fail("repair.reikocars.nothing_selected");
        }
        OptionalLong parsed = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (parsed.isEmpty() || parsed.getAsLong() < 0 || parsed.getAsLong() > Money.MAX_BALANCE / 100) {
            return ShopActionResult.fail("repair.reikocars.bad_price");
        }
        bench.setPrice(part, parsed.getAsLong());
        return ShopActionResult.ok(Component.translatable("repair.reikocars.price_set", itemName(part), EconomyApi.get().format(parsed.getAsLong())));
    }
}
