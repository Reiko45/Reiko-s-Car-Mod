package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.trailer.Hitching;
import com.reiko.cars.trailer.TankerService;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.pump.GasPumpBlockEntity;
import com.reiko.cars.pump.PumpMenu;
import com.reiko.cars.pump.PumpService;
import com.reiko.cars.repair.RepairBenchBlockEntity;
import com.reiko.cars.repair.RepairBenchMenu;
import com.reiko.cars.repair.RepairService;
import com.reiko.economy.network.ShopFeedback;
import com.reiko.economy.shop.ShopActionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the packets and handles them on the server. Replies to the pump screen go back through the economy
 * mod's shop feedback packet, which the pump screen (a shop screen) shows on its status line.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(PumpActionPayload.TYPE, PumpActionPayload.STREAM_CODEC, ModNetwork::handlePump);
        registrar.playToServer(CarActionPayload.TYPE, CarActionPayload.STREAM_CODEC, ModNetwork::handleCar);
        registrar.playToServer(TrailerActionPayload.TYPE, TrailerActionPayload.STREAM_CODEC, ModNetwork::handleTrailer);
        registrar.playToServer(CarWearPayload.TYPE, CarWearPayload.STREAM_CODEC, ModNetwork::handleWear);
        registrar.playToServer(RepairActionPayload.TYPE, RepairActionPayload.STREAM_CODEC, ModNetwork::handleRepair);
    }

    private static void handleWear(CarWearPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.getVehicle() instanceof CarEntity car)
                || !Float.isFinite(payload.braked()) || !Float.isFinite(payload.skidded()) || !Float.isFinite(payload.impact())) {
            return;
        }
        car.applyWearReport(player, Math.max(0.0F, payload.braked()), Math.max(0.0F, payload.skidded()), Math.max(0.0F, payload.impact()));
    }

    private static void handleRepair(RepairActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof RepairBenchMenu menu)
                || menu.containerId != payload.containerId()
                || !menu.stillValid(player)) {
            return;
        }
        RepairBenchBlockEntity bench = menu.bench();
        if (bench == null) {
            return;
        }
        ShopActionResult result = switch (payload.action()) {
            case RepairActionPayload.FIT -> RepairService.fitOwn(player, bench, menu.car(), payload.parts());
            case RepairActionPayload.BUY -> RepairService.buy(player, bench, menu.car(), payload.parts(), payload.expectedTotal(), payload.payment());
            case RepairActionPayload.SET_PRICE -> {
                ShopActionResult set = RepairService.setPrice(player, bench, payload.parts(), payload.priceText());
                if (set.success()) {
                    RepairBenchMenu.open(player, bench); // reopen so the screen shows the new price
                }
                yield set;
            }
            default -> ShopActionResult.fail(Component.literal("?"));
        };
        menu.refresh();
        ShopFeedback.send(player, result);
    }

    private static void handlePump(PumpActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof PumpMenu menu)
                || menu.containerId != payload.containerId()
                || !menu.stillValid(player)) {
            return;
        }
        GasPumpBlockEntity pump = menu.pump();
        if (pump == null) {
            return;
        }
        ShopActionResult result = switch (payload.action()) {
            case PumpActionPayload.FILL -> PumpService.fill(player, pump, menu.car(), payload.millibuckets(),
                    payload.expectedPrice(), payload.payment());
            case PumpActionPayload.FILL_BUCKETS -> PumpService.fillBuckets(player, pump, payload.millibuckets(), payload.expectedPrice(),
                    payload.payment());
            case PumpActionPayload.SET_PRICE -> {
                ShopActionResult set = PumpService.setPrice(player, pump, payload.priceText());
                if (set.success()) {
                    PumpMenu.open(player, pump); // reopen so the screen shows the new price
                }
                yield set;
            }
            default -> ShopActionResult.fail(Component.literal("?"));
        };
        ShopFeedback.send(player, result);
    }

    private static void handleCar(CarActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.getVehicle() instanceof CarEntity car)) {
            return;
        }
        switch (payload.action()) {
            case CarActionPayload.HORN -> {
                if (car.getControllingPassenger() == player) {
                    car.honk();
                }
            }
            case CarActionPayload.TOGGLE_LOCK -> {
                if (!car.toggleLock(player)) {
                    player.displayClientMessage(Component.translatable("car.reikocars.not_yours", car.ownerName()), true);
                }
            }
            case CarActionPayload.HITCH -> Hitching.use(player);
            default -> {
            }
        }
    }

    private static void handleTrailer(TrailerActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.level().getEntity(payload.entityId()) instanceof TrailerEntity tanker)
                || tanker.trailerType().tankBuckets <= 0
                || player.distanceToSqr(tanker) > 12.0 * 12.0
                || !tanker.canBeUsedBy(player)) {
            return;
        }
        switch (payload.action()) {
            case TrailerActionPayload.LOAD -> TankerService.load(player, tanker);
            case TrailerActionPayload.UNLOAD -> TankerService.unload(player, tanker);
            default -> {
            }
        }
    }
}
