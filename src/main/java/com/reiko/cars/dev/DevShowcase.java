package com.reiko.cars.dev;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.client.RepairBenchScreen;
import com.reiko.cars.repair.RepairBenchBlock;
import com.reiko.cars.repair.RepairBenchBlockEntity;
import com.reiko.cars.repair.RepairBenchMenu;
import com.reiko.cars.car.CarType;
import com.reiko.cars.network.CarActionPayload;
import com.reiko.cars.network.PumpActionPayload;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerType;
import com.reiko.cars.pump.GasPumpBlock;
import com.reiko.cars.pump.GasPumpBlockEntity;
import com.reiko.cars.pump.PumpMenu;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModEntities;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.client.CarWorkbenchScreen;
import com.reiko.cars.compat.jei.ReikoCarsJeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.neoforged.fml.ModList;
import com.reiko.cars.workbench.CarPart;
import com.reiko.cars.workbench.CarWorkbenchBlock;
import com.reiko.cars.workbench.CarWorkbenchMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import com.reiko.economy.bank.Purse;
import com.reiko.economy.core.CardService;
import com.reiko.economy.core.EconomyService;
import com.reiko.economy.shop.Payment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * DEV ONLY (left out of the jar by build.gradle). With {@code ./gradlew runClient -Pshowcase}, makes a flat creative
 * world, builds a gas station, buys fuel and drives a car, saving screenshots to {@code run/screenshots/} as it goes,
 * then quits. Used to check the models, paint, seats, dashboard, pump display and pump screen in the real game.
 */
@EventBusSubscriber(modid = ReikoCars.MODID, value = Dist.CLIENT)
public final class DevShowcase {
    /**
     * "true" for the cars and pumps, "workbench" for the Car Workbench, "diesel" for the diesel pump, "semi" for semis
     * and trailers, "bikes" for the motorcycles, "cars2" for the second wave of cars, "repair" for wear and the Repair
     * Bench, "false" for none.
     */
    private static final String MODE = System.getProperty("reikocars.showcase", "false");
    private static final boolean ENABLED = !MODE.equals("false");
    private static final BlockPos BENCH = new BlockPos(2, -60, 3);
    private static final BlockPos PUMP = new BlockPos(4, -60, 6);
    private static final BlockPos PUMP2 = new BlockPos(8, -60, 6);
    private static boolean worldRequested;
    private static int tick = -1;
    private static final List<Step> STEPS = new ArrayList<>();

    private record Step(int at, Consumer<Minecraft> action) {
    }

    private DevShowcase() {
    }

    private static void at(int time, Consumer<Minecraft> action) {
        STEPS.add(new Step(time, action));
    }

    @SubscribeEvent
    static void onScreen(ScreenEvent.Opening event) {
        if (!ENABLED || worldRequested || !(event.getNewScreen() instanceof TitleScreen)) {
            return;
        }
        worldRequested = true;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            LevelSettings settings = new LevelSettings("showcase-" + System.currentTimeMillis(), GameType.CREATIVE, false,
                    Difficulty.PEACEFUL, !MODE.equals("repair") && !MODE.equals("lots") && !MODE.equals("sales") && !MODE.equals("tolls") && !MODE.equals("insurance") && !MODE.equals("work"), rules, WorldDataConfiguration.DEFAULT); // no cheats: a customer, not an admin
            minecraft.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), new TitleScreen());
        });
        if (MODE.equals("workbench")) {
            workbenchScript();
        } else if (MODE.equals("diesel")) {
            dieselScript();
        } else if (MODE.equals("semi")) {
            semiScript();
        } else if (MODE.equals("bikes")) {
            bikeScript();
        } else if (MODE.equals("cars2")) {
            moreCarsScript();
        } else if (MODE.equals("repair")) {
            repairScript();
        } else if (MODE.equals("tow")) {
            towScript();
        } else if (MODE.equals("keys")) {
            keysScript();
        } else if (MODE.equals("fares")) {
            faresScript();
        } else if (MODE.equals("sales")) {
            salesScript();
        } else if (MODE.equals("tolls")) {
            tollsScript();
        } else if (MODE.equals("insurance")) {
            insuranceScript();
        } else if (MODE.equals("work")) {
            workScript();
        } else if (MODE.equals("signals")) {
            signalsScript();
        } else if (MODE.equals("roads")) {
            roadsScript();
        } else if (MODE.equals("radio")) {
            radioScript();
        } else if (MODE.equals("hud")) {
            hudScript();
        } else if (MODE.equals("wall")) {
            wallScript();
        } else if (MODE.equals("water")) {
            waterScript();
        } else if (MODE.equals("headlights")) {
            headlightsScript();
        } else if (MODE.equals("ev")) {
            evScript();
        } else if (MODE.equals("fire")) {
            fireScript();
        } else if (MODE.equals("lots")) {
            lotsScript();
        } else if (MODE.equals("dmv")) {
            dmvScript();
        } else if (MODE.equals("tune")) {
            tuneScript();
        } else if (MODE.equals("wash")) {
            washScript();
        } else if (MODE.equals("weather")) {
            weatherScript();
        } else if (MODE.equals("gps")) {
            gpsScript();
        } else if (MODE.equals("busstops")) {
            busScript();
        } else if (MODE.equals("campers")) {
            campScript();
        } else if (MODE.startsWith("gallery")) {
            galleryScript(MODE.contains(":") ? MODE.substring(MODE.indexOf(':') + 1) : "");
        } else {
            script();
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ENABLED || minecraft.player == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        tick++;
        if (tick == 1 && !MODE.equals("keys")) {
            // The older scripts drive their cars without cutting keys first.
            minecraft.getSingleplayerServer().execute(() -> com.reiko.cars.config.CarsConfig.REQUIRE_KEYS.set(false));
        }
        for (Step step : STEPS) {
            if (step.at == tick) {
                try {
                    step.action.accept(minecraft);
                } catch (RuntimeException e) {
                    ReikoCars.LOGGER.error("Showcase step at tick {} failed", tick, e);
                }
            }
        }
    }

    private static void server(Minecraft minecraft, Consumer<ServerPlayer> action) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        java.util.UUID id = minecraft.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    private static void look(Minecraft minecraft, double x, double y, double z, float yaw, float pitch) {
        server(minecraft, player -> {
            player.teleportTo(player.serverLevel(), x, y, z, yaw, pitch);
            player.getAbilities().flying = true; // hold still in mid-air for the shot
            player.onUpdateAbilities();
        });
    }

    private static void shot(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.getMainRenderTarget(),
                message -> ReikoCars.LOGGER.info("Showcase screenshot: {}", message.getString()));
    }

    private static CarEntity car(ServerLevel level, CarType type) {
        return level.getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(-40, -70, -40, 40, -40, 40),
                car -> car.carType() == type).getFirst();
    }

    /** The first {@code type} found, the one with or without a trailer. */
    private static CarEntity car(ServerLevel level, CarType type, boolean towing) {
        return level.getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(-40, -70, -40, 40, -40, 40),
                car -> car.carType() == type && (car.trailer() != null) == towing).getFirst();
    }

    private static Fluid fuel(String name) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("buildcraftreborn", name));
    }

    private static void placePump(ServerLevel level, BlockPos pos, ServerPlayer owner, String fluid, int amount, long price) {
        placePump(level, ModBlocks.GAS_PUMP.get(), pos, owner, fluid, amount, price);
    }

    private static void placePump(ServerLevel level, GasPumpBlock block, BlockPos pos, ServerPlayer owner, String fluid, int amount, long price) {
        BlockState lower = block.defaultBlockState().setValue(GasPumpBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, lower);
        level.setBlockAndUpdate(pos.above(), lower.setValue(GasPumpBlock.HALF, DoubleBlockHalf.UPPER));
        GasPumpBlockEntity pump = (GasPumpBlockEntity) level.getBlockEntity(pos);
        pump.setOwner(owner);
        pump.setPrice(price);
        if (amount > 0) {
            pump.tank().fill(new FluidStack(fuel(fluid), amount), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private static void script() {
        // Build the scene.
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -45; x <= 34; x++) {
                    for (int z = -5; z <= 4; z++) {
                        BlockState road = z == 0 && x % 3 != 0 ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState();
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), road);
                    }
                    for (int z = 5; z <= 8; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                // A fake second player owns the pumps, so buying from them costs money.
                ServerPlayer owner = player;
                placePump(level, PUMP, owner, "fuel_light", 12_000, 375);
                placePump(level, ModBlocks.DIESEL_PUMP.get(), PUMP2, owner, "fuel_dense", 4_000, 1450);
                GasPumpBlockEntity first = (GasPumpBlockEntity) level.getBlockEntity(PUMP);
                first.setOwner(null); // nobody owns it: the player pays like a customer, money leaves the economy
                spawn(level, CarType.SEDAN, 4.5, 2.5, 90, 0.12); // facing west, down the empty road
                spawn(level, CarType.SPORTS_CAR, 13.0, 2.5, -90, 0.8);
                spawn(level, CarType.PICKUP_TRUCK, -4.0, -2.5, -90, 0.5);
                spawn(level, CarType.OFF_ROADER, 21.0, -2.5, -90, 1.0);
                EconomyService.INSTANCE.ensureAccount(player.server, player.getUUID(), player.getGameProfile().getName());
                Purse.give(player, 5_000);
                CardService.issue(player, "1234");
            });
        });
        // The line-up, from across the road.
        at(80, mc -> look(mc, 8.5, -58.0, -12.0, 0.0F, 12.0F));
        at(140, mc -> shot(mc, "01_lineup"));
        at(145, mc -> look(mc, 22.0, -57.5, 10.5, 135.0F, 25.0F));
        at(200, mc -> shot(mc, "02_rear_three_quarter"));
        at(205, mc -> look(mc, 1.0, -59.0, 9.5, 225.0F, 8.0F));
        at(260, mc -> shot(mc, "03_pump_and_sedan"));
        // Buy fuel at the pump with cash.
        at(265, mc -> look(mc, 4.5, -60.0, 9.0, 180.0F, 15.0F));
        at(290, mc -> server(mc, player -> PumpMenu.open(player, (GasPumpBlockEntity) player.level().getBlockEntity(PUMP))));
        at(320, mc -> shot(mc, "04_pump_screen"));
        at(325, mc -> {
            if (mc.player.containerMenu instanceof PumpMenu menu) {
                PacketDistributor.sendToServer(PumpActionPayload.fill(menu.containerId, 3000, menu.price(), Payment.cash()));
            }
        });
        at(350, mc -> shot(mc, "05_pump_paid"));
        at(355, mc -> mc.player.closeContainer());
        // Drive the sedan.
        at(360, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.SEDAN), true)));
        at(390, mc -> shot(mc, "06_driver_seat"));
        at(392, mc -> mc.options.keyUp.setDown(true));
        at(420, mc -> mc.options.keyLeft.setDown(true));
        at(432, mc -> mc.options.keyLeft.setDown(false));
        at(432, mc -> shot(mc, "07_driving_first_person"));
        at(442, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(470, mc -> shot(mc, "08_driving_third_person"));
        at(472, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(true); // handbrake
        });
        at(500, mc -> mc.options.keyJump.setDown(false));
        // Night, with the lights on.
        at(505, mc -> {
            server(mc, player -> player.serverLevel().setDayTime(18000));
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        at(560, mc -> shot(mc, "09_night_lights"));
        at(565, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.serverLevel().setDayTime(6000);
            });
        });
        // A passenger in the pickup, and the off-roader from the front.
        at(570, mc -> look(mc, 21.0, -59.0, -9.0, 0.0F, 5.0F));
        at(620, mc -> shot(mc, "10_off_roader_front"));
        // The pumps' lit displays, up close.
        at(625, mc -> look(mc, 6.0, -60.0, 3.2, 0.0F, -12.0F));
        at(680, mc -> shot(mc, "11_pump_fronts"));
        at(690, mc -> mc.stop());
    }

    private static void spawn(ServerLevel level, CarType type, double x, double z, float yaw, double fuel) {
        CarEntity car = ModEntities.car(type).create(level);
        car.moveTo(x, -60.0, z, yaw, 0.0F);
        car.setFuel(type.tankCapacity() * fuel);
        level.addFreshEntity(car);
    }

    // ---- the Car Workbench: ./gradlew runClient -Pshowcase=workbench ----

    private static void clickCar(Minecraft minecraft, int index) {
        if (minecraft.player.containerMenu instanceof CarWorkbenchMenu menu) {
            menu.clickMenuButton(minecraft.player, index);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
        }
    }

    /** Puts the mouse over a point of the open screen, in GUI coordinates, so its tooltip shows. */
    private static void hover(Minecraft minecraft, double guiX, double guiY) {
        double scale = minecraft.getWindow().getGuiScale();
        try {
            for (String name : new String[] {"xpos", "ypos"}) {
                java.lang.reflect.Field field = net.minecraft.client.MouseHandler.class.getDeclaredField(name);
                field.setAccessible(true);
                field.setDouble(minecraft.mouseHandler, (name.equals("xpos") ? guiX : guiY) * scale);
            }
        } catch (ReflectiveOperationException e) {
            ReikoCars.LOGGER.warn("Showcase couldn't move the mouse", e);
        }
    }

    private static void give(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        player.getInventory().add(new ItemStack(item, count));
    }

    private static void workbenchScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -6; x <= 12; x++) {
                    for (int z = -2; z <= 10; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                level.setBlockAndUpdate(BENCH, ModBlocks.CAR_WORKBENCH.get().defaultBlockState().setValue(CarWorkbenchBlock.FACING, Direction.SOUTH));
                level.setBlockAndUpdate(BENCH.east(), ModBlocks.CAR_WORKBENCH.get().defaultBlockState().setValue(CarWorkbenchBlock.FACING, Direction.EAST));
                spawn(level, CarType.SPORTS_CAR, 7.0, 4.0, 200, 0.5);
                player.getInventory().clearContent();
                // Half a sedan: no interior, two tires short.
                give(player, ModItems.part(CarType.SEDAN, CarPart.CHASSIS), 1);
                give(player, ModItems.part(CarType.SEDAN, CarPart.BODY), 1);
                give(player, ModItems.CAR_ENGINE.get(), 1);
                give(player, ModItems.TIRE.get(), 2);
                for (CarType type : new CarType[] {CarType.PICKUP_TRUCK, CarType.OFF_ROADER}) {
                    for (CarPart part : CarPart.values()) {
                        give(player, ModItems.part(type, part), 1);
                    }
                }
            });
        });
        at(70, mc -> look(mc, 3.0, -60.0, 6.6, 165.0F, 28.0F));
        at(120, mc -> shot(mc, "12_workbench_block"));
        at(125, mc -> server(mc, player -> player.openMenu(
                player.serverLevel().getBlockState(BENCH).getMenuProvider(player.serverLevel(), BENCH))));
        at(160, mc -> shot(mc, "13_workbench_blueprint"));
        at(165, mc -> clickCar(mc, CarType.SEDAN.ordinal()));
        at(195, mc -> shot(mc, "14_workbench_half_a_sedan"));
        at(200, mc -> server(mc, player -> {
            for (CarPart part : CarPart.values()) {
                give(player, ModItems.part(CarType.SPORTS_CAR, part), 1);
            }
            give(player, ModItems.CAR_ENGINE.get(), 1);
            give(player, ModItems.TIRE.get(), 4);
        }));
        at(210, mc -> clickCar(mc, CarType.SPORTS_CAR.ordinal()));
        at(240, mc -> shot(mc, "15_workbench_sports_car_ready"));
        at(245, mc -> {
            if (mc.screen instanceof CarWorkbenchScreen screen) {
                int left = (screen.width - 236) / 2;
                int top = (screen.height - 212) / 2;
                hover(mc, left + 8 + 24 * CarType.SEDAN.ordinal() + 11, top + 17 + 11);
            }
        });
        at(270, mc -> shot(mc, "16_workbench_tooltip"));
        at(275, mc -> {
            hover(mc, 0, 0);
            if (mc.player.containerMenu instanceof CarWorkbenchMenu menu) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, CarWorkbenchMenu.RESULT, 0, ClickType.QUICK_MOVE, mc.player);
            }
        });
        at(305, mc -> shot(mc, "17_workbench_built"));
        at(310, mc -> mc.player.closeContainer());
        // JEI: the sedan's recipe, what a chassis is used for, and the "+" button's transfer.
        at(320, mc -> jeiShow(RecipeIngredientRole.OUTPUT, new ItemStack(ModItems.car(CarType.SEDAN))));
        at(350, mc -> shot(mc, "18_jei_sedan_recipe"));
        at(355, mc -> jeiShow(RecipeIngredientRole.INPUT, new ItemStack(ModItems.part(CarType.OFF_ROADER, CarPart.CHASSIS))));
        at(385, mc -> shot(mc, "19_jei_chassis_uses"));
        at(390, mc -> {
            mc.setScreen(null);
            server(mc, player -> {
                give(player, ModItems.CAR_ENGINE.get(), 1);
                give(player, ModItems.TIRE.get(), 4);
                player.openMenu(player.serverLevel().getBlockState(BENCH).getMenuProvider(player.serverLevel(), BENCH));
            });
        });
        at(410, mc -> jeiTransfer(mc, CarType.SEDAN));    // the sedan still has no interior: refused
        at(415, mc -> jeiTransfer(mc, CarType.PICKUP_TRUCK));
        at(440, mc -> shot(mc, "20_jei_transfer_pickup"));
        at(445, mc -> jeiShow(RecipeIngredientRole.OUTPUT, new ItemStack(ModItems.car(CarType.PICKUP_TRUCK))));
        at(475, mc -> shot(mc, "21_jei_over_workbench"));
        at(480, mc -> mc.player.closeContainer());
        at(490, mc -> mc.stop());
    }

    private static void jeiShow(RecipeIngredientRole role, ItemStack stack) {
        IJeiRuntime runtime = ModList.get().isLoaded("jei") ? ReikoCarsJeiPlugin.runtime() : null;
        if (runtime == null) {
            ReikoCars.LOGGER.warn("Showcase: JEI isn't running");
            return;
        }
        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack));
    }

    /** Does what JEI's "+" button does on a car's recipe while the workbench is open, logging whether JEI allows it. */
    private static void jeiTransfer(Minecraft minecraft, CarType type) {
        IJeiRuntime runtime = ModList.get().isLoaded("jei") ? ReikoCarsJeiPlugin.runtime() : null;
        if (runtime == null || !(minecraft.player.containerMenu instanceof CarWorkbenchMenu menu)) {
            ReikoCars.LOGGER.warn("Showcase: no JEI or no workbench open");
            return;
        }
        var category = runtime.getRecipeManager().getRecipeCategory(ReikoCarsJeiPlugin.CAR_ASSEMBLY);
        var recipe = menu.recipes().get(type.ordinal());
        var layout = runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe,
                runtime.getJeiHelpers().getFocusFactory().createFocusGroup(List.of())).orElseThrow();
        var handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, category).orElseThrow();
        var check = handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), minecraft.player, false, false);
        ReikoCars.LOGGER.info("Showcase: JEI transfer of the {}: {}", type.id(), check == null ? "allowed" : "refused (" + check.getType() + ")");
        if (check == null) {
            handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), minecraft.player, false, true);
        }
    }

    // ---- the diesel pump: ./gradlew runClient -Pshowcase=diesel ----

    /** Presses the pump screen's button that switches between filling the car and filling buckets. */
    private static void pressTargetButton(Minecraft minecraft) {
        if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
            screen.mouseClicked(screen.getGuiLeft() + 15, screen.getGuiTop() + 51, 0);
        }
    }

    private static void dieselScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -6; x <= 14; x++) {
                    for (int z = -4; z <= 10; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), z <= 3 ? Blocks.GRAY_CONCRETE.defaultBlockState()
                                : Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                placePump(level, PUMP, player, "fuel_light", 12_000, 375);
                placePump(level, ModBlocks.DIESEL_PUMP.get(), PUMP2, player, "fuel_dense", 9_000, 1450);
                ((GasPumpBlockEntity) level.getBlockEntity(PUMP)).setOwner(null);
                ((GasPumpBlockEntity) level.getBlockEntity(PUMP2)).setOwner(null);
                spawn(level, CarType.PICKUP_TRUCK, 8.5, 2.5, 90, 0.3);
                player.getInventory().clearContent();
                EconomyService.INSTANCE.ensureAccount(player.server, player.getUUID(), player.getGameProfile().getName());
                Purse.give(player, 8_000);
                give(player, net.minecraft.world.item.Items.BUCKET, 4);
            });
        });
        // Both pumps from the front: the signs say GAS and DIESEL.
        at(70, mc -> look(mc, 6.0, -60.0, 1.6, 0.0F, -18.0F));
        at(120, mc -> shot(mc, "22_gas_and_diesel_pumps"));
        at(125, mc -> look(mc, 3.0, -59.0, -4.0, 0.0F, 12.0F));
        at(175, mc -> shot(mc, "23_pumps_from_the_road"));
        // The diesel pump's screen: the pickup at the hose runs on gasoline, so it starts on buckets.
        at(180, mc -> look(mc, 8.5, -60.0, 9.0, 180.0F, 15.0F));
        at(200, mc -> server(mc, player -> PumpMenu.open(player, (GasPumpBlockEntity) player.level().getBlockEntity(PUMP2))));
        at(230, mc -> shot(mc, "24_diesel_pump_buckets"));
        at(235, mc -> {
            if (mc.player.containerMenu instanceof PumpMenu menu) {
                PacketDistributor.sendToServer(PumpActionPayload.fillBuckets(menu.containerId, 3, menu.price(), Payment.cash()));
            }
        });
        at(260, mc -> shot(mc, "25_diesel_pump_bought"));
        at(265, mc -> pressTargetButton(mc));
        at(290, mc -> shot(mc, "26_diesel_pump_car_wrong_fuel"));
        at(295, mc -> mc.player.closeContainer());
        // Pouring a bucket of diesel into the pickup is refused.
        at(300, mc -> server(mc, player -> {
            for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
                ItemStack stack = player.getInventory().items.get(slot);
                if (net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(stack).isPresent()) {
                    player.getInventory().selected = slot < 9 ? slot : player.getInventory().selected;
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
                    break;
                }
            }
            CarEntity pickup = car(player.serverLevel(), CarType.PICKUP_TRUCK);
            pickup.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }));
        at(305, mc -> shot(mc, "27_diesel_refused_by_car"));
        at(320, mc -> mc.stop());
    }

    // ---- semis and trailers: ./gradlew runClient -Pshowcase=semi ----

    private static TrailerEntity trailer(ServerLevel level, TrailerType type, double x, double z, float yaw) {
        TrailerEntity trailer = ModEntities.trailer(type).create(level);
        trailer.moveTo(x, -60.0, z, yaw, 0.0F);
        level.addFreshEntity(trailer);
        return trailer;
    }

    /** A trailer standing behind {@code tractor}, its kingpin on the fifth wheel, turned {@code angle} degrees. */
    private static TrailerEntity behind(ServerLevel level, TrailerType type, CarEntity tractor, float angle) {
        float yaw = tractor.getYRot() + angle;
        float radians = yaw * net.minecraft.util.Mth.DEG_TO_RAD;
        net.minecraft.world.phys.Vec3 hitch = tractor.hitchPoint();
        return trailer(level, type, hitch.x + Math.sin(radians) * type.couplerForward(), hitch.z - Math.cos(radians) * type.couplerForward(), yaw);
    }

    private static CarEntity spawned(ServerLevel level, CarType type, double x, double z, float yaw, double fuel) {
        CarEntity car = ModEntities.car(type).create(level);
        car.moveTo(x, -60.0, z, yaw, 0.0F);
        car.setFuel(type.tankCapacity() * fuel);
        level.addFreshEntity(car);
        return car;
    }

    private static void semiScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -50; x <= 50; x++) {
                    for (int z = -26; z <= 26; z++) {
                        BlockState ground = Math.floorMod(z, 12) == 6 && x % 4 != 0 ? Blocks.WHITE_CONCRETE.defaultBlockState()
                                : Blocks.GRAY_CONCRETE.defaultBlockState();
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), ground);
                    }
                }
                // Row 1: a semi with a box trailer lined up behind it, ready to hitch, and a diesel pump.
                CarEntity semi = spawned(level, CarType.SEMI_TRUCK, 0.5, 0.5, -90.0F, 0.7);
                behind(level, TrailerType.BOX_TRAILER, semi, 0.0F);
                placePump(level, ModBlocks.DIESEL_PUMP.get(), new BlockPos(3, -60, -3), player, "fuel_dense", 9_000, 1450);
                // Row 2: a red semi pulling a tanker of diesel, swung round as if turning.
                CarEntity red = spawned(level, CarType.SEMI_TRUCK, 4.5, 12.5, -90.0F, 1.0);
                red.setColor(0xB02E26);
                TrailerEntity tanker = behind(level, TrailerType.TANKER_TRAILER, red, 25.0F);
                tanker.tank().fill(new FluidStack(fuel("fuel_dense"), 41_500), IFluidHandler.FluidAction.EXECUTE);
                tanker.startRiding(red, true);
                // Row 3: a car hauler with a sedan and a sports car aboard, standing on its own.
                TrailerEntity hauler = trailer(level, TrailerType.CAR_HAULER, -2.0, -12.5, -90.0F);
                spawned(level, CarType.SEDAN, -20.0, -12.5, -90.0F, 0.5).startRiding(hauler, true);
                spawned(level, CarType.SPORTS_CAR, -26.0, -12.5, -90.0F, 0.5).startRiding(hauler, true);
                spawned(level, CarType.PICKUP_TRUCK, -12.5, -16.5, -90.0F, 0.5);
                player.getInventory().clearContent();
                give(player, ModItems.car(CarType.SEMI_TRUCK), 1);
                for (TrailerType type : TrailerType.values()) {
                    give(player, ModItems.trailer(type), 1);
                }
                give(player, ModItems.DIESEL_ENGINE.get(), 1);
                for (CarPart part : CarPart.values()) {
                    give(player, ModItems.part(CarType.SEMI_TRUCK, part), 1);
                }
            });
        });
        // The yard, from the side.
        at(80, mc -> look(mc, 26.0, -48.0, -26.0, 49.1F, 18.9F));
        at(140, mc -> shot(mc, "30_semi_yard"));
        // The semi's cab, three-quarters front.
        at(145, mc -> look(mc, 9.5, -60.0, -6.5, 47.0F, 6.8F));
        at(200, mc -> shot(mc, "31_semi_front"));
        // The rear three-quarters: fifth wheel, twin wheels, the trailer's front and landing legs.
        at(205, mc -> look(mc, -4.5, -60.0, -5.0, -28.6F, 11.0F));
        at(260, mc -> shot(mc, "32_fifth_wheel"));
        // The red rig from above: the tanker swung round behind it.
        at(265, mc -> look(mc, -2.0, -40.0, -1.0, 0.0F, 58.0F));
        at(320, mc -> shot(mc, "33_rig_from_above"));
        // The car hauler, loaded.
        at(325, mc -> look(mc, 6.5, -59.0, -20.5, 49.9F, 9.7F));
        at(380, mc -> shot(mc, "34_car_hauler"));
        // In the cab: the dashboard says the trailer can be hitched.
        at(385, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.SEMI_TRUCK, false), true)));
        at(420, mc -> shot(mc, "35_cab_hitch_hint"));
        at(425, mc -> PacketDistributor.sendToServer(new CarActionPayload(CarActionPayload.HITCH)));
        at(430, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(445, mc -> shot(mc, "36_hitched"));
        // Pull away and turn left: the trailer follows round.
        at(450, mc -> mc.options.keyUp.setDown(true));
        at(500, mc -> mc.options.keyLeft.setDown(true));
        at(530, mc -> shot(mc, "37_turning"));
        at(545, mc -> mc.options.keyLeft.setDown(false));
        at(565, mc -> shot(mc, "38_turned"));
        at(566, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(true);
        });
        at(600, mc -> mc.options.keyJump.setDown(false));
        // Night: the trailer's lights come on with the semi's.
        at(605, mc -> {
            server(mc, player -> player.serverLevel().setDayTime(18000));
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        at(650, mc -> shot(mc, "39_night"));
        at(655, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.serverLevel().setDayTime(6000);
            });
        });
        // The tanker's screen.
        at(665, mc -> look(mc, -6.0, -60.0, 16.0, 180.0F, 10.0F));
        at(690, mc -> mc.level.getEntitiesOfClass(TrailerEntity.class, new net.minecraft.world.phys.AABB(-40, -70, -40, 40, -40, 40),
                t -> t.trailerType() == TrailerType.TANKER_TRAILER).forEach(t -> TrailerEntity.openTankerScreen.accept(t)));
        at(720, mc -> shot(mc, "40_tanker_screen"));
        at(725, mc -> mc.setScreen(null));
        // The items.
        at(730, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(760, mc -> shot(mc, "41_items"));
        // Building a semi at the Car Workbench, and its recipe in JEI.
        at(765, mc -> {
            mc.player.closeContainer();
            server(mc, player -> {
                BlockPos bench = new BlockPos(-6, -60, 20);
                player.serverLevel().setBlockAndUpdate(bench, ModBlocks.CAR_WORKBENCH.get().defaultBlockState());
                player.getInventory().clearContent();
                for (CarPart part : CarPart.values()) {
                    give(player, ModItems.part(CarType.SEMI_TRUCK, part), 1);
                }
                give(player, ModItems.DIESEL_ENGINE.get(), 1);
                give(player, ModItems.TIRE.get(), 4);
                player.openMenu(player.serverLevel().getBlockState(bench).getMenuProvider(player.serverLevel(), bench));
            });
        });
        at(785, mc -> clickCar(mc, CarType.SEMI_TRUCK.ordinal()));
        at(810, mc -> shot(mc, "42_workbench_semi"));
        at(815, mc -> jeiShow(RecipeIngredientRole.OUTPUT, new ItemStack(ModItems.car(CarType.SEMI_TRUCK))));
        at(845, mc -> shot(mc, "43_jei_semi_recipe"));
        at(850, mc -> jeiShow(RecipeIngredientRole.OUTPUT, new ItemStack(ModItems.trailer(TrailerType.TANKER_TRAILER))));
        at(880, mc -> shot(mc, "44_jei_tanker_recipe"));
        // A car hauler at the workbench, two tires short: no engine or interior slot for a trailer.
        at(885, mc -> {
            mc.player.closeContainer(); // hands the semi's parts back, so take them away after
            server(mc, player -> {
                BlockPos bench = new BlockPos(-6, -60, 20);
                player.closeContainer();
                player.getInventory().clearContent();
                give(player, ModItems.TRAILER_CHASSIS.get(), 1);
                give(player, ModItems.trailerBody(TrailerType.CAR_HAULER), 1);
                give(player, ModItems.TIRE.get(), 2);
                for (TrailerType type : TrailerType.values()) {
                    give(player, ModItems.trailerBody(type), 1);
                }
                player.openMenu(player.serverLevel().getBlockState(bench).getMenuProvider(player.serverLevel(), bench));
            });
        });
        at(905, mc -> clickCar(mc, CarType.values().length + TrailerType.CAR_HAULER.ordinal()));
        at(930, mc -> shot(mc, "45_workbench_car_hauler"));
        at(932, mc -> {
            if (mc.screen instanceof CarWorkbenchScreen screen) {
                int left = (screen.width - 236) / 2;
                int top = (screen.height - 212) / 2;
                hover(mc, left + 8 + 24 * (CarType.values().length + TrailerType.TANKER_TRAILER.ordinal()) + 11, top + 17 + 11);
            }
        });
        at(955, mc -> shot(mc, "46_workbench_trailer_tooltip"));
        at(960, mc -> mc.stop());
    }

    // ---- motorcycles: ./gradlew runClient -Pshowcase=bikes ----

    private static void bikeScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -40; x <= 40; x++) {
                    for (int z = -20; z <= 20; z++) {
                        BlockState ground = Math.floorMod(z, 12) == 6 && x % 4 != 0 ? Blocks.WHITE_CONCRETE.defaultBlockState()
                                : Blocks.GRAY_CONCRETE.defaultBlockState();
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), ground);
                    }
                }
                // Parked in a row, angled like at a bike rack: over on their kickstands.
                spawned(level, CarType.SPORT_BIKE, 0.5, 0.5, -90.0F, 0.8);
                spawned(level, CarType.CRUISER, 0.5, 3.5, -90.0F, 0.8);
                spawned(level, CarType.DIRT_BIKE, 0.5, 6.5, -90.0F, 0.8);
                spawned(level, CarType.SCOOTER, 0.5, 9.5, -90.0F, 0.8);
                spawned(level, CarType.SEDAN, -9.0, 4.5, -90.0F, 0.5);
                player.getInventory().clearContent();
                for (CarType type : new CarType[] {CarType.SPORT_BIKE, CarType.CRUISER, CarType.DIRT_BIKE, CarType.SCOOTER}) {
                    give(player, ModItems.car(type), 1);
                }
                give(player, ModItems.MOTORCYCLE_ENGINE.get(), 1);
                for (CarPart part : CarPart.values()) {
                    give(player, ModItems.part(CarType.CRUISER, part), 1);
                }
            });
        });
        at(80, mc -> look(mc, 7.0, -59.0, -4.0, 40.0F, 16.0F));
        at(130, mc -> shot(mc, "50_bikes_parked"));
        at(135, mc -> look(mc, 2.5, -60.0, -3.0, 20.0F, 12.0F));
        at(185, mc -> shot(mc, "51_bikes_side"));
        at(190, mc -> look(mc, 4.0, -60.0, 13.0, 140.0F, 14.0F));
        at(240, mc -> shot(mc, "52_scooter_and_dirt_bike"));
        // Ride the sport bike, with the cruiser's rider behind.
        at(245, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.SPORT_BIKE), true)));
        at(275, mc -> shot(mc, "53_sport_bike_seat"));
        at(280, mc -> mc.options.keyUp.setDown(true));
        at(330, mc -> mc.options.keyLeft.setDown(true));
        at(342, mc -> shot(mc, "54_leaning_first_person"));
        at(343, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(352, mc -> shot(mc, "55_leaning_third_person"));
        at(353, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        at(362, mc -> shot(mc, "56_leaning_front"));
        at(378, mc -> {
            mc.options.keyLeft.setDown(false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        at(400, mc -> shot(mc, "57_upright"));
        at(402, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(true);
        });
        at(440, mc -> mc.options.keyJump.setDown(false));
        at(445, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> player.stopRiding());
        });
        // The cruiser's saddlebags.
        at(455, mc -> server(mc, player -> {
            CarEntity cruiser = car(player.serverLevel(), CarType.CRUISER);
            player.teleportTo(player.serverLevel(), cruiser.getX() + 1.5, cruiser.getY(), cruiser.getZ(), 90.0F, 30.0F);
            cruiser.bed().setItem(0, new ItemStack(net.minecraft.world.item.Items.BREAD, 12));
            cruiser.bed().setItem(4, new ItemStack(net.minecraft.world.item.Items.MAP));
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) ->
                    new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inventory, cruiser.bed(), 1),
                    net.minecraft.network.chat.Component.translatable(CarType.CRUISER.storageKey())));
        }));
        at(480, mc -> shot(mc, "58_saddlebags"));
        at(482, mc -> mc.player.closeContainer());
        // Building a cruiser at the workbench, and the scooter in JEI.
        at(490, mc -> server(mc, player -> {
            BlockPos bench = new BlockPos(-6, -60, -10);
            player.teleportTo(player.serverLevel(), -6.5, -60.0, -7.5, 180.0F, 30.0F);
            player.serverLevel().setBlockAndUpdate(bench, ModBlocks.CAR_WORKBENCH.get().defaultBlockState());
            give(player, ModItems.TIRE.get(), 1);
            player.openMenu(player.serverLevel().getBlockState(bench).getMenuProvider(player.serverLevel(), bench));
        }));
        at(510, mc -> clickCar(mc, CarType.CRUISER.ordinal()));
        at(535, mc -> shot(mc, "59_workbench_cruiser"));
        at(540, mc -> jeiShow(RecipeIngredientRole.OUTPUT, new ItemStack(ModItems.car(CarType.SCOOTER))));
        at(570, mc -> shot(mc, "60_jei_scooter"));
        at(575, mc -> mc.player.closeContainer());
        at(585, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(610, mc -> shot(mc, "61_items"));
        at(620, mc -> mc.stop());
    }

    // ---- the second wave of cars: ./gradlew runClient -Pshowcase=cars2 ----

    private static void moreCarsScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -40; x <= 40; x++) {
                    for (int z = -24; z <= 24; z++) {
                        BlockState ground = z > 14 ? Blocks.GRASS_BLOCK.defaultBlockState()
                                : Math.floorMod(z, 12) == 6 && x % 4 != 0 ? Blocks.WHITE_CONCRETE.defaultBlockState()
                                : Blocks.GRAY_CONCRETE.defaultBlockState();
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), ground);
                    }
                }
                // Gasoline in front, diesel behind, all facing east.
                spawned(level, CarType.HATCHBACK, -12.0, 0.5, -90.0F, 0.6);
                spawned(level, CarType.MUSCLE_CAR, -6.0, 0.5, -90.0F, 0.6);
                spawned(level, CarType.CONVERTIBLE, 0.0, 0.5, -90.0F, 0.6);
                spawned(level, CarType.MINIVAN, 6.0, 0.5, -90.0F, 0.6);
                spawned(level, CarType.DELIVERY_VAN, -14.0, 7.5, -90.0F, 0.6);
                spawned(level, CarType.HEAVY_PICKUP, -8.0, 7.5, -90.0F, 0.6);
                spawned(level, CarType.FARM_TRACTOR, -2.0, 7.5, -90.0F, 0.6);
                spawned(level, CarType.CITY_BUS, 7.0, 7.5, -90.0F, 0.6);
                player.getInventory().clearContent();
                for (CarType type : new CarType[] {CarType.HATCHBACK, CarType.MUSCLE_CAR, CarType.CONVERTIBLE, CarType.MINIVAN,
                        CarType.DELIVERY_VAN, CarType.CITY_BUS, CarType.HEAVY_PICKUP, CarType.FARM_TRACTOR}) {
                    give(player, ModItems.car(type), 1);
                }
            });
        });
        at(80, mc -> look(mc, 12.0, -57.0, -8.0, 50.0F, 18.0F));
        at(130, mc -> shot(mc, "70_gas_cars_front"));
        at(135, mc -> look(mc, -20.0, -57.0, -6.0, -55.0F, 16.0F));
        at(185, mc -> shot(mc, "71_gas_cars_rear"));
        at(190, mc -> look(mc, -20.0, -56.5, 16.0, -125.0F, 16.0F));
        at(240, mc -> shot(mc, "72_diesel_rear"));
        at(245, mc -> look(mc, 18.0, -56.5, 15.0, 120.0F, 14.0F));
        at(295, mc -> shot(mc, "73_bus_front"));
        // Riding in the bus: driver's seat, then third person.
        at(300, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.CITY_BUS), true)));
        at(330, mc -> shot(mc, "74_bus_driver"));
        at(332, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(350, mc -> shot(mc, "75_bus_third_person"));
        at(352, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> player.stopRiding());
        });
        // The tractor in the field, and the van's cargo area.
        at(360, mc -> look(mc, 3.0, -58.0, 13.0, 150.0F, 18.0F));
        at(400, mc -> shot(mc, "76_tractor"));
        at(405, mc -> server(mc, player -> {
            CarEntity van = car(player.serverLevel(), CarType.DELIVERY_VAN);
            player.teleportTo(player.serverLevel(), van.getX(), van.getY(), van.getZ() - 2.5, 0.0F, 20.0F);
            van.bed().setItem(0, new ItemStack(net.minecraft.world.item.Items.CHEST, 16));
            van.bed().setItem(30, new ItemStack(net.minecraft.world.item.Items.OAK_LOG, 64));
            van.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(true);
            van.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
        }));
        at(430, mc -> shot(mc, "77_van_cargo"));
        at(432, mc -> mc.player.closeContainer());
        at(440, mc -> jeiShow(RecipeIngredientRole.OUTPUT, new ItemStack(ModItems.car(CarType.CITY_BUS))));
        at(470, mc -> shot(mc, "78_jei_bus"));
        at(475, mc -> mc.setScreen(null));
        at(480, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(505, mc -> shot(mc, "79_items"));
        at(515, mc -> mc.stop());
    }

    // ---- every model from four sides: ./gradlew runClient -Pshowcase=gallery:sedan,box_trailer,pumps ----

    private static final int GALLERY_SPAN = 150;

    /** A camera at (dx, dy, dz) blocks from (tx, ty, tz), looking at it. */
    private static void aim(Minecraft minecraft, double tx, double ty, double tz, double dx, double dy, double dz) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
        float pitch = (float) Math.toDegrees(Math.atan2(dy, horizontal));
        look(minecraft, tx + dx, ty + dy - 1.62, tz + dz, yaw, pitch);
    }

    private static void galleryScript(String list) {
        List<String> names = new ArrayList<>();
        if (list.isEmpty()) {
            for (CarType type : CarType.values()) {
                names.add(type.id());
            }
            for (TrailerType type : TrailerType.values()) {
                names.add(type.id());
            }
            names.add("pumps");
        } else {
            names.addAll(List.of(list.split(",")));
        }
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -14; x <= 14; x++) {
                    for (int z = -14; z <= 14; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), (x + z & 1) == 0
                                ? Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                player.getInventory().clearContent();
            });
        });
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            int t = 60 + i * GALLERY_SPAN;
            at(t, mc -> server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(-20, -70, -20, 20, -40, 20),
                        e -> e instanceof CarEntity || e instanceof TrailerEntity).forEach(net.minecraft.world.entity.Entity::discard);
                for (int x = -3; x <= 3; x++) {
                    for (int y = -60; y <= -58; y++) {
                        level.setBlockAndUpdate(new BlockPos(x, y, 0), Blocks.AIR.defaultBlockState());
                    }
                }
                if (name.equals("pumps")) {
                    placePump(level, new BlockPos(-2, -60, 0), player, "fuel_light", 8_000, 375);
                    placePump(level, ModBlocks.DIESEL_PUMP.get(), new BlockPos(0, -60, 0), player, "fuel_dense", 4_000, 1450);
                    level.setBlockAndUpdate(new BlockPos(2, -60, 0), ModBlocks.CAR_WORKBENCH.get().defaultBlockState()
                            .setValue(CarWorkbenchBlock.FACING, Direction.NORTH));
                    return;
                }
                for (CarType type : CarType.values()) {
                    if (type.id().equals(name)) {
                        spawned(level, type, 0.5, 0.5, 0.0F, 0.5);
                    }
                }
                for (TrailerType type : TrailerType.values()) {
                    if (type.id().equals(name)) {
                        trailer(level, type, 0.5, 0.5, 0.0F);
                    }
                }
            }));
            // Sizes: how far back to stand, and the middle of the thing, in blocks.
            double length = 3.0;
            double height = 1.6;
            for (CarType type : CarType.values()) {
                if (type.id().equals(name)) {
                    length = type.length;
                    height = type.height;
                }
            }
            for (TrailerType type : TrailerType.values()) {
                if (type.id().equals(name)) {
                    length = type.length;
                    height = type.height * 0.8;
                }
            }
            double far = Math.max(3.0, length * 0.8);
            double cx = name.equals("pumps") ? 0.5 : 0.5;
            double cy = -60.0 + height * 0.45;
            double cz = name.equals("pumps") ? 0.5 : 0.5;
            // Facing south (+z): its left is east (+x).
            at(t + 10, mc -> aim(mc, cx, cy, cz, far * 0.62, far * 0.38, far * 0.78));
            at(t + 45, mc -> shot(mc, "gallery_" + name + "_1front"));
            at(t + 50, mc -> aim(mc, cx, cy, cz, -far * 0.62, far * 0.36, -far * 0.78));
            at(t + 80, mc -> shot(mc, "gallery_" + name + "_2rear"));
            at(t + 85, mc -> aim(mc, cx, cy, cz, far * 1.02, far * 0.12, 0.0));
            at(t + 110, mc -> shot(mc, "gallery_" + name + "_3side"));
            at(t + 115, mc -> aim(mc, cx, cy, cz, far * 0.18, far * 0.9, far * 0.2));
            at(t + 145, mc -> shot(mc, "gallery_" + name + "_4above"));
        }
        at(60 + names.size() * GALLERY_SPAN + 10, mc -> mc.stop());
    }

    // ---- wear and tear and the Repair Bench: ./gradlew runClient -Pshowcase=repair ----

    private static final BlockPos REPAIR = new BlockPos(2, -60, 3);
    private static final CarCondition WORN = new CarCondition(0.18F, 0.22F, 0.12F, 0.3F, 0.45F);

    /** Clicks the open screen at a point in its own coordinates (from its top left corner). */
    private static void click(Minecraft minecraft, int x, int y) {
        if (minecraft.screen instanceof RepairBenchScreen screen) {
            screen.mouseClicked(screen.getGuiLeft() + x, screen.getGuiTop() + y, 0);
        }
    }

    private static void hoverAt(Minecraft minecraft, int x, int y) {
        if (minecraft.screen instanceof RepairBenchScreen screen) {
            hover(minecraft, screen.getGuiLeft() + x, screen.getGuiTop() + y);
        }
    }

    private static RepairBenchBlockEntity repairBench(ServerPlayer player) {
        return (RepairBenchBlockEntity) player.serverLevel().getBlockEntity(REPAIR);
    }

    private static void repairScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -6; x <= 14; x++) {
                    for (int z = -2; z <= 22; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                level.setBlockAndUpdate(REPAIR, ModBlocks.REPAIR_BENCH.get().defaultBlockState().setValue(RepairBenchBlock.FACING, Direction.SOUTH));
                RepairBenchBlockEntity bench = repairBench(player);
                java.util.UUID mo = java.util.UUID.nameUUIDFromBytes("mechanic".getBytes());
                EconomyService.INSTANCE.ensureAccount(level.getServer(), mo, "Mo");
                bench.setOwner(mo, "Mo");
                long[] prices = {1200, 3500, 1800, 2200, 2500};
                for (Part part : Part.values()) {
                    bench.setPrice(part, prices[part.ordinal()]);
                }
                bench.stock().insertItem(0, new ItemStack(ModItems.TIRE.get(), 12), false);
                bench.stock().insertItem(1, new ItemStack(ModItems.MOTOR_OIL.get(), 6), false);
                bench.stock().insertItem(2, new ItemStack(ModItems.ENGINE_PARTS.get(), 8), false);
                bench.stock().insertItem(3, new ItemStack(ModItems.BRAKE_PADS.get(), 5), false);
                bench.stock().insertItem(4, new ItemStack(ModItems.BODY_PANEL.get(), 9), false);
                spawn(level, CarType.SEDAN, 5.2, 3.5, 180, 0.6);
                CarEntity car = car(level, CarType.SEDAN);
                car.setOwner(player);
                car.setCondition(WORN);
                player.getInventory().clearContent();
                give(player, ModItems.ENGINE_PARTS.get(), 2);
                give(player, ModItems.MOTOR_OIL.get(), 1);
                Purse.give(player, 20_000);
            });
        });
        at(70, mc -> {
            mc.options.hideGui = true;
            look(mc, 3.9, -59.6, 5.9, 150.0F, 32.0F);
        });
        at(120, mc -> shot(mc, "40_repair_bench"));
        at(122, mc -> mc.options.hideGui = false);
        at(125, mc -> server(mc, player -> RepairBenchMenu.open(player, repairBench(player))));
        at(160, mc -> shot(mc, "41_repair_screen"));
        // Pick tires and brakes from the bench's stock, and see what the tires cost.
        at(165, mc -> click(mc, 140, 30 + 2 * 18 + 6));
        at(167, mc -> click(mc, 140, 30 + 3 * 18 + 6));
        at(170, mc -> hoverAt(mc, 150, 30 + 2 * 18 + 6));
        at(195, mc -> shot(mc, "42_repair_bill"));
        // Fit your own oil, then pay cash for the rest.
        at(200, mc -> click(mc, 106, 30 + 6));
        at(215, mc -> hoverAt(mc, 4, 4));
        at(220, mc -> click(mc, 140, 30 + 5 * 18 + 3 + 12 + 12 + 20 + 6));
        at(250, mc -> shot(mc, "43_repair_paid"));
        at(255, mc -> mc.player.closeContainer());
        // The owner's tabs.
        at(260, mc -> server(mc, player -> {
            repairBench(player).setOwner(player);
            RepairBenchMenu.open(player, repairBench(player));
        }));
        at(275, mc -> click(mc, 95, 16 + 6));
        at(300, mc -> shot(mc, "44_repair_stock"));
        at(305, mc -> click(mc, 155, 16 + 6));
        at(330, mc -> shot(mc, "45_repair_prices"));
        at(335, mc -> mc.player.closeContainer());
        // Driving a tired car: warning lights on the dashboard, smoke from under the bonnet.
        at(340, mc -> server(mc, player -> {
            CarEntity car = car(player.serverLevel(), CarType.SEDAN);
            car.setCondition(new CarCondition(0.2F, 0.15F, 0.08F, 0.6F, 0.3F));
            car.moveTo(5.5, -60.0, 6.0, 0.0F, 0.0F);
            player.startRiding(car, true);
        }));
        at(345, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(350, mc -> mc.options.keyUp.setDown(true));
        at(372, mc -> mc.options.keyUp.setDown(false));
        at(395, mc -> shot(mc, "46_worn_car_driving"));
        at(400, mc -> server(mc, player -> car(player.serverLevel(), CarType.SEDAN).setCondition(new CarCondition(0.2F, 0.0F, 0.08F, 0.6F, 0.3F))));
        at(430, mc -> shot(mc, "47_broken_down"));
        // A semi with a worn trailer beside the bench: the arrows pick the trailer, which has no oil or engine.
        at(435, mc -> server(mc, player -> {
            player.stopRiding();
            ServerLevel level = player.serverLevel();
            CarEntity sedan = car(level, CarType.SEDAN);
            sedan.setCondition(CarCondition.NEW);
            sedan.moveTo(-1.5, -60.0, 4.0, 0.0F, 0.0F);
            spawn(level, CarType.SEMI_TRUCK, 5.5, 10.5, 0.0F, 0.7);
            CarEntity semi = car(level, CarType.SEMI_TRUCK);
            semi.setOwner(player);
            net.minecraft.world.phys.Vec3 fifthWheel = semi.fifthWheel();
            TrailerEntity trailer = ModEntities.trailer(TrailerType.BOX_TRAILER).create(level);
            trailer.moveTo(fifthWheel.x, -60.0, fifthWheel.z - com.reiko.cars.trailer.TrailerType.BOX_TRAILER.couplerForward(), 0.0F, 0.0F);
            level.addFreshEntity(trailer);
            trailer.setCondition(new CarCondition(1.0F, 1.0F, 0.15F, 0.08F, 0.4F));
            trailer.startRiding(semi, true);
            look(mc, 4.5, -59.5, 0.5, 60.0F, 25.0F);
            RepairBenchMenu.open(player, repairBench(player));
        }));
        for (int t = 455; t <= 470; t += 5) {
            at(t, mc -> {
                if (mc.player.containerMenu instanceof RepairBenchMenu menu && !menu.targetName().getString().contains("Trailer")) {
                    click(mc, 180, 33);
                }
            });
        }
        at(490, mc -> shot(mc, "48_repair_trailer"));
        at(495, mc -> mc.player.closeContainer());
        at(500, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.SEMI_TRUCK), true)));
        at(505, mc -> mc.options.keyUp.setDown(true));
        at(530, mc -> mc.options.keyUp.setDown(false));
        at(560, mc -> shot(mc, "49_trailer_dashboard"));
        at(570, mc -> mc.stop());
    }

    // ---- the tow trucks, the mail van and mailboxes: ./gradlew runClient -Pshowcase=tow ----

    private static final BlockPos HOME_BOX = new BlockPos(35, -60, 3);
    private static final BlockPos REIKO_BOX = new BlockPos(35, -60, 21);
    private static final java.util.UUID REIKO = java.util.UUID.nameUUIDFromBytes("reiko".getBytes());

    private static void clickAt(Minecraft minecraft, int x, int y) {
        if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
            screen.mouseClicked(screen.getGuiLeft() + x, screen.getGuiTop() + y, 0);
        }
    }

    private static void hoverOn(Minecraft minecraft, int x, int y) {
        if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
            hover(minecraft, screen.getGuiLeft() + x, screen.getGuiTop() + y);
        }
    }

    private static com.reiko.cars.mail.MailboxBlockEntity mailboxAt(ServerPlayer player, BlockPos pos) {
        return (com.reiko.cars.mail.MailboxBlockEntity) player.serverLevel().getBlockEntity(pos);
    }

    private static CarEntity loaded(ServerLevel level, CarType truckType, CarType carType, double x, double z) {
        spawn(level, truckType, x, z, 0.0F, 0.7);
        CarEntity truck = car(level, truckType);
        net.minecraft.world.phys.Vec3 point = com.reiko.cars.tow.Towing.pickupPoint(truck);
        spawn(level, carType, point.x, point.z - carType.length / 2.0 - 0.3, 0.0F, 0.4);
        car(level, carType).startRiding(truck, true);
        return truck;
    }

    private static void towScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -10; x <= 44; x++) {
                    for (int z = -12; z <= 40; z++) {
                        boolean road = x >= 29 && x <= 33;
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), (road ? Blocks.GRAY_CONCRETE : Blocks.SMOOTH_STONE).defaultBlockState());
                    }
                }
                CarEntity flatbed = loaded(level, CarType.FLATBED, CarType.SEDAN, 0.5, 4.0);
                flatbed.setOwner(player);
                CarEntity wrecker = loaded(level, CarType.WRECKER, CarType.HATCHBACK, 16.5, 4.0);
                wrecker.setOwner(player);
                spawn(level, CarType.MAIL_VAN, 31.5, 3.5, 180.0F, 0.8); // northbound: right-hand drive, the mailboxes on its right
                car(level, CarType.MAIL_VAN).setOwner(player);
                for (BlockPos pos : new BlockPos[] {HOME_BOX, REIKO_BOX}) {
                    level.setBlockAndUpdate(pos, ModBlocks.MAILBOX.get().defaultBlockState()
                            .setValue(com.reiko.cars.mail.MailboxBlock.FACING, Direction.WEST));
                }
                mailboxAt(player, HOME_BOX).setOwner(player);
                level.getServer().getProfileCache().add(new com.mojang.authlib.GameProfile(REIKO, "Reiko"));
                EconomyService.INSTANCE.ensureAccount(level.getServer(), REIKO, "Reiko");
                mailboxAt(player, REIKO_BOX).setOwner(REIKO, "Reiko");
                player.getInventory().clearContent();
                Purse.give(player, 5_000);
            });
        });
        // The flatbed with a sedan up on its deck, and the wrecker with a hatchback on its wheel-lift.
        at(60, mc -> {
            mc.options.hideGui = true;
            aim(mc, 0.5, -59.0, 3.0, 8.5, 3.0, -5.5);
        });
        at(110, mc -> shot(mc, "50_flatbed_loaded"));
        at(112, mc -> aim(mc, 16.5, -59.2, 1.5, 7.5, 2.6, -7.0));
        at(160, mc -> shot(mc, "51_wrecker_towing"));
        at(162, mc -> aim(mc, 16.5, -59.6, 0.5, 9.5, 0.6, 0.0));
        at(205, mc -> shot(mc, "52_wrecker_side"));
        // Towing round a bend: the car swings out behind.
        at(210, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.WRECKER), true)));
        at(214, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        at(220, mc -> mc.options.keyUp.setDown(true));
        at(240, mc -> mc.options.keyLeft.setDown(true));
        at(262, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyDown.setDown(true);
        });
        at(270, mc -> mc.options.keyDown.setDown(false));
        at(280, mc -> {
            mc.options.hideGui = true;
            server(mc, player -> {
                CarEntity wrecker = car(player.serverLevel(), CarType.WRECKER);
                player.stopRiding();
                net.minecraft.world.phys.Vec3 at = wrecker.position();
                player.teleportTo(player.serverLevel(), at.x + 7.0, at.y + 9.0, at.z - 4.0, 60.0F, 52.0F);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
        });
        at(286, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        at(320, mc -> shot(mc, "53_wrecker_cornering"));
        // Posting a parcel to Reiko from your own mailbox.
        at(330, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.teleportTo(player.serverLevel(), 37.5, -60.0, 3.5, 90.0F, 20.0F);
                com.reiko.cars.mail.MailboxMenu.open(player, mailboxAt(player, HOME_BOX));
            });
        });
        at(340, mc -> server(mc, player -> {
            if (player.containerMenu instanceof com.reiko.cars.mail.MailboxMenu menu) {
                menu.packing().setItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
                menu.packing().setItem(1, new ItemStack(net.minecraft.world.item.Items.BREAD, 6));
                menu.packing().setItem(2, new ItemStack(ModItems.TIRE.get(), 2));
                menu.broadcastChanges();
            }
        }));
        at(345, mc -> clickAt(mc, 40, 35));
        at(348, mc -> {
            for (char c : "Reiko".toCharArray()) {
                mc.screen.charTyped(c, 0);
            }
        });
        at(355, mc -> hoverOn(mc, 4, 4));
        at(370, mc -> shot(mc, "54_mailbox_send"));
        at(372, mc -> clickAt(mc, 140, 30 + 70 + 10));
        at(395, mc -> shot(mc, "55_mailbox_posted"));
        at(397, mc -> mc.player.closeContainer());
        // The flag's up: the mail van stops for it, collects the parcel and takes it to Reiko's mailbox.
        at(400, mc -> {
            mc.options.hideGui = true;
            aim(mc, 33.5, -59.5, 3.5, -6.0, 2.6, 7.5);
        });
        at(445, mc -> shot(mc, "56_mail_van_mailbox"));
        at(448, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.MAIL_VAN), true)));
        at(452, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        at(480, mc -> shot(mc, "57_mail_van_hud"));
        at(482, mc -> server(mc, com.reiko.cars.trailer.Hitching::use));
        at(495, mc -> shot(mc, "58_mail_collected"));
        at(498, mc -> server(mc, player -> {
            CarEntity van = car(player.serverLevel(), CarType.MAIL_VAN);
            van.moveTo(31.5, -60.0, 21.5, 180.0F, 0.0F);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket(van));
        }));
        at(515, mc -> server(mc, com.reiko.cars.trailer.Hitching::use));
        at(528, mc -> shot(mc, "59_mail_delivered"));
        // Mail for you, in your inbox.
        at(532, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.teleportTo(player.serverLevel(), 37.5, -60.0, 3.5, 90.0F, 20.0F);
                com.reiko.cars.mail.MailboxBlockEntity box = mailboxAt(player, HOME_BOX);
                box.deliver(com.reiko.cars.mail.ParcelItem.of(new com.reiko.cars.mail.Parcel(player.getUUID(), player.getGameProfile().getName(),
                        REIKO, "Reiko", 200, net.minecraft.world.item.component.ItemContainerContents.fromItems(
                        List.of(new ItemStack(net.minecraft.world.item.Items.EMERALD, 12), new ItemStack(ModItems.MOTOR_OIL.get(), 2))),
                        java.util.Optional.of(net.minecraft.core.GlobalPos.of(player.level().dimension(), HOME_BOX)))));
                com.reiko.cars.mail.MailboxMenu.open(player, box);
            });
        });
        at(545, mc -> clickAt(mc, 8 + 58 + 2 + 35, 16 + 6));
        at(550, mc -> hoverOn(mc, 16 + 8, 50 + 8));
        at(570, mc -> shot(mc, "60_mailbox_inbox"));
        at(575, mc -> mc.stop());
    }

    // ---- car keys: ./gradlew runClient -Pshowcase=keys ----

    private static void keysScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(12600);
                for (int x = -10; x <= 30; x++) {
                    for (int z = -10; z <= 30; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                spawn(level, CarType.SEDAN, 4.5, 6.5, 30.0F, 0.8);
                CarEntity sedan = car(level, CarType.SEDAN);
                sedan.setOwner(player);
                sedan.setColor(0xC62828);
                sedan.setLocked(true);
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(ModItems.BLANK_KEY.get(), 3));
            });
        });
        // Even its owner can't start it without a key.
        at(50, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.SEDAN), true)));
        at(54, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(70, mc -> shot(mc, "61_no_key"));
        // Cut a key: sneak and right-click the car with a blank.
        at(75, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.teleportTo(player.serverLevel(), 7.5, -60.0, 3.0, 50.0F, 22.0F);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            });
        });
        at(90, mc -> server(mc, player -> {
            player.setShiftKeyDown(true);
            car(player.serverLevel(), CarType.SEDAN).interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
        }));
        at(100, mc -> shot(mc, "62_key_cut"));
        // The key, and what it says.
        at(102, mc -> server(mc, player -> player.setGameMode(GameType.SURVIVAL))); // the survival inventory, to see the key
        at(110, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(114, mc -> hoverOn(mc, 8 + 18 + 8, 142 + 8));
        at(125, mc -> shot(mc, "63_key_tooltip"));
        at(127, mc -> {
            mc.player.closeContainer();
            server(mc, player -> player.setGameMode(GameType.CREATIVE));
        });
        // With the key, it starts.
        at(130, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.SEDAN), true)));
        at(134, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(140, mc -> mc.options.keyUp.setDown(true));
        at(156, mc -> mc.options.keyUp.setDown(false));
        at(175, mc -> shot(mc, "64_driving_with_key"));
        // Walk away and lock it with the key: the lights flash.
        at(180, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                CarEntity sedan = car(player.serverLevel(), CarType.SEDAN);
                player.stopRiding();
                sedan.setLocked(false);
                net.minecraft.world.phys.Vec3 at = sedan.position();
                player.teleportTo(player.serverLevel(), at.x + 6.0, at.y, at.z - 7.0, 40.0F, 12.0F);
                player.getInventory().selected = 1;
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(1));
            });
        });
        at(200, mc -> server(mc, player -> com.reiko.cars.key.CarKeys.fob(player, player.getInventory().getItem(1))));
        at(203, mc -> shot(mc, "65_fob_lock_a"));
        at(205, mc -> shot(mc, "65_fob_lock_b"));
        at(212, mc -> shot(mc, "65_fob_lock_c"));
        at(220, mc -> mc.stop());
    }

    // ---- bus fares and the taxi meter: ./gradlew runClient -Pshowcase=fares ----

    /** Another player, logged in on a dummy connection, to drive (as the game tests do). */
    private static ServerPlayer extraPlayer(MinecraftServer server, String name) {
        java.util.UUID id = java.util.UUID.nameUUIDFromBytes(name.getBytes());
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(id, name);
        net.minecraft.server.network.CommonListenerCookie cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, server.overworld(), profile, cookie.clientInformation());
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        try {
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
        } catch (RuntimeException e) {
            // a mod's login packet the dummy connection can't take: the player's in by then
        }
        EconomyService.INSTANCE.ensureAccount(server, id, name);
        return player;
    }

    private static void faresScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(5000);
                for (int x = -10; x <= 30; x++) {
                    for (int z = -10; z <= 70; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.GRAY_CONCRETE.defaultBlockState());
                    }
                }
                spawn(level, CarType.CITY_BUS, 4.5, 8.5, 0.0F, 0.8);
                spawn(level, CarType.TAXI, 16.5, 4.5, 0.0F, 0.8);
                ServerPlayer bea = extraPlayer(level.getServer(), "Bea");
                ServerPlayer tex = extraPlayer(level.getServer(), "Tex");
                CarEntity bus = car(level, CarType.CITY_BUS);
                bus.setOwner(bea);
                bea.teleportTo(level, 4.5, -60.0, 8.5, 0.0F, 0.0F);
                bea.startRiding(bus, true);
                CarEntity taxi = car(level, CarType.TAXI);
                taxi.setOwner(tex);
                tex.teleportTo(level, 16.5, -60.0, 4.5, 0.0F, 0.0F);
                tex.startRiding(taxi, true);
                player.getInventory().clearContent();
                Purse.give(player, 5_000);
                player.setGameMode(GameType.SURVIVAL);
            });
        });
        // Getting on the bus: pay the fare, or get off.
        at(50, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.CITY_BUS), true)));
        at(80, mc -> shot(mc, "66_bus_fare"));
        at(82, mc -> clickAt(mc, 99 + 40, 78 + 10));
        at(100, mc -> shot(mc, "67_bus_paid"));
        // A taxi: the meter runs, and the fare comes out of the bank when you get out.
        at(105, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            server(mc, player -> {
                player.stopRiding();
                player.startRiding(car(player.serverLevel(), CarType.TAXI), true);
            });
        });
        at(130, mc -> shot(mc, "68_taxi_meter"));
        for (int t = 132; t < 160; t++) {
            at(t, mc -> server(mc, player -> {
                CarEntity taxi = car(player.serverLevel(), CarType.TAXI);
                taxi.setPos(taxi.getX(), taxi.getY(), taxi.getZ() + 1.0);
            }));
        }
        at(185, mc -> shot(mc, "69_taxi_meter_running"));
        at(188, mc -> server(mc, ServerPlayer::stopRiding));
        at(200, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            shot(mc, "70_taxi_paid");
        });
        // The owner's fares.
        at(205, mc -> server(mc, player -> {
            CarEntity taxi = car(player.serverLevel(), CarType.TAXI);
            taxi.setOwner(player);
            com.reiko.cars.fare.FareSettingsMenu.open(player, taxi);
        }));
        at(225, mc -> shot(mc, "71_fare_settings"));
        at(230, mc -> mc.stop());
    }

    // ---- the car dealership, buying, trading in and For Sale Signs: ./gradlew runClient -Pshowcase=sales ----

    private static final BlockPos DESK = new BlockPos(4, -60, 4);

    private static com.reiko.cars.sale.DealershipBlockEntity desk(ServerPlayer player) {
        return (com.reiko.cars.sale.DealershipBlockEntity) player.serverLevel().getBlockEntity(DESK);
    }

    private static void salesScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -10; x <= 24; x++) {
                    for (int z = -10; z <= 24; z++) {
                        boolean line = (x - 1) % 4 == 0 && z >= 8 && z <= 13;
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), (line ? Blocks.WHITE_CONCRETE : Blocks.GRAY_CONCRETE).defaultBlockState());
                    }
                }
                level.setBlockAndUpdate(DESK, ModBlocks.DEALERSHIP.get().defaultBlockState()
                        .setValue(com.reiko.cars.sale.DealershipBlock.FACING, Direction.NORTH));
                ServerPlayer dee = extraPlayer(level.getServer(), "Dee");
                EconomyService.INSTANCE.deposit(level.getServer(), dee.getUUID(), 500_000, "showcase");
                com.reiko.cars.sale.DealershipBlockEntity lot = desk(player);
                lot.setOwner(dee);
                lot.setOffer(CarType.SEDAN, 8_000);
                lot.setOffer(CarType.HATCHBACK, 6_000);
                lot.setOffer(CarType.PICKUP_TRUCK, 9_000);
                Object[][] stock = {{CarType.SEDAN, 3.0, 0xC62828, 12_000L}, {CarType.SPORTS_CAR, 7.0, 0x1E1E22, 25_000L},
                        {CarType.PICKUP_TRUCK, 11.0, 0xF4F5F7, 9_500L}};
                for (Object[] each : stock) {
                    CarType type = (CarType) each[0];
                    spawn(level, type, (Double) each[1], 11.0, 160.0F, 0.6);
                    CarEntity car = car(level, type);
                    car.setOwner(dee);
                    car.setColor((Integer) each[2]);
                    car.setSalePrice((Long) each[3]);
                    car.setLocked(true);
                }
                spawn(level, CarType.HATCHBACK, -1.0, 11.0, 160.0F, 0.3);
                CarEntity mine = car(level, CarType.HATCHBACK);
                mine.setOwner(player);
                mine.setCondition(new CarCondition(0.6F, 0.9F, 0.5F, 0.7F, 0.8F));
                player.getInventory().clearContent();
                Purse.give(player, 50_000);
                player.setGameMode(GameType.SURVIVAL);
            });
        });
        // The lot, FOR SALE signs up.
        at(60, mc -> {
            mc.options.hideGui = true;
            aim(mc, 6.0, -58.5, 10.0, -3.0, 2.5, -8.5);
        });
        at(110, mc -> shot(mc, "72_dealer_lot"));
        // A customer at the desk: browse, buy.
        at(114, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.teleportTo(player.serverLevel(), 4.5, -60.0, 1.5, 0.0F, 30.0F);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                com.reiko.cars.sale.DealershipMenu.open(player, desk(player));
            });
        });
        at(135, mc -> shot(mc, "73_dealership_buy"));
        at(137, mc -> clickAt(mc, 97, 82));
        at(155, mc -> shot(mc, "74_buy_screen"));
        at(157, mc -> clickAt(mc, 139, 100));
        at(175, mc -> shot(mc, "75_bought"));
        // Trading in the worn hatchback.
        at(180, mc -> server(mc, player -> com.reiko.cars.sale.DealershipMenu.open(player, desk(player))));
        at(190, mc -> clickAt(mc, 103, 22));
        at(196, mc -> {
            for (int i = 0; i < 3; i++) {
                if (mc.player.containerMenu instanceof com.reiko.cars.sale.DealershipMenu menu && menu.carType() != CarType.HATCHBACK) {
                    clickAt(mc, 180, 33);
                }
            }
        });
        at(215, mc -> shot(mc, "76_trade_in"));
        at(217, mc -> clickAt(mc, 97, 82));
        at(235, mc -> shot(mc, "77_traded"));
        at(237, mc -> mc.player.closeContainer());
        // The dealer's side: the lot and the trade-in offers.
        at(240, mc -> server(mc, player -> {
            desk(player).setOwner(player);
            for (CarEntity car : player.serverLevel().getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(-20, -70, -20, 40, -40, 40),
                    car -> "Dee".equals(car.ownerName()))) {
                car.setOwner(player);
            }
            com.reiko.cars.sale.DealershipMenu.open(player, desk(player));
        }));
        at(260, mc -> shot(mc, "78_dealer_lot_tab"));
        at(262, mc -> clickAt(mc, 103, 22));
        at(280, mc -> shot(mc, "79_dealer_offers"));
        at(282, mc -> mc.player.closeContainer());
        // A For Sale Sign on a car of your own, anywhere.
        at(285, mc -> server(mc, player -> com.reiko.cars.sale.CarSaleMenu.open(player, car(player.serverLevel(), CarType.SEDAN))));
        at(305, mc -> shot(mc, "80_sale_sign"));
        at(310, mc -> mc.stop());
    }

    // ---- parking meters and toll gates ----

    private static final BlockPos[] METERS = {new BlockPos(0, -60, 2), new BlockPos(0, -60, 8), new BlockPos(0, -60, 14)};
    private static final BlockPos GATE = new BlockPos(9, -60, 24);
    private static final BlockPos BOOTH = new BlockPos(10, -60, 22);

    private static com.reiko.cars.parking.ParkingMeterBlockEntity meterAt(ServerPlayer player, int i) {
        return (com.reiko.cars.parking.ParkingMeterBlockEntity) player.serverLevel().getBlockEntity(METERS[i]);
    }

    private static void tollsScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                com.reiko.cars.config.CarsConfig.PARKING_GRACE.set(1); // tickets come quickly for the camera
                for (int x = -10; x <= 24; x++) {
                    for (int z = -10; z <= 34; z++) {
                        net.minecraft.world.level.block.Block ground = x <= 0 ? Blocks.LIGHT_GRAY_CONCRETE
                                : x <= 3 && (z == 5 || z == 11 || z == 17 || z == -1) ? Blocks.WHITE_CONCRETE
                                : x == 4 && Math.floorMod(z, 3) == 0 ? Blocks.WHITE_CONCRETE
                                : x == 10 && z < 21 && Math.floorMod(z, 3) != 2 ? Blocks.YELLOW_CONCRETE
                                : Blocks.GRAY_CONCRETE;
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), ground.defaultBlockState());
                    }
                }
                ServerPlayer town = extraPlayer(level.getServer(), "Town");
                ServerPlayer dee = extraPlayer(level.getServer(), "Dee");
                for (BlockPos pos : METERS) {
                    level.setBlockAndUpdate(pos, ModBlocks.PARKING_METER.get().defaultBlockState()
                            .setValue(com.reiko.cars.parking.ParkingMeterBlock.FACING, Direction.WEST)); // fed from the sidewalk
                    ((com.reiko.cars.parking.ParkingMeterBlockEntity) level.getBlockEntity(pos)).setOwner(town);
                }
                // Dee's paid up at the first meter; the player's hatchback, at the second, isn't.
                spawn(level, CarType.SEDAN, 2.4, 2.5, 0.0F, 0.5);
                CarEntity dees = car(level, CarType.SEDAN);
                dees.setOwner(dee);
                dees.setColor(0xC62828);
                com.reiko.cars.parking.MeterService.pay(town, meterAt(player, 0), 60,
                        com.reiko.cars.parking.MeterService.cost(meterAt(player, 0), 60), com.reiko.economy.shop.Payment.cash());
                spawn(level, CarType.HATCHBACK, 2.4, 8.5, 0.0F, 0.5);
                car(level, CarType.HATCHBACK).setOwner(player);
                EconomyService.INSTANCE.deposit(level.getServer(), player.getUUID(), 20_000, "showcase");
                // The toll plaza down the road: Dee's gate and booth.
                BlockState gate = ModBlocks.TOLL_GATE.get().defaultBlockState().setValue(com.reiko.cars.toll.TollGateBlock.FACING, Direction.NORTH);
                level.setBlockAndUpdate(GATE, gate);
                gate.getBlock().setPlacedBy(level, GATE, gate, dee, ItemStack.EMPTY);
                level.setBlockAndUpdate(BOOTH, ModBlocks.TOLL_BOOTH.get().defaultBlockState()
                        .setValue(com.reiko.cars.toll.TollBoothBlock.FACING, Direction.WEST));
                com.reiko.cars.toll.TollBoothBlockEntity booth = (com.reiko.cars.toll.TollBoothBlockEntity) level.getBlockEntity(BOOTH);
                booth.setOwner(dee);
                booth.setToll(250);
                spawn(level, CarType.SPORTS_CAR, 7.0, 24.0 - 0.7 - CarType.SPORTS_CAR.length / 2.0, 0.0F, 0.6);
                CarEntity mine = car(level, CarType.SPORTS_CAR);
                mine.setOwner(player);
                mine.setColor(0x1E5AA8);
                player.getInventory().clearContent();
                Purse.give(player, 5_000);
            });
        });
        // The street: one meter paid, one EXPIRED, one free.
        at(70, mc -> {
            mc.options.hideGui = true;
            aim(mc, 0.5, -58.8, 8.5, -6.5, 3.0, 0.0);
        });
        at(120, mc -> shot(mc, "81_parking_street"));
        at(122, mc -> aim(mc, 0.5, -58.8, 8.5, -2.0, 0.3, 1.1));
        at(160, mc -> shot(mc, "82_meter_expired"));
        // The ticket in chat, from the sidewalk; then the meter's screen.
        at(162, mc -> {
            mc.options.hideGui = false;
            mc.player.getInventory().selected = 8;
            server(mc, player -> player.getInventory().selected = 8);
            aim(mc, 0.5, -58.8, 8.5, -3.0, 0.8, -1.5);
        });
        at(190, mc -> shot(mc, "83_ticket"));
        at(192, mc -> server(mc, player -> com.reiko.cars.parking.ParkingMeterMenu.open(player, meterAt(player, 1))));
        at(215, mc -> shot(mc, "84_meter_pay"));
        at(217, mc -> clickAt(mc, 140, 130));
        at(240, mc -> shot(mc, "85_meter_paid"));
        at(242, mc -> mc.player.closeContainer());
        // An owner's meter: the Settings tab.
        at(245, mc -> server(mc, player -> {
            meterAt(player, 2).setOwner(player);
            com.reiko.cars.parking.ParkingMeterMenu.open(player, meterAt(player, 2));
        }));
        at(262, mc -> clickAt(mc, 100, 22));
        at(280, mc -> shot(mc, "86_meter_settings"));
        at(282, mc -> mc.player.closeContainer());
        // The toll plaza, the sports car pulled up to the closed gate.
        at(285, mc -> {
            mc.options.hideGui = true;
            aim(mc, 7.5, -59.0, 23.0, 7.0, 3.5, -5.5);
        });
        at(335, mc -> shot(mc, "87_toll_plaza"));
        at(337, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(car(player.serverLevel(), CarType.SPORTS_CAR), true);
            });
        });
        at(365, mc -> shot(mc, "88_toll_hint"));
        at(367, mc -> server(mc, player -> com.reiko.cars.toll.TollService.use(player, car(player.serverLevel(), CarType.SPORTS_CAR))));
        at(390, mc -> shot(mc, "89_toll_pay"));
        at(392, mc -> clickAt(mc, 140, 76));
        at(396, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(420, mc -> shot(mc, "90_toll_paid"));
        at(422, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
            server(mc, player -> player.stopRiding());
        });
        at(426, mc -> aim(mc, 7.0, -58.5, 24.0, 8.5, 3.0, 7.0));
        at(455, mc -> shot(mc, "91_gate_up"));
        // The booth's own screen, for its owner.
        at(457, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                com.reiko.cars.toll.TollBoothBlockEntity booth = (com.reiko.cars.toll.TollBoothBlockEntity) player.serverLevel().getBlockEntity(BOOTH);
                booth.setOwner(player);
                player.teleportTo(player.serverLevel(), 12.5, -60.0, 22.5, 90.0F, 20.0F);
                com.reiko.cars.toll.TollBoothMenu.open(player, booth);
            });
        });
        at(480, mc -> shot(mc, "92_toll_booth"));
        at(482, mc -> mc.player.closeContainer());
        at(485, mc -> server(mc, player -> {
            give(player, ModItems.PARKING_METER.get(), 1);
            give(player, ModItems.TOLL_BOOTH.get(), 1);
            give(player, ModItems.TOLL_GATE.get(), 1);
        }));
        at(505, mc -> shot(mc, "93_items"));
        at(510, mc -> mc.stop());
    }

    // ---- insurance and the job board ----

    private static final BlockPos OFFICE = new BlockPos(2, -60, 6);
    private static final BlockPos CLAIM_BENCH = new BlockPos(11, -60, 7);
    private static final BlockPos JOB_BOARD = new BlockPos(-3, -60, 6);

    private static com.reiko.cars.insurance.InsuranceOfficeBlockEntity office(ServerPlayer player) {
        return (com.reiko.cars.insurance.InsuranceOfficeBlockEntity) player.serverLevel().getBlockEntity(OFFICE);
    }

    private static void insuranceScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (int x = -10; x <= 20; x++) {
                    for (int z = -10; z <= 20; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), (z >= 5 && x >= -6 ? Blocks.SMOOTH_STONE : Blocks.GRASS_BLOCK).defaultBlockState());
                    }
                }
                ServerPlayer ivy = extraPlayer(level.getServer(), "Ivy");
                ServerPlayer mo = extraPlayer(level.getServer(), "Mo");
                EconomyService.INSTANCE.deposit(level.getServer(), ivy.getUUID(), 100_000, "showcase");
                level.setBlockAndUpdate(OFFICE, ModBlocks.INSURANCE_OFFICE.get().defaultBlockState()
                        .setValue(com.reiko.cars.insurance.InsuranceOfficeBlock.FACING, Direction.NORTH));
                office(player).setOwner(ivy);
                level.setBlockAndUpdate(CLAIM_BENCH, ModBlocks.REPAIR_BENCH.get().defaultBlockState().setValue(RepairBenchBlock.FACING, Direction.WEST));
                com.reiko.cars.repair.RepairBenchBlockEntity bench = (com.reiko.cars.repair.RepairBenchBlockEntity) level.getBlockEntity(CLAIM_BENCH);
                bench.setOwner(mo);
                for (CarCondition.Part part : CarCondition.Part.values()) {
                    bench.setPrice(part, 2000);
                }
                bench.stock().insertItem(CarCondition.Part.BODY.ordinal(), new ItemStack(ModItems.BODY_PANEL.get(), 32), false);
                level.setBlockAndUpdate(JOB_BOARD, net.minecraft.core.registries.BuiltInRegistries.BLOCK
                        .get(ResourceLocation.fromNamespaceAndPath("reikoeconomy", "job_board")).defaultBlockState());
                spawn(level, CarType.SEDAN, 7.5, 7.5, 90.0F, 0.6);
                CarEntity mine = car(level, CarType.SEDAN);
                mine.setOwner(player);
                mine.setColor(0xC62828);
                mine.setCondition(new CarCondition(0.9F, 0.9F, 0.8F, 0.8F, 0.35F));
                player.getInventory().clearContent();
                Purse.give(player, 20_000);
                player.getInventory().selected = 8;
            });
        });
        // The office, the car out front.
        at(60, mc -> {
            mc.options.hideGui = true;
            mc.player.getInventory().selected = 8;
            aim(mc, 5.0, -59.4, 6.5, -1.5, 1.8, -4.8);
        });
        at(110, mc -> shot(mc, "94_insurance_office"));
        // Insuring the car.
        at(112, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.teleportTo(player.serverLevel(), 2.5, -60.0, 3.5, 0.0F, 30.0F);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                com.reiko.cars.insurance.InsuranceMenu.open(player, office(player));
            });
        });
        at(135, mc -> shot(mc, "95_insure_screen"));
        at(137, mc -> clickAt(mc, 140, 136));
        at(155, mc -> shot(mc, "96_insured"));
        at(157, mc -> mc.player.closeContainer());
        // The crash damage, paid for at Mo's bench and claimed back.
        at(160, mc -> server(mc, player -> com.reiko.cars.repair.RepairBenchMenu.open(player,
                (com.reiko.cars.repair.RepairBenchBlockEntity) player.serverLevel().getBlockEntity(CLAIM_BENCH))));
        at(178, mc -> clickAt(mc, 158, 110));
        at(180, mc -> hoverOn(mc, -60, 10));
        at(195, mc -> shot(mc, "97_bench_insured"));
        at(197, mc -> clickAt(mc, 140, 177));
        at(215, mc -> shot(mc, "98_claim_paid"));
        at(217, mc -> mc.player.closeContainer());
        at(222, mc -> shot(mc, "99_claim_chat"));
        // The insurer's side.
        at(225, mc -> server(mc, player -> {
            office(player).setOwner(player);
            com.reiko.cars.insurance.InsuranceMenu.open(player, office(player));
        }));
        at(245, mc -> shot(mc, "100_insurer_settings"));
        at(247, mc -> mc.player.closeContainer());
        // The job board, with the car jobs.
        at(250, mc -> server(mc, player -> {
            player.setGameMode(GameType.SURVIVAL);
            for (String job : new String[] {"driver", "mechanic", "trucker"}) {
                com.reiko.economy.jobs.JobService.join(player, ReikoCars.id(job));
            }
            BlockState board = player.serverLevel().getBlockState(JOB_BOARD);
            board.useWithoutItem(player.serverLevel(), player, new net.minecraft.world.phys.BlockHitResult(JOB_BOARD.getCenter(), Direction.SOUTH, JOB_BOARD, false));
        }));
        at(268, mc -> {
            if (mc.screen != null) {
                for (int i = 0; i < 4; i++) {
                    mc.screen.mouseScrolled(0, 0, 0, -1);
                }
            }
        });
        at(272, mc -> hoverOn(mc, 70, 18 + 2 * 26 + 12));
        at(290, mc -> shot(mc, "101_car_jobs"));
        at(292, mc -> clickAt(mc, 120, 10));
        at(296, mc -> hoverOn(mc, 300, 300));
        at(312, mc -> shot(mc, "102_car_contracts"));
        at(314, mc -> mc.player.closeContainer());
        at(316, mc -> server(mc, player -> give(player, ModItems.INSURANCE_OFFICE.get(), 1)));
        at(335, mc -> shot(mc, "103_office_item"));
        at(340, mc -> mc.stop());
    }

    // ---- the work vehicles: ./gradlew runClient -Pshowcase=work ----

    private static void tile(ServerLevel level, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                level.setBlockAndUpdate(new BlockPos(x, -61, z), ((x + z) & 1) == 0
                        ? Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
    }

    private static void workScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                tile(level, -14, -10, 30, 26);
                spawn(level, CarType.POLICE_CAR, 0.5, 0.5, 0.0F, 0.7);
                spawn(level, CarType.AMBULANCE, 5.5, 0.5, 0.0F, 0.7);
                car(level, CarType.POLICE_CAR).setSiren(true);
                car(level, CarType.AMBULANCE).setSiren(true);
                spawn(level, CarType.DUMP_TRUCK, 16.5, 0.5, 0.0F, 0.7);
                CarEntity dump = car(level, CarType.DUMP_TRUCK);
                for (int i = 0; i < 6; i++) {
                    dump.bed().addItem(new ItemStack(i % 2 == 0 ? Blocks.DIRT : Blocks.GRAVEL, 64));
                }
                spawn(level, CarType.GARBAGE_TRUCK, 0.5, 16.5, 0.0F, 0.7);
                for (BlockPos can : new BlockPos[] {new BlockPos(2, -60, 12), new BlockPos(-2, -60, 12)}) {
                    level.setBlockAndUpdate(can, ModBlocks.TRASH_CAN.get().defaultBlockState()
                            .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.WEST));
                    ((com.reiko.cars.garbage.TrashCanBlockEntity) level.getBlockEntity(can)).setItem(0, new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH, 12));
                }
                ServerPlayer ivy = extraPlayer(level.getServer(), "Ivy");
                spawn(level, CarType.ICE_CREAM_TRUCK, 10.5, 16.5, 0.0F, 0.7);
                CarEntity truck = car(level, CarType.ICE_CREAM_TRUCK);
                truck.setOwner(ivy);
                truck.bed().addItem(new ItemStack(ModItems.VANILLA_ICE_CREAM.get(), 16));
                truck.bed().addItem(new ItemStack(ModItems.CHOCOLATE_ICE_CREAM.get(), 12));
                truck.bed().addItem(new ItemStack(ModItems.STRAWBERRY_ICE_CREAM.get(), 9));
                truck.shopPrices().put(ModItems.VANILLA_ICE_CREAM.get(), 200L);
                truck.shopPrices().put(ModItems.CHOCOLATE_ICE_CREAM.get(), 250L);
                truck.shopPrices().put(ModItems.STRAWBERRY_ICE_CREAM.get(), 225L);
                spawn(level, CarType.CEMENT_MIXER, 22.5, 16.5, 0.0F, 0.7);
                CarEntity mixer = car(level, CarType.CEMENT_MIXER);
                mixer.bed().addItem(new ItemStack(Blocks.LIGHT_GRAY_CONCRETE_POWDER, 24));
                mixer.bed().addItem(new ItemStack(Blocks.YELLOW_CONCRETE_POWDER, 16));
                player.getInventory().clearContent();
                Purse.give(player, 20_000);
            });
        });
        // Lights and sirens, by day and by night.
        at(60, mc -> aim(mc, 3.0, -59.2, 0.5, 3.4, 1.5, 6.0));
        at(100, mc -> shot(mc, "104_lights_a"));
        at(104, mc -> shot(mc, "105_lights_b"));
        at(106, mc -> server(mc, player -> player.serverLevel().setDayTime(18000)));
        at(130, mc -> shot(mc, "106_lights_night_a"));
        at(134, mc -> shot(mc, "107_lights_night_b"));
        at(136, mc -> server(mc, player -> player.serverLevel().setDayTime(6000)));
        // The dump truck tips its load.
        at(140, mc -> {
            aim(mc, 16.5, -58.5, 0.0, 9.5, 2.5, -3.0);
            server(mc, player -> car(player.serverLevel(), CarType.DUMP_TRUCK).setTipStart(player.serverLevel().getGameTime()));
        });
        at(140 + 30, mc -> shot(mc, "108_dump_tipping"));
        at(140 + 62, mc -> shot(mc, "109_dump_up"));
        at(140 + 64, mc -> aim(mc, 16.5, -59.0, -2.5, 4.5, 2.6, -5.5));
        at(140 + 100, mc -> shot(mc, "110_dump_load"));
        // The garbage truck at the cans.
        at(260, mc -> aim(mc, 0.5, -59.2, 12.5, -4.5, 2.6, -4.5));
        at(290, mc -> shot(mc, "111_garbage_cans"));
        at(292, mc -> server(mc, player -> {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.startRiding(car(player.serverLevel(), CarType.GARBAGE_TRUCK), true);
        }));
        at(296, mc -> mc.options.hideGui = false);
        at(320, mc -> shot(mc, "112_garbage_hud"));
        at(322, mc -> server(mc, com.reiko.cars.trailer.Hitching::use));
        at(330, mc -> shot(mc, "113_garbage_collected"));
        // The police car's siren key.
        at(335, mc -> server(mc, player -> {
            player.stopRiding();
            player.startRiding(car(player.serverLevel(), CarType.POLICE_CAR), true);
        }));
        at(360, mc -> shot(mc, "114_police_hud"));
        // The ice cream truck's shop.
        at(365, mc -> server(mc, player -> {
            player.stopRiding();
            player.teleportTo(player.serverLevel(), 13.0, -60.0, 16.5, 90.0F, 10.0F);
            com.reiko.cars.icecream.IceCreamMenu.open(player, car(player.serverLevel(), CarType.ICE_CREAM_TRUCK));
        }));
        at(385, mc -> shot(mc, "115_ice_cream_shop"));
        at(387, mc -> clickAt(mc, 180, 27));
        at(395, mc -> clickAt(mc, 180, 27));
        at(405, mc -> clickAt(mc, 140, 116));
        at(412, mc -> hoverOn(mc, 300, 300));
        at(425, mc -> shot(mc, "116_ice_cream_bought"));
        at(427, mc -> mc.player.closeContainer());
        at(430, mc -> server(mc, player -> {
            car(player.serverLevel(), CarType.ICE_CREAM_TRUCK).setOwner(player);
            com.reiko.cars.icecream.IceCreamMenu.open(player, car(player.serverLevel(), CarType.ICE_CREAM_TRUCK));
        }));
        at(450, mc -> shot(mc, "117_ice_cream_owner"));
        at(452, mc -> mc.player.closeContainer());
        at(455, mc -> {
            mc.options.hideGui = true;
            aim(mc, 10.5, -58.8, 16.5, 5.0, 1.2, 3.0);
        });
        at(480, mc -> shot(mc, "118_ice_cream_hatch"));
        // The mixer, with someone driving it: the drum turns and the powder sets; then it pours.
        at(482, mc -> server(mc, player -> {
            ServerPlayer mo = extraPlayer(player.server, "Mo");
            CarEntity mixer = car(player.serverLevel(), CarType.CEMENT_MIXER);
            mo.teleportTo(player.serverLevel(), mixer.getX(), mixer.getY(), mixer.getZ(), 0.0F, 0.0F);
            mo.startRiding(mixer, true);
        }));
        at(485, mc -> aim(mc, 22.5, -58.5, 17.5, 8.0, 2.0, 1.0));
        at(520, mc -> shot(mc, "119_mixer_turning_a"));
        at(527, mc -> shot(mc, "120_mixer_turning_b"));
        at(530, mc -> server(mc, player -> {
            CarEntity mixer = car(player.serverLevel(), CarType.CEMENT_MIXER);
            ReikoCars.LOGGER.info("Showcase mixer drum: {} light gray concrete, {} powder left", mixer.bed().countItem(Blocks.LIGHT_GRAY_CONCRETE.asItem()),
                    mixer.bed().countItem(Blocks.LIGHT_GRAY_CONCRETE_POWDER.asItem()) + mixer.bed().countItem(Blocks.YELLOW_CONCRETE_POWDER.asItem()));
            com.reiko.cars.car.WorkVehicles.pour((ServerPlayer) mixer.getControllingPassenger(), mixer);
        }));
        at(532, mc -> aim(mc, 22.5, -59.0, 12.0, 6.0, 3.0, -6.0));
        at(560, mc -> shot(mc, "121_mixer_poured"));
        // The new items.
        at(565, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.getInventory().clearContent();
                give(player, ModItems.VANILLA_ICE_CREAM.get(), 3);
                give(player, ModItems.CHOCOLATE_ICE_CREAM.get(), 3);
                give(player, ModItems.STRAWBERRY_ICE_CREAM.get(), 3);
                give(player, ModItems.TRASH_CAN.get(), 1);
                for (CarType type : new CarType[] {CarType.POLICE_CAR, CarType.AMBULANCE, CarType.GARBAGE_TRUCK, CarType.ICE_CREAM_TRUCK, CarType.DUMP_TRUCK}) {
                    give(player, ModItems.car(type), 1);
                }
            });
        });
        at(585, mc -> shot(mc, "122_items"));
        at(590, mc -> mc.stop());
    }

    // ---- turn signals, hazards and the alarm: ./gradlew runClient -Pshowcase=signals ----

    /** The back of the semi's trailer, along z. */
    private static double trailerZ(Minecraft minecraft) {
        TrailerEntity trailer = minecraft.level.getEntitiesOfClass(TrailerEntity.class, new net.minecraft.world.phys.AABB(10, -70, -30, 30, -50, 20)).getFirst();
        return trailer.getZ() - trailer.trailerType().length / 2.0;
    }

    private static void signalsScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                tile(level, -16, -24, 40, 34);
                // Two rows with their hazards on, facing south.
                CarType[][] rows = {{CarType.SEDAN, CarType.MUSCLE_CAR, CarType.PICKUP_TRUCK}, {CarType.DELIVERY_VAN, CarType.CRUISER, CarType.SCOOTER}};
                for (int row = 0; row < rows.length; row++) {
                    for (int i = 0; i < rows[row].length; i++) {
                        spawn(level, rows[row][i], 0.5 + i * 4.0, 0.5 + row * 12.0, 0.0F, 0.6);
                        car(level, rows[row][i]).setSignal(com.reiko.cars.car.CarSignals.HAZARDS);
                    }
                }
                // A semi and trailer signalling right, and a hatchback signalling left.
                spawn(level, CarType.SEMI_TRUCK, 20.5, 6.5, 0.0F, 0.6);
                CarEntity semi = car(level, CarType.SEMI_TRUCK);
                behind(level, TrailerType.BOX_TRAILER, semi, 0.0F).startRiding(semi, true);
                semi.setSignal(com.reiko.cars.car.CarSignals.RIGHT);
                spawn(level, CarType.HATCHBACK, 30.5, 14.5, 0.0F, 0.6);
                car(level, CarType.HATCHBACK).setSignal(com.reiko.cars.car.CarSignals.LEFT);
                // A sports car for the alarm.
                spawn(level, CarType.SPORTS_CAR, 4.5, 24.5, 0.0F, 0.6);
                player.getInventory().clearContent();
            });
        });
        at(60, mc -> aim(mc, 4.5, -59.3, 0.5, 1.0, 1.6, 6.0));
        at(100, mc -> shot(mc, "123_hazards_front_a"));
        at(108, mc -> shot(mc, "124_hazards_front_b"));
        at(110, mc -> aim(mc, 4.5, -59.3, 0.5, -1.0, 1.6, -6.0));
        at(140, mc -> shot(mc, "125_hazards_rear_a"));
        at(148, mc -> shot(mc, "126_hazards_rear_b"));
        at(150, mc -> aim(mc, 4.5, -59.4, 12.5, -0.6, 1.3, -4.8));
        at(180, mc -> shot(mc, "127_hazards_bikes_a"));
        at(188, mc -> shot(mc, "128_hazards_bikes_b"));
        at(190, mc -> aim(mc, 20.5, -59.0, trailerZ(mc), -2.5, 1.8, -5.5));
        at(220, mc -> shot(mc, "129_semi_right_a"));
        at(228, mc -> shot(mc, "130_semi_right_b"));
        at(230, mc -> aim(mc, 30.5, -59.4, 14.5, 2.5, 1.4, -4.0));
        at(260, mc -> shot(mc, "131_hatchback_left_a"));
        at(268, mc -> shot(mc, "132_hatchback_left_b"));
        at(270, mc -> server(mc, player -> player.serverLevel().setDayTime(18000)));
        at(295, mc -> shot(mc, "133_hatchback_night_a"));
        at(303, mc -> shot(mc, "134_hatchback_night_b"));
        at(305, mc -> aim(mc, 4.5, -59.3, 0.5, -1.0, 1.6, -6.0));
        at(335, mc -> shot(mc, "135_hazards_night_a"));
        at(343, mc -> shot(mc, "136_hazards_night_b"));
        at(345, mc -> server(mc, player -> player.serverLevel().setDayTime(6000)));
        // Driving with the left signal on: the dashboard arrow.
        at(350, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(car(player.serverLevel(), CarType.HATCHBACK), true);
            });
        });
        at(375, mc -> shot(mc, "137_dash_left_a"));
        at(383, mc -> shot(mc, "138_dash_left_b"));
        // The alarm: the player's locked sports car, and Mo trying its door.
        at(386, mc -> server(mc, player -> {
            player.stopRiding();
            ServerPlayer mo = extraPlayer(player.server, "Mo");
            CarEntity sports = car(player.serverLevel(), CarType.SPORTS_CAR);
            sports.setOwner(player);
            sports.setLocked(true);
            mo.teleportTo(player.serverLevel(), sports.getX() + 2.0, sports.getY(), sports.getZ(), 0.0F, 0.0F);
            sports.interact(mo, net.minecraft.world.InteractionHand.MAIN_HAND);
        }));
        at(390, mc -> aim(mc, 4.5, -59.4, 24.5, 2.5, 1.5, 3.5));
        at(415, mc -> shot(mc, "139_alarm_a"));
        at(423, mc -> shot(mc, "140_alarm_b"));
        at(425, mc -> server(mc, player -> car(player.serverLevel(), CarType.SPORTS_CAR).toggleLock(player)));
        at(440, mc -> shot(mc, "141_alarm_off"));
        at(445, mc -> mc.stop());
    }

    // ---- roads, traffic lights, signs and lamps: ./gradlew runClient -Pshowcase=roads ----

    private static void paint(ServerLevel level, int x, int z, com.reiko.cars.road.RoadMarking marking, Direction facing) {
        level.setBlockAndUpdate(new BlockPos(x, -61, z), ModBlocks.ASPHALT.get().defaultBlockState()
                .setValue(com.reiko.cars.road.Asphalt.MARKING, marking).setValue(com.reiko.cars.road.Asphalt.FACING, facing));
    }

    private static void post(ServerLevel level, int x, int z, int poles, BlockState top) {
        for (int y = 0; y < poles; y++) {
            level.setBlockAndUpdate(new BlockPos(x, -60 + y, z), ModBlocks.STREET_POLE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(new BlockPos(x, -60 + poles, z), top);
    }

    /** A crossing of two five-wide roads at the origin, with lights, signs and lamps. */
    private static void crossing(ServerLevel level, ServerPlayer owner) {
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = -60; y <= -50; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        com.reiko.cars.road.RoadMarking none = com.reiko.cars.road.RoadMarking.NONE;
        for (int d = -24; d <= 24; d++) {
            for (int w = -2; w <= 2; w++) {
                paint(level, w, d, none, Direction.NORTH);
                paint(level, d, w, none, Direction.NORTH);
            }
            if (Math.abs(d) > 4) {
                // the north-south road: double yellow down the middle, edge lines
                paint(level, 0, d, com.reiko.cars.road.RoadMarking.CENTER_SOLID, Direction.NORTH);
                paint(level, -2, d, com.reiko.cars.road.RoadMarking.EDGE, Direction.NORTH);
                paint(level, 2, d, com.reiko.cars.road.RoadMarking.EDGE, Direction.SOUTH);
                // the east-west road: a dashed center line
                paint(level, d, 0, com.reiko.cars.road.RoadMarking.CENTER_DASHED, Direction.EAST);
                paint(level, d, -2, com.reiko.cars.road.RoadMarking.EDGE, Direction.EAST);
                paint(level, d, 2, com.reiko.cars.road.RoadMarking.EDGE, Direction.WEST);
            }
        }
        for (int w = -2; w <= 2; w++) {
            // crosswalks on all four sides
            paint(level, w, 3, com.reiko.cars.road.RoadMarking.CROSSWALK, Direction.EAST);
            paint(level, w, -3, com.reiko.cars.road.RoadMarking.CROSSWALK, Direction.EAST);
            paint(level, 3, w, com.reiko.cars.road.RoadMarking.CROSSWALK, Direction.NORTH);
            paint(level, -3, w, com.reiko.cars.road.RoadMarking.CROSSWALK, Direction.NORTH);
        }
        for (int lane : new int[] {1, 2}) {
            paint(level, lane, 4, com.reiko.cars.road.RoadMarking.STOP_LINE, Direction.NORTH);
            paint(level, -lane, -4, com.reiko.cars.road.RoadMarking.STOP_LINE, Direction.SOUTH);
            paint(level, -4, lane, com.reiko.cars.road.RoadMarking.STOP_LINE, Direction.EAST);
            paint(level, 4, -lane, com.reiko.cars.road.RoadMarking.STOP_LINE, Direction.WEST);
        }
        paint(level, 1, 7, com.reiko.cars.road.RoadMarking.ARROW_LEFT, Direction.NORTH);
        paint(level, 2, 7, com.reiko.cars.road.RoadMarking.ARROW_STRAIGHT, Direction.NORTH);
        paint(level, 0, 4, com.reiko.cars.road.RoadMarking.CENTER_SOLID, Direction.NORTH);
        // lights on the near right corners, facing the traffic coming up to them
        BlockState light = ModBlocks.TRAFFIC_LIGHT.get().defaultBlockState();
        post(level, 3, 4, 2, light.setValue(com.reiko.cars.traffic.TrafficLightBlock.FACING, Direction.SOUTH));
        post(level, -3, -4, 2, light.setValue(com.reiko.cars.traffic.TrafficLightBlock.FACING, Direction.NORTH));
        post(level, -4, 3, 2, light.setValue(com.reiko.cars.traffic.TrafficLightBlock.FACING, Direction.WEST));
        post(level, 4, -3, 2, light.setValue(com.reiko.cars.traffic.TrafficLightBlock.FACING, Direction.EAST));
        for (BlockPos p : new BlockPos[] {new BlockPos(3, -58, 4), new BlockPos(-3, -58, -4), new BlockPos(-4, -58, 3), new BlockPos(4, -58, -3)}) {
            if (level.getBlockEntity(p) instanceof com.reiko.cars.traffic.TrafficLightBlockEntity be) {
                be.setOwner(owner);
            }
        }
        // street lamps down the north-south road, their arms over it
        BlockState lamp = ModBlocks.STREET_LAMP.get().defaultBlockState();
        for (int z : new int[] {-16, -8, 10, 18}) {
            post(level, 4, z, 3, lamp.setValue(com.reiko.cars.road.StreetLampBlock.FACING, Direction.WEST));
            post(level, -4, z + 4, 3, lamp.setValue(com.reiko.cars.road.StreetLampBlock.FACING, Direction.EAST));
        }
        // a speed limit sign for northbound traffic, and a stop sign on a side road
        post(level, 3, 13, 1, ModBlocks.SPEED_SIGN.get().defaultBlockState().setValue(com.reiko.cars.traffic.SpeedSignBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(new BlockPos(3, -59, 13)) instanceof com.reiko.cars.traffic.SpeedSignBlockEntity sign) {
            sign.setOwner(owner);
            sign.setLimit(30, false);
        }
        for (int x = 2; x <= 16; x++) {
            for (int w = 9; w <= 11; w++) {
                paint(level, x, w, none, Direction.NORTH);
            }
        }
        post(level, 5, 12, 1, ModBlocks.STOP_SIGN.get().defaultBlockState().setValue(com.reiko.cars.road.StopSignBlock.FACING, Direction.EAST));
        paint(level, 5, 10, com.reiko.cars.road.RoadMarking.STOP_LINE, Direction.WEST);
        paint(level, 5, 11, com.reiko.cars.road.RoadMarking.STOP_LINE, Direction.WEST);
    }

    private static void roadsScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                crossing(level, player);
                spawn(level, CarType.SEDAN, 1.5, 7.0, 180.0F, 0.6); // northbound, waiting at the light
                spawn(level, CarType.CITY_BUS, -10.0, 1.5, -90.0F, 0.6); // eastbound
                spawn(level, CarType.TAXI, -1.5, -12.0, 0.0F, 0.6); // southbound
                player.getInventory().clearContent();
            });
        });
        at(60, mc -> look(mc, 14.0, -46.0, 20.0, 145.0F, 38.0F));
        at(100, mc -> shot(mc, "142_crossing_a"));
        // Half a cycle later the other road has the green.
        at(104, mc -> aim(mc, 3.5, -57.5, 4.5, 2.0, 0.6, 3.5));
        at(130, mc -> {
            shot(mc, "143_light_a");
            ReikoCars.LOGGER.info("Showcase light: {} at {} (clock {})", mc.level.getBlockState(new BlockPos(3, -58, 4)), mc.level.getGameTime(),
                    com.reiko.cars.traffic.TrafficLightBlock.signal(mc.level, mc.level.getBlockState(new BlockPos(3, -58, 4))));
        });
        at(130 + 300, mc -> {
            shot(mc, "144_light_b");
            ReikoCars.LOGGER.info("Showcase light: {} at {} (clock {})", mc.level.getBlockState(new BlockPos(3, -58, 4)), mc.level.getGameTime(),
                    com.reiko.cars.traffic.TrafficLightBlock.signal(mc.level, mc.level.getBlockState(new BlockPos(3, -58, 4))));
        });
        at(130 + 304, mc -> look(mc, 14.0, -46.0, 20.0, 145.0F, 38.0F));
        at(130 + 330, mc -> shot(mc, "145_crossing_b"));
        // Signs, close up.
        at(545, mc -> aim(mc, 3.5, -58.6, 13.5, 0.4, 0.3, 3.2));
        at(570, mc -> shot(mc, "146_speed_sign"));
        at(572, mc -> aim(mc, 5.5, -58.6, 12.5, 2.8, 0.3, 0.6));
        at(595, mc -> shot(mc, "147_stop_sign"));
        at(597, mc -> aim(mc, 2.0, -59.6, 7.0, 0.0, 6.0, 3.0));
        at(620, mc -> shot(mc, "148_markings"));
        // Night: the street lamps come on.
        at(622, mc -> server(mc, player -> player.serverLevel().setDayTime(18000)));
        at(624, mc -> look(mc, 8.0, -54.0, 26.0, 160.0F, 15.0F));
        at(675, mc -> shot(mc, "149_night"));
        at(677, mc -> server(mc, player -> player.serverLevel().setDayTime(6000)));
        // A red-light camera: Mo drives the player's taxi past a red light owned by Ivy.
        at(680, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                ServerPlayer ivy = extraPlayer(player.server, "Ivy");
                ServerPlayer mo = extraPlayer(player.server, "Mo");
                BlockPos lightPos = new BlockPos(-3, -58, -4);
                level.setBlockAndUpdate(lightPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                if (level.getBlockEntity(lightPos) instanceof com.reiko.cars.traffic.TrafficLightBlockEntity be) {
                    be.setOwner(ivy);
                }
                CarEntity taxi = car(level, CarType.TAXI);
                taxi.setOwner(player);
                mo.teleportTo(level, taxi.getX(), taxi.getY(), taxi.getZ(), 0.0F, 0.0F);
                mo.startRiding(taxi, true);
                net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(-1.5, -60.0, -6.0);
                net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(-1.5, -60.0, -3.0);
                taxi.moveTo(to.x, to.y, to.z, 0.0F, 0.0F);
                com.reiko.cars.traffic.TrafficCameras.check(taxi, from, to, 0.6);
            });
            aim(mc, -1.5, -59.0, -5.0, 3.0, 2.0, -5.0);
        });
        at(706, mc -> shot(mc, "150_red_light_ticket"));
        // The owner's screen for the speed sign.
        at(708, mc -> server(mc, player -> {
            player.teleportTo(player.serverLevel(), 3.5, -60.0, 16.0, 180.0F, 0.0F);
            if (player.serverLevel().getBlockEntity(new BlockPos(3, -59, 13)) instanceof com.reiko.cars.traffic.SpeedSignBlockEntity sign) {
                com.reiko.cars.traffic.RoadSignMenu.open(player, sign);
            }
        }));
        at(730, mc -> shot(mc, "151_speed_sign_screen"));
        at(732, mc -> mc.player.closeContainer());
        at(735, mc -> server(mc, player -> {
            player.getInventory().clearContent();
            give(player, ModItems.ASPHALT.get(), 64);
            give(player, ModItems.ASPHALT_SLAB.get(), 64);
            give(player, ModItems.ASPHALT_STAIRS.get(), 64);
            give(player, ModItems.ROAD_PAINT.get(), 1);
            give(player, ModItems.STREET_POLE.get(), 16);
            give(player, ModItems.TRAFFIC_LIGHT.get(), 4);
            give(player, ModItems.STOP_SIGN.get(), 4);
            give(player, ModItems.SPEED_SIGN.get(), 4);
            give(player, ModItems.STREET_LAMP.get(), 8);
        }));
        at(755, mc -> shot(mc, "152_items"));
        at(760, mc -> mc.stop());
    }

    // ---- the car radio: ./gradlew runClient -Pshowcase=radio ----

    private static void radioButton(Minecraft minecraft, int button) {
        if (minecraft.player.containerMenu instanceof com.reiko.cars.radio.RadioMenu menu) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private static void radioScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                tile(level, -10, -10, 10, 10);
                spawn(level, CarType.CONVERTIBLE, 0.5, 0.5, 0.0F, 0.6);
                spawn(level, CarType.SEDAN, 8.5, 0.5, 0.0F, 0.6);
                player.getInventory().clearContent();
                give(player, net.minecraft.world.item.Items.MUSIC_DISC_CAT, 1);
                give(player, net.minecraft.world.item.Items.MUSIC_DISC_PIGSTEP, 1);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(car(level, CarType.CONVERTIBLE), true);
            });
        });
        at(50, mc -> server(mc, player -> com.reiko.cars.radio.RadioMenu.open(player, car(player.serverLevel(), CarType.CONVERTIBLE))));
        at(70, mc -> shot(mc, "153_radio_off"));
        at(72, mc -> {
            radioButton(mc, com.reiko.cars.radio.CarRadio.BUTTON_SOURCE);
            ReikoCars.LOGGER.info("Showcase radio tuned to Neon FM at game time {}: {}", mc.level.getGameTime(),
                    com.reiko.cars.radio.RadioStation.NEON.at(mc.level.getGameTime()));
        });
        at(76, mc -> { for (int i = 0; i < 3; i++) radioButton(mc, com.reiko.cars.radio.CarRadio.BUTTON_LOUDER); });
        at(100, mc -> shot(mc, "154_radio_neon"));
        at(500, mc -> radioButton(mc, com.reiko.cars.radio.CarRadio.BUTTON_SOURCE + 1));
        at(330, mc -> shot(mc, "155_radio_chill"));
        // a disc: shift-click Cat into the slot, then DISC
        at(332, mc -> {
            if (mc.player.containerMenu instanceof com.reiko.cars.radio.RadioMenu menu) {
                for (int i = 1; i < menu.slots.size(); i++) {
                    if (menu.getSlot(i).getItem().is(net.minecraft.world.item.Items.MUSIC_DISC_CAT)) {
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
                    }
                }
            }
        });
        at(336, mc -> radioButton(mc, com.reiko.cars.radio.CarRadio.BUTTON_SOURCE + com.reiko.cars.radio.CarRadio.DISC));
        at(360, mc -> shot(mc, "156_radio_disc"));
        at(362, mc -> radioButton(mc, com.reiko.cars.radio.CarRadio.BUTTON_SOURCE + 2));
        at(365, mc -> mc.player.closeContainer());
        at(400, mc -> shot(mc, "157_radio_now_playing"));
        // Getting out: with nobody aboard, the radio stops.
        at(600, mc -> server(mc, ServerPlayer::stopRiding));
        at(660, mc -> mc.stop());
    }

    // ---- the dashboard at different GUI scales: ./gradlew runClient -Pshowcase=hud ----

    private static void guiScale(Minecraft minecraft, int scale) {
        minecraft.options.guiScale().set(scale);
        minecraft.resizeDisplay();
    }

    private static void hudScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                tile(level, -12, -20, 12, 20);
                spawn(level, CarType.SEMI_TRUCK, 0.5, 0.5, 0.0F, 0.6);
                CarEntity semi = car(level, CarType.SEMI_TRUCK);
                behind(level, TrailerType.BOX_TRAILER, semi, 0.0F).startRiding(semi, true);
                player.getInventory().clearContent();
                give(player, ModItems.car(CarType.SEDAN), 1);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(semi, true);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 6000));
            });
        });
        at(60, mc -> server(mc, player -> player.displayClientMessage(net.minecraft.network.chat.Component.literal("A long action bar message, as a car sends"), true)));
        at(80, mc -> shot(mc, "158_hud_wide"));
        // with the attack indicator beside the hotbar, there's no room: the dashboard goes to the top right
        at(82, mc -> mc.options.attackIndicator().set(net.minecraft.client.AttackIndicatorStatus.HOTBAR));
        at(84, mc -> server(mc, player -> player.displayClientMessage(net.minecraft.network.chat.Component.literal("A long action bar message, as a car sends"), true)));
        at(100, mc -> shot(mc, "159_hud_narrow"));
        at(102, mc -> mc.options.attackIndicator().set(net.minecraft.client.AttackIndicatorStatus.CROSSHAIR));
        // every vehicle's icon, in a survival inventory
        at(104, mc -> server(mc, player -> {
            player.stopRiding();
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            for (CarType type : CarType.values()) {
                give(player, ModItems.car(type), 1);
            }
            for (TrailerType type : TrailerType.values()) {
                give(player, ModItems.trailer(type), 1);
            }
        }));
        at(115, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(118, mc -> hoverOn(mc, 2, 2)); // the mouse out of the way, so no tooltip covers the icons
        at(125, mc -> shot(mc, "160_icons"));
        at(130, mc -> mc.stop());
    }

    /** A rig backing up towards a wall: it should stop when the trailer's back reaches it. */
    private static void wallScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                tile(level, -12, -24, 12, 20);
                spawn(level, CarType.SEMI_TRUCK, 0.5, 0.5, 0.0F, 0.6);
                CarEntity semi = car(level, CarType.SEMI_TRUCK);
                TrailerEntity trailer = behind(level, TrailerType.BOX_TRAILER, semi, 0.0F);
                trailer.startRiding(semi, true);
                // a wall three blocks high, two and a half blocks behind the trailer
                net.minecraft.world.phys.Vec3 wall = trailer.rear().add(trailer.forward().scale(-2.5));
                BlockPos base = BlockPos.containing(wall.x, trailer.getY(), wall.z);
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = 0; dy < 3; dy++) {
                        level.setBlockAndUpdate(base.offset(dx, dy, 0), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
                ReikoCars.LOGGER.info("Showcase wall at z {}; trailer rear at z {}", base.getZ(), trailer.rear().z);
                player.getInventory().clearContent();
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(semi, true);
            });
        });
        at(50, mc -> mc.options.keyDown.setDown(true));
        for (int t = 70; t <= 250; t += 30) {
            at(t, mc -> server(mc, player -> {
                CarEntity semi = car(player.serverLevel(), CarType.SEMI_TRUCK);
                TrailerEntity trailer = semi.trailer();
                ReikoCars.LOGGER.info("Showcase wall: semi z {}, trailer rear z {}, speed {}, trailer blocked {}", semi.getZ(),
                        trailer == null ? Double.NaN : trailer.rear().z, semi.getDeltaMovement().length(),
                        trailer != null && trailer.blockedAt(new TrailerEntity.Pose(trailer.position(), trailer.getYRot())));
            }));
        }
        at(252, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        at(262, mc -> shot(mc, "161_trailer_wall"));
        at(264, mc -> mc.options.keyDown.setDown(false));
        at(270, mc -> mc.stop());
    }

    /** Water seen through a car's windows: from the driver's seat, from behind, and from outside. */
    private static void waterScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                tile(level, -14, -16, 14, 26);
                for (int x = -9; x <= 9; x++) {
                    for (int z = 5; z <= 24; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.WATER.defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(x, -62, z), Blocks.WATER.defaultBlockState());
                    }
                }
                spawn(level, CarType.SEDAN, 0.5, 0.5, 0.0F, 0.6);
                spawn(level, CarType.CITY_BUS, -6.5, 1.5, 90.0F, 0.6);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(car(level, CarType.SEDAN), true);
            });
        });
        at(40, mc -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(8.0F);
        });
        at(95, mc -> shot(mc, "162_water_driver"));
        at(97, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(110, mc -> shot(mc, "163_water_behind"));
        at(112, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> player.stopRiding());
        });
        // outside, looking through the bus's side windows at the pool beyond it
        at(116, mc -> look(mc, -6.0, -60.0, -5.0, 0.0F, 6.0F));
        at(135, mc -> shot(mc, "164_water_outside"));
        // rain, from the driver's seat
        at(137, mc -> server(mc, player -> {
            player.serverLevel().setWeatherParameters(0, 6000, true, false);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.startRiding(car(player.serverLevel(), CarType.SEDAN), true);
        }));
        at(180, mc -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(8.0F);
        });
        at(190, mc -> shot(mc, "165_rain_driver"));
        // (for Fabulous! graphics, run it with graphicsMode:2 in run/options.txt)
        at(195, mc -> ReikoCars.LOGGER.info("Showcase graphics: {}", mc.options.graphicsMode().get()));
        at(200, mc -> mc.stop());
    }

    private static long glowCount(ServerLevel level) {
        long count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(-12, -62, -14, 12, -55, 90)) {
            if (level.getBlockState(pos).getBlock() instanceof com.reiko.cars.car.HeadlightGlowBlock) {
                count++;
            }
        }
        return count;
    }

    /** A night drive down a road between walls, a police car's beacons by the roadside, then the same by day. */
    private static void headlightsScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(18000);
                tile(level, -12, -14, 12, 90);
                for (int z = 4; z <= 80; z += 5) {
                    for (int y = -60; y <= -58; y++) {
                        level.setBlockAndUpdate(new BlockPos(-6, y, z), Blocks.STONE_BRICKS.defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(6, y, z), Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
                for (int x = -5; x <= 5; x++) {
                    for (int y = -60; y <= -57; y++) {
                        level.setBlockAndUpdate(new BlockPos(x, y, 88), Blocks.BRICKS.defaultBlockState());
                    }
                }
                spawn(level, CarType.SEDAN, 0.5, 0.5, 0.0F, 0.6);
                spawn(level, CarType.POLICE_CAR, -3.5, 40.5, 0.0F, 0.6);
                car(level, CarType.POLICE_CAR).setSiren(true);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(car(level, CarType.SEDAN), true);
            });
        });
        // first with real headlights off in the settings, to compare
        at(21, mc -> com.reiko.cars.config.CarsConfig.REAL_HEADLIGHTS.set(false));
        at(40, mc -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(10.0F);
        });
        at(60, mc -> shot(mc, "169_off_driver"));
        at(62, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(76, mc -> shot(mc, "169_off_behind"));
        at(78, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            com.reiko.cars.config.CarsConfig.REAL_HEADLIGHTS.set(true);
        });
        at(100, mc -> server(mc, player -> ReikoCars.LOGGER.info("Showcase headlights: {} lights at night", glowCount(player.serverLevel()))));
        at(102, mc -> shot(mc, "170_headlights_driver"));
        at(104, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(118, mc -> shot(mc, "171_headlights_behind"));
        at(120, mc -> mc.options.keyUp.setDown(true));
        at(170, mc -> shot(mc, "172_headlights_driving"));
        at(172, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(true);
        });
        at(210, mc -> {
            mc.options.keyJump.setDown(false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        at(220, mc -> shot(mc, "173_headlights_driver_later"));
        // the police car's beacons, from the roadside
        at(222, mc -> server(mc, player -> player.stopRiding()));
        at(226, mc -> look(mc, 3.5, -59.0, 33.0, 40.0F, 18.0F));
        at(245, mc -> shot(mc, "174_beacons"));
        at(247, mc -> server(mc, player -> player.serverLevel().setDayTime(6000)));
        at(275, mc -> server(mc, player -> ReikoCars.LOGGER.info("Showcase headlights: {} lights by day", glowCount(player.serverLevel()))));
        at(280, mc -> mc.stop());
    }

    private static com.reiko.cars.charge.ChargingStationBlockEntity station(ServerLevel level) {
        return (com.reiko.cars.charge.ChargingStationBlockEntity) level.getBlockEntity(new BlockPos(3, -60, 1));
    }

    /** A charging station by a city EV low on charge: the station, its screen, a paid charge, the battery on the dashboard. */
    private static void evScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                tile(level, -12, -12, 12, 20);
                BlockState lower = ModBlocks.CHARGING_STATION.get().defaultBlockState()
                        .setValue(com.reiko.cars.charge.ChargingStationBlock.FACING, Direction.WEST);
                level.setBlockAndUpdate(new BlockPos(3, -60, 1), lower);
                level.setBlockAndUpdate(new BlockPos(3, -59, 1), lower.setValue(com.reiko.cars.charge.ChargingStationBlock.HALF, DoubleBlockHalf.UPPER));
                var station = station(level);
                station.setPrice(5);
                station.addMj(350_000);
                spawn(level, CarType.CITY_EV, 0.5, 1.0, 0.0F, 0.15);
                spawn(level, CarType.ELECTRIC_SEDAN, -4.0, 1.5, 0.0F, 0.6);
                spawn(level, CarType.ELECTRIC_MOPED, 6.5, 2.5, 180.0F, 0.6);
                player.getInventory().clearContent();
                com.reiko.economy.bank.Purse.give(player, 5000);
            });
        });
        at(40, mc -> look(mc, -3.5, -59.0, -6.0, -25.0F, 15.0F));
        at(60, mc -> shot(mc, "175_charger"));
        at(62, mc -> look(mc, 0.5, -60.0, -1.2, -90.0F, 12.0F));
        at(75, mc -> shot(mc, "176_charger_display"));
        at(77, mc -> server(mc, player -> com.reiko.cars.charge.ChargerMenu.open(player, station(player.serverLevel()))));
        at(90, mc -> shot(mc, "177_charger_screen"));
        at(92, mc -> server(mc, player -> {
            var station = station(player.serverLevel());
            CarEntity ev = car(player.serverLevel(), CarType.CITY_EV);
            var result = com.reiko.cars.charge.ChargerService.charge(player, station, ev, 40_000, 5, com.reiko.economy.shop.Payment.cash());
            ReikoCars.LOGGER.info("Showcase charge: {} {}", result.success(), result.message().getString());
            com.reiko.economy.network.ShopFeedback.send(player, result);
            if (player.containerMenu instanceof com.reiko.cars.charge.ChargerMenu menu) {
                menu.refresh();
            }
        }));
        at(105, mc -> shot(mc, "178_charged"));
        at(107, mc -> {
            mc.setScreen(null);
            mc.options.hideGui = false;
            server(mc, player -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(car(player.serverLevel(), CarType.CITY_EV), true);
            });
        });
        at(125, mc -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(15.0F);
        });
        at(135, mc -> shot(mc, "179_ev_dashboard"));
        at(140, mc -> mc.stop());
    }

    // ---- the fire truck: ./gradlew runClient -Pshowcase=fire ----

    private static final BlockPos FIRE_PILE = new BlockPos(9, -60, 0);
    private static final BlockPos HYDRANT = new BlockPos(-4, -60, 4);

    private static void fireScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false, level.getServer());
                tile(level, -16, -16, 16, 22);
                // a brick building behind where the truck parks, fires on its roof
                for (int x = -4; x <= 4; x++) {
                    for (int z = 12; z <= 18; z++) {
                        for (int y = -60; y <= -52; y++) {
                            boolean wall = x == -4 || x == 4 || z == 12 || z == 18;
                            if (wall) {
                                level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.BRICKS.defaultBlockState());
                            }
                        }
                        level.setBlockAndUpdate(new BlockPos(x, -51, z), Blocks.OAK_PLANKS.defaultBlockState());
                    }
                }
                for (int y = -59; y <= -57; y += 2) {
                    for (int x = -2; x <= 2; x += 2) {
                        level.setBlockAndUpdate(new BlockPos(x, y, 12), Blocks.GLASS.defaultBlockState());
                    }
                }
                for (BlockPos fire : List.of(new BlockPos(-2, -50, 14), new BlockPos(1, -50, 15), new BlockPos(2, -50, 13), new BlockPos(-1, -50, 16))) {
                    level.setBlockAndUpdate(fire, Blocks.FIRE.defaultBlockState());
                }
                // a burning woodpile out front, for the hose
                for (int x = 0; x <= 1; x++) {
                    for (int z = 0; z <= 1; z++) {
                        level.setBlockAndUpdate(FIRE_PILE.offset(x, 0, z), Blocks.OAK_LOG.defaultBlockState());
                        level.setBlockAndUpdate(FIRE_PILE.offset(x, 1, z), Blocks.FIRE.defaultBlockState());
                    }
                }
                level.setBlockAndUpdate(HYDRANT, ModBlocks.FIRE_HYDRANT.get().defaultBlockState()
                        .setValue(com.reiko.cars.fire.FireHydrantBlock.FACING, Direction.EAST));
                CarEntity truck = spawned(level, CarType.FIRE_TRUCK, 0.5, 12.0 - 0.7 - CarType.FIRE_TRUCK.length / 2.0, 180.0F, 0.8);
                truck.setWater(com.reiko.cars.fire.FireTrucks.tankCapacity());
                truck.setSiren(true);
                player.getInventory().clearContent();
            });
        });
        at(40, mc -> look(mc, -9.0, -57.0, -3.5, -40.0F, 12.0F));
        at(55, mc -> shot(mc, "180_fire_truck"));
        at(57, mc -> shot(mc, "181_fire_truck_flash"));
        at(60, mc -> look(mc, 9.5, -57.5, -1.0, 35.0F, 10.0F));
        at(75, mc -> shot(mc, "182_fire_truck_back"));
        // the ladder: up, from the side
        at(78, mc -> {
            look(mc, -21.0, -53.0, 6.0, -90.0F, -12.0F);
            server(mc, player -> {
                CarEntity truck = car(player.serverLevel(), CarType.FIRE_TRUCK);
                BlockPos base = com.reiko.cars.fire.FireLadder.base(truck);
                int room = com.reiko.cars.fire.FireLadder.room(player.serverLevel(), base);
                ReikoCars.LOGGER.info("Showcase ladder: base {} room {}", base, room);
                com.reiko.cars.fire.FireLadder.raise(truck, base, room);
            });
        });
        at(98, mc -> shot(mc, "183_ladder_swinging"));
        at(118, mc -> shot(mc, "184_ladder_extending"));
        at(150, mc -> shot(mc, "185_ladder_up"));
        at(132, mc -> look(mc, -9.0, -57.0, 4.0, -60.0F, -30.0F));
        at(140, mc -> shot(mc, "185b_ladder_close"));
        at(152, mc -> look(mc, -14.0, -56.0, -6.0, -42.0F, -14.0F));
        at(165, mc -> shot(mc, "186_ladder_hero"));
        // climbing it: stand at its foot, facing the wall, and hold jump
        at(168, mc -> server(mc, player -> {
            CarEntity truck = car(player.serverLevel(), CarType.FIRE_TRUCK);
            BlockPos base = com.reiko.cars.fire.FireLadder.base(truck);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.teleportTo(player.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 0.3, 0.0F, -10.0F);
        }));
        at(178, mc -> {
            ReikoCars.LOGGER.info("Showcase climb: start y {} climbable {}", mc.player.getY(), mc.player.onClimbable());
            mc.options.keyJump.setDown(true);
        });
        at(198, mc -> {
            ReikoCars.LOGGER.info("Showcase climb: after 1s y {}", mc.player.getY());
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(true); // hold on
            mc.player.setXRot(35.0F);
        });
        at(206, mc -> shot(mc, "187_climbing"));
        at(208, mc -> {
            mc.options.keyShift.setDown(false);
            mc.options.keyJump.setDown(true);
        });
        at(272, mc -> {
            ReikoCars.LOGGER.info("Showcase climb: after 4s y {}", mc.player.getY());
            mc.options.keyJump.setDown(false);
            mc.player.setYRot(0.0F);
            mc.player.setXRot(10.0F);
            mc.options.keyUp.setDown(true); // step off onto the roof
        });
        for (int t = 274; t < 290; t += 2) {
            at(t, mc -> ReikoCars.LOGGER.info("Showcase step-off: {} up {} forward {} climbable {} hcoll {}", mc.player.position(),
                    mc.options.keyUp.isDown(), mc.player.input.forwardImpulse, mc.player.onClimbable(), mc.player.horizontalCollision));
        }
        at(282, mc -> {
            mc.options.keyUp.setDown(false);
            ReikoCars.LOGGER.info("Showcase climb: on the roof at {} on ground {}", mc.player.position(), mc.player.onGround());
            mc.player.setYRot(180.0F);
            mc.player.setXRot(40.0F);
        });
        at(290, mc -> shot(mc, "188_on_the_roof"));
        // the hose, on the woodpile
        at(293, mc -> server(mc, player -> {
            player.teleportTo(player.serverLevel(), 5.5, -60.0, -5.0, 0.0F, 0.0F);
            player.getInventory().clearContent();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIRE_HOSE.get()));
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                    new net.minecraft.world.phys.Vec3(FIRE_PILE.getX() + 1.0, -58.2, FIRE_PILE.getZ() + 1.0));
        }));
        at(296, mc -> {
            mc.options.hideGui = false;
            mc.options.keyUse.setDown(true);
        });
        at(306, mc -> shot(mc, "189_hose_spray"));
        at(308, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(314, mc -> shot(mc, "190_hose_third_person"));
        at(316, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        at(322, mc -> shot(mc, "191_hose_front"));
        at(324, mc -> {
            mc.options.keyUse.setDown(false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                ReikoCars.LOGGER.info("Showcase hose: pile fires left {}, truck water {}",
                        java.util.stream.Stream.of(FIRE_PILE.above(), FIRE_PILE.offset(1, 1, 0), FIRE_PILE.offset(0, 1, 1), FIRE_PILE.offset(1, 1, 1))
                                .filter(p -> level.getBlockState(p).is(Blocks.FIRE)).count(),
                        car(level, CarType.FIRE_TRUCK).water());
            });
        });
        // the hydrant filling the truck back up
        at(326, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            CarEntity truck = car(level, CarType.FIRE_TRUCK);
            truck.setWater(2000);
            var hydrant = (com.reiko.cars.fire.FireHydrantBlockEntity) level.getBlockEntity(HYDRANT);
            hydrant.tank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 6000), IFluidHandler.FluidAction.EXECUTE);
            player.getInventory().clearContent();
        }));
        at(328, mc -> {
            mc.options.hideGui = true;
            look(mc, -9.5, -58.0, 1.0, -120.0F, 25.0F);
        });
        at(344, mc -> shot(mc, "192_hydrant"));
        at(360, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            var hydrant = (com.reiko.cars.fire.FireHydrantBlockEntity) level.getBlockEntity(HYDRANT);
            ReikoCars.LOGGER.info("Showcase hydrant: truck water {}, hydrant {}", car(level, CarType.FIRE_TRUCK).water(),
                    hydrant.tank().getFluidAmount());
            com.reiko.cars.fire.FireLadder.lower(car(level, CarType.FIRE_TRUCK));
        }));
        // the dashboard
        at(420, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(car(player.serverLevel(), CarType.FIRE_TRUCK), true);
            });
        });
        at(440, mc -> {
            mc.player.setYRot(180.0F);
            mc.player.setXRot(10.0F);
        });
        at(450, mc -> shot(mc, "193_fire_dashboard"));
        // the items
        at(452, mc -> server(mc, player -> {
            player.stopRiding();
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            give(player, ModItems.car(CarType.FIRE_TRUCK), 1);
            give(player, ModItems.FIRE_HOSE.get(), 1);
            give(player, ModItems.FIRE_HYDRANT.get(), 1);
            for (com.reiko.cars.workbench.CarPart part : com.reiko.cars.workbench.CarPart.values()) {
                give(player, ModItems.part(CarType.FIRE_TRUCK, part), 1);
            }
        }));
        at(462, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(465, mc -> hoverOn(mc, 2, 2));
        at(470, mc -> shot(mc, "194_fire_items"));
        at(472, mc -> hoverOn(mc, 16, 150));
        at(478, mc -> shot(mc, "195_fire_truck_tooltip"));
        at(480, mc -> hoverOn(mc, 34, 150));
        at(486, mc -> shot(mc, "196_hose_tooltip"));
        at(490, mc -> mc.stop());
    }

    // ---- car loans, rentals and the impound lot ----

    private static final BlockPos RENT_DESK = new BlockPos(-12, -60, 2);
    private static final BlockPos LOAN_DESK = new BlockPos(6, -60, 2);
    private static final BlockPos POUND = new BlockPos(22, -60, 2);

    private static com.reiko.cars.rental.RentalDeskBlockEntity rentalDesk(ServerPlayer player) {
        return (com.reiko.cars.rental.RentalDeskBlockEntity) player.serverLevel().getBlockEntity(RENT_DESK);
    }

    private static com.reiko.cars.impound.ImpoundOfficeBlockEntity pound(ServerPlayer player) {
        return (com.reiko.cars.impound.ImpoundOfficeBlockEntity) player.serverLevel().getBlockEntity(POUND);
    }

    private static List<CarEntity> carsOf(ServerLevel level, java.util.function.Predicate<CarEntity> which) {
        return level.getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(-60, -70, -60, 80, -40, 80), which);
    }

    private static void standAt(ServerPlayer player, BlockPos desk) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.teleportTo(player.serverLevel(), desk.getX() + 0.5, -60.0, desk.getZ() + 2.5, 180.0F, 30.0F);
    }

    private static void lotsScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                MinecraftServer server = level.getServer();
                level.setDayTime(6000);
                tile(level, -24, -8, 32, 28);
                // the rental agency: a desk and three cars nosed in
                level.setBlockAndUpdate(RENT_DESK, ModBlocks.RENTAL_DESK.get().defaultBlockState()
                        .setValue(com.reiko.cars.rental.RentalDeskBlock.FACING, Direction.SOUTH));
                ServerPlayer rhea = extraPlayer(server, "Rhea");
                rentalDesk(player).setOwner(rhea);
                Object[][] fleet = {{CarType.SEDAN, -16.0, 0x1565C0}, {CarType.HATCHBACK, -12.0, 0xF4F5F7}, {CarType.PICKUP_TRUCK, -7.5, 0x2E7D32}};
                for (Object[] each : fleet) {
                    CarEntity car = spawned(level, (CarType) each[0], (Double) each[1], 9.0, 0.0F, 0.8);
                    car.setOwner(rhea);
                    car.setColor((Integer) each[2]);
                    car.setLocked(true);
                }
                // the dealership, with a sports car on finance
                level.setBlockAndUpdate(LOAN_DESK, ModBlocks.DEALERSHIP.get().defaultBlockState()
                        .setValue(com.reiko.cars.sale.DealershipBlock.FACING, Direction.SOUTH));
                ServerPlayer dee = extraPlayer(server, "Dee");
                EconomyService.INSTANCE.deposit(server, dee.getUUID(), 500_000, "showcase");
                desk2(player).setOwner(dee);
                CarEntity sports = spawned(level, CarType.SPORTS_CAR, 6.0, 9.0, 180.0F, 0.7);
                sports.setOwner(dee);
                sports.setColor(0xC62828);
                sports.setSalePrice(150_000);
                sports.setLocked(true);
                // the impound lot: an office, a fence, and a pickup towed off an expired meter
                level.setBlockAndUpdate(POUND, ModBlocks.IMPOUND_OFFICE.get().defaultBlockState()
                        .setValue(com.reiko.cars.impound.ImpoundOfficeBlock.FACING, Direction.SOUTH));
                ServerPlayer city = extraPlayer(server, "City");
                pound(player).setOwner(city);
                for (int x = 15; x <= 30; x++) {
                    for (int z = 0; z <= 14; z++) {
                        boolean edge = x == 15 || x == 30 || z == 0 || z == 14;
                        boolean gate = z == 14 && x >= 20 && x <= 24;
                        if (edge && !gate) {
                            level.setBlockAndUpdate(new BlockPos(x, -60, z), Blocks.IRON_BARS.defaultBlockState());
                        }
                    }
                }
                ServerPlayer bo = extraPlayer(server, "Bo");
                CarEntity towed = spawned(level, CarType.PICKUP_TRUCK, 26.5, 8.0, 160.0F, 0.4);
                towed.setOwner(bo);
                towed.setColor(0xF9A825);
                com.reiko.cars.impound.ImpoundService.impound(towed, pound(player), com.reiko.cars.impound.Impound.PARKING, 0, null);
                player.getInventory().clearContent();
                Purse.give(player, 500_000);
                EconomyService.INSTANCE.deposit(server, player.getUUID(), 200_000, "showcase");
            });
        });
        // the three lots
        at(50, mc -> look(mc, -11.5, -53.5, 20.0, 180.0F, 24.0F));
        at(70, mc -> shot(mc, "200_rental_lot"));
        at(72, mc -> look(mc, -11.5, -58.4, 5.8, 180.0F, 16.0F));
        at(90, mc -> shot(mc, "201_rental_desk"));
        at(92, mc -> look(mc, 22.5, -52.5, 22.0, 180.0F, 26.0F));
        at(112, mc -> shot(mc, "202_impound_lot"));
        at(114, mc -> look(mc, 22.5, -58.4, 5.8, 180.0F, 16.0F));
        at(132, mc -> shot(mc, "203_impound_office"));
        // buying the sports car on finance
        at(134, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.setGameMode(GameType.SURVIVAL);
                standAt(player, LOAN_DESK.offset(0, 0, 4));
                com.reiko.cars.sale.CarSaleMenu.open(player, car(player.serverLevel(), CarType.SPORTS_CAR));
            });
        });
        at(154, mc -> shot(mc, "204_sale_finance_offer"));
        at(156, mc -> clickAt(mc, 140, 142));
        at(166, mc -> shot(mc, "205_sale_finance"));
        at(168, mc -> clickAt(mc, 142, 120));
        at(186, mc -> shot(mc, "206_financed"));
        // what's owed, at the dealership's Trade tab
        at(188, mc -> server(mc, player -> com.reiko.cars.sale.DealershipMenu.open(player, desk2(player))));
        at(200, mc -> clickAt(mc, 103, 22));
        at(206, mc -> {
            for (int i = 0; i < 3; i++) {
                if (mc.player.containerMenu instanceof com.reiko.cars.sale.DealershipMenu menu && menu.carType() != CarType.SPORTS_CAR) {
                    clickAt(mc, 180, 33);
                }
            }
        });
        at(222, mc -> shot(mc, "207_loan_owed"));
        at(224, mc -> mc.player.closeContainer());
        // three days with nothing in the bank: repossessed, to the impound lot
        at(226, mc -> server(mc, player -> {
            MinecraftServer server = player.server;
            EconomyService.INSTANCE.withdraw(server, player.getUUID(), EconomyService.INSTANCE.getBalance(server, player.getUUID()), "showcase");
            CarEntity sports = car(player.serverLevel(), CarType.SPORTS_CAR);
            java.util.UUID loan = sports.loanId();
            ReikoCars.LOGGER.info("Showcase loan: {} owed {}", loan, com.reiko.cars.finance.LoanService.owed(sports));
            for (int i = 0; i < 3 && loan != null; i++) {
                com.reiko.cars.finance.LoanService.dueNow(server, loan);
                com.reiko.cars.finance.LoanService.tick(server);
            }
        }));
        at(270, mc -> server(mc, player -> carsOf(player.serverLevel(), c -> c.carType() == CarType.SPORTS_CAR)
                .forEach(c -> ReikoCars.LOGGER.info("Showcase repossessed: at {} impounded {}", c.position(), c.isImpounded()))));
        at(272, mc -> {
            mc.options.hideGui = true;
            server(mc, player -> player.setGameMode(GameType.CREATIVE));
            look(mc, 22.5, -52.5, 22.0, 180.0F, 26.0F);
        });
        at(292, mc -> shot(mc, "208_repossessed"));
        at(294, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.setGameMode(GameType.SURVIVAL);
                standAt(player, POUND);
                com.reiko.cars.impound.ImpoundMenu.open(player, pound(player));
            });
        });
        at(314, mc -> shot(mc, "209_impound_customer"));
        at(316, mc -> mc.player.closeContainer());
        // renting a hatchback for two days
        at(318, mc -> server(mc, player -> {
            standAt(player, RENT_DESK);
            com.reiko.cars.rental.RentalMenu.open(player, rentalDesk(player));
        }));
        at(336, mc -> clickAt(mc, 87, 68));
        at(342, mc -> shot(mc, "210_rental_customer"));
        at(344, mc -> clickAt(mc, 142, 132));
        at(362, mc -> shot(mc, "211_rented"));
        at(364, mc -> server(mc, player -> {
            List<CarEntity> mine = carsOf(player.serverLevel(), c -> c.rentedBy(player));
            ReikoCars.LOGGER.info("Showcase rented: {}", mine);
            if (!mine.isEmpty()) {
                player.startRiding(mine.getFirst(), true);
            }
        }));
        at(380, mc -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(10.0F);
        });
        at(392, mc -> shot(mc, "212_rental_hud"));
        // the owners' side
        at(394, mc -> server(mc, player -> {
            player.stopRiding();
            ServerLevel level = player.serverLevel();
            rentalDesk(player).setOwner(player);
            for (CarEntity car : carsOf(level, c -> "Rhea".equals(c.ownerName()))) {
                car.setOwner(player);
            }
            pound(player).setOwner(player);
            standAt(player, POUND);
            com.reiko.cars.impound.ImpoundMenu.open(player, pound(player));
        }));
        at(414, mc -> shot(mc, "213_impound_manager"));
        at(416, mc -> clickAt(mc, 180, 18));
        at(426, mc -> shot(mc, "214_impound_manager_next"));
        at(428, mc -> {
            mc.player.closeContainer();
            server(mc, player -> {
                standAt(player, RENT_DESK);
                com.reiko.cars.rental.RentalMenu.open(player, rentalDesk(player));
            });
        });
        at(448, mc -> shot(mc, "215_rental_manager"));
        at(450, mc -> clickAt(mc, 97, 22));
        at(460, mc -> shot(mc, "216_rental_prices"));
        at(462, mc -> mc.player.closeContainer());
        // the items
        at(464, mc -> server(mc, player -> {
            player.getInventory().clearContent();
            give(player, ModItems.RENTAL_DESK.get(), 1);
            give(player, ModItems.IMPOUND_OFFICE.get(), 1);
        }));
        at(474, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(477, mc -> hoverOn(mc, 16, 150));
        at(484, mc -> shot(mc, "217_rental_desk_item"));
        at(486, mc -> hoverOn(mc, 34, 150));
        at(492, mc -> shot(mc, "218_impound_office_item"));
        at(496, mc -> mc.stop());
    }

    private static com.reiko.cars.sale.DealershipBlockEntity desk2(ServerPlayer player) {
        return (com.reiko.cars.sale.DealershipBlockEntity) player.serverLevel().getBlockEntity(LOAN_DESK);
    }

    // ---- license plates, the DMV and driver's licenses ----

    private static final BlockPos DMV = new BlockPos(0, -60, 0);
    private static final int[] CONE_Z = {10, 22, 34, 46};

    private static com.reiko.cars.dmv.DmvOfficeBlockEntity dmv(ServerPlayer player) {
        return (com.reiko.cars.dmv.DmvOfficeBlockEntity) player.serverLevel().getBlockEntity(DMV);
    }

    private static void type(Minecraft minecraft, String text) {
        if (minecraft.screen != null) {
            for (char c : text.toCharArray()) {
                minecraft.screen.charTyped(c, 0);
            }
        }
    }

    private static void dmvScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                MinecraftServer server = level.getServer();
                level.setDayTime(6000);
                tile(level, -16, -8, 32, 56);
                level.setBlockAndUpdate(DMV, ModBlocks.DMV_OFFICE.get().defaultBlockState()
                        .setValue(com.reiko.cars.dmv.DmvOfficeBlock.FACING, Direction.SOUTH));
                ServerPlayer city = extraPlayer(server, "City");
                dmv(player).setOwner(city);
                // the test route: four cones down the road
                for (int i = 0; i < CONE_Z.length; i++) {
                    BlockPos at = new BlockPos(6, -60, CONE_Z[i]);
                    level.setBlockAndUpdate(at, ModBlocks.TRAFFIC_CONE.get().defaultBlockState());
                    var cone = (com.reiko.cars.road.TrafficConeBlockEntity) level.getBlockEntity(at);
                    cone.setOwner(city);
                    cone.setNumber(i + 1);
                }
                for (int z = 6; z <= 50; z += 4) {
                    level.setBlockAndUpdate(new BlockPos(9, -60, z), ModBlocks.TRAFFIC_CONE.get().defaultBlockState()); // the kerb
                }
                // cars to show their plates
                spawned(level, CarType.SEDAN, 20.0, 4.0, 180.0F, 0.6).setColor(0x1565C0);
                spawned(level, CarType.SPORT_BIKE, 26.0, 4.0, 180.0F, 0.6);
                spawned(level, CarType.PICKUP_TRUCK, 30.0, 4.0, 0.0F, 0.6).setColor(0xF4F5F7);
                CarEntity mine = spawned(level, CarType.HATCHBACK, 6.0, 4.0, 0.0F, 0.9);
                mine.setOwner(player);
                mine.setColor(0xC62828);
                player.getInventory().clearContent();
                Purse.give(player, 50_000);
                EconomyService.INSTANCE.deposit(server, player.getUUID(), 20_000, "showcase");
            });
        });
        // the DMV and its cones, and plates
        at(50, mc -> look(mc, -6.0, -55.5, 14.0, -140.0F, 18.0F));
        at(70, mc -> shot(mc, "230_dmv_and_route"));
        at(72, mc -> look(mc, 0.5, -58.5, 3.6, 180.0F, 15.0F));
        at(90, mc -> shot(mc, "231_dmv_office"));
        at(92, mc -> look(mc, 7.6, -59.6, 11.6, 140.0F, 25.0F));
        at(110, mc -> shot(mc, "232_numbered_cone"));
        at(112, mc -> look(mc, 20.0, -60.0, 8.4, 180.0F, 22.0F));
        at(130, mc -> shot(mc, "233_sedan_plate"));
        at(132, mc -> look(mc, 26.0, -60.0, 7.4, 180.0F, 24.0F));
        at(150, mc -> shot(mc, "234_bike_plate"));
        at(152, mc -> look(mc, 30.0, -60.0, 8.6, 180.0F, 26.0F));
        at(170, mc -> shot(mc, "235_pickup_front_plate"));
        // no license: the DMV's screen, and paying for the test
        at(172, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.setGameMode(GameType.SURVIVAL);
                standAt(player, DMV);
                com.reiko.cars.dmv.DmvMenu.open(player, dmv(player));
            });
        });
        at(192, mc -> shot(mc, "236_dmv_no_license"));
        at(194, mc -> clickAt(mc, 142, 144));
        at(210, mc -> shot(mc, "237_test_started"));
        // the test: into the hatchback, and down the road through the cones
        at(212, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.HATCHBACK), true)));
        at(222, mc -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(12.0F);
            mc.options.keyUp.setDown(true);
        });
        at(250, mc -> shot(mc, "238_driving_test"));
        at(300, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(true);
            ReikoCars.LOGGER.info("Showcase test drive: at {}", mc.player.position());
        });
        at(330, mc -> mc.options.keyDown.setDown(false));
        at(336, mc -> shot(mc, "239_test_passed"));
        at(338, mc -> server(mc, player -> {
            ReikoCars.LOGGER.info("Showcase license: {}", com.reiko.cars.license.LicenseService.statusLine(player.server, player.getUUID()).getString());
            player.stopRiding();
            CarEntity hatch = carsOf(player.serverLevel(), c -> c.carType() == CarType.HATCHBACK).getFirst();
            hatch.teleportTo(6.0, -60.0, 4.0);
            standAt(player, DMV);
            com.reiko.cars.dmv.DmvMenu.open(player, dmv(player));
        }));
        at(358, mc -> shot(mc, "240_dmv_licensed"));
        // a custom plate for the hatchback
        at(360, mc -> clickAt(mc, 97, 22));
        at(366, mc -> clickAt(mc, 100, 70));
        at(368, mc -> type(mc, "REIKO 1"));
        at(376, mc -> shot(mc, "241_custom_plate"));
        at(378, mc -> clickAt(mc, 142, 144));
        at(392, mc -> shot(mc, "242_plate_bought"));
        at(394, mc -> {
            mc.player.closeContainer();
            mc.options.hideGui = true;
            look(mc, 6.0, -60.0, 0.6, 0.0F, 24.0F);
        });
        at(412, mc -> shot(mc, "243_custom_plate_on_car"));
        // the owner's side
        at(414, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                dmv(player).setOwner(player);
                for (int z : CONE_Z) {
                    ((com.reiko.cars.road.TrafficConeBlockEntity) player.serverLevel().getBlockEntity(new BlockPos(6, -60, z))).setOwner(player);
                }
                standAt(player, DMV);
                com.reiko.cars.dmv.DmvMenu.open(player, dmv(player));
            });
        });
        at(430, mc -> clickAt(mc, 157, 22));
        at(440, mc -> shot(mc, "244_dmv_settings"));
        // a police plate check on an unlicensed driver
        at(442, mc -> {
            mc.player.closeContainer();
            server(mc, player -> {
                ServerPlayer bo = extraPlayer(player.server, "Bo");
                EconomyService.INSTANCE.deposit(player.server, bo.getUUID(), 10_000, "showcase");
                CarEntity sedan = car(player.serverLevel(), CarType.SEDAN);
                bo.startRiding(sedan, true);
                player.getInventory().clearContent();
                give(player, ModItems.TICKET_BOOK.get(), 1);
                player.teleportTo(player.serverLevel(), 18.5, -60.0, 6.5, 160.0F, 20.0F);
                com.reiko.cars.license.TicketService.checkCar(player, sedan);
            });
        });
        at(462, mc -> shot(mc, "245_plate_check"));
        // licenses required: no license, no driving
        at(464, mc -> server(mc, player -> {
            com.reiko.cars.license.LicenseData.get(player.server).revoke(player.getUUID());
            com.reiko.cars.config.CarsConfig.LICENSE_REQUIRED.set(true);
            player.startRiding(car(player.serverLevel(), CarType.PICKUP_TRUCK), true);
        }));
        at(480, mc -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(10.0F);
        });
        at(486, mc -> shot(mc, "246_no_license_hud"));
        at(488, mc -> server(mc, player -> {
            com.reiko.cars.config.CarsConfig.LICENSE_REQUIRED.set(false);
            player.stopRiding();
            player.getInventory().clearContent();
            give(player, ModItems.DMV_OFFICE.get(), 1);
            give(player, ModItems.TRAFFIC_CONE.get(), 4);
            give(player, ModItems.TICKET_BOOK.get(), 1);
            var license = com.reiko.cars.license.LicenseService.issue(player, "City");
            com.reiko.cars.license.LicenseService.addPoints(player.server, player.getUUID(), 3, "license.reikocars.reason.red_light");
        }));
        at(498, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(501, mc -> hoverOn(mc, 70, 150));
        at(508, mc -> shot(mc, "247_license_card"));
        at(510, mc -> hoverOn(mc, 52, 150));
        at(516, mc -> shot(mc, "248_ticket_book"));
        at(518, mc -> hoverOn(mc, 16, 150));
        at(524, mc -> shot(mc, "249_dmv_item"));
        at(530, mc -> mc.stop());
    }

    // ---- tuning and racing ----

    private static final BlockPos TUNER = new BlockPos(20, -60, 0);
    private static final BlockPos CONTROL = new BlockPos(0, -60, 0);
    private static final int[][] FLAGS = {{4, 20, 1}, {4, 36, 2}, {-10, 44, 3}, {-14, 20, 4}};

    private static com.reiko.cars.tuning.TuningBenchBlockEntity tuner(ServerPlayer player) {
        return (com.reiko.cars.tuning.TuningBenchBlockEntity) player.serverLevel().getBlockEntity(TUNER);
    }

    private static com.reiko.cars.racing.RaceControlBlockEntity control(ServerPlayer player) {
        return (com.reiko.cars.racing.RaceControlBlockEntity) player.serverLevel().getBlockEntity(CONTROL);
    }

    private static void tuneScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                MinecraftServer server = level.getServer();
                level.setDayTime(6000);
                tile(level, -24, -16, 32, 56);
                level.setBlockAndUpdate(TUNER, ModBlocks.TUNING_BENCH.get().defaultBlockState()
                        .setValue(com.reiko.cars.tuning.TuningBenchBlock.FACING, Direction.SOUTH));
                ServerPlayer mo = extraPlayer(server, "Mo");
                tuner(player).setOwner(mo);
                int slot = 0;
                for (com.reiko.cars.tuning.TuningKit kit : com.reiko.cars.tuning.TuningKit.values()) {
                    tuner(player).stock().insertItem(slot++, new ItemStack(ModItems.kit(kit), 3), false);
                }
                CarEntity sedan = spawned(level, CarType.SEDAN, 20.0, 6.0, -90.0F, 0.8);
                sedan.setOwner(player);
                sedan.setColor(0x1565C0);
                level.setBlockAndUpdate(CONTROL, ModBlocks.RACE_CONTROL.get().defaultBlockState()
                        .setValue(com.reiko.cars.racing.RaceControlBlock.FACING, Direction.SOUTH));
                control(player).setOwner(player);
                for (int[] f : FLAGS) {
                    BlockPos at = new BlockPos(f[0], -60, f[1]);
                    level.setBlockAndUpdate(at, ModBlocks.RACE_FLAG.get().defaultBlockState());
                    var flag = (com.reiko.cars.racing.RaceFlagBlockEntity) level.getBlockEntity(at);
                    flag.setOwner(player);
                    flag.setNumber(f[2]);
                }
                CarEntity racer = spawned(level, CarType.SPORTS_CAR, 3.0, -10.0, 0.0F, 0.9);
                racer.setOwner(player);
                racer.setColor(0xC62828);
                racer.setUpgrades(com.reiko.cars.tuning.Upgrades.STOCK.withEngine(3).withTurbo(true).withTires(com.reiko.cars.tuning.Upgrades.Tires.SPORT));
                racer.setLivery(com.reiko.cars.tuning.Livery.of(27, net.minecraft.world.item.DyeColor.WHITE));
                player.getInventory().clearContent();
                Purse.give(player, 50_000);
                give(player, ModItems.kit(com.reiko.cars.tuning.TuningKit.SPORT_TIRES), 1);
            });
        });
        at(50, mc -> look(mc, 25.0, -56.0, 12.0, 155.0F, 22.0F));
        at(70, mc -> shot(mc, "250_tuning_bench"));
        // the Tuning Bench's screen
        at(72, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.setGameMode(GameType.SURVIVAL);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.teleportTo(player.serverLevel(), 20.5, -60.0, 2.5, 180.0F, 30.0F);
                com.reiko.cars.tuning.TuningBenchMenu.open(player, tuner(player));
            });
        });
        at(90, mc -> clickAt(mc, 160, 44 + 1 * 15 + 7));
        at(92, mc -> clickAt(mc, 160, 44 + 8 * 15 + 7)); // the fuel tank
        at(100, mc -> shot(mc, "251_tune_tab"));
        at(102, mc -> clickAt(mc, 142, 44 + 9 * 15 + 3 + 12 + 12 + 20 + 5));
        at(118, mc -> shot(mc, "252_tuned"));
        at(120, mc -> clickAt(mc, 74, 22));
        at(124, mc -> {
            clickAt(mc, 125, 32 + 30);
            if (mc.screen != null) {
                for (char ch : "42".toCharArray()) {
                    mc.screen.charTyped(ch, 0);
                }
            }
        });
        at(126, mc -> {
            for (int i = 0; i < 12; i++) {
                clickAt(mc, 180, 32 + 52); // through the colours to yellow
            }
        });
        at(132, mc -> shot(mc, "253_livery_tab"));
        at(134, mc -> clickAt(mc, 97, 32 + 80));
        at(146, mc -> shot(mc, "254_livery_applied"));
        at(148, mc -> {
            mc.player.closeContainer();
            mc.options.hideGui = true;
            look(mc, 20.0, -59.0, 11.5, 180.0F, 12.0F);
        });
        at(166, mc -> shot(mc, "255_race_number_side"));
        at(168, mc -> look(mc, 15.0, -52.0, 6.0, -90.0F, 55.0F));
        at(186, mc -> shot(mc, "256_stripes_top"));
        // the race track
        at(188, mc -> look(mc, -2.0, -50.0, -14.0, -10.0F, 28.0F));
        at(206, mc -> shot(mc, "257_race_track"));
        at(208, mc -> look(mc, 1.5, -58.5, 4.5, 175.0F, 18.0F));
        at(224, mc -> shot(mc, "258_race_control"));
        at(226, mc -> look(mc, 5.8, -59.4, 22.4, 150.0F, 18.0F));
        at(242, mc -> shot(mc, "259_race_flag"));
        // a time trial in the tuned sports car
        at(244, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                standAt(player, CONTROL);
                com.reiko.cars.racing.RaceControlMenu.open(player, control(player));
            });
        });
        at(262, mc -> shot(mc, "260_race_control_screen"));
        at(264, mc -> clickAt(mc, 97, 188));
        at(270, mc -> server(mc, player -> player.startRiding(car(player.serverLevel(), CarType.SPORTS_CAR), true)));
        at(280, mc -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(12.0F);
            mc.options.keyUp.setDown(true);
        });
        at(318, mc -> shot(mc, "261_time_trial_hud"));
        at(336, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(true);
        });
        at(360, mc -> mc.options.keyDown.setDown(false));
        at(362, mc -> server(mc, player -> {
            com.reiko.cars.racing.RaceService.endTrial(player.server, player.getUUID());
            player.stopRiding();
            CarEntity racer = carsOf(player.serverLevel(), c -> c.carType() == CarType.SPORTS_CAR).getFirst();
            ReikoCars.LOGGER.info("Showcase trial: car at {}", racer.position());
            racer.teleportTo(3.0, -60.0, -2.0);
            racer.setYRot(0.0F);
            standAt(player, CONTROL);
            com.reiko.cars.racing.RaceControlMenu.open(player, control(player));
        }));
        // a race: the owner enters and starts it alone
        at(378, mc -> clickAt(mc, 142, 140));
        at(390, mc -> shot(mc, "262_race_entered"));
        at(392, mc -> {
            mc.player.closeContainer();
            server(mc, player -> player.startRiding(carsOf(player.serverLevel(), c -> c.carType() == CarType.SPORTS_CAR).getFirst(), true));
        });
        at(400, mc -> server(mc, player -> {
            mc.execute(() -> {
                mc.player.setYRot(0.0F);
                mc.player.setXRot(12.0F);
            });
            ReikoCars.LOGGER.info("Showcase race start: {}", com.reiko.cars.racing.RaceService.start(player, control(player)).message().getString());
        }));
        at(424, mc -> shot(mc, "263_race_countdown"));
        at(462, mc -> mc.options.keyUp.setDown(true));
        at(482, mc -> shot(mc, "264_racing"));
        at(484, mc -> mc.options.keyUp.setDown(false));
        // the kits
        at(486, mc -> server(mc, player -> {
            com.reiko.cars.racing.RaceService.stopAll(player.server);
            player.stopRiding();
            player.getInventory().clearContent();
            for (com.reiko.cars.tuning.TuningKit kit : com.reiko.cars.tuning.TuningKit.values()) {
                give(player, ModItems.kit(kit), 1);
            }
            give(player, ModItems.TUNING_BENCH.get(), 1);
            give(player, ModItems.RACE_CONTROL.get(), 1);
        }));
        at(496, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(499, mc -> hoverOn(mc, 16, 150));
        at(505, mc -> shot(mc, "265_engine_kit"));
        at(507, mc -> hoverOn(mc, 34, 150));
        at(513, mc -> shot(mc, "266_turbo"));
        at(515, mc -> hoverOn(mc, 124, 150));
        at(521, mc -> shot(mc, "267_lift_kit"));
        at(530, mc -> mc.stop());
    }

    // ---- dirt and the car wash: ./gradlew runClient -Pshowcase=wash ----

    private static final BlockPos WASH = new BlockPos(2, -60, -24);
    private static final BlockPos BAY = new BlockPos(12, -60, -24);

    private static com.reiko.cars.wash.CarWashBlockEntity wash(ServerPlayer player) {
        return (com.reiko.cars.wash.CarWashBlockEntity) player.serverLevel().getBlockEntity(WASH);
    }

    private static com.reiko.cars.wash.WashBayBlockEntity bay(ServerPlayer player) {
        return (com.reiko.cars.wash.WashBayBlockEntity) player.serverLevel().getBlockEntity(BAY);
    }

    private static void washScript() {
        at(20, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                MinecraftServer server = level.getServer();
                level.setDayTime(6000);
                tile(level, -8, -40, 34, 14);
                float[] dirt = {0.0F, 0.2F, 0.45F, 0.75F, 1.0F};
                for (int i = 0; i < dirt.length; i++) {
                    CarEntity sedan = spawned(level, CarType.SEDAN, i * 4.0, 0.0, 0.0F, 0.8);
                    sedan.setDirt(dirt[i]);
                }
                spawned(level, CarType.OFF_ROADER, 21.0, 0.0, 0.0F, 0.8).setDirt(1.0F);
                spawned(level, CarType.CITY_EV, 25.0, 0.0, 0.0F, 0.8).setDirt(0.6F);
                CarEntity pickup = spawned(level, CarType.PICKUP_TRUCK, 29.0, 0.0, 0.0F, 0.8);
                pickup.setDirt(0.85F);
                pickup.setColor(0xEDEEF0);
                // Mo's car wash, facing south, and the player's wash bay beside it
                ServerPlayer mo = extraPlayer(server, "Mo");
                BlockState arch = ModBlocks.CAR_WASH.get().defaultBlockState().setValue(com.reiko.cars.wash.CarWashBlock.FACING, Direction.SOUTH);
                level.setBlockAndUpdate(WASH, arch);
                arch.getBlock().setPlacedBy(level, WASH, arch, mo, ItemStack.EMPTY);
                BlockState stand = ModBlocks.WASH_BAY.get().defaultBlockState().setValue(com.reiko.cars.wash.WashBayBlock.FACING, Direction.SOUTH);
                level.setBlockAndUpdate(BAY, stand);
                stand.getBlock().setPlacedBy(level, BAY, stand, player, ItemStack.EMPTY);
                CarEntity muddy = spawned(level, CarType.SEDAN, 5.0, -17.0, 180.0F, 0.8);
                muddy.setDirt(0.95F);
                muddy.setColor(0xC62828);
                muddy.setOwner(player);
                CarEntity truck = spawned(level, CarType.PICKUP_TRUCK, 15.0, -18.5, 90.0F, 0.8);
                truck.setDirt(0.9F);
                truck.setColor(0xEDEEF0);
                Purse.give(player, 50_000);
            });
        });
        at(50, mc -> look(mc, 8.0, -57.5, 9.5, 160.0F, 18.0F));
        at(70, mc -> shot(mc, "270_dirt_row"));
        at(72, mc -> look(mc, 23.0, -57.5, 8.5, 165.0F, 16.0F));
        at(90, mc -> shot(mc, "271_dirt_row_2"));
        at(92, mc -> look(mc, 19.5, -58.6, 4.0, 130.0F, 14.0F));
        at(110, mc -> shot(mc, "272_filthy_close"));
        at(112, mc -> look(mc, 12.5, -56.0, -5.5, 30.0F, 30.0F));
        at(130, mc -> shot(mc, "273_dirt_rear"));
        // the car wash
        at(132, mc -> look(mc, 12.0, -57.0, -11.0, 152.0F, 10.0F));
        at(150, mc -> shot(mc, "274_car_wash"));
        at(152, mc -> look(mc, 4.0, -58.4, -31.5, -20.0F, 6.0F));
        at(168, mc -> shot(mc, "275_car_wash_back"));
        // paying from the driver's seat
        at(170, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.setGameMode(GameType.SURVIVAL);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                CarEntity muddy = car(player.serverLevel(), CarType.SEDAN, false);
                for (CarEntity c : player.serverLevel().getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(-40, -70, -40, 40, -40, -10))) {
                    if (c.carType() == CarType.SEDAN) {
                        muddy = c;
                    }
                }
                player.startRiding(muddy, true);
                com.reiko.cars.wash.CarWashService.offer(player, muddy, wash(player));
            });
        });
        at(186, mc -> shot(mc, "276_wash_pay"));
        at(188, mc -> clickAt(mc, 99 + 40, 76 + 10)); // cash
        at(196, mc -> shot(mc, "277_wash_paid"));
        // driving through, slowly (the camera beside the arch)
        at(198, mc -> {
            mc.options.hideGui = true;
            if (mc.screen != null) {
                mc.player.closeContainer();
            }
            server(mc, player -> {
                player.stopRiding();
                player.setGameMode(GameType.CREATIVE);
            });
        });
        at(200, mc -> look(mc, 8.5, -58.0, -31.5, 28.0F, 10.0F));
        for (int t = 0; t < 70; t++) {
            int step = t;
            at(205 + t, mc -> server(mc, player -> {
                for (CarEntity c : player.serverLevel().getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(0, -70, -40, 10, -40, -10),
                        c -> c.carType() == CarType.SEDAN)) {
                    c.setPos(5.0, -60.0, -18.0 - 0.13 * step);
                }
            }));
        }
        at(232, mc -> shot(mc, "278_washing"));
        at(250, mc -> shot(mc, "279_washing_2"));
        at(276, mc -> shot(mc, "280_washed"));
        // the wash bay: its owner screen, then spraying the pickup
        at(280, mc -> {
            mc.options.hideGui = false;
            server(mc, player -> {
                player.teleportTo(player.serverLevel(), 12.5, -60.0, -21.0, 0.0F, 0.0F);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                com.reiko.cars.wash.WashOwnerMenu.open(player, bay(player), com.reiko.cars.wash.WashOwnerMenu.BAY);
            });
        });
        at(296, mc -> shot(mc, "281_bay_owner"));
        at(298, mc -> {
            mc.player.closeContainer();
            server(mc, player -> {
                player.getInventory().clearContent();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.PRESSURE_WASHER.get()));
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, new net.minecraft.world.phys.Vec3(15.0, -59.0, -18.5));
            });
        });
        at(302, mc -> mc.options.keyUse.setDown(true));
        at(314, mc -> shot(mc, "282_bay_spraying"));
        at(316, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at(324, mc -> shot(mc, "283_bay_third_person"));
        at(326, mc -> {
            mc.options.keyUse.setDown(false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
        });
        at(328, mc -> look(mc, 11.6, -60.0, -21.4, 200.0F, 24.0F));
        at(344, mc -> shot(mc, "284_wash_bay"));
        // the items
        at(346, mc -> server(mc, player -> {
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            give(player, ModItems.CAR_WASH.get(), 1);
            give(player, ModItems.WASH_BAY.get(), 1);
            give(player, ModItems.PRESSURE_WASHER.get(), 1);
            CarEntity dirty = spawned(player.serverLevel(), CarType.HATCHBACK, 30.0, -30.0, 0.0F, 0.6);
            dirty.setDirt(0.8F);
            player.getInventory().add(dirty.toItem());
            dirty.discard();
        }));
        at(350, mc -> {
            mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        });
        at(353, mc -> hoverOn(mc, 2, 2));
        at(358, mc -> shot(mc, "285_wash_items"));
        at(360, mc -> hoverOn(mc, 8 + 3 * 18 + 8, 142 + 8));
        at(366, mc -> shot(mc, "286_dirty_car_tooltip"));
        at(368, mc -> hoverOn(mc, 8 + 2 * 18 + 8, 142 + 8));
        at(374, mc -> shot(mc, "287_lance_tooltip"));
        at(380, mc -> mc.stop());
    }

    // ---- weather: ./gradlew runClient -Pshowcase=weather ----

    private static CarEntity carAt(ServerLevel level, double x, double z) {
        return level.getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(x - 1.5, -70, z - 1.5, x + 1.5, -40, z + 1.5)).getFirst();
    }

    private static void weatherScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                MinecraftServer server = level.getServer();
                level.setDayTime(5000);
                level.setWeatherParameters(0, 12000, true, false);
                tile(level, -10, -20, 44, 20);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                        "fillbiome 18 -64 -20 44 -48 20 minecraft:snowy_plains");
                for (int x = 19; x <= 44; x++) {
                    for (int z = -20; z <= 20; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -60, z), Blocks.SNOW.defaultBlockState());
                    }
                }
                CarEntity sedan = spawned(level, CarType.SEDAN, 0.5, 0.5, 0.0F, 0.9);
                sedan.setColor(0xC62828);
                CarEntity pickup = spawned(level, CarType.PICKUP_TRUCK, 6.5, 0.5, 0.0F, 0.9);
                pickup.setColor(0x1565C0);
                spawned(level, CarType.HATCHBACK, 12.5, 0.5, 0.0F, 0.9).setColor(0xF2F2EE);
                spawned(level, CarType.SEDAN, 28.5, 0.5, 0.0F, 0.9).setSnow(0.9F);
                spawned(level, CarType.CITY_BUS, 35.5, 0.5, 0.0F, 0.9).setSnow(0.6F);
                CarEntity off = spawned(level, CarType.OFF_ROADER, 23.0, -8.0, 0.0F, 0.9);
                off.setSnow(1.0F);
                ServerPlayer mo = extraPlayer(server, "Mo");
                mo.teleportTo(level, 6.5, -60.0, 0.5, 0.0F, 0.0F);
                mo.startRiding(pickup, true);
                player.setGameMode(GameType.SURVIVAL);
                player.startRiding(sedan, true);
            });
        });
        at(70, mc -> shot(mc, "290_rain_driver_view"));
        at(79, mc -> shot(mc, "291_rain_driver_view_2"));
        // from outside: the pickup's wipers going, the hatchback's screen collecting rain
        at(82, mc -> server(mc, player -> {
            player.stopRiding();
            player.setGameMode(GameType.CREATIVE);
        }));
        at(84, mc -> look(mc, 9.5, -58.3, 7.5, 165.0F, 12.0F));
        at(110, mc -> shot(mc, "292_wipers_outside"));
        at(116, mc -> shot(mc, "293_wipers_outside_2"));
        at(118, mc -> look(mc, 12.5, -59.3, 3.6, 180.0F, 14.0F));
        at(140, mc -> shot(mc, "294_drops_parked"));
        // spray off the tires, driving on in the rain
        at(142, mc -> server(mc, player -> {
            player.setGameMode(GameType.SURVIVAL);
            player.startRiding(carAt(player.serverLevel(), 0.5, 0.5), true);
        }));
        at(146, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.keyUp.setDown(true);
            mc.options.hideGui = true;
            mc.player.setXRot(4.0F);
        });
        at(178, mc -> shot(mc, "295_tire_spray"));
        at(180, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.hideGui = false;
        });
        // snow
        at(184, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.setGameMode(GameType.CREATIVE);
            });
        });
        at(186, mc -> look(mc, 31.0, -57.6, 9.0, 160.0F, 16.0F));
        at(206, mc -> shot(mc, "296_snow_on_cars"));
        at(208, mc -> look(mc, 25.5, -58.6, -2.5, 210.0F, 22.0F));
        at(224, mc -> shot(mc, "297_snowed_in"));
        at(226, mc -> server(mc, player -> {
            player.setGameMode(GameType.SURVIVAL);
            player.startRiding(carAt(player.serverLevel(), 28.5, 0.5), true);
        }));
        at(276, mc -> shot(mc, "298_snow_driver_view"));
        at(278, mc -> {
            mc.options.keyUp.setDown(true);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.hideGui = true;
            mc.player.setXRot(4.0F);
        });
        at(300, mc -> shot(mc, "299_snow_spray"));
        at(302, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.hideGui = false;
        });
        // a thunderstorm, and lightning striking the hatchback
        at(306, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.setGameMode(GameType.CREATIVE);
                player.serverLevel().setWeatherParameters(0, 12000, true, true);
            });
        });
        at(308, mc -> look(mc, 12.5, -57.0, 12.0, 180.0F, 10.0F));
        at(340, mc -> server(mc, player -> com.reiko.cars.weather.CarWeather.strike(carAt(player.serverLevel(), 12.5, 0.5))));
        at(342, mc -> shot(mc, "300_lightning"));
        at(350, mc -> server(mc, player -> ReikoCars.LOGGER.info("Showcase weather: hatchback body {}",
                carAt(player.serverLevel(), 12.5, 0.5).condition().body())));
        at(360, mc -> mc.stop());
    }

    // ---- the GPS ----

    /** Places a business block at {@code pos}, owned by {@code owner}, and tells the GPS about it. */
    private static void business(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block, ServerPlayer owner) {
        BlockState state = block.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        block.setPlacedBy(level, pos, level.getBlockState(pos), owner, ItemStack.EMPTY);
        com.reiko.cars.gps.PlacesRegistry.get(level.getServer()).scan(level.getChunkAt(pos));
    }

    @org.jetbrains.annotations.Nullable
    private static com.reiko.cars.network.GpsPlacesPayload.Entry gpsPlace(com.reiko.cars.gps.PlaceKind kind, String owner) {
        return com.reiko.cars.gps.GpsInbox.places().stream().filter(place -> place.kind() == kind && place.owner().equals(owner)).findFirst()
                .orElse(null);
    }

    private static void gpsGoTo(Minecraft minecraft, com.reiko.cars.gps.PlaceKind kind, String owner) {
        com.reiko.cars.network.GpsPlacesPayload.Entry place = gpsPlace(kind, owner);
        if (place == null) {
            ReikoCars.LOGGER.error("Showcase GPS: no {} of {}'s", kind, owner);
            return;
        }
        com.reiko.cars.client.GpsClient.navigate(net.minecraft.network.chat.Component.translatable("gps.reikocars.place_owner", kind.displayName(),
                owner), minecraft.level.dimension(), place.pos().getBottomCenter(), true, null, kind.color, "GPS");
    }

    /** A click on the GPS screen's tab {@code index} of {@code count}. */
    private static void gpsTab(Minecraft minecraft, int index, int count) {
        int left = (minecraft.getWindow().getGuiScaledWidth() - 284) / 2;
        int top = (minecraft.getWindow().getGuiScaledHeight() - 226) / 2;
        int tabWidth = (284 - 20) / count;
        minecraft.screen.mouseClicked(left + 10 + tabWidth * index + tabWidth / 2.0, top + 28, 0);
    }

    private static void gpsScript() {
        boolean xaero = com.reiko.cars.client.GpsClient.XAERO;
        int tabs = xaero ? 4 : 3;
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                MinecraftServer server = level.getServer();
                level.setDayTime(5000);
                ServerPlayer mo = extraPlayer(server, "Mo");
                ServerPlayer sam = extraPlayer(server, "Sam");
                // A road north to Mo's gas station, with parking along it.
                for (int x = 0; x <= 6; x++) {
                    for (int z = -100; z <= 8; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), ModBlocks.ASPHALT.get().defaultBlockState());
                    }
                }
                for (int z = -40; z >= -52; z -= 6) {
                    business(level, new BlockPos(-1, -60, z), ModBlocks.PARKING_METER.get(), player);
                }
                placePump(level, new BlockPos(10, -60, -92), mo, "fuel_light", 9_000, 249);
                placePump(level, new BlockPos(13, -60, -92), mo, "fuel_light", 9_000, 259);
                placePump(level, ModBlocks.DIESEL_PUMP.get(), new BlockPos(16, -60, -92), mo, "fuel_dense", 9_000, 289);
                com.reiko.cars.gps.PlacesRegistry.get(server).scan(level.getChunkAt(new BlockPos(10, -60, -92)));
                business(level, new BlockPos(20, -60, -92), ModBlocks.CHARGING_STATION.get(), mo);
                business(level, new BlockPos(24, -60, -70), ModBlocks.REPAIR_BENCH.get(), mo);
                business(level, new BlockPos(-30, -60, -40), ModBlocks.WASH_BAY.get(), mo);
                business(level, new BlockPos(90, -60, 60), ModBlocks.DEALERSHIP.get(), sam);
                business(level, new BlockPos(-60, -60, 140), ModBlocks.DMV_OFFICE.get(), sam);
                business(level, new BlockPos(150, -60, -20), ModBlocks.TUNING_BENCH.get(), sam);
                // Sam's cheaper station across town, out of diesel.
                placePump(level, new BlockPos(-150, -60, 60), sam, "fuel_light", 9_000, 219);
                placePump(level, ModBlocks.DIESEL_PUMP.get(), new BlockPos(-147, -60, 60), sam, "fuel_dense", 0, 279);
                com.reiko.cars.gps.PlacesRegistry.get(server).scan(level.getChunkAt(new BlockPos(-150, -60, 60)));

                // Your cars: the sedan you're in, and a pickup left on the far side of town.
                CarEntity sedan = spawned(level, CarType.SEDAN, 3.5, 0.5, 180.0F, 0.8);
                sedan.setOwner(player);
                sedan.setColor(0xC62828);
                CarEntity pickup = spawned(level, CarType.PICKUP_TRUCK, -180.5, 120.5, 90.0F, 0.6);
                pickup.setOwner(player);
                pickup.setColor(0x1565C0);
                CarEntity ev = spawned(level, CarType.CITY_EV, 30.5, 30.5, 0.0F, 0.9);
                ev.setOwner(player);
                ev.setColor(0xF2F2EE);
                com.reiko.cars.gps.CarLocator locator = com.reiko.cars.gps.CarLocator.get(server);
                locator.note(sedan);
                locator.note(pickup);
                locator.note(ev);
                player.setGameMode(GameType.SURVIVAL);
                player.startRiding(sedan, true);
            });
        });
        if (xaero) {
            at(30, mc -> GpsShowcaseXaero.addWaypoints(mc));
        }
        at(40, mc -> mc.setScreen(new com.reiko.cars.client.GpsScreen()));
        at(60, mc -> shot(mc, "310_gps_places"));
        at(62, mc -> {
            for (char c : "gas".toCharArray()) {
                mc.screen.charTyped(c, 0);
            }
        });
        at(70, mc -> shot(mc, "311_gps_search"));
        at(72, mc -> {
            for (int i = 0; i < 3; i++) {
                mc.screen.keyPressed(259, 0, 0); // backspace
            }
            gpsTab(mc, 1, tabs);
        });
        at(82, mc -> shot(mc, "312_gps_cars"));
        if (xaero) {
            at(84, mc -> gpsTab(mc, 2, tabs));
            at(94, mc -> shot(mc, "313_gps_waypoints"));
        }
        at(96, mc -> gpsTab(mc, tabs - 1, tabs));
        at(106, mc -> shot(mc, "314_gps_coordinates"));
        at(108, mc -> {
            gpsTab(mc, 0, tabs);
            mc.setScreen(null);
            gpsGoTo(mc, com.reiko.cars.gps.PlaceKind.GAS, "Mo");
        });
        at(140, mc -> shot(mc, "315_gps_driving"));
        at(142, mc -> {
            mc.options.keyUp.setDown(true);
            mc.options.keyLeft.setDown(true);
        });
        at(156, mc -> mc.options.keyLeft.setDown(false));
        at(196, mc -> shot(mc, "316_gps_driving_2"));
        at(198, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        at(226, mc -> shot(mc, "317_gps_third_person"));
        if (xaero) {
            at(228, mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                for (net.minecraft.client.KeyMapping key : mc.options.keyMappings) {
                    if (key.getName().equals("gui.xaero_open_map")) {
                        net.minecraft.client.KeyMapping.click(key.getKey());
                    }
                }
            });
            at(268, mc -> shot(mc, "318_gps_world_map"));
            at(270, mc -> mc.setScreen(null));
        }
        // On foot, back to the pickup.
        at(274, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> player.stopRiding());
            for (com.reiko.cars.network.GpsCarsPayload.Entry car : com.reiko.cars.gps.GpsInbox.cars()) {
                if (car.type() == CarType.PICKUP_TRUCK) {
                    com.reiko.cars.client.GpsClient.navigate(net.minecraft.network.chat.Component.translatable("entity.reikocars.pickup_truck"),
                            car.dimension(), car.pos().getBottomCenter(), true, car.id(), 0xFF000000 | car.color(), "PT");
                }
            }
        });
        at(300, mc -> shot(mc, "319_gps_on_foot"));
        // Driving into Mo's station: there.
        at(302, mc -> {
            gpsGoTo(mc, com.reiko.cars.gps.PlaceKind.GAS, "Mo");
            server(mc, player -> {
                // (a fresh car, facing north: a car someone's been driving keeps its own idea of where it's facing)
                CarEntity hatchback = spawned(player.serverLevel(), CarType.HATCHBACK, 13.5, -66.5, 180.0F, 0.8);
                hatchback.setOwner(player);
                player.teleportTo(player.serverLevel(), 13.5, -60.0, -64.0, 180.0F, 0.0F);
            });
        });
        at(308, mc -> server(mc, player -> player.startRiding(carAt(player.serverLevel(), 13.5, -66.5), true)));
        at(312, mc -> mc.options.keyUp.setDown(true));
        at(358, mc -> mc.options.keyUp.setDown(false));
        at(368, mc -> shot(mc, "320_gps_arrived"));
        at(370, mc -> ReikoCars.LOGGER.info("Showcase GPS: destination now {}", com.reiko.cars.client.GpsClient.destination()));
        at(378, mc -> mc.stop());
    }

    /** Some waypoints in Xaero's Minimap, to pick from. Only loaded when that mod is there. */
    private static final class GpsShowcaseXaero {
        static void addWaypoints(Minecraft minecraft) {
            xaero.hud.minimap.module.MinimapSession session = xaero.hud.minimap.BuiltInHudModules.MINIMAP.getCurrentSession();
            xaero.hud.minimap.world.MinimapWorld world = session == null ? null : session.getWorldManager().getCurrentWorld();
            if (world == null) {
                ReikoCars.LOGGER.error("Showcase GPS: no Xaero world yet");
                return;
            }
            xaero.hud.minimap.waypoint.set.WaypointSet set = world.getCurrentWaypointSet();
            set.add(new xaero.common.minimap.waypoints.Waypoint(-40, -60, 30, "Home", "H", xaero.hud.minimap.waypoint.WaypointColor.GREEN,
                    xaero.hud.minimap.waypoint.WaypointPurpose.NORMAL, false, true));
            set.add(new xaero.common.minimap.waypoints.Waypoint(120, -60, -150, "Iron mine", "M", xaero.hud.minimap.waypoint.WaypointColor.GRAY,
                    xaero.hud.minimap.waypoint.WaypointPurpose.NORMAL, false, true));
            set.add(new xaero.common.minimap.waypoints.Waypoint(-90, -60, -20, "Village", "V", xaero.hud.minimap.waypoint.WaypointColor.GOLD,
                    xaero.hud.minimap.waypoint.WaypointPurpose.NORMAL, false, true));
        }
    }

    // ---- bus stops ----

    private static com.reiko.cars.bus.BusStopBlockEntity stopAt(ServerLevel level, BlockPos pos) {
        return (com.reiko.cars.bus.BusStopBlockEntity) level.getBlockEntity(pos);
    }

    private static final BlockPos SIGN_STOP = new BlockPos(8, -60, -2);
    private static final BlockPos SHELTER_STOP = new BlockPos(9, -60, -52);

    private static void busScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                MinecraftServer server = level.getServer();
                level.setDayTime(5000);
                com.reiko.cars.config.CarsConfig.VILLAGER_CHANCE.set(1.0);
                for (int x = 0; x <= 6; x++) {
                    for (int z = -70; z <= 10; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), ModBlocks.ASPHALT.get().defaultBlockState());
                    }
                }
                for (int x = 7; x <= 11; x++) {
                    for (int z = -70; z <= 10; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                // Main St: a sign, facing the road; Oak Ave: a shelter, open to the road.
                BlockState sign = ModBlocks.BUS_STOP_SIGN.get().defaultBlockState().setValue(com.reiko.cars.bus.BusStopSignBlock.FACING, Direction.WEST);
                level.setBlockAndUpdate(SIGN_STOP, sign);
                sign.getBlock().setPlacedBy(level, SIGN_STOP, sign, player, ItemStack.EMPTY);
                BlockState shelter = ModBlocks.BUS_SHELTER.get().defaultBlockState().setValue(com.reiko.cars.bus.BusShelterBlock.FACING, Direction.WEST);
                level.setBlockAndUpdate(SHELTER_STOP, shelter);
                shelter.getBlock().setPlacedBy(level, SHELTER_STOP, shelter, player, ItemStack.EMPTY);
                com.reiko.cars.bus.BusStopService.configure(player, stopAt(level, SIGN_STOP), "Main St", new int[] {1, 1});
                com.reiko.cars.bus.BusStopService.configure(player, stopAt(level, SHELTER_STOP), "Oak Ave", new int[] {1, 2, 4, 3});
                // Villagers by the shelter, and Mo waiting at Main St.
                for (double z : new double[] {-48.5, -54.5}) {
                    net.minecraft.world.entity.npc.Villager villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
                    villager.moveTo(7.5, -60.0, z, 90.0F, 0.0F);
                    villager.setNoAi(true);
                    level.addFreshEntity(villager);
                }
                ServerPlayer mo = extraPlayer(server, "Mo");
                mo.teleportTo(level, 7.5, -60.0, -0.5, 90.0F, 0.0F);
                com.reiko.cars.bus.BusService.useStop(mo, stopAt(level, SIGN_STOP), false);
                // The bus, on route 1, free rides (Mo's dummy connection can't be shown the fare screen).
                CarEntity bus = spawned(level, CarType.CITY_BUS, 4.5, 6.5, 180.0F, 0.9);
                bus.setOwner(player);
                bus.fares().setPrices(0, 0);
                bus.busLine().setRoute(1);
                player.getInventory().setItem(0, new ItemStack(ModItems.BUS_STOP_SIGN.get()));
                player.getInventory().setItem(1, new ItemStack(ModItems.BUS_SHELTER.get()));
                player.setGameMode(GameType.CREATIVE);
            });
        });
        at(22, mc -> mc.player.getInventory().selected = 8); // an empty hand
        // The sign, and the shelter, from the road
        at(30, mc -> look(mc, 5.6, -59.1, -1.5, -90.0F, 14.0F));
        at(50, mc -> shot(mc, "330_bus_stop_sign"));
        at(52, mc -> look(mc, 5.0, -59.3, -51.0, -90.0F, 12.0F));
        at(72, mc -> shot(mc, "331_bus_shelter"));
        // Someone sitting on the bench
        at(74, mc -> server(mc, player -> {
            ServerLevel level = player.serverLevel();
            ServerPlayer sam = extraPlayer(level.getServer(), "Sam");
            sam.teleportTo(level, 8.5, -60.0, -51.5, 90.0F, 0.0F);
            BlockPos bench = SHELTER_STOP.relative(Direction.SOUTH, 1);
            com.reiko.cars.bus.BusService.sit(sam, stopAt(level, SHELTER_STOP), level.getBlockState(bench), bench);
        }));
        at(76, mc -> look(mc, 5.5, -59.4, -53.5, -60.0F, 10.0F));
        at(96, mc -> shot(mc, "332_bus_shelter_bench"));
        // The owner's screens: the stop's, and the bus's route
        at(98, mc -> server(mc, player -> com.reiko.cars.bus.BusStopMenu.open(player, stopAt(player.serverLevel(), SHELTER_STOP))));
        at(110, mc -> shot(mc, "333_bus_stop_screen"));
        at(112, mc -> {
            mc.setScreen(null);
            look(mc, 7.5, -60.0, 6.5, 90.0F, 10.0F);
        });
        at(116, mc -> server(mc, player -> com.reiko.cars.fare.FareSettingsMenu.open(player, carAt(player.serverLevel(), 4.5, 6.5))));
        at(128, mc -> shot(mc, "334_bus_route_screen"));
        // Driving, at Main St: the route on the dashboard and [G] Open doors
        at(130, mc -> {
            mc.setScreen(null);
            server(mc, player -> {
                player.setGameMode(GameType.SURVIVAL);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.startRiding(carAt(player.serverLevel(), 4.5, 6.5), true);
            });
        });
        at(154, mc -> shot(mc, "335_bus_at_main_st"));
        at(156, mc -> server(mc, com.reiko.cars.trailer.Hitching::use)); // the doors: Mo gets on
        at(162, mc -> server(mc, player -> {
            ServerPlayer mo = player.server.getPlayerList().getPlayerByName("Mo");
            com.reiko.cars.trailer.Hitching.use(mo); // and rings for the next stop
        }));
        at(170, mc -> shot(mc, "336_bus_stop_requested"));
        // Driving on to Oak Ave, and stopping by it
        for (int t = 172; t < 360; t++) {
            at(t, mc -> {
                if (!(mc.player.getVehicle() instanceof CarEntity bus)) {
                    return;
                }
                boolean there = bus.getZ() < -40.0;
                mc.options.keyUp.setDown(!there);
                mc.options.keyDown.setDown(there && bus.displaySpeed() > 0.02);
            });
        }
        at(360, mc -> {
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
        });
        at(376, mc -> server(mc, com.reiko.cars.trailer.Hitching::use)); // Mo gets off, the villagers get on
        at(380, mc -> shot(mc, "337_bus_at_oak_ave"));
        at(382, mc -> server(mc, player -> ReikoCars.LOGGER.info("Showcase bus: passengers {}",
                player.getVehicle() == null ? "none" : player.getVehicle().getPassengers())));
        at(384, mc -> {
            CarEntity bus = (CarEntity) mc.player.getVehicle();
            double z = bus == null ? -49.0 : bus.getZ();
            server(mc, player -> player.stopRiding());
            look(mc, -2.5, -58.6, z + 1.0, -90.0F, 6.0F);
        });
        at(404, mc -> shot(mc, "338_bus_villagers_aboard"));
        at(406, mc -> mc.setScreen(new com.reiko.cars.client.GpsScreen()));
        at(412, mc -> {
            for (char c : "bus".toCharArray()) {
                mc.screen.charTyped(c, 0);
            }
        });
        at(420, mc -> shot(mc, "339_bus_stops_gps"));
        at(422, mc -> {
            mc.screen.keyPressed(259, 0, 0);
            mc.screen.keyPressed(259, 0, 0);
            mc.screen.keyPressed(259, 0, 0);
            mc.setScreen(null);
        });
        // Sitting on the bench yourself
        at(424, mc -> server(mc, player -> {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.teleportTo(player.serverLevel(), 8.0, -60.0, -50.5, 90.0F, 0.0F);
            BlockPos bench = SHELTER_STOP.relative(Direction.SOUTH, 2);
            com.reiko.cars.bus.BusService.sit(player, stopAt(player.serverLevel(), SHELTER_STOP), player.serverLevel().getBlockState(bench), bench);
        }));
        at(428, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setXRot(10.0F);
        });
        at(446, mc -> shot(mc, "340_bus_shelter_sitting"));
        at(448, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.serverLevel().setDayTime(18000);
            });
        });
        at(452, mc -> look(mc, 7.0, -59.0, -44.0, -162.0F, 10.0F));
        at(476, mc -> shot(mc, "341_bus_shelter_night"));
        at(478, mc -> look(mc, 5.6, -59.1, -1.5, -90.0F, 14.0F));
        at(500, mc -> shot(mc, "342_bus_stop_sign_night"));
        at(508, mc -> mc.stop());
    }

    // ---- campers and livestock trailers: ./gradlew runClient -Pshowcase=campers ----

    private static TrailerEntity trailerNear(ServerLevel level, TrailerType type, double x, double z) {
        return level.getEntitiesOfClass(TrailerEntity.class, new net.minecraft.world.phys.AABB(x - 6, -70, z - 6, x + 6, -40, z + 6),
                trailer -> trailer.trailerType() == type).getFirst();
    }

    private static void loadAnimals(ServerLevel level, TrailerEntity trailer, double x, double z,
            net.minecraft.world.entity.EntityType<?>... types) {
        for (net.minecraft.world.entity.EntityType<?> type : types) {
            net.minecraft.world.entity.Mob animal = (net.minecraft.world.entity.Mob) type.create(level);
            animal.moveTo(x, -60.0, z, 0.0F, 0.0F);
            level.addFreshEntity(animal);
            com.reiko.cars.trailer.Livestock.load(trailer, animal);
        }
    }

    private static void campScript() {
        at(20, mc -> {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                // A pickup with a camper on its ball, and one parked with its camp out
                CarEntity pickup = spawned(level, CarType.PICKUP_TRUCK, 0.5, 6.5, 180.0F, 0.9);
                behind(level, TrailerType.CAMPER, pickup, 0.0F).startRiding(pickup, true);
                TrailerEntity camp = trailer(level, TrailerType.CAMPER, 14.5, 0.5, 0.0F);
                camp.stove().items().setItem(com.reiko.cars.trailer.CamperStove.FOOD, new ItemStack(net.minecraft.world.item.Items.BEEF, 6));
                camp.stove().items().setItem(com.reiko.cars.trailer.CamperStove.FUEL, new ItemStack(net.minecraft.world.item.Items.COAL, 8));
                camp.cargo().setItem(0, new ItemStack(net.minecraft.world.item.Items.BREAD, 12));
                camp.cargo().setItem(1, new ItemStack(net.minecraft.world.item.Items.TORCH, 32));
                camp.cargo().setItem(9, new ItemStack(net.minecraft.world.item.Items.FISHING_ROD));
                camp.setOwner(player);
                // A livestock trailer, parked, with cows, a horse and sheep in it
                TrailerEntity stock = trailer(level, TrailerType.LIVESTOCK_TRAILER, -14.5, 0.5, 0.0F);
                loadAnimals(level, stock, -14.5, -4.0, net.minecraft.world.entity.EntityType.COW, net.minecraft.world.entity.EntityType.COW,
                        net.minecraft.world.entity.EntityType.HORSE, net.minecraft.world.entity.EntityType.SHEEP,
                        net.minecraft.world.entity.EntityType.SHEEP);
                for (int i = 0; i < 2; i++) {
                    net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
                    pig.moveTo(-15.5 + i * 2.0, -60.0, -6.5, 180.0F, 0.0F);
                    pig.setNoAi(true);
                    level.addFreshEntity(pig);
                }
                // The semi with a fifth-wheel camper, another parked with its camp out, and a semi livestock trailer
                CarEntity semi = spawned(level, CarType.SEMI_TRUCK, 34.5, 6.5, 180.0F, 0.9);
                behind(level, TrailerType.FIFTH_WHEEL_CAMPER, semi, 0.0F).startRiding(semi, true);
                trailer(level, TrailerType.FIFTH_WHEEL_CAMPER, 52.5, 0.5, 0.0F);
                CarEntity hauler = spawned(level, CarType.SEMI_TRUCK, -34.5, 6.5, 180.0F, 0.9);
                TrailerEntity pot = behind(level, TrailerType.SEMI_LIVESTOCK_TRAILER, hauler, 0.0F);
                pot.startRiding(hauler, true);
                loadAnimals(level, pot, -34.5, 14.0, net.minecraft.world.entity.EntityType.COW, net.minecraft.world.entity.EntityType.COW,
                        net.minecraft.world.entity.EntityType.COW, net.minecraft.world.entity.EntityType.COW,
                        net.minecraft.world.entity.EntityType.HORSE, net.minecraft.world.entity.EntityType.LLAMA,
                        net.minecraft.world.entity.EntityType.LLAMA, net.minecraft.world.entity.EntityType.GOAT,
                        net.minecraft.world.entity.EntityType.CHICKEN, net.minecraft.world.entity.EntityType.CHICKEN,
                        net.minecraft.world.entity.EntityType.CHICKEN);
                // An off-roader with a livestock trailer swung round behind it, for the hitch close up
                CarEntity offRoader = spawned(level, CarType.OFF_ROADER, 20.5, 30.5, 180.0F, 0.9);
                behind(level, TrailerType.LIVESTOCK_TRAILER, offRoader, 35.0F).startRiding(offRoader, true);
                player.getInventory().setItem(0, new ItemStack(ModItems.trailer(TrailerType.CAMPER)));
                player.getInventory().setItem(1, new ItemStack(ModItems.trailer(TrailerType.LIVESTOCK_TRAILER)));
                player.setGameMode(GameType.CREATIVE);
            });
        });
        at(22, mc -> mc.player.getInventory().selected = 8);
        at(30, mc -> look(mc, -5.0, -58.0, -2.0, -32.0F, 12.0F));
        at(54, mc -> shot(mc, "343_pickup_with_camper"));
        at(56, mc -> look(mc, 7.5, -58.4, -5.5, -48.0F, 10.0F));
        at(80, mc -> shot(mc, "344_camper_camp"));
        at(82, mc -> look(mc, -9.0, -58.6, 0.5, 90.0F, 8.0F));
        at(100, mc -> shot(mc, "345_livestock_trailer"));
        at(102, mc -> look(mc, -11.0, -57.6, -8.5, 25.0F, 18.0F));
        at(120, mc -> shot(mc, "346_livestock_ramp"));
        at(122, mc -> look(mc, 28.0, -57.0, -3.0, -32.0F, 12.0F));
        at(142, mc -> shot(mc, "347_semi_fifth_wheel_camper"));
        at(144, mc -> look(mc, 44.0, -57.0, -7.0, -38.0F, 10.0F));
        at(164, mc -> shot(mc, "348_fifth_wheel_camp"));
        at(166, mc -> look(mc, -28.0, -57.4, 14.0, 105.0F, 8.0F));
        at(186, mc -> shot(mc, "349_semi_livestock_trailer"));
        // The tow ball, close up, with the trailer swung round
        at(188, mc -> server(mc, player -> {
            CarEntity offRoader = carAt(player.serverLevel(), 20.5, 30.5);
            net.minecraft.world.phys.Vec3 hitch = offRoader.hitchPoint();
            player.teleportTo(player.serverLevel(), hitch.x, hitch.y + 1.6, hitch.z, 180.0F, 90.0F); // straight down on it
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }));
        at(208, mc -> shot(mc, "350_tow_ball_close_up"));
        at(210, mc -> look(mc, 14.0, -55.5, 35.0, -125.0F, 28.0F));
        at(230, mc -> shot(mc, "351_offroader_turning_trailer"));
        // Inside the camper: cupboards and the stove
        at(232, mc -> look(mc, 11.0, -60.0, 0.5, -90.0F, 0.0F));
        at(236, mc -> server(mc, player -> com.reiko.cars.trailer.CamperMenu.open(player,
                trailerNear(player.serverLevel(), TrailerType.CAMPER, 14.5, 0.5))));
        at(260, mc -> shot(mc, "352_camper_screen"));
        // Sitting in a camp chair
        at(262, mc -> {
            mc.setScreen(null);
            server(mc, player -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.teleportTo(player.serverLevel(), 11.0, -60.0, 0.5, -90.0F, 0.0F);
                trailerNear(player.serverLevel(), TrailerType.CAMPER, 14.5, 0.5).sitOnChair(player, 0);
            });
        });
        at(266, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setXRot(18.0F);
        });
        at(286, mc -> shot(mc, "353_camp_chair"));
        // Night at camp: the lights and windows glow
        at(288, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> {
                player.stopRiding();
                player.serverLevel().setDayTime(18000);
            });
        });
        at(292, mc -> look(mc, 7.5, -58.4, -5.5, -48.0F, 10.0F));
        at(316, mc -> shot(mc, "354_camper_camp_night"));
        // To bed
        at(318, mc -> server(mc, player -> {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(player.serverLevel(), 11.0, -60.0, 0.5, -90.0F, 0.0F);
            com.reiko.cars.trailer.CamperLife.sleep(player, trailerNear(player.serverLevel(), TrailerType.CAMPER, 14.5, 0.5));
        }));
        at(350, mc -> shot(mc, "355_camper_sleeping"));
        at(352, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.setYRot(-150.0F);
            mc.player.setXRot(40.0F);
            for (TrailerEntity trailer : mc.level.getEntitiesOfClass(TrailerEntity.class, mc.player.getBoundingBox().inflate(8.0))) {
                trailer.setInvisible(true); // a cutaway, to see the sleeper lying on the mattress
            }
        });
        at(370, mc -> mc.level.setDayTime(6000)); // lit up, for the cutaway (the server puts night back soon)
        at(372, mc -> shot(mc, "356_camper_sleeper_cutaway"));
        at(374, mc -> {
            for (TrailerEntity trailer : mc.level.getEntitiesOfClass(TrailerEntity.class, mc.player.getBoundingBox().inflate(8.0))) {
                trailer.setInvisible(false);
            }
            ReikoCars.LOGGER.info("Showcase camper: sleeping {} at {}", mc.player.isSleeping(), mc.player.position());
        });
        at(376, mc -> server(mc, player -> player.stopSleepInBed(false, true)));
        at(382, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server(mc, player -> ReikoCars.LOGGER.info("Showcase camper: up at {}, spawn {}", player.position(), player.getRespawnPosition()));
        });
        at(386, mc -> mc.stop());
    }
}
