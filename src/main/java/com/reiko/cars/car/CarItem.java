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
 * A parked-away car. Use it on the ground to put the car down facing the way you look; it keeps the fuel and paint
 * it had when it was picked up. Whoever puts it down owns it.
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
            car.setColor(colorOf(stack, type));
            car.setOwner(player);
            car.setLocked(CarsConfig.get(CarsConfig.LOCK_NEW_CARS));
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
        tooltip.add(Component.translatable("item.reikocars.car.fuel", type.fuel().displayName(), percent).withStyle(ChatFormatting.GRAY));
        CarsClientConfig.SpeedUnit unit = CarsClientConfig.speedUnit();
        tooltip.add(Component.translatable("item.reikocars.car.stats", type.seatCount(),
                Math.round(type.topSpeed * CarsConfig.get(CarsConfig.SPEED) * unit.perBlockPerTick),
                Component.translatable(unit.translationKey), type.tankBuckets).withStyle(ChatFormatting.DARK_GRAY));
        if (type.storageRows > 0) {
            tooltip.add(Component.translatable("item.reikocars.car.bed").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.offRoad) {
            tooltip.add(Component.translatable("item.reikocars.car.off_road").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.isTractor()) {
            tooltip.add(Component.translatable("item.reikocars.car.tractor").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
