package com.reiko.cars.car;

import com.reiko.cars.client.CarsClientConfig;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModEntities;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A parked-away car. Use it on the ground to put the car down facing the way you look; it keeps the fuel, wear and
 * paint it had when it was picked up. Whoever puts it down owns it.
 */
public final class CarItem extends Item {
    private final CarType type;

    public CarItem(CarType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public CarType carType() {
        return type;
    }

    /** Sets the MJ of fuel in a car item's tank and returns the stack. */
    public static ItemStack withFuel(ItemStack stack, double mj) {
        if (mj > 0) {
            stack.set(ModDataComponents.FUEL.get(), mj);
        } else {
            stack.remove(ModDataComponents.FUEL.get());
        }
        return stack;
    }

    /** Sets the millibuckets of water in a fire truck item's tank and returns the stack. */
    public static ItemStack withWater(ItemStack stack, int millibuckets) {
        if (millibuckets > 0) {
            stack.set(ModDataComponents.WATER.get(), millibuckets);
        } else {
            stack.remove(ModDataComponents.WATER.get());
        }
        return stack;
    }

    /** Sets a car item's wear and tear (a new car's is left off) and returns the stack. */
    public static ItemStack withCondition(ItemStack stack, CarCondition condition) {
        if (condition.isNew()) {
            stack.remove(ModDataComponents.CONDITION.get());
        } else {
            stack.set(ModDataComponents.CONDITION.get(), condition);
        }
        return stack;
    }

    public static CarCondition conditionOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CONDITION.get(), CarCondition.NEW);
    }

    /** "Condition: 73%", then each worn part, for a car or trailer item's tooltip. Nothing for a new one. */
    public static void addConditionLines(List<Component> tooltip, CarCondition condition, java.util.function.Predicate<CarCondition.Part> has) {
        if (condition.isNew()) {
            return;
        }
        CarCondition.Level worst = CarCondition.Level.GOOD;
        for (CarCondition.Part part : CarCondition.Part.values()) {
            if (has.test(part) && condition.level(part).ordinal() > worst.ordinal()) {
                worst = condition.level(part);
            }
        }
        tooltip.add(Component.translatable("item.reikocars.car.condition", CarCondition.percent(condition.overall(has)))
                .withStyle(worst == CarCondition.Level.GOOD ? ChatFormatting.GRAY : worst.color()));
        for (CarCondition.Part part : CarCondition.Part.values()) {
            CarCondition.Level level = condition.level(part);
            if (has.test(part) && level != CarCondition.Level.GOOD) {
                tooltip.add(Component.translatable("item.reikocars.car.worn", part.displayName(),
                        CarCondition.percent(condition.get(part))).withStyle(level.color()));
            }
        }
    }

    public static double fuelOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FUEL.get(), 0.0);
    }

    /** The car's paint: its dyed colour, or the type's factory colour. */
    public static int colorOf(ItemStack stack, CarType type) {
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        return dyed != null ? dyed.rgb() : type.defaultColor;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        // Don't put a car down inside another entity the player is looking through (as boats do).
        Vec3 view = player.getViewVector(1.0F);
        List<Entity> nearby = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0),
                EntitySelector.NO_SPECTATORS.and(Entity::isPickable));
        for (Entity entity : nearby) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(player.getEyePosition())) {
                return InteractionResultHolder.pass(stack);
            }
        }

        Vec3 at = ((BlockHitResult) hit).getLocation();
        CarEntity car = ModEntities.car(type).create(level);
        if (car == null) {
            return InteractionResultHolder.fail(stack);
        }
        car.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
        if (!level.noCollision(car, car.getBoundingBox())) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("car.reikocars.no_room_to_place"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            car.setFuel(fuelOf(stack));
            car.setWater(stack.getOrDefault(ModDataComponents.WATER.get(), 0));
            car.setCondition(conditionOf(stack));
            car.setColor(colorOf(stack, type));
            car.setLockCode(stack.getOrDefault(ModDataComponents.LOCK.get(), 0L)); // the keys cut for it still fit
            car.setOwner(player);
            car.setPolicy(stack.get(ModDataComponents.INSURANCE.get())); // cover only counts while it's still the policyholder's
            car.setLocked(CarsConfig.get(CarsConfig.LOCK_NEW_CARS));
            car.setUpgrades(stack.getOrDefault(ModDataComponents.UPGRADES.get(), com.reiko.cars.tuning.Upgrades.STOCK));
            car.setLivery(stack.getOrDefault(ModDataComponents.LIVERY.get(), com.reiko.cars.tuning.Livery.PLAIN));
            String plate = stack.get(ModDataComponents.PLATE.get());
            if (plate != null && !plate.isEmpty() && level.getServer() != null) {
                car.setPlate(plate); // the plate it had when it was picked up
                com.reiko.cars.plate.PlateRegistry.get(level.getServer()).keep(plate);
            }
            if (level instanceof ServerLevel serverLevel) {
                EntityType.<CarEntity>createDefaultStackConfig(serverLevel, stack, player).accept(car); // name tag, entity data
            }
            level.addFreshEntity(car);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int percent = (int) Math.round(fuelOf(stack) / type.tankCapacity() * 100.0);
        tooltip.add((type.isElectric() ? Component.translatable("item.reikocars.car.battery", percent)
                : Component.translatable("item.reikocars.car.fuel", type.fuel().displayName(), percent)).withStyle(ChatFormatting.GRAY));
        if (type.isFireTruck()) {
            tooltip.add(Component.translatable("item.reikocars.car.water", stack.getOrDefault(ModDataComponents.WATER.get(), 0) / 1000,
                    com.reiko.cars.fire.FireTrucks.tankCapacity() / 1000).withStyle(ChatFormatting.GRAY));
        }
        addConditionLines(tooltip, conditionOf(stack), part -> part != CarCondition.Part.OIL || !type.isElectric());
        CarsClientConfig.SpeedUnit unit = CarsClientConfig.speedUnit();
        long speed = Math.round(type.topSpeed * CarsConfig.get(CarsConfig.SPEED) * unit.perBlockPerTick);
        tooltip.add((type.isElectric()
                ? Component.translatable("item.reikocars.car.stats_ev", type.seatsText(), speed, Component.translatable(unit.translationKey),
                        Math.round(type.tankCapacity() / 1000.0))
                : Component.translatable("item.reikocars.car.stats", type.seatsText(), speed, Component.translatable(unit.translationKey),
                        type.tankBuckets)).withStyle(ChatFormatting.DARK_GRAY));
        if (type.isElectric()) {
            tooltip.add(Component.translatable("item.reikocars.car.electric").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.isFireTruck()) {
            tooltip.add(Component.translatable("item.reikocars.car.fire_truck").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.storageRows > 0) {
            tooltip.add(Component.translatable("item.reikocars.car.storage", Component.translatable(type.storageKey()), type.storageRows * 9)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.offRoad) {
            tooltip.add(Component.translatable("item.reikocars.car.off_road").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.isTractor()) {
            tooltip.add(Component.translatable("item.reikocars.car.tractor").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.takesFares()) {
            tooltip.add(Component.translatable("item.reikocars.car.fares").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (stack.has(ModDataComponents.LOCK.get())) {
            tooltip.add(Component.translatable("item.reikocars.car.keyed").withStyle(ChatFormatting.DARK_GRAY));
        }
        String plate = stack.get(ModDataComponents.PLATE.get());
        if (plate != null && !plate.isEmpty()) {
            tooltip.add(Component.translatable("item.reikocars.car.plate", plate).withStyle(ChatFormatting.GRAY));
        }
        com.reiko.cars.tuning.Upgrades upgrades = stack.get(ModDataComponents.UPGRADES.get());
        if (upgrades != null && !upgrades.isStock()) {
            tooltip.add(Component.translatable("item.reikocars.car.tuned").withStyle(ChatFormatting.AQUA));
            upgrades.describe().forEach(line -> tooltip.add(Component.literal("  ").append(line).withStyle(ChatFormatting.DARK_AQUA)));
        }
        com.reiko.cars.tuning.Livery livery = stack.get(ModDataComponents.LIVERY.get());
        if (livery != null && livery.number() > 0) {
            tooltip.add(Component.translatable("item.reikocars.car.race_number", livery.number()).withStyle(ChatFormatting.GRAY));
        }
        com.reiko.cars.insurance.Policy policy = stack.get(ModDataComponents.INSURANCE.get());
        if (policy != null && context.level() != null) {
            int days = policy.daysLeft(context.level().getGameTime());
            tooltip.add((days > 0 ? Component.translatable("item.reikocars.car.insured", policy.insurerName(), days)
                    : Component.translatable("item.reikocars.car.insurance_lapsed", policy.insurerName())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
