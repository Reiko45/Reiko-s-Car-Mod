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
        registrar.playToServer(ChargerActionPayload.TYPE, ChargerActionPayload.STREAM_CODEC, ModNetwork::handleCharger);
        registrar.playToServer(CarActionPayload.TYPE, CarActionPayload.STREAM_CODEC, ModNetwork::handleCar);
        registrar.playToServer(TrailerActionPayload.TYPE, TrailerActionPayload.STREAM_CODEC, ModNetwork::handleTrailer);
        registrar.playToServer(CarWearPayload.TYPE, CarWearPayload.STREAM_CODEC, ModNetwork::handleWear);
        registrar.playToServer(RepairActionPayload.TYPE, RepairActionPayload.STREAM_CODEC, ModNetwork::handleRepair);
        registrar.playToServer(MailActionPayload.TYPE, MailActionPayload.STREAM_CODEC, ModNetwork::handleMail);
        registrar.playToServer(FareActionPayload.TYPE, FareActionPayload.STREAM_CODEC, ModNetwork::handleFare);
        registrar.playToServer(SaleActionPayload.TYPE, SaleActionPayload.STREAM_CODEC, ModNetwork::handleSale);
        registrar.playToServer(LotActionPayload.TYPE, LotActionPayload.STREAM_CODEC, ModNetwork::handleLot);
        registrar.playToServer(TuningActionPayload.TYPE, TuningActionPayload.STREAM_CODEC, ModNetwork::handleTuning);
        registrar.playToServer(MeterActionPayload.TYPE, MeterActionPayload.STREAM_CODEC, ModNetwork::handleMeter);
        registrar.playToServer(TollActionPayload.TYPE, TollActionPayload.STREAM_CODEC, ModNetwork::handleToll);
        registrar.playToServer(InsuranceActionPayload.TYPE, InsuranceActionPayload.STREAM_CODEC, ModNetwork::handleInsurance);
        registrar.playToServer(IceCreamActionPayload.TYPE, IceCreamActionPayload.STREAM_CODEC, ModNetwork::handleIceCream);
        registrar.playToServer(RoadSignActionPayload.TYPE, RoadSignActionPayload.STREAM_CODEC, ModNetwork::handleRoadSign);
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

    private static void handleInsurance(InsuranceActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof com.reiko.cars.insurance.InsuranceMenu menu)
                || menu.containerId != payload.containerId() || !menu.stillValid(player) || menu.office() == null) {
            return;
        }
        ShopActionResult result;
        if (payload.action() == InsuranceActionPayload.BUY) {
            result = com.reiko.cars.insurance.InsuranceService.buy(player, menu.office(), menu.selected(), payload.expected(), payload.payment());
        } else {
            result = com.reiko.cars.insurance.InsuranceService.setTerms(player, menu.office(), payload.premium(), payload.deductible(), payload.limit());
            if (result.success()) {
                com.reiko.cars.insurance.InsuranceMenu.open(player, menu.office()); // reopen to show the new terms
            }
        }
        menu.refresh();
        ShopFeedback.send(player, result);
    }

    private static void handleRoadSign(RoadSignActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof com.reiko.cars.traffic.RoadSignMenu menu)
                || menu.containerId != payload.containerId() || !menu.stillValid(player) || menu.sign() == null) {
            return;
        }
        ShopActionResult result = com.reiko.cars.traffic.RoadSignService.set(player, menu.sign(), payload.fine(), payload.limit(), payload.kmh());
        if (result.success()) {
            com.reiko.cars.traffic.RoadSignMenu.open(player, menu.sign()); // reopen with the new settings
        }
        ShopFeedback.send(player, result);
    }

    private static void handleIceCream(IceCreamActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof com.reiko.cars.icecream.IceCreamMenu menu)
                || menu.containerId != payload.containerId() || !menu.stillValid(player) || menu.truck() == null || menu.selected() == null) {
            return;
        }
        ShopActionResult result = payload.action() == IceCreamActionPayload.BUY
                ? com.reiko.cars.icecream.IceCreamService.buy(player, menu.truck(), menu.selected(), payload.expected(), payload.payment())
                : com.reiko.cars.icecream.IceCreamService.setPrice(player, menu.truck(), menu.selected(), payload.text());
        menu.refresh();
        ShopFeedback.send(player, result);
    }

    private static void handleTuning(TuningActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof com.reiko.cars.tuning.TuningBenchMenu menu)
                || menu.containerId != payload.containerId() || !menu.stillValid(player) || menu.bench() == null) {
            return;
        }
        com.reiko.cars.tuning.TuningKit[] kits = com.reiko.cars.tuning.TuningKit.values();
        ShopActionResult result = switch (payload.action()) {
            case TuningActionPayload.FIT -> payload.value() < 0 || payload.value() >= kits.length ? null
                    : com.reiko.cars.tuning.TuningService.fitOwn(player, menu.bench(), menu.target(), kits[payload.value()]);
            case TuningActionPayload.BUY -> com.reiko.cars.tuning.TuningService.buy(player, menu.bench(), menu.target(), payload.value(),
                    payload.expected(), payload.payment());
            case TuningActionPayload.SET_PRICE -> {
                if (payload.value() < 0 || payload.value() >= kits.length) {
                    yield null;
                }
                ShopActionResult set = com.reiko.cars.tuning.TuningService.setPrice(player, menu.bench(), kits[payload.value()], payload.text());
                if (set.success()) {
                    com.reiko.cars.tuning.TuningBenchMenu.open(player, menu.bench()); // reopen to show the new price
                }
                yield set;
            }
            case TuningActionPayload.LIVERY -> {
                com.reiko.cars.tuning.Livery livery = com.reiko.cars.tuning.Livery.unpack(payload.value());
                yield com.reiko.cars.tuning.TuningService.setLivery(player, menu.bench(), menu.target(), livery.number(), livery.stripeColor());
            }
            default -> null;
        };
        if (result != null) {
            if (player.containerMenu instanceof com.reiko.cars.tuning.TuningBenchMenu open) {
                open.refresh();
            }
            ShopFeedback.send(player, result);
        }
    }

    private static void handleLot(LotActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.containerMenu.containerId != payload.containerId()
                || !player.containerMenu.stillValid(player)) {
            return;
        }
        Component nothing = Component.translatable("sale.reikocars.nothing_picked");
        if (player.containerMenu instanceof com.reiko.cars.impound.ImpoundMenu menu && menu.office() != null) {
            CarEntity car = menu.selected();
            ShopActionResult result = switch (payload.action()) {
                case LotActionPayload.RELEASE -> car == null ? ShopActionResult.fail(nothing)
                        : com.reiko.cars.impound.ImpoundService.release(player, menu.office(), car, payload.expected(), payload.payment());
                case LotActionPayload.RELEASE_FREE -> car == null ? ShopActionResult.fail(nothing)
                        : com.reiko.cars.impound.ImpoundService.releaseFree(player, menu.office(), car);
                case LotActionPayload.SET_FEE -> {
                    ShopActionResult set = com.reiko.cars.impound.ImpoundService.setFee(player, menu.office(), payload.text());
                    if (set.success()) {
                        com.reiko.cars.impound.ImpoundMenu.open(player, menu.office()); // reopen to show the new fee
                    }
                    yield set;
                }
                default -> null;
            };
            if (result != null) {
                if (player.containerMenu instanceof com.reiko.cars.impound.ImpoundMenu open) {
                    open.refresh();
                }
                ShopFeedback.send(player, result);
            }
            return;
        }
        if (player.containerMenu instanceof com.reiko.cars.racing.RaceControlMenu menu && menu.control() != null) {
            com.reiko.cars.racing.RaceControlBlockEntity control = menu.control();
            ShopActionResult result = switch (payload.action()) {
                case LotActionPayload.RACE_ENTER -> com.reiko.cars.racing.RaceService.enter(player, control, payload.expected(), payload.payment());
                case LotActionPayload.RACE_LEAVE -> com.reiko.cars.racing.RaceService.leave(player, control);
                case LotActionPayload.RACE_START -> com.reiko.cars.racing.RaceService.start(player, control);
                case LotActionPayload.RACE_CANCEL -> com.reiko.cars.racing.RaceService.cancel(player, control);
                case LotActionPayload.RACE_TRIAL -> {
                    ShopActionResult started = com.reiko.cars.racing.RaceService.trial(player, control);
                    if (started.success()) {
                        player.closeContainer();
                        player.sendSystemMessage(started.message());
                        yield null;
                    }
                    yield started;
                }
                case LotActionPayload.RACE_END_TRIAL -> {
                    com.reiko.cars.racing.RaceService.endTrial(player.server, player.getUUID());
                    yield ShopActionResult.ok(net.minecraft.network.chat.Component.translatable("race.reikocars.trial_ended"));
                }
                case LotActionPayload.RACE_SET_LAPS -> com.reiko.cars.racing.RaceService.setLaps(player, control, payload.text());
                case LotActionPayload.RACE_SET_FEE -> com.reiko.cars.racing.RaceService.setFee(player, control, payload.text());
                case LotActionPayload.RACE_SET_CUT -> com.reiko.cars.racing.RaceService.setCut(player, control, payload.text());
                default -> null;
            };
            if (result != null) {
                if (result.success() && payload.action() != LotActionPayload.RACE_START) {
                    com.reiko.cars.racing.RaceControlMenu.open(player, control); // reopen to show who's in, or the new settings
                }
                if (player.containerMenu instanceof com.reiko.cars.racing.RaceControlMenu open) {
                    open.refresh();
                }
                ShopFeedback.send(player, result);
            }
            return;
        }
        if (player.containerMenu instanceof com.reiko.cars.dmv.DmvMenu menu && menu.dmv() != null) {
            com.reiko.cars.dmv.DmvOfficeBlockEntity dmv = menu.dmv();
            ShopActionResult result = switch (payload.action()) {
                case LotActionPayload.TAKE_TEST -> {
                    ShopActionResult started = com.reiko.cars.dmv.DmvService.takeTest(player, dmv, payload.expected(), payload.payment());
                    if (started.success()) {
                        player.closeContainer();
                        player.sendSystemMessage(started.message());
                        yield null;
                    }
                    yield started;
                }
                case LotActionPayload.REPLACE_LICENSE -> com.reiko.cars.dmv.DmvService.replace(player, dmv, payload.expected(), payload.payment());
                case LotActionPayload.BUY_PLATE -> com.reiko.cars.dmv.DmvService.buyPlate(player, dmv, menu.selected(), payload.text(),
                        payload.expected(), payload.payment());
                case LotActionPayload.SET_DMV_FEE -> {
                    ShopActionResult set = com.reiko.cars.dmv.DmvService.setFee(player, dmv, payload.value(), payload.text());
                    if (set.success()) {
                        com.reiko.cars.dmv.DmvMenu.open(player, dmv); // reopen to show the new fee
                    }
                    yield set;
                }
                default -> null;
            };
            if (result != null) {
                if (player.containerMenu instanceof com.reiko.cars.dmv.DmvMenu open) {
                    open.refresh();
                }
                ShopFeedback.send(player, result);
            }
            return;
        }
        if (player.containerMenu instanceof com.reiko.cars.rental.RentalMenu menu && menu.desk() != null) {
            CarEntity car = menu.selected();
            ShopActionResult result = switch (payload.action()) {
                case LotActionPayload.RENT -> car == null ? ShopActionResult.fail(nothing)
                        : com.reiko.cars.rental.RentalService.rent(player, menu.desk(), car, payload.value(), payload.expected(), payload.payment());
                case LotActionPayload.SET_RENT -> {
                    if (payload.value() < 0 || payload.value() >= CarType.values().length) {
                        yield ShopActionResult.fail(Component.literal("?"));
                    }
                    ShopActionResult set = com.reiko.cars.rental.RentalService.setPrice(player, menu.desk(), CarType.values()[payload.value()],
                            payload.text());
                    if (set.success()) {
                        com.reiko.cars.rental.RentalMenu.open(player, menu.desk()); // reopen so the screen shows the new prices
                    }
                    yield set;
                }
                default -> null;
            };
            if (result != null) {
                if (result.success() && payload.action() == LotActionPayload.RENT) {
                    player.closeContainer();
                    player.sendSystemMessage(result.message());
                    return;
                }
                if (player.containerMenu instanceof com.reiko.cars.rental.RentalMenu open) {
                    open.refresh();
                }
                ShopFeedback.send(player, result);
            }
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
                case SaleActionPayload.BUY, SaleActionPayload.FINANCE -> {
                    ShopActionResult result = SaleService.buy(player, car, payload.expected(), payload.payment(),
                            payload.action() == SaleActionPayload.FINANCE);
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
            case SaleActionPayload.PAY_OFF -> car == null ? ShopActionResult.fail(Component.translatable("sale.reikocars.nothing_picked"))
                    : com.reiko.cars.finance.LoanService.payOff(player, car);
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

    private static void handleCharger(ChargerActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof com.reiko.cars.charge.ChargerMenu menu)
                || menu.containerId != payload.containerId()
                || !menu.stillValid(player)
                || menu.station() == null) {
            return;
        }
        com.reiko.cars.charge.ChargingStationBlockEntity station = menu.station();
        ShopActionResult result = switch (payload.action()) {
            case ChargerActionPayload.CHARGE -> com.reiko.cars.charge.ChargerService.charge(player, station, menu.car(),
                    Math.max(0, payload.mj()), payload.expectedPrice(), payload.payment());
            case ChargerActionPayload.SET_PRICE -> {
                ShopActionResult set = com.reiko.cars.charge.ChargerService.setPrice(player, station, payload.priceText());
                if (set.success()) {
                    com.reiko.cars.charge.ChargerMenu.open(player, station); // reopen so the screen shows the new price
                }
                yield set;
            }
            default -> ShopActionResult.fail(Component.literal("?"));
        };
        menu.refresh();
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
            case CarActionPayload.SIREN -> com.reiko.cars.car.WorkVehicles.toggleSiren(player, car);
            case CarActionPayload.SIGNAL_LEFT -> com.reiko.cars.car.CarSignals.toggle(player, car, com.reiko.cars.car.CarSignals.LEFT);
            case CarActionPayload.SIGNAL_RIGHT -> com.reiko.cars.car.CarSignals.toggle(player, car, com.reiko.cars.car.CarSignals.RIGHT);
            case CarActionPayload.HAZARDS -> com.reiko.cars.car.CarSignals.toggle(player, car, com.reiko.cars.car.CarSignals.HAZARDS);
            case CarActionPayload.RADIO -> com.reiko.cars.radio.RadioMenu.open(player, car);
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
