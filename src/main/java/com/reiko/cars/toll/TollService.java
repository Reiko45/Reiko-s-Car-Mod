package com.reiko.cars.toll;

import com.reiko.cars.car.CarEntity;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Tolls. A driver who pulls up to a closed toll gate and presses the hitch key (G) pays the toll of the owner's booth
 * nearby, by card or cash plus sales tax, and the gate lifts for {@link #OPEN_TICKS} ticks (and stays up while anything's
 * under it). The gate's owner drives through free. A gate with no booth only opens for its owner.
 */
public final class TollService {
    /** How long a gate stays up once paid, in ticks. */
    public static final int OPEN_TICKS = 100;
    /** How far a gate looks for its owner's booth, in blocks. */
    public static final int BOOTH_REACH = 12;

    private TollService() {
    }

    /** The pole of the closed toll gate just ahead of {@code car}, if there's one. Works on either side. */
    @Nullable
    public static BlockPos gateAhead(CarEntity car) {
        Level level = car.level();
        Vec3 front = car.position().add(car.forward().scale(car.carType().length / 2.0 + 1.0));
        BlockPos center = BlockPos.containing(front);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 1, 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TollGateBlock && !state.getValue(TollGateBlock.OPEN)) {
                double distance = pos.getCenter().distanceToSqr(front.x, pos.getCenter().y, front.z);
                if (distance < bestDistance && distance <= 2.5 * 2.5) {
                    bestDistance = distance;
                    best = TollGateBlock.polePos(state, pos.immutable());
                }
            }
        }
        return best;
    }

    /** The booth whose toll {@code pole}'s gate charges: its owner's nearest within reach. */
    @Nullable
    public static TollBoothBlockEntity boothFor(Level level, BlockPos pole) {
        if (!(level.getBlockEntity(pole) instanceof TollGateBlockEntity gate) || gate.owner() == null) {
            return null;
        }
        TollBoothBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(pole.offset(-BOOTH_REACH, -3, -BOOTH_REACH), pole.offset(BOOTH_REACH, 3, BOOTH_REACH))) {
            if (level.getBlockEntity(pos) instanceof TollBoothBlockEntity booth && gate.owner().equals(booth.owner())) {
                double distance = pos.distSqr(pole);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = booth;
                }
            }
        }
        return best;
    }

    /** The hitch key at a closed gate, on the server: through free for its owner, else the pay screen. */
    public static boolean use(ServerPlayer driver, CarEntity car) {
        BlockPos pole = gateAhead(car);
        if (pole == null || !(car.level() instanceof ServerLevel level) || !(level.getBlockEntity(pole) instanceof TollGateBlockEntity gate)) {
            return false;
        }
        if (gate.isOwner(driver)) {
            lift(level, pole, OPEN_TICKS);
            driver.displayClientMessage(Component.translatable("toll.reikocars.your_gate"), true);
            return true;
        }
        TollBoothBlockEntity booth = boothFor(level, pole);
        if (booth == null) {
            driver.displayClientMessage(Component.translatable("toll.reikocars.gate_closed", gate.ownerName()), true);
            return true;
        }
        if (booth.toll() <= 0) {
            booth.recordToll(0);
            lift(level, pole, OPEN_TICKS);
            driver.displayClientMessage(Component.translatable("toll.reikocars.free"), true);
            return true;
        }
        TollMenu.open(driver, pole, booth);
        return true;
    }

    /**
     * {@code driver} pays the toll at the gate {@code pole}, by card or cash plus sales tax, to the booth's owner.
     *
     * @param expected the toll the screen showed, before tax
     */
    public static ShopActionResult pay(ServerPlayer driver, BlockPos pole, BlockPos boothPos, long expected, Payment payment) {
        ServerLevel level = driver.serverLevel();
        if (!(level.getBlockEntity(boothPos) instanceof TollBoothBlockEntity booth) || booth.owner() == null
                || !(level.getBlockState(pole).getBlock() instanceof TollGateBlock)) {
            return ShopActionResult.fail("toll.reikocars.gone");
        }
        long toll = booth.toll();
        EconomyApi eco = EconomyApi.get();
        if (toll != expected) {
            return ShopActionResult.fail("toll.reikocars.toll_changed", eco.format(toll + EconomyConfig.taxRates().salesTax(toll)));
        }
        long tax = EconomyConfig.taxRates().salesTax(toll);
        if (toll + tax > Money.MAX_BALANCE) {
            return ShopActionResult.fail("toll.reikocars.bad_price");
        }
        Component description = Component.translatable("toll.reikocars.bill_line", booth.ownerName());
        ShopPayments.Charge charge = ShopPayments.charge(driver, payment, toll, tax, booth.owner(), "reikocars.toll", description);
        if (!charge.success()) {
            return ShopActionResult.fail(charge.message());
        }
        booth.recordToll(toll);
        lift(level, pole, OPEN_TICKS);
        Component line = tax > 0
                ? Component.translatable("toll.reikocars.paid_tax", eco.format(toll + tax), eco.format(tax))
                : Component.translatable("toll.reikocars.paid", eco.format(toll));
        return ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** Opens the gate at {@code pole} and shuts it again after {@code ticks}, once the way's clear. */
    public static void lift(Level level, BlockPos pole, int ticks) {
        setOpen(level, pole, true);
        level.scheduleTick(pole, level.getBlockState(pole).getBlock(), ticks);
    }

    /** The owner holds the gate open, or lets it shut. */
    public static void toggleHeld(ServerPlayer player, ServerLevel level, BlockPos pole) {
        if (!(level.getBlockEntity(pole) instanceof TollGateBlockEntity gate)) {
            return;
        }
        gate.setHeld(!gate.isHeld());
        if (gate.isHeld()) {
            setOpen(level, pole, true);
            player.displayClientMessage(Component.translatable("toll.reikocars.held_open"), true);
        } else {
            level.scheduleTick(pole, level.getBlockState(pole).getBlock(), 1);
            player.displayClientMessage(Component.translatable("toll.reikocars.let_close"), true);
        }
    }

    /** The gate's time is up: it shuts, unless it's held open or something's under the arm. */
    public static void tryClose(Level level, BlockPos pole) {
        if (!(level.getBlockEntity(pole) instanceof TollGateBlockEntity gate) || gate.isHeld()) {
            return;
        }
        BlockState state = level.getBlockState(pole);
        if (!state.getValue(TollGateBlock.OPEN)) {
            return;
        }
        AABB under = armBox(state, pole);
        if (!level.getEntitiesOfClass(Entity.class, under, entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable()).isEmpty()) {
            level.scheduleTick(pole, state.getBlock(), 10); // wait for it to clear
            return;
        }
        setOpen(level, pole, false);
    }

    /** Where the closed arm's wall stands, over all its blocks. */
    public static AABB armBox(BlockState state, BlockPos pole) {
        Direction arm = TollGateBlock.armDirection(state);
        AABB box = TollGateBlock.wall(state).bounds().move(pole);
        for (int i = 1; i <= TollGateBlock.ARMS; i++) {
            box = box.minmax(TollGateBlock.wall(state).bounds().move(pole.relative(arm, i)));
        }
        return box;
    }

    private static void setOpen(Level level, BlockPos pole, boolean open) {
        BlockState state = level.getBlockState(pole);
        if (!(state.getBlock() instanceof TollGateBlock) || state.getValue(TollGateBlock.OPEN) == open) {
            return;
        }
        Direction arm = TollGateBlock.armDirection(state);
        for (int i = 0; i <= TollGateBlock.ARMS; i++) {
            BlockPos at = pole.relative(arm, i);
            BlockState part = level.getBlockState(at);
            if (part.getBlock() instanceof TollGateBlock) {
                level.setBlock(at, part.setValue(TollGateBlock.OPEN, open), Block.UPDATE_ALL);
            }
        }
        level.playSound(null, pole, open ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6F, open ? 1.2F : 1.0F);
    }

    /** The booth's owner sets the toll. */
    public static ShopActionResult setToll(ServerPlayer player, TollBoothBlockEntity booth, String text) {
        if (!booth.canManage(player) || booth.owner() == null) {
            return ShopActionResult.fail("toll.reikocars.owner_only", booth.ownerName());
        }
        OptionalLong toll = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (toll.isEmpty() || toll.getAsLong() < 0 || toll.getAsLong() > Money.MAX_BALANCE / 100) {
            return ShopActionResult.fail("toll.reikocars.bad_price");
        }
        booth.setToll(toll.getAsLong());
        return ShopActionResult.ok(Component.translatable("toll.reikocars.toll_set", EconomyApi.get().format(toll.getAsLong())));
    }
}
