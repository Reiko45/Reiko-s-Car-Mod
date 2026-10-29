package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.fare.FareMenu;
import com.reiko.cars.fare.FareService;
import com.reiko.cars.fare.FareSettingsMenu;
import com.reiko.cars.mail.MailService;
import com.reiko.cars.car.CarType;
import com.reiko.cars.sale.CarSaleMenu;
import com.reiko.cars.sale.DealershipMenu;
import com.reiko.cars.sale.SaleService;
import com.reiko.cars.parking.MeterService;
import com.reiko.cars.parking.ParkingMeterMenu;
import com.reiko.cars.toll.TollBoothMenu;
import com.reiko.cars.toll.TollMenu;
import com.reiko.cars.toll.TollService;
import com.reiko.cars.mail.MailboxMenu;
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
        registrar.playToServer(MailActionPayload.TYPE, MailActionPayload.STREAM_CODEC, ModNetwork::handleMail);
        registrar.playToServer(FareActionPayload.TYPE, FareActionPayload.STREAM_CODEC, ModNetwork::handleFare);
        registrar.playToServer(SaleActionPayload.TYPE, SaleActionPayload.STREAM_CODEC, ModNetwork::handleSale);
        registrar.playToServer(MeterActionPayload.TYPE, MeterActionPayload.STREAM_CODEC, ModNetwork::handleMeter);
        registrar.playToServer(TollActionPayload.TYPE, TollActionPayload.STREAM_CODEC, ModNetwork::handleToll);
    }

    private static void handleWear(CarWearPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.getVehicle() instanceof CarEntity car)
                || !Float.isFinite(payload.braked()) || !Float.isFinite(payload.skidded()) || !Float.isFinite(payload.impact())) {
            return;
        }
        car.applyWearReport(player, Math.max(0.0F, payload.braked()), Math.max(0.0F, payload.skidded()), Math.max(0.0F, payload.impact()));
    }

    private static void handleMeter(MeterActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof ParkingMeterMenu menu)
                || menu.containerId != payload.containerId() || !menu.stillValid(player) || menu.meter() == null) {
            return;
        }
        ShopActionResult result;
        if (payload.action() == MeterActionPayload.PAY) {
            result = MeterService.pay(player, menu.meter(), payload.minutes(), payload.expected(), payload.payment());
        } else {
            result = MeterService.setPrices(player, menu.meter(), payload.rate(), payload.fine());
            if (result.success()) {
                ParkingMeterMenu.open(player, menu.meter()); // reopen to show the new prices
            }
        }
        menu.refresh();
        ShopFeedback.send(player, result);
    }

    private static void handleToll(TollActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.containerMenu.containerId != payload.containerId()
                || !player.containerMenu.stillValid(player)) {
            return;
        }
        if (payload.action() == TollActionPayload.PAY && player.containerMenu instanceof TollMenu menu) {
            ShopActionResult result = TollService.pay(player, menu.pole(), menu.booth(), payload.expected(), payload.payment());
            if (result.success()) {
                player.closeContainer();
                player.displayClientMessage(result.message(), true);
            } else {
                ShopFeedback.send(player, result);
            }
        } else if (payload.action() == TollActionPayload.SET && player.containerMenu instanceof TollBoothMenu menu && menu.booth() != null) {
            ShopActionResult result = TollService.setToll(player, menu.booth(), payload.text());
            if (result.success()) {
                TollBoothMenu.open(player, menu.booth());
            }
            ShopFeedback.send(player, result);
        }
    }

    private static void handleSale(SaleActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.containerMenu.containerId != payload.containerId()
                || !player.containerMenu.stillValid(player)) {
            return;
        }
        if (player.containerMenu instanceof CarSaleMenu menu && menu.car() != null) {
            CarEntity car = menu.car();
            switch (payload.action()) {
                case SaleActionPayload.BUY -> {
                    ShopActionResult result = SaleService.buy(player, car, payload.expected(), payload.payment());
                    if (result.success()) {
                        player.closeContainer();
                        player.sendSystemMessage(result.message());
                    } else {
                        ShopFeedback.send(player, result);
                    }
                }
                case SaleActionPayload.SET_PRICE, SaleActionPayload.OFF_SALE -> {
                    ShopActionResult result = payload.action() == SaleActionPayload.SET_PRICE
                            ? SaleService.setPrice(player, car, payload.text()) : SaleService.takeOffSale(player, car);
                    if (result.success()) {
                        CarSaleMenu.open(player, car); // reopen to show it
                    }
                    ShopFeedback.send(player, result);
                }
                default -> {
                }
            }
            return;
        }
        if (!(player.containerMenu instanceof DealershipMenu menu) || menu.lot() == null) {
            return;
        }
        CarEntity car = menu.selected();
        ShopActionResult result = switch (payload.action()) {
            case SaleActionPayload.OPEN_BUY -> {
                if (car != null && car.isForSale()) {
                    CarSaleMenu.open(player, car);
                    yield null;
                }
                yield ShopActionResult.fail(Component.translatable("sale.reikocars.nothing_picked"));
            }
            case SaleActionPayload.TRADE_IN -> car == null ? ShopActionResult.fail(Component.translatable("sale.reikocars.nothing_picked"))
                    : SaleService.tradeIn(player, menu.lot(), car, payload.expected());
            case SaleActionPayload.SET_PRICE -> car == null ? ShopActionResult.fail(Component.translatable("sale.reikocars.nothing_picked"))
                    : SaleService.setPrice(player, car, payload.text());
            case SaleActionPayload.OFF_SALE -> car == null ? ShopActionResult.fail(Component.translatable("sale.reikocars.nothing_picked"))
                    : SaleService.takeOffSale(player, car);
            case SaleActionPayload.SET_OFFER -> {
                if (payload.carType() < 0 || payload.carType() >= CarType.values().length) {
                    yield ShopActionResult.fail(Component.literal("?"));
                }
                ShopActionResult set = SaleService.setOffer(player, menu.lot(), CarType.values()[payload.carType()], payload.text());
                if (set.success()) {
                    DealershipMenu.open(player, menu.lot()); // reopen so the screen shows the new offers
                }
                yield set;
            }
            default -> ShopActionResult.fail(Component.literal("?"));
        };
        menu.refresh();
        if (result != null) {
            ShopFeedback.send(player, result);
        }
    }

    private static void handleFare(FareActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.containerMenu.containerId != payload.containerId()
                || !player.containerMenu.stillValid(player)) {
            return;
        }
        if (payload.action() == FareActionPayload.PAY && player.containerMenu instanceof FareMenu menu && menu.bus() != null) {
            ShopActionResult result = FareService.payBusFare(player, menu.bus(), payload.expected(), payload.payment());
            if (result.success()) {
                player.closeContainer();
                player.displayClientMessage(result.message(), true);
            } else {
                ShopFeedback.send(player, result);
            }
        } else if (payload.action() == FareActionPayload.SET && player.containerMenu instanceof FareSettingsMenu menu && menu.car() != null) {
            ShopFeedback.send(player, FareService.setFares(player, menu.car(), payload.start(), payload.per100()));
        }
    }

    private static void handleMail(MailActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof MailboxMenu menu)
                || menu.containerId != payload.containerId()
                || !menu.stillValid(player)) {
            return;
        }
        ShopFeedback.send(player, MailService.send(player, menu, payload.recipient(), payload.expectedPostage(), payload.payment()));
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
            case RepairActionPayload.FIT -> RepairService.fitOwn(player, bench, menu.target(), payload.parts());
            case RepairActionPayload.BUY -> RepairService.buy(player, bench, menu.target(), payload.parts(), payload.expectedTotal(), payload.payment());
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
