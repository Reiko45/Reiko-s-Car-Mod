package com.reiko.cars.dev;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.client.GpsClient;
import com.reiko.cars.client.ReikoCarsClient;
import com.reiko.cars.network.PumpActionPayload;
import com.reiko.cars.pump.PumpMenu;
import com.reiko.cars.trailer.CamperMenu;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerType;
import com.reiko.cars.workbench.CarWorkbenchMenu;
import com.reiko.economy.shop.Payment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * DEV ONLY (left out of the jar). {@code ./gradlew runClient -Pshowcase=mp} joins a dedicated server (by default
 * localhost:25565, or {@code -Dreikocars.mpserver}) whose mod jar has {@link DevServerTest} in it, and plays through
 * the mod as a survival player would: the manual on joining, building a sedan at the workbench, cutting a key, buying
 * fuel, driving, the radio and GPS, a semi and a pickup hitching and towing, a camper's camp, screen and bed, and
 * loading cows into a livestock trailer; then leaves and joins again. Every check is logged as an "MPCHECK" line and
 * screenshots go to run/screenshots/mp_*.png.
 */
@EventBusSubscriber(modid = ReikoCars.MODID, value = Dist.CLIENT)
public final class DevMultiplayer {
    private static final boolean ENABLED = System.getProperty("reikocars.showcase", "false").equals("mp");
    private static final String SERVER = System.getProperty("reikocars.mpserver", "localhost:25565");
    private static final BlockPos BENCH = DevServerTest.BENCH;
    private static final BlockPos PUMP = DevServerTest.PUMP;
    private static final BlockPos REPAIR = DevServerTest.REPAIR;
    private static int joins;
    private static boolean connecting;
    private static int tick = -1;
    private static int passes;
    private static int fails;
    private static final List<List<Step>> PHASES = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    private static Vec3 carStart = Vec3.ZERO;
    private static Vec3 trailerStart = Vec3.ZERO;
    private static double fuelBefore;

    private record Step(int at, Consumer<Minecraft> action) {
    }

    private DevMultiplayer() {
    }

    private static void at(int phase, int time, Consumer<Minecraft> action) {
        PHASES.get(phase).add(new Step(time, action));
    }

    @SubscribeEvent
    static void onScreen(ScreenEvent.Opening event) {
        if (ENABLED && event.getNewScreen() instanceof net.minecraft.client.gui.screens.DisconnectedScreen) {
            event.setNewScreen(new TitleScreen()); // kicked (the server stopping): back to the title, to try again from there
            connecting = false;
            waiting = 0;
            return;
        }
        if (ENABLED && connecting && joins > 0 && event.getNewScreen() instanceof TitleScreen) {
            connecting = false; // that try didn't get in (the server's restarting): try again in a while
            waiting = 0;
            return;
        }
        if (!ENABLED || connecting || !(event.getNewScreen() instanceof TitleScreen) || joins > 0) {
            return;
        }
        script();
        connect(Minecraft.getInstance());
    }

    private static void connect(Minecraft minecraft) {
        connecting = true;
        minecraft.execute(() -> ConnectScreen.startConnecting(new TitleScreen(), minecraft, ServerAddress.parseString(SERVER),
                new ServerData("Reiko test", SERVER, ServerData.Type.OTHER), false, null));
    }

    private static int waiting;

    @SubscribeEvent
    static void onChat(ClientChatReceivedEvent event) {
        if (ENABLED) {
            ReikoCars.LOGGER.info("MPCHAT {}", event.getMessage().getString());
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ENABLED && joins >= 1 && joins <= 2 && !connecting && minecraft.player == null && minecraft.screen instanceof TitleScreen
                && ++waiting % (joins == 1 ? 60 : 200) == 0) {
            connect(minecraft); // back to the title screen after leaving: join again, once it's settled (or the server's back)
        }
        if (!ENABLED || minecraft.player == null || minecraft.getConnection() == null || joins >= 4) {
            return;
        }
        if (connecting) {
            connecting = false;
            joins++;
            tick = -1;
            ReikoCars.LOGGER.info("MPCHECK joined the server ({} time)", joins);
        }
        tick++;
        for (Step step : PHASES.get(joins - 1)) {
            if (step.at == tick) {
                try {
                    step.action.accept(minecraft);
                } catch (RuntimeException e) {
                    fail("step at tick " + tick, e.toString());
                    ReikoCars.LOGGER.error("MP step at tick {} failed", tick, e);
                }
            }
        }
    }

    // ---- helpers ----

    private static void check(String what, boolean ok, String detail) {
        if (ok) {
            passes++;
        } else {
            fails++;
        }
        ReikoCars.LOGGER.info("MPCHECK {} {} ({})", ok ? "PASS" : "FAIL", what, detail);
    }

    private static void fail(String what, String detail) {
        check(what, false, detail);
    }

    private static void cmd(Minecraft minecraft, String command) {
        minecraft.player.connection.sendCommand(command);
    }

    private static void shot(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, "mp_" + name + ".png", minecraft.getMainRenderTarget(), message -> {
        });
    }

    private static void press(KeyMapping key) {
        KeyMapping.click(key.getKey());
    }

    private static String screen(Minecraft minecraft) {
        return minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName();
    }

    private static int count(Minecraft minecraft, Predicate<ItemStack> what) {
        return minecraft.player.getInventory().items.stream().filter(what).mapToInt(ItemStack::getCount).sum();
    }

    private static boolean isManual(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("patchouli:guide_book");
    }

    /** Puts the first stack matching {@code what} in hand (moving it into the hotbar if need be); false if there's none. */
    private static boolean hold(Minecraft minecraft, Predicate<ItemStack> what) {
        var inventory = minecraft.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (what.test(inventory.items.get(i))) {
                inventory.selected = i;
                return true;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (what.test(inventory.items.get(i))) {
                minecraft.gameMode.handleInventoryMouseClick(minecraft.player.inventoryMenu.containerId, i, 8, ClickType.SWAP, minecraft.player);
                inventory.selected = 8;
                return true;
            }
        }
        return false;
    }

    private static boolean holdItem(Minecraft minecraft, Item item) {
        return hold(minecraft, stack -> stack.is(item));
    }

    private static void emptyHand(Minecraft minecraft) {
        if (!hold(minecraft, ItemStack::isEmpty)) {
            fail("an empty hand", "no empty slot");
        }
    }

    private static void useBlock(Minecraft minecraft, BlockPos pos, Direction face) {
        Vec3 at = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, new BlockHitResult(at, face, pos, false));
    }

    private static void interact(Minecraft minecraft, Entity entity) {
        if (entity == null) {
            fail("something to interact with", "none found");
            return;
        }
        // As a player clicks: the nearest part of a trailer, which is what's under the crosshair close up.
        Entity target = entity;
        if (entity instanceof TrailerEntity trailer) {
            for (var part : trailer.getParts()) {
                if (part.isPickable() && !(part instanceof com.reiko.cars.trailer.TrailerPart seat
                        && seat.role() == com.reiko.cars.trailer.TrailerPart.Role.CHAIR) && part.getBoundingBox().distanceToSqr(minecraft.player.getEyePosition())
                        < target.getBoundingBox().distanceToSqr(minecraft.player.getEyePosition())) {
                    target = part;
                }
            }
        }
        minecraft.gameMode.interact(minecraft.player, target, InteractionHand.MAIN_HAND);
    }

    private static <T extends Entity> T nearest(Minecraft minecraft, Class<T> kind, Predicate<T> what) {
        List<T> found = new ArrayList<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (kind.isInstance(entity) && what.test(kind.cast(entity))) {
                found.add(kind.cast(entity));
            }
        }
        return found.stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(minecraft.player))).orElse(null);
    }

    private static CarEntity car(Minecraft minecraft, CarType type) {
        return nearest(minecraft, CarEntity.class, car -> car.carType() == type);
    }

    private static TrailerEntity trailer(Minecraft minecraft, TrailerType type) {
        return nearest(minecraft, TrailerEntity.class, trailer -> trailer.trailerType() == type);
    }

    /** Logs where the camera is, against the player and their vehicle (and its trailer). */
    private static void camera(Minecraft minecraft, String when) {
        var camera = minecraft.gameRenderer.getMainCamera();
        Entity vehicle = minecraft.player.getRootVehicle();
        ReikoCars.LOGGER.info("MPCAMERA {}: type={} camera={} yaw={} pitch={} eye={} playerYaw={} vehicle={} vehicleYaw={} trailer={} distance={}",
                when, minecraft.options.getCameraType(), camera.getPosition(), camera.getYRot(), camera.getXRot(), minecraft.player.getEyePosition(),
                minecraft.player.getYRot(), pos(vehicle), vehicle.getYRot(),
                vehicle instanceof CarEntity car && car.trailer() != null ? pos(car.trailer()) : "none",
                String.format("%.2f", camera.getPosition().distanceTo(minecraft.player.getEyePosition())));
    }

    private static String pos(Entity entity) {
        return entity == null ? "none" : String.format("%.1f,%.1f,%.1f", entity.getX(), entity.getY(), entity.getZ());
    }

    // ---- the play-through ----

    private static void script() {
        // Joined: the manual should be waiting in the inventory.
        at(0, 40, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            int manuals = count(mc, DevMultiplayer::isManual);
            check("the Car Manual is given on first join", manuals == 1, manuals + " in the inventory");
            shot(mc, "00_joined");
        });
        at(0, 45, mc -> com.reiko.cars.guide.Guide.openFront());
        at(0, 70, mc -> {
            check("the manual opens", screen(mc).contains("Book"), screen(mc));
            shot(mc, "01_manual");
            mc.setScreen(null);
        });
        at(0, 75, mc -> cmd(mc, "reikotest setup"));
        at(0, 100, mc -> cmd(mc, "reikotest report setup"));
        at(0, 105, mc -> shot(mc, "02_scene"));

        // The workbench: open it, pick the sedan, take the car.
        at(0, 110, mc -> {
            emptyHand(mc);
            useBlock(mc, BENCH, Direction.SOUTH);
        });
        at(0, 130, mc -> {
            check("the workbench opens", mc.player.containerMenu instanceof CarWorkbenchMenu, screen(mc));
            shot(mc, "03_workbench");
            if (mc.player.containerMenu instanceof CarWorkbenchMenu menu) {
                menu.clickMenuButton(mc.player, CarType.SEDAN.ordinal());
                mc.gameMode.handleInventoryButtonClick(menu.containerId, CarType.SEDAN.ordinal());
            }
        });
        at(0, 160, mc -> {
            shot(mc, "04_workbench_filled");
            if (mc.player.containerMenu instanceof CarWorkbenchMenu menu) {
                ItemStack result = menu.getSlot(CarWorkbenchMenu.RESULT).getItem();
                check("the sedan's parts go in and it's ready to take", !result.isEmpty(), result.toString());
                mc.gameMode.handleInventoryMouseClick(menu.containerId, CarWorkbenchMenu.RESULT, 0, ClickType.QUICK_MOVE, mc.player);
            }
        });
        at(0, 185, mc -> {
            int sedans = count(mc, stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("sedan"));
            check("the sedan is built and in the inventory", sedans == 1, sedans + " sedans");
            int engines = count(mc, stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("car_engine"));
            check("its parts are used up", engines == 0, engines + " engines left");
        });
        // The workbench's Guide button opens the manual at the workbench page.
        at(0, 190, mc -> {
            Button guide = null;
            if (mc.screen != null) {
                for (var child : mc.screen.children()) {
                    if (child instanceof Button button && button.getMessage().getString().equals("Guide")) {
                        guide = button;
                    }
                }
            }
            check("the workbench has a Guide button", guide != null, screen(mc));
            if (guide != null) {
                guide.onPress();
            }
        });
        at(0, 210, mc -> {
            check("the Guide button opens the manual", screen(mc).contains("Book"), screen(mc));
            shot(mc, "05_guide_button");
            mc.setScreen(null);
        });

        // Put the sedan down by the pump, cut a key.
        at(0, 215, mc -> cmd(mc, "tp @s 6.5 -60 3.5 0 50"));
        at(0, 235, mc -> {
            check("holding the sedan", holdItem(mc, BuiltInRegistries.ITEM.get(ReikoCars.id("sedan"))), "");
            // As a right-click on the ground does: the block first (nothing happens), then the item (like a boat).
            if (mc.hitResult instanceof BlockHitResult hit) {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            }
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        });
        at(0, 260, mc -> {
            CarEntity sedan = car(mc, CarType.SEDAN);
            check("the sedan is put down", sedan != null, pos(sedan));
            if (sedan != null) {
                carStart = sedan.position();
                check("new cars start locked", sedan.isLocked(), "locked=" + sedan.isLocked());
            }
            shot(mc, "06_car_placed");
            check("holding a blank key", holdItem(mc, BuiltInRegistries.ITEM.get(ReikoCars.id("blank_key"))), "");
            mc.options.keyShift.setDown(true);
        });
        at(0, 265, mc -> interact(mc, car(mc, CarType.SEDAN)));
        at(0, 270, mc -> mc.options.keyShift.setDown(false));
        at(0, 290, mc -> {
            int keys = count(mc, stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("car_key"));
            check("a key is cut for the sedan", keys == 1, keys + " keys");
        });

        // Buy fuel with cash.
        at(0, 295, mc -> {
            emptyHand(mc);
            useBlock(mc, PUMP, Direction.SOUTH);
        });
        at(0, 315, mc -> {
            check("the pump opens", mc.player.containerMenu instanceof PumpMenu, screen(mc));
            shot(mc, "07_pump");
            CarEntity sedan = car(mc, CarType.SEDAN);
            fuelBefore = sedan == null ? 0.0 : sedan.fuel();
            if (mc.player.containerMenu instanceof PumpMenu menu) {
                PacketDistributor.sendToServer(PumpActionPayload.fill(menu.containerId, 3000, menu.price(), Payment.cash()));
            }
        });
        at(0, 345, mc -> {
            shot(mc, "08_pump_paid");
            CarEntity sedan = car(mc, CarType.SEDAN);
            check("the sedan has the fuel it was sold", sedan != null && sedan.fuel() > fuelBefore, sedan == null ? "no car" : "fuel " + fuelBefore + " -> " + sedan.fuel());
            mc.player.closeContainer();
            cmd(mc, "reikotest report fueled");
        });

        // Get in and drive along the road.
        at(0, 350, mc -> {
            emptyHand(mc);
            interact(mc, car(mc, CarType.SEDAN));
        });
        at(0, 370, mc -> {
            check("in the driver's seat", mc.player.getVehicle() instanceof CarEntity, String.valueOf(mc.player.getVehicle()));
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            shot(mc, "09_in_car");
            press(ReikoCarsClient.SIGNAL_LEFT);
            press(ReikoCarsClient.HORN);
        });
        at(0, 380, mc -> press(ReikoCarsClient.HAZARDS));
        at(0, 390, mc -> {
            press(ReikoCarsClient.HAZARDS);
            mc.options.keyUp.setDown(true);
        });
        at(0, 410, mc -> mc.options.keyLeft.setDown(true));
        at(0, 428, mc -> mc.options.keyLeft.setDown(false));
        at(0, 470, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        at(0, 490, mc -> {
            shot(mc, "10_drove");
            CarEntity sedan = car(mc, CarType.SEDAN);
            double moved = sedan == null ? 0.0 : sedan.position().distanceTo(carStart);
            check("the sedan drives", moved > 8.0, String.format("moved %.1f blocks, to %s", moved, pos(sedan)));
            ReikoCars.LOGGER.info("MPCHECK client sees the sedan at {}", pos(sedan));
            cmd(mc, "reikotest report drove");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        // The radio and the GPS.
        at(0, 495, mc -> press(ReikoCarsClient.RADIO));
        at(0, 515, mc -> {
            check("the radio opens", screen(mc).equals("RadioScreen"), screen(mc));
            shot(mc, "11_radio");
            mc.player.closeContainer();
        });
        at(0, 520, mc -> press(GpsClient.KEY));
        at(0, 545, mc -> {
            check("the GPS opens", screen(mc).equals("GpsScreen"), screen(mc));
            shot(mc, "12_gps");
            mc.setScreen(null);
        });
        at(0, 550, mc -> mc.options.keyShift.setDown(true));
        at(0, 560, mc -> mc.options.keyShift.setDown(false));
        at(0, 575, mc -> {
            check("out of the car", mc.player.getVehicle() == null, String.valueOf(mc.player.getVehicle()));
            CarEntity sedan = car(mc, CarType.SEDAN);
            boolean before = sedan != null && sedan.isLocked();
            press(ReikoCarsClient.LOCK);
            ReikoCars.LOGGER.info("MPCHECK lock before pressing the lock key: {}", before);
        });
        at(0, 595, mc -> {
            CarEntity sedan = car(mc, CarType.SEDAN);
            ReikoCars.LOGGER.info("MPCHECK lock after pressing the lock key: {}", sedan != null && sedan.isLocked());
        });
        // The repair bench screen.
        at(0, 600, mc -> cmd(mc, "tp @s 14.5 -60 0.5 180 30"));
        at(0, 620, mc -> {
            emptyHand(mc);
            useBlock(mc, REPAIR, Direction.SOUTH);
        });
        at(0, 640, mc -> {
            ReikoCars.LOGGER.info("MPCHECK repair bench shows {}", screen(mc));
            shot(mc, "13_repair_bench");
            mc.player.closeContainer();
            mc.setScreen(null);
        });

        // A semi hitching its box trailer and pulling it.
        at(0, 650, mc -> cmd(mc, "reikotest semi"));
        at(0, 680, mc -> {
            emptyHand(mc);
            interact(mc, car(mc, CarType.SEMI_TRUCK));
        });
        at(0, 700, mc -> {
            check("in the semi", mc.player.getVehicle() instanceof CarEntity car && car.carType() == CarType.SEMI_TRUCK, String.valueOf(mc.player.getVehicle()));
            press(ReikoCarsClient.HITCH);
        });
        at(0, 725, mc -> {
            TrailerEntity box = trailer(mc, TrailerType.BOX_TRAILER);
            check("the box trailer hitches to the semi", box != null && box.isPassenger(), box == null ? "no trailer" : "passenger=" + box.isPassenger());
            trailerStart = box == null ? Vec3.ZERO : box.position();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            shot(mc, "14_semi_hitched");
            cmd(mc, "reikotest report hitched");
            mc.options.keyUp.setDown(true);
        });
        at(0, 760, mc -> {
            camera(mc, "semi third person, moving");
            shot(mc, "14b_semi_towing");
        });
        at(0, 800, mc -> mc.options.keyUp.setDown(false));
        at(0, 815, mc -> {
            TrailerEntity box = trailer(mc, TrailerType.BOX_TRAILER);
            double moved = box == null ? 0.0 : box.position().distanceTo(trailerStart);
            check("the trailer comes along", moved > 8.0, String.format("moved %.1f blocks", moved));
            camera(mc, "semi third person");
            shot(mc, "15_semi_drove");
            cmd(mc, "reikotest report semi_drove");
            press(ReikoCarsClient.HITCH);
        });
        at(0, 840, mc -> {
            TrailerEntity box = trailer(mc, TrailerType.BOX_TRAILER);
            check("the trailer unhitches", box != null && !box.isPassenger(), box == null ? "no trailer" : "passenger=" + box.isPassenger());
            mc.options.keyShift.setDown(true);
        });
        at(0, 850, mc -> mc.options.keyShift.setDown(false));

        // A pickup hitching a camper, towing it, parking it: the camp, the camper's screen, a night's sleep.
        at(0, 860, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            cmd(mc, "reikotest camper");
        });
        at(0, 890, mc -> {
            emptyHand(mc);
            interact(mc, car(mc, CarType.PICKUP_TRUCK));
        });
        at(0, 910, mc -> {
            check("in the pickup", mc.player.getVehicle() instanceof CarEntity car && car.carType() == CarType.PICKUP_TRUCK, String.valueOf(mc.player.getVehicle()));
            press(ReikoCarsClient.HITCH);
        });
        at(0, 935, mc -> {
            TrailerEntity camper = trailer(mc, TrailerType.CAMPER);
            check("the camper hitches to the pickup's ball", camper != null && camper.isPassenger(), camper == null ? "no camper" : "passenger=" + camper.isPassenger());
            check("a hitched camper's camp is folded", camper != null && !camper.campOut(), camper == null ? "" : "campOut=" + camper.campOut());
            trailerStart = camper == null ? Vec3.ZERO : camper.position();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.keyUp.setDown(true);
        });
        at(0, 960, mc -> {
            camera(mc, "pickup third person, moving");
            shot(mc, "16_camper_towing");
        });
        at(0, 985, mc -> mc.options.keyUp.setDown(false));
        at(0, 1000, mc -> {
            TrailerEntity camper = trailer(mc, TrailerType.CAMPER);
            double moved = camper == null ? 0.0 : camper.position().distanceTo(trailerStart);
            check("the camper comes along", moved > 5.0, String.format("moved %.1f blocks", moved));
            cmd(mc, "reikotest report camper_drove");
            press(ReikoCarsClient.HITCH);
        });
        at(0, 1015, mc -> mc.options.keyShift.setDown(true));
        at(0, 1025, mc -> mc.options.keyShift.setDown(false));
        at(0, 1090, mc -> {
            TrailerEntity camper = trailer(mc, TrailerType.CAMPER);
            check("the camper unhitches", camper != null && !camper.isPassenger(), camper == null ? "" : "passenger=" + camper.isPassenger());
            check("parked, the camp unfolds", camper != null && camper.campOut(), camper == null ? "" : "campOut=" + camper.campOut());
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            shot(mc, "17_camp");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            cmd(mc, "reikotest night");
        });
        at(0, 1100, mc -> {
            TrailerEntity camper = trailer(mc, TrailerType.CAMPER);
            if (camper != null) {
                cmd(mc, String.format("tp @s %.1f -60 %.1f", camper.getX() + 0.0, camper.getZ() + 3.5));
            }
        });
        at(0, 1120, mc -> {
            emptyHand(mc);
            interact(mc, trailer(mc, TrailerType.CAMPER));
        });
        at(0, 1140, mc -> {
            check("the camper's screen opens", mc.player.containerMenu instanceof CamperMenu, screen(mc));
            shot(mc, "18_camper_screen");
            if (mc.player.containerMenu instanceof CamperMenu menu) {
                mc.gameMode.handleInventoryButtonClick(menu.containerId, CamperMenu.SLEEP);
            }
        });
        at(0, 1165, mc -> {
            check("asleep in the camper", mc.player.isSleeping(), "sleeping=" + mc.player.isSleeping());
            shot(mc, "19_sleeping");
            cmd(mc, "reikotest report asleep");
        });
        at(0, 1320, mc -> {
            long time = mc.level.getDayTime() % 24000;
            check("the night passes and they wake", !mc.player.isSleeping() && time < 12000, "sleeping=" + mc.player.isSleeping() + " time=" + time);
            shot(mc, "20_woke");
            cmd(mc, "reikotest report woke");
        });

        // Cows on leads into a livestock trailer, and out again.
        at(0, 1330, mc -> cmd(mc, "reikotest livestock"));
        at(0, 1360, mc -> check("holding leads", holdItem(mc, Items.LEAD), ""));
        for (int i = 0; i < 3; i++) {
            int n = i;
            at(0, 1365 + i * 5, mc -> {
                List<Cow> cows = new ArrayList<>();
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof Cow cow && !cow.isLeashed() && cow.distanceTo(mc.player) < 6.0) {
                        cows.add(cow);
                    }
                }
                if (cows.isEmpty()) {
                    fail("a cow to lead", "cow " + n);
                } else {
                    holdItem(mc, Items.LEAD);
                    interact(mc, cows.getFirst());
                }
            });
        }
        at(0, 1390, mc -> {
            long led = mc.level.getEntitiesOfClass(Cow.class, mc.player.getBoundingBox().inflate(10.0), Cow::isLeashed).size();
            check("three cows on leads", led == 3, led + " led");
            cmd(mc, "tp @s 20.5 -60 -3.5 0 20");
        });
        at(0, 1430, mc -> {
            emptyHand(mc);
            interact(mc, trailer(mc, TrailerType.LIVESTOCK_TRAILER));
        });
        at(0, 1455, mc -> {
            TrailerEntity trailer = trailer(mc, TrailerType.LIVESTOCK_TRAILER);
            int riding = trailer == null ? 0 : (int) trailer.getPassengers().stream().filter(Cow.class::isInstance).count();
            check("the cows load into the trailer", riding == 3, riding + " riding");
            int leads = count(mc, stack -> stack.is(Items.LEAD));
            check("the leads come back", leads == 3, leads + " leads");
            shot(mc, "21_livestock_loaded");
            cmd(mc, "reikotest report loaded");
        });
        at(0, 1460, mc -> {
            emptyHand(mc);
            interact(mc, trailer(mc, TrailerType.LIVESTOCK_TRAILER));
        });
        at(0, 1500, mc -> {
            TrailerEntity trailer = trailer(mc, TrailerType.LIVESTOCK_TRAILER);
            int riding = trailer == null ? 0 : trailer.getPassengers().size();
            check("the cows are let out", riding == 0, riding + " riding");
            shot(mc, "22_let_out");
        });

        // Leave, losing the manual, and come back: it isn't given again.
        at(0, 1510, mc -> cmd(mc, "reikotest clearmanual"));
        at(0, 1530, mc -> cmd(mc, "reikotest report leaving"));
        at(0, 1550, mc -> {
            ReikoCars.LOGGER.info("MPCHECK leaving the server");
            mc.level.disconnect();
            mc.disconnect(new TitleScreen());
        });
        at(1, 60, mc -> {
            int manuals = count(mc, DevMultiplayer::isManual);
            check("the manual isn't given again on rejoining", manuals == 0, manuals + " in the inventory");
            CarEntity sedan = car(mc, CarType.SEDAN);
            check("the sedan is still there after rejoining", sedan != null, pos(sedan));
            shot(mc, "23_rejoined");
            cmd(mc, "reikotest report rejoined");
        });
        // A hitched rig and a trailer with cows aboard, to be saved when the server is restarted and loaded again.
        at(1, 65, mc -> cmd(mc, "reikotest rig"));
        at(1, 150, mc -> cmd(mc, "reikotest report before_restart"));
        at(1, 170, mc -> {
            ReikoCars.LOGGER.info("MPCHECK leaving for the server's restart");
            mc.level.disconnect();
            mc.disconnect(new TitleScreen());
        });
        // After the restart: everything as it was left.
        at(2, 60, mc -> {
            int manuals = count(mc, DevMultiplayer::isManual);
            check("still no manual after the restart", manuals == 0, manuals + " in the inventory");
            CarEntity sedan = car(mc, CarType.SEDAN);
            check("the sedan survives the restart, with its fuel", sedan != null && sedan.fuel() > 200000,
                    sedan == null ? "none" : pos(sedan) + " fuel " + sedan.fuel());
            TrailerEntity tanker = trailer(mc, TrailerType.TANKER_TRAILER);
            check("the semi's tanker is still hitched after the restart", tanker != null && tanker.getVehicle() instanceof CarEntity,
                    tanker == null ? "no tanker" : "riding " + tanker.getVehicle());
            // The rig scene's livestock trailer (there's another, emptied earlier): the one by (8.5, 20.5).
            TrailerEntity pens = nearest(mc, TrailerEntity.class, trailer -> trailer.trailerType() == TrailerType.LIVESTOCK_TRAILER
                    && trailer.distanceToSqr(8.5, -60.0, 20.5) < 4.0);
            long aboard = pens == null ? 0 : pens.getPassengers().stream().filter(Cow.class::isInstance).count();
            check("the cows in a livestock trailer are still aboard after the restart", aboard == 2, aboard + " aboard");
            TrailerEntity camper = trailer(mc, TrailerType.CAMPER);
            check("the parked camper keeps its camp", camper != null && camper.campOut(), camper == null ? "none" : "campOut=" + camper.campOut());
            long cows = mc.level.getEntitiesOfClass(Cow.class, mc.player.getBoundingBox().inflate(80.0)).size();
            check("the cows let out are still about", cows >= 5, cows + " cows");
            shot(mc, "24_after_restart");
            if (sedan != null) {
                cmd(mc, String.format("tp @s %.1f -60 %.1f", sedan.getX(), sedan.getZ() + 2.5));
            }
        });
        at(2, 80, mc -> {
            emptyHand(mc);
            CarEntity sedan = car(mc, CarType.SEDAN);
            carStart = sedan == null ? Vec3.ZERO : sedan.position();
            interact(mc, sedan);
        });
        at(2, 100, mc -> mc.options.keyUp.setDown(true));
        at(2, 140, mc -> {
            mc.options.keyUp.setDown(false);
            CarEntity sedan = car(mc, CarType.SEDAN);
            double moved = sedan == null ? 0.0 : sedan.position().distanceTo(carStart);
            check("its key still starts the sedan after the restart", moved > 3.0, String.format("moved %.1f blocks", moved));
            cmd(mc, "reikotest report after_restart");
        });
        at(2, 160, mc -> {
            ReikoCars.LOGGER.info("MPCHECK done: {} passed, {} failed", passes, fails);
            mc.stop();
        });
    }
}
