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
                    Difficulty.PEACEFUL, !MODE.equals("repair") && !MODE.equals("sales") && !MODE.equals("tolls") && !MODE.equals("insurance") && !MODE.equals("work"), rules, WorldDataConfiguration.DEFAULT); // no cheats: a customer, not an admin
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
        net.minecraft.world.phys.Vec3 fifthWheel = tractor.fifthWheel();
        return trailer(level, type, fifthWheel.x + Math.sin(radians) * TrailerType.KINGPIN_FORWARD,
                fifthWheel.z - Math.cos(radians) * TrailerType.KINGPIN_FORWARD, yaw);
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
                    length = TrailerType.LENGTH;
                    height = 3.0;
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
            trailer.moveTo(fifthWheel.x, -60.0, fifthWheel.z - com.reiko.cars.trailer.TrailerType.KINGPIN_FORWARD, 0.0F, 0.0F);
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
        return trailer.getZ() - TrailerType.LENGTH / 2.0;
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
}
