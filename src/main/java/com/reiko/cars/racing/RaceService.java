package com.reiko.cars.racing;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Races and time trials. A track is a Race Control and its owner's numbered race flags nearby; a lap is every flag in
 * order (passing within {@link #FLAG_REACH} blocks of each) and back past the Race Control (within {@link #LINE_REACH}
 * blocks: the line).
 *
 * <ul>
 *   <li><b>Time trials</b> are free and solo: get into a car, and the clock starts when you cross the line. Every lap
 *       is timed and the best go on the Race Control's board. Getting out ends it.</li>
 *   <li><b>Races</b>: drivers enter at the Race Control, paying its entry fee into the pot. An entrant starts it (with at
 *       least two in, or the owner with one): everyone in a car at the line races, from a standing start after a
 *       three-second countdown. First past the line after the last lap wins the pot, less the owner's cut; the rest
 *       get a minute to finish. Leaving your car puts you out.</li>
 * </ul>
 *
 * <p>Races and trials aren't saved: a restart calls them off, and entry fees are refunded.
 */
public final class RaceService {
    public static final double FLAG_REACH = 5.0;
    public static final double LINE_REACH = 8.0;
    /** How close to the Race Control a racer has to be at the start. */
    public static final double GRID_REACH = 24.0;
    public static final int COUNTDOWN = 60;
    /** How long the others have to finish once someone's won, in ticks. */
    public static final long AFTER_WINNER = 1200;
    public static final int MAX_RACERS = 12;

    public enum State { OPEN, COUNTDOWN, RUNNING }

    /** Someone racing (or on a time trial): how far round they've got. */
    static final class Racer {
        final UUID id;
        final String name;
        long paid;
        @Nullable
        UUID car;
        String carType = "";
        int lap;
        int next;
        boolean inLine;
        @Nullable
        Vec3 last;
        long lapStart;
        long best = -1;
        long finished = -1;
        boolean out;

        Racer(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** A race at a Race Control. */
    static final class Race {
        final GlobalPos control;
        final UUID owner;
        final List<BlockPos> track;
        final int laps;
        final int cut;
        long pot;
        State state = State.OPEN;
        long start;
        long winnerAt = -1;
        final LinkedHashMap<UUID, Racer> racers = new LinkedHashMap<>();

        Race(GlobalPos control, UUID owner, List<BlockPos> track, int laps, int cut) {
            this.control = control;
            this.owner = owner;
            this.track = track;
            this.laps = laps;
            this.cut = cut;
        }
    }

    /** A time trial. */
    static final class Trial {
        final GlobalPos control;
        final List<BlockPos> track;
        final Racer racer;
        final long deadline;
        boolean running;

        Trial(GlobalPos control, List<BlockPos> track, Racer racer, long deadline) {
            this.control = control;
            this.track = track;
            this.racer = racer;
            this.deadline = deadline;
        }
    }

    private static final Map<GlobalPos, Race> RACES = new HashMap<>();
    private static final Map<UUID, Trial> TRIALS = new HashMap<>();

    private RaceService() {
    }

    /** The track: the owner's numbered race flags within {@code racing.trackRadius}, in order. */
    public static List<BlockPos> track(RaceControlBlockEntity control) {
        if (!(control.getLevel() instanceof ServerLevel level) || control.owner() == null) {
            return List.of();
        }
        return FlagRegistry.get(level.getServer()).route(level.dimension(), control.getBlockPos(), CarsConfig.get(CarsConfig.TRACK_RADIUS),
                control.owner());
    }

    private static GlobalPos key(RaceControlBlockEntity control) {
        return GlobalPos.of(control.getLevel().dimension(), control.getBlockPos());
    }

    @Nullable
    static Race race(RaceControlBlockEntity control) {
        return RACES.get(key(control));
    }

    /** The state of the race at {@code control}, or null if there's none. */
    @Nullable
    public static State state(RaceControlBlockEntity control) {
        Race race = race(control);
        return race == null ? null : race.state;
    }

    /** Who's entered the race at {@code control}, in the order they entered. */
    public static List<String> entrants(RaceControlBlockEntity control) {
        Race race = race(control);
        return race == null ? List.of() : race.racers.values().stream().map(racer -> racer.name).toList();
    }

    public static long pot(RaceControlBlockEntity control) {
        Race race = race(control);
        return race == null ? 0 : race.pot;
    }

    public static boolean entered(RaceControlBlockEntity control, UUID player) {
        Race race = race(control);
        return race != null && race.racers.containsKey(player);
    }

    /** Whether {@code player} is in a race or on a time trial anywhere. */
    public static boolean racing(UUID player) {
        return TRIALS.containsKey(player) || RACES.values().stream().anyMatch(race -> race.racers.containsKey(player));
    }

    public static boolean onTrial(UUID player) {
        return TRIALS.containsKey(player);
    }

    // ---- entering, starting and leaving ----

    public static ShopActionResult enter(ServerPlayer player, RaceControlBlockEntity control, long expected, Payment payment) {
        if (control.owner() == null) {
            return ShopActionResult.fail("race.reikocars.no_owner");
        }
        List<BlockPos> track = track(control);
        if (track.isEmpty()) {
            return ShopActionResult.fail("race.reikocars.no_track", control.ownerName());
        }
        Race race = race(control);
        if (race != null && race.state != State.OPEN) {
            return ShopActionResult.fail("race.reikocars.under_way");
        }
        if (race != null && race.racers.containsKey(player.getUUID())) {
            return ShopActionResult.fail("race.reikocars.already_in");
        }
        if (racing(player.getUUID())) {
            return ShopActionResult.fail("race.reikocars.busy");
        }
        if (race != null && race.racers.size() >= MAX_RACERS) {
            return ShopActionResult.fail("race.reikocars.full", MAX_RACERS);
        }
        long fee = control.entryFee();
        if (fee != expected) {
            return ShopActionResult.fail("race.reikocars.price_changed", EconomyApi.get().format(fee));
        }
        ShopPayments.Charge charge = null;
        if (fee > 0) {
            if (fee > Money.MAX_BALANCE) {
                return ShopActionResult.fail("race.reikocars.too_expensive");
            }
            // The fee goes into the pot (out of everyone's account) until the race is won, or handed back.
            charge = ShopPayments.charge(player, payment, fee, 0, null, "reikocars.race_entry",
                    Component.translatable("race.reikocars.bill_line", control.ownerName()));
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }
        if (race == null) {
            race = new Race(key(control), control.owner(), track, control.laps(), control.cut());
            RACES.put(race.control, race);
        }
        Racer racer = new Racer(player.getUUID(), player.getGameProfile().getName());
        racer.paid = fee;
        race.racers.put(player.getUUID(), racer);
        race.pot += fee;
        for (Racer other : race.racers.values()) {
            ServerPlayer them = player.server.getPlayerList().getPlayer(other.id);
            if (them != null && them != player) {
                them.sendSystemMessage(Component.translatable("race.reikocars.joined", racer.name, race.racers.size()));
            }
        }
        Component done = Component.translatable("race.reikocars.entered", race.laps, race.track.size(), EconomyApi.get().format(race.pot));
        return ShopActionResult.ok(charge == null ? done : ShopPayments.receipt(done, payment, charge));
    }

    /** {@code player} pulls out of a race that hasn't started, and gets their fee back. */
    public static ShopActionResult leave(ServerPlayer player, RaceControlBlockEntity control) {
        Race race = race(control);
        Racer racer = race == null ? null : race.racers.get(player.getUUID());
        if (racer == null) {
            return ShopActionResult.fail("race.reikocars.not_in");
        }
        if (race.state != State.OPEN) {
            return ShopActionResult.fail("race.reikocars.under_way");
        }
        race.racers.remove(player.getUUID());
        race.pot -= racer.paid;
        refund(player.server, racer);
        if (race.racers.isEmpty()) {
            RACES.remove(race.control);
        }
        return ShopActionResult.ok(Component.translatable("race.reikocars.left", EconomyApi.get().format(racer.paid)));
    }

    /** The owner calls the race off, and everyone gets their fee back. */
    public static ShopActionResult cancel(ServerPlayer player, RaceControlBlockEntity control) {
        Race race = race(control);
        if (race == null) {
            return ShopActionResult.fail("race.reikocars.no_race");
        }
        if (!control.canManage(player)) {
            return ShopActionResult.fail("race.reikocars.owner_only", control.ownerName());
        }
        callOff(player.server, race, "race.reikocars.called_off");
        return ShopActionResult.ok(Component.translatable("race.reikocars.cancelled"));
    }

    /** An entrant (with two or more in) or the owner starts the race: the countdown begins. */
    public static ShopActionResult start(ServerPlayer player, RaceControlBlockEntity control) {
        Race race = race(control);
        if (race == null || race.state != State.OPEN) {
            return ShopActionResult.fail(race == null ? "race.reikocars.no_race" : "race.reikocars.under_way");
        }
        boolean manager = control.canManage(player);
        if (!manager && !race.racers.containsKey(player.getUUID())) {
            return ShopActionResult.fail("race.reikocars.enter_first");
        }
        if (!manager && race.racers.size() < 2) {
            return ShopActionResult.fail("race.reikocars.need_two");
        }
        MinecraftServer server = player.server;
        long now = server.overworld().getGameTime();
        Vec3 line = race.control.pos().getCenter();
        List<Racer> missing = new ArrayList<>();
        for (Racer racer : race.racers.values()) {
            ServerPlayer driver = server.getPlayerList().getPlayer(racer.id);
            CarEntity car = driving(driver);
            if (car == null || car.level().dimension() != race.control.dimension() || flat(car.position(), line) > GRID_REACH) {
                missing.add(racer);
            } else {
                racer.car = car.getUUID();
                racer.carType = car.carType().id();
                racer.last = car.position();
                racer.inLine = flat(car.position(), line) <= LINE_REACH;
            }
        }
        if (missing.size() == race.racers.size()) {
            return ShopActionResult.fail("race.reikocars.nobody_at_line", (int) GRID_REACH);
        }
        for (Racer racer : missing) {
            // Not at the line in a car: not racing, fee back.
            race.racers.remove(racer.id);
            race.pot -= racer.paid;
            refund(server, racer);
            ServerPlayer them = server.getPlayerList().getPlayer(racer.id);
            if (them != null) {
                them.sendSystemMessage(Component.translatable("race.reikocars.not_at_line", (int) GRID_REACH, EconomyApi.get().format(racer.paid)));
            }
        }
        race.state = State.COUNTDOWN;
        race.start = now + COUNTDOWN;
        for (Racer racer : race.racers.values()) {
            racer.lapStart = race.start;
        }
        return ShopActionResult.ok(Component.translatable("race.reikocars.starting", race.racers.size()));
    }

    /** {@code player} starts a time trial: the clock starts when they cross the line in a car. */
    public static ShopActionResult trial(ServerPlayer player, RaceControlBlockEntity control) {
        if (control.owner() == null) {
            return ShopActionResult.fail("race.reikocars.no_owner");
        }
        List<BlockPos> track = track(control);
        if (track.isEmpty()) {
            return ShopActionResult.fail("race.reikocars.no_track", control.ownerName());
        }
        if (racing(player.getUUID())) {
            return ShopActionResult.fail("race.reikocars.busy");
        }
        long now = player.server.overworld().getGameTime();
        Racer racer = new Racer(player.getUUID(), player.getGameProfile().getName());
        racer.inLine = true; // they have to drive out and over the line
        TRIALS.put(player.getUUID(), new Trial(key(control), track, racer, now + CarsConfig.get(CarsConfig.RACE_MINUTES) * 1200L));
        control.recordTrial();
        return ShopActionResult.ok(Component.translatable("race.reikocars.trial_started", track.size()));
    }

    /** Ends {@code player}'s time trial. */
    public static void endTrial(MinecraftServer server, UUID player) {
        Trial trial = TRIALS.remove(player);
        if (trial != null) {
            clearCar(server, trial.racer);
            ServerPlayer driver = server.getPlayerList().getPlayer(player);
            if (driver != null) {
                driver.sendSystemMessage(trial.racer.best < 0 ? Component.translatable("race.reikocars.trial_over_none")
                        : Component.translatable("race.reikocars.trial_over", lapTime(trial.racer.best)));
            }
        }
    }

    // ---- settings ----

    public static ShopActionResult setLaps(ServerPlayer player, RaceControlBlockEntity control, String text) {
        if (!control.canManage(player)) {
            return ShopActionResult.fail("race.reikocars.owner_only", control.ownerName());
        }
        try {
            int laps = Integer.parseInt(text.trim());
            if (laps < 1 || laps > 10) {
                throw new NumberFormatException();
            }
            control.setLaps(laps);
            return ShopActionResult.ok(Component.translatable("race.reikocars.laps_set", laps));
        } catch (NumberFormatException e) {
            return ShopActionResult.fail("race.reikocars.bad_laps");
        }
    }

    public static ShopActionResult setFee(ServerPlayer player, RaceControlBlockEntity control, String text) {
        if (!control.canManage(player)) {
            return ShopActionResult.fail("race.reikocars.owner_only", control.ownerName());
        }
        OptionalLong fee = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (fee.isEmpty() || fee.getAsLong() < 0 || fee.getAsLong() > Money.MAX_BALANCE / (MAX_RACERS + 1)) {
            return ShopActionResult.fail("race.reikocars.bad_fee");
        }
        control.setEntryFee(fee.getAsLong());
        return ShopActionResult.ok(Component.translatable("race.reikocars.fee_set", EconomyApi.get().format(fee.getAsLong())));
    }

    public static ShopActionResult setCut(ServerPlayer player, RaceControlBlockEntity control, String text) {
        if (!control.canManage(player)) {
            return ShopActionResult.fail("race.reikocars.owner_only", control.ownerName());
        }
        try {
            int cut = Integer.parseInt(text.trim().replace("%", ""));
            if (cut < 0 || cut > 100) {
                throw new NumberFormatException();
            }
            control.setCut(cut);
            return ShopActionResult.ok(Component.translatable("race.reikocars.cut_set", cut));
        } catch (NumberFormatException e) {
            return ShopActionResult.fail("race.reikocars.bad_cut");
        }
    }

    // ---- every tick ----

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        Iterator<Race> races = RACES.values().iterator();
        while (races.hasNext()) {
            Race race = races.next();
            if (race.state == State.OPEN) {
                continue;
            }
            if (tickRace(server, race, now)) {
                races.remove();
            }
        }
        Iterator<Map.Entry<UUID, Trial>> trials = TRIALS.entrySet().iterator();
        while (trials.hasNext()) {
            Trial trial = trials.next().getValue();
            if (tickTrial(server, trial, now)) {
                trials.remove();
            }
        }
    }

    /** Moves a race on a tick. True when it's over. */
    private static boolean tickRace(MinecraftServer server, Race race, long now) {
        ServerLevel level = server.getLevel(race.control.dimension());
        if (level == null) {
            callOff(server, race, "race.reikocars.called_off");
            return true;
        }
        if (race.state == State.COUNTDOWN) {
            long left = race.start - now;
            if (left % 20 == 0 && left > 0) {
                forEachDriver(server, race, driver -> driver.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 0.8F));
            }
            if (left <= 0) {
                race.state = State.RUNNING;
                forEachDriver(server, race, driver -> driver.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.6F));
            }
        }
        List<Racer> order = standings(race, level);
        int place = 0;
        for (Racer racer : order) {
            place++;
            ServerPlayer driver = server.getPlayerList().getPlayer(racer.id);
            CarEntity car = driving(driver);
            if (racer.finished >= 0 || racer.out) {
                continue;
            }
            if (car == null || !car.getUUID().equals(racer.car)) {
                knockOut(server, racer, "race.reikocars.out_left_car");
                continue;
            }
            car.setRace(race.start, new RaceInfo(Math.min(racer.lap + 1, race.laps), race.laps, racer.next + 1, race.track.size() + 1, place,
                    race.racers.size()).pack());
            if (race.state != State.RUNNING) {
                racer.last = car.position();
                continue;
            }
            if (advance(racer, race.track, race.control, car)) {
                long lap = now - racer.lapStart;
                record(server, race.control, racer, lap, driver);
                racer.lapStart = now;
                racer.lap++;
                if (racer.lap >= race.laps) {
                    finish(server, race, racer, now, driver);
                } else if (driver != null) {
                    driver.displayClientMessage(Component.translatable("race.reikocars.lap_done", racer.lap, race.laps, lapTime(lap)), true);
                }
            }
        }
        boolean allDone = race.racers.values().stream().allMatch(racer -> racer.finished >= 0 || racer.out);
        boolean late = race.state == State.RUNNING && (race.winnerAt >= 0 && now - race.winnerAt > AFTER_WINNER
                || now - race.start > CarsConfig.get(CarsConfig.RACE_MINUTES) * 1200L);
        if (allDone || late) {
            for (Racer racer : race.racers.values()) {
                if (racer.finished < 0 && !racer.out) {
                    knockOut(server, racer, "race.reikocars.out_time");
                }
                clearCar(server, racer);
            }
            if (race.winnerAt < 0) {
                // Nobody finished: everyone's fee back.
                race.racers.values().forEach(racer -> refund(server, racer));
                race.pot = 0;
            }
            return true;
        }
        return false;
    }

    private static void finish(MinecraftServer server, Race race, Racer racer, long now, @Nullable ServerPlayer driver) {
        racer.finished = now;
        long time = now - race.start;
        int position = (int) race.racers.values().stream().filter(other -> other.finished >= 0).count();
        EconomyApi eco = EconomyApi.get();
        if (race.winnerAt < 0) {
            race.winnerAt = now;
            long house = race.pot * race.cut / 100;
            long prize = race.pot - house;
            if (prize > 0) {
                eco.deposit(server, racer.id, prize, "reikocars.race_prize");
            }
            if (house > 0) {
                eco.deposit(server, race.owner, house, "reikocars.race_house");
            }
            ServerLevel level = server.getLevel(race.control.dimension());
            if (level != null && level.isLoaded(race.control.pos()) && level.getBlockEntity(race.control.pos()) instanceof RaceControlBlockEntity control) {
                control.recordRace(house);
            }
            race.pot = 0;
            for (Racer other : race.racers.values()) {
                ServerPlayer them = server.getPlayerList().getPlayer(other.id);
                if (them != null) {
                    them.sendSystemMessage((other == racer ? Component.translatable("race.reikocars.you_won", lapTime(time), eco.format(prize))
                            : Component.translatable("race.reikocars.winner", racer.name, lapTime(time))).withStyle(ChatFormatting.GOLD));
                }
            }
            if (driver != null) {
                driver.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            com.reiko.cars.advancement.CarAdvancements.award(server.getPlayerList().getPlayer(racer.id), "race_win");
        } else if (driver != null) {
            driver.sendSystemMessage(Component.translatable("race.reikocars.finished", position, lapTime(time)).withStyle(ChatFormatting.AQUA));
        }
        clearCar(server, racer);
    }

    /** Moves a time trial on a tick. True when it's over. */
    private static boolean tickTrial(MinecraftServer server, Trial trial, long now) {
        Racer racer = trial.racer;
        ServerPlayer driver = server.getPlayerList().getPlayer(racer.id);
        if (driver == null || now > trial.deadline) {
            clearCar(server, racer);
            if (driver != null) {
                driver.sendSystemMessage(Component.translatable("race.reikocars.trial_time_up"));
            }
            return true;
        }
        CarEntity car = driving(driver);
        if (racer.car == null) {
            if (car == null) {
                return false; // not in a car yet
            }
            racer.car = car.getUUID();
            racer.carType = car.carType().id();
            racer.last = car.position();
        } else if (car == null || !car.getUUID().equals(racer.car)) {
            clearCar(server, racer);
            driver.sendSystemMessage(racer.best < 0 ? Component.translatable("race.reikocars.trial_over_none")
                    : Component.translatable("race.reikocars.trial_over", lapTime(racer.best)));
            return true;
        }
        if (car.level().dimension() != trial.control.dimension()) {
            return false;
        }
        Vec3 line = trial.control.pos().getCenter();
        if (!trial.running) {
            boolean in = flat(car.position(), line) <= LINE_REACH;
            if (in && !racer.inLine) {
                trial.running = true;
                racer.lapStart = now;
                racer.next = 0;
                driver.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.6F);
            }
            racer.inLine = in;
            racer.last = car.position();
        } else if (advance(racer, trial.track, trial.control, car)) {
            long lap = now - racer.lapStart;
            boolean best = racer.best < 0 || lap < racer.best;
            record(server, trial.control, racer, lap, driver);
            racer.lap++;
            racer.lapStart = now;
            driver.sendSystemMessage(Component.translatable(best ? "race.reikocars.trial_lap_best" : "race.reikocars.trial_lap", racer.lap,
                    lapTime(lap), lapTime(racer.best)).withStyle(best ? ChatFormatting.GREEN : ChatFormatting.AQUA));
        }
        car.setRace(trial.running ? racer.lapStart : -1, new RaceInfo(racer.lap + 1, 0, racer.next + 1, trial.track.size() + 1, 0, 0).pack());
        return false;
    }

    /**
     * Moves {@code racer} on past whatever they've just driven by: the next flag, or the line once they've passed them
     * all. True when that completed a lap.
     */
    private static boolean advance(Racer racer, List<BlockPos> track, GlobalPos control, CarEntity car) {
        Vec3 now = car.position();
        Vec3 before = racer.last == null ? now : racer.last;
        racer.last = now;
        Vec3 line = control.pos().getCenter();
        boolean in = flat(now, line) <= LINE_REACH;
        boolean crossed = in && !racer.inLine;
        racer.inLine = in;
        if (racer.next < track.size()) {
            BlockPos flag = track.get(racer.next);
            if (Math.abs(now.y - flag.getY()) <= 6.0 && segmentDistance(before, now, flag.getCenter()) <= FLAG_REACH) {
                racer.next++;
            }
            return false;
        }
        if (crossed) {
            racer.next = 0;
            return true;
        }
        return false;
    }

    private static void record(MinecraftServer server, GlobalPos at, Racer racer, long lap, @Nullable ServerPlayer driver) {
        if (racer.best < 0 || lap < racer.best) {
            racer.best = lap;
        }
        ServerLevel level = server.getLevel(at.dimension());
        if (level != null && level.isLoaded(at.pos()) && level.getBlockEntity(at.pos()) instanceof RaceControlBlockEntity control) {
            int place = control.recordLap(new RaceControlBlockEntity.Lap(racer.id, racer.name, racer.carType, lap, level.getGameTime()));
            if (place >= 0 && driver != null) {
                driver.sendSystemMessage(Component.translatable("race.reikocars.board", lapTime(lap), place + 1).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** The racers in race order: finished first (soonest first), then by laps and flags done and how near the next is. */
    private static List<Racer> standings(Race race, ServerLevel level) {
        List<Racer> order = new ArrayList<>(race.racers.values());
        order.sort(Comparator.<Racer>comparingInt(racer -> racer.out ? 2 : racer.finished >= 0 ? 0 : 1)
                .thenComparingLong(racer -> racer.finished >= 0 ? racer.finished : 0)
                .thenComparingInt(racer -> -racer.lap)
                .thenComparingInt(racer -> -racer.next)
                .thenComparingDouble(racer -> {
                    if (racer.last == null) {
                        return Double.MAX_VALUE;
                    }
                    BlockPos target = racer.next < race.track.size() ? race.track.get(racer.next) : race.control.pos();
                    return flat(racer.last, target.getCenter());
                }));
        return order;
    }

    private static void knockOut(MinecraftServer server, Racer racer, String why) {
        racer.out = true;
        clearCar(server, racer);
        ServerPlayer driver = server.getPlayerList().getPlayer(racer.id);
        if (driver != null) {
            driver.sendSystemMessage(Component.translatable(why).withStyle(ChatFormatting.RED));
        }
    }

    private static void clearCar(MinecraftServer server, Racer racer) {
        if (racer.car == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(racer.car) instanceof CarEntity car) {
                car.setRace(0, 0);
                return;
            }
        }
    }

    private static void refund(MinecraftServer server, Racer racer) {
        if (racer.paid > 0) {
            EconomyApi.get().deposit(server, racer.id, racer.paid, "reikocars.race_refund");
            racer.paid = 0;
        }
    }

    /** Calls off a race: everyone's fee back, and a word to each. */
    private static void callOff(MinecraftServer server, Race race, String why) {
        for (Racer racer : race.racers.values()) {
            refund(server, racer);
            clearCar(server, racer);
            ServerPlayer driver = server.getPlayerList().getPlayer(racer.id);
            if (driver != null) {
                driver.sendSystemMessage(Component.translatable(why));
            }
        }
        race.pot = 0;
        RACES.remove(race.control);
    }

    /** The Race Control at {@code pos} was broken: its race is off, and its trials are over. */
    static void controlGone(ServerLevel level, BlockPos pos) {
        GlobalPos at = GlobalPos.of(level.dimension(), pos);
        Race race = RACES.get(at);
        if (race != null) {
            callOff(level.getServer(), race, "race.reikocars.called_off");
        }
        List<UUID> ending = TRIALS.entrySet().stream().filter(entry -> entry.getValue().control.equals(at)).map(Map.Entry::getKey).toList();
        ending.forEach(id -> endTrial(level.getServer(), id));
    }

    /** The server's stopping: every race is off, with the fees back. */
    public static void stopAll(MinecraftServer server) {
        new ArrayList<>(RACES.values()).forEach(race -> callOff(server, race, "race.reikocars.called_off"));
        new ArrayList<>(TRIALS.keySet()).forEach(id -> endTrial(server, id));
    }

    private static void forEachDriver(MinecraftServer server, Race race, java.util.function.Consumer<ServerPlayer> action) {
        for (Racer racer : race.racers.values()) {
            ServerPlayer driver = server.getPlayerList().getPlayer(racer.id);
            if (driver != null) {
                action.accept(driver);
            }
        }
    }

    @Nullable
    private static CarEntity driving(@Nullable ServerPlayer player) {
        return player != null && player.getVehicle() instanceof CarEntity car && car.getControllingPassenger() == player ? car : null;
    }

    private static double flat(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** How close the ground track from {@code a} to {@code b} comes to {@code p}. */
    static double segmentDistance(Vec3 a, Vec3 b, Vec3 p) {
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        double length = dx * dx + dz * dz;
        double t = length < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((p.x - a.x) * dx + (p.z - a.z) * dz) / length));
        double x = a.x + dx * t - p.x;
        double z = a.z + dz * t - p.z;
        return Math.sqrt(x * x + z * z);
    }

    /** {@code ticks} as a lap time: "1:02.35". */
    public static String lapTime(long ticks) {
        long hundredths = Math.max(0, ticks) * 5;
        return String.format(Locale.ROOT, "%d:%02d.%02d", hundredths / 6000, hundredths / 100 % 60, hundredths % 100);
    }
}
