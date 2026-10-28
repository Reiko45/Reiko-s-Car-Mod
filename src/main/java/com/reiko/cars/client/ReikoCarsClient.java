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
import com.reiko.cars.client.model.CruiserModel;
import com.reiko.cars.client.model.DirtBikeModel;
import com.reiko.cars.client.model.ScooterModel;
import com.reiko.cars.client.model.SemiTruckModel;
import com.reiko.cars.client.model.SportBikeModel;
import com.reiko.cars.client.model.SportsCarModel;
import com.reiko.cars.client.model.TankerTrailerModel;
import com.reiko.cars.network.CarActionPayload;
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
        };
    }

    private static Supplier<LayerDefinition> definition(CarType type) {
        return switch (type) {
            case SEDAN -> SedanModel::createBodyLayer;
            case SPORTS_CAR -> SportsCarModel::createBodyLayer;
            case PICKUP_TRUCK -> PickupTruckModel::createBodyLayer;
            case OFF_ROADER -> OffRoaderModel::createBodyLayer;
            case SEMI_TRUCK -> SemiTruckModel::createBodyLayer;
            case SPORT_BIKE -> SportBikeModel::createBodyLayer;
            case CRUISER -> CruiserModel::createBodyLayer;
            case DIRT_BIKE -> DirtBikeModel::createBodyLayer;
            case SCOOTER -> ScooterModel::createBodyLayer;
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
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PUMP.get(), PumpScreen::new);
        event.register(ModMenus.CAR_WORKBENCH.get(), CarWorkbenchScreen::new);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(HORN);
        event.register(LOCK);
        event.register(HITCH);
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ReikoCars.id("car_dashboard"), new CarHud());
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
    }

    /** Hands the driver's keys to their car before it ticks, and sends the horn and lock keys. */
    private static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            HORN.consumeClick();
            LOCK.consumeClick();
            HITCH.consumeClick();
            return;
        }
        if (car.getControllingPassenger() == player) {
            Input input = player.input;
            boolean typing = minecraft.screen != null;
            car.setInput(!typing && input.up, !typing && input.down, !typing && input.left, !typing && input.right,
                    !typing && input.jumping);
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
    }

    /** The engine sound playing for each nearby car. Weak keys, so cars that unload are forgotten. */
    private static final Map<CarEntity, EngineSound> ENGINES = new WeakHashMap<>();

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
            double length = car.carType().length + (car.trailer() != null ? TrailerType.LENGTH : 0.0);
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
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused() || minecraft.level.getGameTime() % 20 != 0) {
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
}
