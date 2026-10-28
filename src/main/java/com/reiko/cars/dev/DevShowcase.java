package com.reiko.cars.dev;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.network.PumpActionPayload;
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
    /** "true" for the cars and pumps, "workbench" for the Car Workbench, "diesel" for the diesel pump, "false" for none. */
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
                    Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
            minecraft.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), new TitleScreen());
        });
        if (MODE.equals("workbench")) {
            workbenchScript();
        } else if (MODE.equals("diesel")) {
            dieselScript();
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
        server(minecraft, player -> player.teleportTo(player.serverLevel(), x, y, z, yaw, pitch));
    }

    private static void shot(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.getMainRenderTarget(),
                message -> ReikoCars.LOGGER.info("Showcase screenshot: {}", message.getString()));
    }

    private static CarEntity car(ServerLevel level, CarType type) {
        return level.getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(-40, -70, -40, 40, -40, 40),
                car -> car.carType() == type).getFirst();
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
}
