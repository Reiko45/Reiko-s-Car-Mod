package com.reiko.cars.client;

import net.neoforged.bus.api.EventPriority;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.InputConstants;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import net.minecraft.world.entity.Entity;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.car.CarType;
import com.reiko.cars.client.model.BoxTrailerModel;
import com.reiko.cars.client.model.CarHaulerModel;
import com.reiko.cars.client.model.OffRoaderModel;
import com.reiko.cars.client.model.PickupTruckModel;
import com.reiko.cars.client.model.SedanModel;
import com.reiko.cars.client.model.TaxiModel;
import com.reiko.cars.client.model.CityBusModel;
import com.reiko.cars.client.model.ConvertibleModel;
import com.reiko.cars.client.model.CruiserModel;
import com.reiko.cars.client.model.DeliveryVanModel;
import com.reiko.cars.client.model.FarmTractorModel;
import com.reiko.cars.client.model.FlatbedModel;
import com.reiko.cars.client.model.MailVanModel;
import com.reiko.cars.client.model.WreckerModel;
import com.reiko.cars.client.model.HatchbackModel;
import com.reiko.cars.client.model.HeavyPickupModel;
import com.reiko.cars.client.model.MinivanModel;
import com.reiko.cars.client.model.MuscleCarModel;
import com.reiko.cars.client.model.DirtBikeModel;
import com.reiko.cars.client.model.ScooterModel;
import com.reiko.cars.client.model.SemiTruckModel;
import com.reiko.cars.client.model.SportBikeModel;
import com.reiko.cars.client.model.SportsCarModel;
import com.reiko.cars.client.model.TankerTrailerModel;
import com.reiko.cars.network.CarActionPayload;
import com.reiko.cars.network.CarWearPayload;
import com.reiko.cars.registry.ModBlockEntities;
import com.reiko.cars.registry.ModEntities;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.registry.ModMenus;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerItem;
import com.reiko.cars.trailer.TrailerType;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client setup: car and pump renderers, the pump screen, the dashboard, the horn and lock keys, and steering.
 * Driving uses the normal movement keys (forward, back, left, right), jump for the handbrake and sneak to get out.
 */
@Mod(value = ReikoCars.MODID, dist = Dist.CLIENT)
public final class ReikoCarsClient {
    public static final KeyMapping HORN = new KeyMapping("key.reikocars.horn", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_H, "key.categories.reikocars");
    public static final KeyMapping LOCK = new KeyMapping("key.reikocars.lock", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_L, "key.categories.reikocars");
    public static final KeyMapping HITCH = new KeyMapping("key.reikocars.hitch", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_G, "key.categories.reikocars");
    public static final KeyMapping SIREN = new KeyMapping("key.reikocars.siren", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_K, "key.categories.reikocars");
    public static final KeyMapping SIGNAL_LEFT = new KeyMapping("key.reikocars.signal_left", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_LEFT, "key.categories.reikocars");
    public static final KeyMapping SIGNAL_RIGHT = new KeyMapping("key.reikocars.signal_right", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_RIGHT, "key.categories.reikocars");
    public static final KeyMapping HAZARDS = new KeyMapping("key.reikocars.hazards", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_DOWN, "key.categories.reikocars");
    public static final KeyMapping RADIO = new KeyMapping("key.reikocars.radio", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.KEY_R, "key.categories.reikocars");

    public ReikoCarsClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CarsClientConfig.SPEC);
        modBus.addListener(ReikoCarsClient::registerLayers);
        modBus.addListener(ReikoCarsClient::registerRenderers);
        modBus.addListener(ReikoCarsClient::registerScreens);
        modBus.addListener(ReikoCarsClient::registerKeys);
        modBus.addListener(ReikoCarsClient::registerHud);
        modBus.addListener(ReikoCarsClient::registerItemColors);
        NeoForge.EVENT_BUS.addListener(ReikoCarsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ReikoCarsClient::keepEnginesRunning);
        NeoForge.EVENT_BUS.addListener(ReikoCarsClient::pullCameraBack);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ReikoCarsClient::leanRider);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ReikoCarsClient::unleanRider);
        NeoForge.EVENT_BUS.addListener(ReikoCarsClient::rollCamera);
        NeoForge.EVENT_BUS.addListener(ReikoCarsClient::raiseActionBar);
        NeoForge.EVENT_BUS.addListener(CarGlass::onRenderStage);
        NeoForge.EVENT_BUS.addListener(ReikoCarsClient::lowerActionBar);
        NeoForge.EVENT_BUS.addListener(GpsClient::onClientTick);
        CamperClient.register();
        NeoForge.EVENT_BUS.addListener(GpsClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(GpsHud::onFrame);
        NeoForge.EVENT_BUS.addListener(GpsHud::onBossBar);
        TrailerEntity.openTankerScreen = tanker -> Minecraft.getInstance().setScreen(new TankerScreen(tanker));
    }

    public static ModelLayerLocation layer(CarType type) {
        return new ModelLayerLocation(ReikoCars.id(type.id()), "main");
    }

    public static ModelLayerLocation layer(TrailerType type) {
        return new ModelLayerLocation(ReikoCars.id(type.id()), "main");
    }

    private static Supplier<LayerDefinition> definition(TrailerType type) {
        return switch (type) {
            case BOX_TRAILER -> BoxTrailerModel::createBodyLayer;
            case TANKER_TRAILER -> TankerTrailerModel::createBodyLayer;
            case CAR_HAULER -> CarHaulerModel::createBodyLayer;
            case CAMPER -> com.reiko.cars.client.model.CamperModel::createBodyLayer;
            case LIVESTOCK_TRAILER -> com.reiko.cars.client.model.LivestockTrailerModel::createBodyLayer;
            case FIFTH_WHEEL_CAMPER -> com.reiko.cars.client.model.FifthWheelCamperModel::createBodyLayer;
            case SEMI_LIVESTOCK_TRAILER -> com.reiko.cars.client.model.SemiLivestockTrailerModel::createBodyLayer;
        };
    }

    private static Supplier<LayerDefinition> definition(CarType type) {
        return switch (type) {
            case SEDAN -> SedanModel::createBodyLayer;
            case TAXI -> TaxiModel::createBodyLayer;
            case SPORTS_CAR -> SportsCarModel::createBodyLayer;
            case PICKUP_TRUCK -> PickupTruckModel::createBodyLayer;
            case OFF_ROADER -> OffRoaderModel::createBodyLayer;
            case SEMI_TRUCK -> SemiTruckModel::createBodyLayer;
            case HATCHBACK -> HatchbackModel::createBodyLayer;
            case MUSCLE_CAR -> MuscleCarModel::createBodyLayer;
            case CONVERTIBLE -> ConvertibleModel::createBodyLayer;
            case MINIVAN -> MinivanModel::createBodyLayer;
            case DELIVERY_VAN -> DeliveryVanModel::createBodyLayer;
            case CITY_BUS -> CityBusModel::createBodyLayer;
            case HEAVY_PICKUP -> HeavyPickupModel::createBodyLayer;
            case FARM_TRACTOR -> FarmTractorModel::createBodyLayer;
            case FLATBED -> FlatbedModel::createBodyLayer;
            case WRECKER -> WreckerModel::createBodyLayer;
            case MAIL_VAN -> MailVanModel::createBodyLayer;
            case POLICE_CAR -> com.reiko.cars.client.model.PoliceCarModel::createBodyLayer;
            case AMBULANCE -> com.reiko.cars.client.model.AmbulanceModel::createBodyLayer;
            case GARBAGE_TRUCK -> com.reiko.cars.client.model.GarbageTruckModel::createBodyLayer;
            case ICE_CREAM_TRUCK -> com.reiko.cars.client.model.IceCreamTruckModel::createBodyLayer;
            case DUMP_TRUCK -> com.reiko.cars.client.model.DumpTruckModel::createBodyLayer;
            case CEMENT_MIXER -> com.reiko.cars.client.model.CementMixerModel::createBodyLayer;
            case FIRE_TRUCK -> com.reiko.cars.client.model.FireTruckModel::createBodyLayer;
            case SPORT_BIKE -> SportBikeModel::createBodyLayer;
            case CRUISER -> CruiserModel::createBodyLayer;
            case DIRT_BIKE -> DirtBikeModel::createBodyLayer;
            case SCOOTER -> ScooterModel::createBodyLayer;
            case CITY_EV -> com.reiko.cars.client.model.CityEvModel::createBodyLayer;
            case ELECTRIC_SEDAN -> com.reiko.cars.client.model.ElectricSedanModel::createBodyLayer;
            case ELECTRIC_PICKUP -> com.reiko.cars.client.model.ElectricPickupModel::createBodyLayer;
            case ELECTRIC_VAN -> com.reiko.cars.client.model.ElectricVanModel::createBodyLayer;
            case ELECTRIC_MOPED -> com.reiko.cars.client.model.ElectricMopedModel::createBodyLayer;
        };
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (CarType type : CarType.values()) {
            event.registerLayerDefinition(layer(type), definition(type));
        }
        for (TrailerType type : TrailerType.values()) {
            event.registerLayerDefinition(layer(type), definition(type));
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (CarType type : CarType.values()) {
            event.registerEntityRenderer(ModEntities.car(type), context -> new CarRenderer(context, type, layer(type)));
        }
        for (TrailerType type : TrailerType.values()) {
            event.registerEntityRenderer(ModEntities.trailer(type), context -> new TrailerRenderer(context, type, layer(type)));
        }
        BlockEntityRenderers.register(ModBlockEntities.GAS_PUMP.get(), GasPumpRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CHARGING_STATION.get(), ChargingStationRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TOLL_GATE.get(), TollGateRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SPEED_SIGN.get(), SpeedSignRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TRAFFIC_CONE.get(), TrafficConeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.RACE_FLAG.get(), RaceFlagRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CAR_WASH.get(), CarWashRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.WASH_BAY.get(), WashBayRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BUS_STOP.get(), BusStopRenderer::new);
        event.registerEntityRenderer(ModEntities.BENCH_SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PUMP.get(), PumpScreen::new);
        event.register(ModMenus.CHARGER.get(), ChargerScreen::new);
        event.register(ModMenus.CAR_WORKBENCH.get(), CarWorkbenchScreen::new);
        event.register(ModMenus.REPAIR_BENCH.get(), RepairBenchScreen::new);
        event.register(ModMenus.MAILBOX.get(), MailboxScreen::new);
        event.register(ModMenus.FARE.get(), FareScreen::new);
        event.register(ModMenus.FARE_SETTINGS.get(), FareSettingsScreen::new);
        event.register(ModMenus.CAR_SALE.get(), CarSaleScreen::new);
        event.register(ModMenus.DEALERSHIP.get(), DealershipScreen::new);
        event.register(ModMenus.IMPOUND.get(), ImpoundScreen::new);
        event.register(ModMenus.DMV.get(), DmvScreen::new);
        event.register(ModMenus.TUNING_BENCH.get(), TuningBenchScreen::new);
        event.register(ModMenus.RACE_CONTROL.get(), RaceControlScreen::new);
        event.register(ModMenus.RENTAL.get(), RentalScreen::new);
        event.register(ModMenus.PARKING_METER.get(), ParkingMeterScreen::new);
        event.register(ModMenus.TOLL.get(), TollScreen::new);
        event.register(ModMenus.TOLL_BOOTH.get(), TollBoothScreen::new);
        event.register(ModMenus.WASH_PAY.get(), WashPayScreen::new);
        event.register(ModMenus.WASH_OWNER.get(), WashOwnerScreen::new);
        event.register(ModMenus.ROAD_SIGN.get(), RoadSignScreen::new);
        event.register(ModMenus.BUS_STOP.get(), BusStopScreen::new);
        event.register(ModMenus.CAMPER.get(), CamperScreen::new);
        event.register(ModMenus.RADIO.get(), RadioScreen::new);
        event.register(ModMenus.INSURANCE.get(), InsuranceScreen::new);
        event.register(ModMenus.ICE_CREAM.get(), IceCreamScreen::new);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(HORN);
        event.register(LOCK);
        event.register(HITCH);
        event.register(SIREN);
        event.register(SIGNAL_LEFT);
        event.register(SIGNAL_RIGHT);
        event.register(HAZARDS);
        event.register(RADIO);
        event.register(GpsClient.KEY);
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ReikoCars.id("car_dashboard"), new CarHud());
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ReikoCars.id("gps"), new GpsHud());
    }

    /** Layer 0 of a car item's icon is its paintwork, tinted the car's colour. */
    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (CarType type : CarType.values()) {
            event.register((stack, tintIndex) -> tintIndex == 0 ? FastColor.ARGB32.opaque(CarItem.colorOf(stack, type)) : -1,
                    ModItems.car(type));
        }
        for (TrailerType type : TrailerType.values()) {
            event.register((stack, tintIndex) -> tintIndex == 0 ? FastColor.ARGB32.opaque(TrailerItem.colorOf(stack, type)) : -1,
                    ModItems.trailer(type));
        }
        // A key's fob in its car's paint.
        event.register((stack, tintIndex) -> {
            com.reiko.cars.key.CarKey key = stack.get(com.reiko.cars.registry.ModDataComponents.CAR_KEY.get());
            return tintIndex == 0 ? FastColor.ARGB32.opaque(key == null ? 0x808080 : key.color()) : -1;
        }, ModItems.CAR_KEY.get());
    }

    /** Hands the driver's keys to their car before it ticks, and sends the horn and lock keys. */
    private static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            HORN.consumeClick();
            LOCK.consumeClick();
            HITCH.consumeClick();
            SIREN.consumeClick();
            SIGNAL_LEFT.consumeClick();
            SIGNAL_RIGHT.consumeClick();
            HAZARDS.consumeClick();
            RADIO.consumeClick();
            return;
        }
        if (car.getControllingPassenger() == player) {
            Input input = player.input;
            boolean typing = minecraft.screen != null;
            car.setInput(!typing && input.up, !typing && input.down, !typing && input.left, !typing && input.right,
                    !typing && input.jumping);
            float[] wear = car.takeWearReport();
            if (wear != null) {
                PacketDistributor.sendToServer(new CarWearPayload(wear[0], wear[1], wear[2]));
            }
        }
        while (HORN.consumeClick()) {
            PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.HORN));
        }
        while (LOCK.consumeClick()) {
            PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.TOGGLE_LOCK));
        }
        while (HITCH.consumeClick()) {
            PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.HITCH));
        }
        while (SIREN.consumeClick()) {
            PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.SIREN));
        }
        while (SIGNAL_LEFT.consumeClick()) {
            PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.SIGNAL_LEFT));
        }
        while (SIGNAL_RIGHT.consumeClick()) {
            PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.SIGNAL_RIGHT));
        }
        while (HAZARDS.consumeClick()) {
            PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.HAZARDS));
        }
        while (RADIO.consumeClick()) {
            PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.RADIO));
        }
        tickTock(minecraft, car);
    }

    /** Whether the signal lamps of the car we're in were lit last tick, for the relay's tick and tock. */
    private static boolean wasLit;

    /** The signal relay clicking, heard inside the car: a tick as the lamps light, a tock as they go out. */
    private static void tickTock(Minecraft minecraft, CarEntity car) {
        boolean blinking = com.reiko.cars.car.CarSignals.blinking(car, true) || com.reiko.cars.car.CarSignals.blinking(car, false);
        boolean lit = blinking && com.reiko.cars.car.CarSignals.blinkOn(car);
        if (blinking && lit != wasLit && !car.alarmOn()) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    (lit ? com.reiko.cars.registry.ModSounds.SIGNAL_TICK : com.reiko.cars.registry.ModSounds.SIGNAL_TOCK).get(), 1.0F, 0.35F));
        }
        wasLit = lit;
    }

    /** The engine sound playing for each nearby car. Weak keys, so cars that unload are forgotten. */
    private static final Map<CarEntity, EngineSound> ENGINES = new WeakHashMap<>();
    private static final Map<CarEntity, SirenSound> SIRENS = new WeakHashMap<>();
    private static final Map<net.minecraft.world.entity.player.Player, HoseSound> HOSES = new WeakHashMap<>();

    /** Starts the rush of water for everyone nearby spraying a fire hose. */
    private static void keepHosesGoing(Minecraft minecraft) {
        SoundManager sounds = minecraft.getSoundManager();
        HOSES.values().removeIf(sound -> sound.isStopped());
        for (net.minecraft.world.entity.player.Player player : minecraft.level.players()) {
            if (HoseSound.spraying(player) && player.distanceToSqr(minecraft.player) < 48.0 * 48.0) {
                HoseSound sound = HOSES.get(player);
                if (sound == null || !sounds.isActive(sound)) {
                    if (sound != null) {
                        sounds.stop(sound);
                    }
                    HoseSound fresh = new HoseSound(player);
                    HOSES.put(player, fresh);
                    sounds.play(fresh);
                }
            }
        }
    }

    /**
     * Once a second, gives every nearby car with its engine running an engine sound, and restarts any that the
     * sound engine dropped (it can, when too many sounds play at once).
     */
    /**
     * In third person, stands the camera far enough back to see the whole vehicle: a semi and its trailer are 15
     * blocks long, and the usual 4 blocks would put the camera inside the trailer.
     */
    private static void pullCameraBack(CalculateDetachedCameraDistanceEvent event) {
        Entity viewer = event.getCamera().getEntity();
        if (viewer.getRootVehicle() instanceof CarEntity car && car != viewer) {
            double length = car.carType().length + (car.trailer() != null ? car.trailer().trailerType().length + 1.0 : 0.0);
            event.setDistance(Math.max(event.getDistance(), (float) (length * 0.75 + 1.0)));
        }
    }

    /** Riders drawn leaning with their motorcycle; their ids, so the pose is put back after. */
    private static final java.util.Set<Integer> LEANING = new java.util.HashSet<>();

    /** Tips a motorcycle's riders over with it, about the line where its wheels touch the ground. */
    private static void leanRider(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity rider = event.getEntity();
        if (!(rider.getVehicle() instanceof CarEntity bike) || !bike.carType().isBike()) {
            return;
        }
        float partialTick = event.getPartialTick();
        float lean = bike.lean(partialTick);
        if (lean == 0.0F) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // The pose's origin is the rider; move to the bike's, lean, and come back.
        double dx = Mth.lerp(partialTick, bike.xo, bike.getX()) - Mth.lerp(partialTick, rider.xo, rider.getX());
        double dy = Mth.lerp(partialTick, bike.yo, bike.getY()) - Mth.lerp(partialTick, rider.yo, rider.getY());
        double dz = Mth.lerp(partialTick, bike.zo, bike.getZ()) - Mth.lerp(partialTick, rider.zo, rider.getZ());
        pose.translate(dx, dy, dz);
        CarRenderer.leanOver(pose, bike, bike.getViewYRot(partialTick), partialTick);
        pose.translate(-dx, -dy, -dz);
        LEANING.add(rider.getId());
    }

    private static void unleanRider(RenderLivingEvent.Post<?, ?> event) {
        if (LEANING.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    /** In first person on a motorcycle, the view tilts a little as the bike leans. */
    private static void rollCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.getCameraType().isFirstPerson() && minecraft.player != null
                && minecraft.player.getVehicle() instanceof CarEntity bike && bike.carType().isBike()) {
            event.setRoll(event.getRoll() + bike.lean((float) event.getPartialTick()) * 0.6F);
        }
    }

    private static void keepEnginesRunning(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        if (minecraft.level.getGameTime() % 5 == 0) {
            keepSirensGoing(minecraft);
            RadioPlayer.tick(minecraft);
        }
        keepHosesGoing(minecraft);
        if (minecraft.level.getGameTime() % 20 != 0) {
            return;
        }
        SoundManager sounds = minecraft.getSoundManager();
        ENGINES.values().removeIf(sound -> sound.isStopped());
        for (CarEntity car : minecraft.level.getEntitiesOfClass(CarEntity.class, minecraft.player.getBoundingBox().inflate(48.0))) {
            EngineSound sound = ENGINES.get(car);
            if (car.isEngineRunning() && (sound == null || !sounds.isActive(sound))) {
                if (sound != null) {
                    sounds.stop(sound);
                }
                EngineSound fresh = new EngineSound(car);
                ENGINES.put(car, fresh);
                sounds.play(fresh);
            }
        }
    }

    private static final Map<CarEntity, AlarmSound> ALARMS = new WeakHashMap<>();

    /** Starts the siren of every police car and ambulance nearby that has its lights and siren on, and any alarm going off. */
    private static void keepSirensGoing(Minecraft minecraft) {
        SoundManager manager = minecraft.getSoundManager();
        ALARMS.values().removeIf(sound -> sound.isStopped());
        for (CarEntity car : minecraft.level.getEntitiesOfClass(CarEntity.class, minecraft.player.getBoundingBox().inflate(64.0), CarEntity::alarmOn)) {
            AlarmSound sound = ALARMS.get(car);
            if (sound == null || !manager.isActive(sound)) {
                if (sound != null) {
                    manager.stop(sound);
                }
                AlarmSound fresh = new AlarmSound(car);
                ALARMS.put(car, fresh);
                manager.play(fresh);
            }
        }
        SoundManager sounds = minecraft.getSoundManager();
        SIRENS.values().removeIf(sound -> sound.isStopped());
        for (CarEntity car : minecraft.level.getEntitiesOfClass(CarEntity.class, minecraft.player.getBoundingBox().inflate(64.0), CarEntity::sirenOn)) {
            SirenSound sound = SIRENS.get(car);
            if (sound == null || !sounds.isActive(sound)) {
                if (sound != null) {
                    sounds.stop(sound);
                }
                SirenSound fresh = new SirenSound(car);
                SIRENS.put(car, fresh);
                sounds.play(fresh);
            }
        }
    }

    /** Whether the action bar message is drawn raised this frame. */
    private static boolean actionBarRaised;

    /**
     * While the dashboard's up, the action bar message (the ones cars send, and "Press Shift to Dismount") is drawn
     * a little higher, clear of the key hints over the dashboard.
     */
    private static void raiseActionBar(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getName().equals(VanillaGuiLayers.OVERLAY_MESSAGE) && minecraft.player != null
                && minecraft.player.getVehicle() instanceof CarEntity car && CarsClientConfig.showHud()
                && CarHud.fitsBesideHotbar(minecraft, event.getGuiGraphics().guiWidth())) {
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0.0F, -CarHud.actionBarRaise(minecraft, car), 0.0F);
            actionBarRaised = true;
        }
    }

    private static void lowerActionBar(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post event) {
        if (actionBarRaised && event.getName().equals(VanillaGuiLayers.OVERLAY_MESSAGE)) {
            event.getGuiGraphics().pose().popPose();
            actionBarRaised = false;
        }
    }
}
