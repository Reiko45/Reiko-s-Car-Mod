package com.reiko.cars.trailer;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.key.CarKeys;
import com.reiko.cars.key.Keyed;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModDataSerializers;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.registry.ModSounds;
import com.reiko.cars.repair.Repairable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * A semi trailer: a box trailer full of cargo, a tanker full of any fluid, or a car hauler carrying two cars.
 *
 * <p>Hitched, a trailer rides its tractor the way a player rides a car: it's one of the tractor's passengers, so
 * Minecraft moves it straight after the tractor, every tick, on every side, and saves it with the tractor. Where it
 * goes is worked out here ({@link #followTractor}): the kingpin stays on the tractor's fifth wheel and the axles at the
 * back trail behind, so the trailer swings out on turns and cuts corners as a real one does. Unhitched, it stands on
 * its landing legs where it was left.
 *
 * <p>Cars ride a car hauler the same way, as its passengers, so they go wherever it goes.
 *
 * <p>Trailers wear out too, though they have no engine: their tires wear as they're towed (faster loaded and off the
 * road), their brakes whenever the tractor brakes, and their body when the rig crashes or jackknifes. Worn trailer
 * brakes and tires make the rig slower to stop, a battered trailer slows it down, and one with a part worn out can't
 * be towed: the rig breaks down until it's unhitched or the trailer is repaired.
 */
public class TrailerEntity extends VehicleEntity implements Repairable, Keyed {
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LOCKED = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** The lock its keys fit (0 before any are cut). */
    private static final EntityDataAccessor<Long> DATA_LOCK_CODE = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.LONG);
    private static final byte FLASH_EVENT = 101;
    private static final EntityDataAccessor<Integer> DATA_FLUID = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLUID_AMOUNT = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CarCondition> DATA_CONDITION = SynchedEntityData.defineId(TrailerEntity.class, ModDataSerializers.CONDITION);
    /** A trailer's two axles carry four wheels. */
    public static final int WHEELS = 4;
    /** The share of a crash the rig's trailer takes: its load shoves it into the cab. */
    public static final double CRASH_SHARE = 0.5;

    /** Set by the client: opens the tanker's screen. */
    public static Consumer<TrailerEntity> openTankerScreen = trailer -> {
    };

    /** The parts' distance ahead of and behind the middle, in blocks. With the hitbox they cover the whole length. */
    private static final double PART_OFFSET = 3.0;

    private final TrailerType type;
    private final SimpleContainer cargo;
    private final FluidTank tank;
    private final TrailerPart[] parts;
    private String ownerName = "";
    /** Ticks left of the lights flashing for a key's lock or unlock. Client-side. */
    private int flashTicks;
    /** Where the axles are, on the ground, while hitched. Null until the trailer has been placed behind a tractor. */
    @Nullable
    private Vec3 axle;
    private CarCondition condition = CarCondition.NEW;
    /** Swung all the way round against the tractor last tick. */
    private boolean jackknifed;

    // Lerping toward the server's position, on clients, while parked
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;

    // Client-side animation
    private float wheelAngle;
    private float wheelAngleO;

    public TrailerEntity(EntityType<? extends TrailerEntity> entityType, Level level, TrailerType type) {
        super(entityType, level);
        this.type = type;
        this.blocksBuilding = true;
        this.parts = new TrailerPart[] {new TrailerPart(this, 3.2F, type.height), new TrailerPart(this, 3.2F, type.height)};
        // Part ids follow the trailer's, so the client can match them up (as the ender dragon's do).
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        this.cargo = new SimpleContainer(Math.max(1, type.cargoRows) * 9) {
            @Override
            public boolean stillValid(Player player) {
                return TrailerEntity.this.isAlive() && player.distanceToSqr(TrailerEntity.this) < 100.0;
            }
        };
        this.tank = new FluidTank(Math.max(1, type.tankBuckets) * 1000) {
            @Override
            protected void onContentsChanged() {
                syncFluid();
            }
        };
        this.entityData.set(DATA_COLOR, type.defaultColor);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, 0xFFFFFF);
        builder.define(DATA_LOCKED, false);
        builder.define(DATA_LOCK_CODE, 0L);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_FLUID, 0);
        builder.define(DATA_FLUID_AMOUNT, 0);
        builder.define(DATA_CONDITION, CarCondition.NEW);
    }

    public TrailerType trailerType() {
        return type;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < parts.length; i++) {
            parts[i].setId(id + i + 1);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    // ---- paint, owner, lock (as for cars) ----

    public int color() {
        return entityData.get(DATA_COLOR);
    }

    public void setColor(int rgb) {
        entityData.set(DATA_COLOR, rgb & 0xFFFFFF);
    }

    public boolean isLocked() {
        return entityData.get(DATA_LOCKED);
    }

    public void setLocked(boolean locked) {
        entityData.set(DATA_LOCKED, locked);
    }

    @Nullable
    public UUID owner() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(@Nullable Player player) {
        entityData.set(DATA_OWNER, Optional.ofNullable(player).map(Entity::getUUID));
        ownerName = player == null ? "" : player.getGameProfile().getName();
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner());
    }

    /** Who may open, hitch or load it: anyone unless it's locked, and then its owner, admins and anyone with its key. */
    public boolean canBeUsedBy(Player player) {
        return !isLocked() || owner() == null || isOwner(player) || player.hasPermissions(2) || CarKeys.holds(player, lockCode());
    }

    // ---- keys ----

    @Override
    public long lockCode() {
        return entityData.get(DATA_LOCK_CODE);
    }

    @Override
    public void setLockCode(long code) {
        entityData.set(DATA_LOCK_CODE, code);
    }

    @Override
    public String nameKey() {
        return "entity.reikocars." + type.id();
    }

    /** Locks or unlocks it for its owner or someone with its key; false, changing nothing, for anyone else. */
    @Override
    public boolean toggleLock(ServerPlayer player) {
        if (owner() == null || !(canManage(player) || CarKeys.holds(player, lockCode()))) {
            return false;
        }
        setLocked(!isLocked());
        playSound(ModSounds.LOCK.get(), 0.7F, isLocked() ? 1.0F : 1.3F);
        level().broadcastEntityEvent(this, FLASH_EVENT);
        player.displayClientMessage(Component.translatable(isLocked() ? "trailer.reikocars.now_locked" : "trailer.reikocars.now_unlocked"), true);
        return true;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == FLASH_EVENT) {
            flashTicks = 12;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Who may pick it up, repaint it or lock it: its owner, an admin, or anyone if nobody owns it. */
    public boolean canManage(Player player) {
        return owner() == null || isOwner(player) || player.hasPermissions(2);
    }

    // ---- cargo and fluid ----

    /** The box trailer's 54 slots. */
    public SimpleContainer cargo() {
        return cargo;
    }

    /** The tanker's tank: 64 buckets of any one fluid. */
    public FluidTank tank() {
        return tank;
    }

    private void syncFluid() {
        FluidStack fluid = tank.getFluid();
        entityData.set(DATA_FLUID, fluid.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(fluid.getFluid()));
        entityData.set(DATA_FLUID_AMOUNT, fluid.getAmount());
    }

    /** What's in the tank, as the client sees it. */
    public FluidStack shownFluid() {
        int amount = entityData.get(DATA_FLUID_AMOUNT);
        Fluid fluid = BuiltInRegistries.FLUID.byId(entityData.get(DATA_FLUID));
        return amount <= 0 || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    /** The cars on a car hauler, front first. */
    public List<CarEntity> cars() {
        return getPassengers().stream().filter(CarEntity.class::isInstance).map(CarEntity.class::cast).toList();
    }

    // ---- wear and tear ----

    @Override
    public CarCondition condition() {
        return level().isClientSide ? entityData.get(DATA_CONDITION) : condition;
    }

    /** Sets the trailer's wear and tear, telling the tractor's driver when a part runs low or gives out. Server side. */
    @Override
    public void setCondition(CarCondition newCondition) {
        CarCondition before = condition;
        condition = newCondition;
        if (newCondition.differs(entityData.get(DATA_CONDITION), 0.005F) || newCondition.isNew()) {
            entityData.set(DATA_CONDITION, newCondition);
        }
        if (level().isClientSide || tractor() == null) {
            return;
        }
        Part brokeNow = CarsConfig.get(CarsConfig.BREAKDOWNS) ? newCondition.brokenPart() : null;
        if (brokeNow != null && before.brokenPart() == null) {
            if (brokeNow == Part.TIRES) {
                playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.35F, 1.9F); // a blowout
            } else {
                playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.8F, 0.6F);
            }
            tellDriver(Component.translatable("car.reikocars.trailer_broke_down." + brokeNow.id()));
            return;
        }
        for (Part part : Part.values()) {
            CarCondition.Level was = before.level(part);
            CarCondition.Level now = newCondition.level(part);
            if (now.ordinal() > was.ordinal() && now != CarCondition.Level.BROKEN) {
                tellDriver(Component.translatable("car.reikocars.trailer_part_" + now.name().toLowerCase(java.util.Locale.ROOT),
                        part.displayName(), CarCondition.percent(newCondition.get(part))));
            }
        }
    }

    /** Whether a worn-out part keeps the trailer from being towed. */
    public boolean isBrokenDown() {
        return CarsConfig.get(CarsConfig.BREAKDOWNS) && condition().brokenPart() != null;
    }

    private void tellDriver(Component message) {
        CarEntity tractor = tractor();
        if (tractor != null && tractor.getControllingPassenger() instanceof Player player) {
            player.displayClientMessage(message, true);
        }
    }

    /** A trailer has tires, brakes and a body, but no oil or engine. */
    @Override
    public boolean hasPart(Part part) {
        return part == Part.TIRES || part == Part.BRAKES || part == Part.BODY;
    }

    @Override
    public int wheelCount() {
        return WHEELS;
    }

    @Override
    public boolean isHeavy() {
        return true;
    }

    @Override
    public Component kindName() {
        return Component.translatable("entity.reikocars." + type.id());
    }

    /** How full it is, 0 to 1: a loaded trailer is harder on its tires. */
    public double fullness() {
        if (type.cargoRows > 0) {
            int used = 0;
            for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
                if (!cargo.getItem(slot).isEmpty()) {
                    used++;
                }
            }
            return used / (double) cargo.getContainerSize();
        }
        if (type.tankBuckets > 0) {
            return tank.getFluidAmount() / (double) tank.getCapacity();
        }
        return type.carSlots > 0 ? cars().size() / (double) type.carSlots : 0.0;
    }

    /** The tractor braked: the trailer's brakes did their share, and a skid scrubbed its tires. Server side. */
    public void brakeWear(double braked, double skidded) {
        setCondition(condition.afterBraking(braked, skidded, 1.0 + 0.4 * fullness(), CarsConfig.get(CarsConfig.WEAR)));
    }

    /** The trailer took a knock at {@code impact} blocks per tick, with a clang if {@code loud}. Server side. */
    public void crash(double impact, boolean loud) {
        if (impact <= CarCondition.CRASH_THRESHOLD) {
            return;
        }
        double damage = CarsConfig.get(CarsConfig.CRASH_DAMAGE);
        if (damage > 0.0) {
            setCondition(condition.with(Part.BODY, condition.afterCrash(impact, damage).body()));
        }
        if (loud) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.NEUTRAL,
                    (float) Math.min(1.2, 0.4 + impact), 0.6F);
            if (level() instanceof ServerLevel server) {
                Vec3 nose = position().add(forward().scale(TrailerType.KINGPIN_FORWARD));
                server.sendParticles(ParticleTypes.CRIT, nose.x, getY() + 1.5, nose.z, 10, 0.8, 0.4, 0.8, 0.2);
            }
        }
    }

    /** Wear from being towed {@code moved} blocks this tick. Server side. */
    private void wearFromTowing(double moved) {
        if (moved <= 0.0 || moved > 8.0) {
            return; // standing still, or teleported
        }
        BlockPos under = BlockPos.containing(getX(), getY() - 0.2, getZ());
        double tires = level().getBlockState(under).is(CarEntity.OFF_ROAD) || level().getBlockState(blockPosition()).is(CarEntity.OFF_ROAD)
                ? 2.5 : 1.0;
        setCondition(condition.afterRolling(moved, tires, 1.0 + 0.4 * fullness(), CarsConfig.get(CarsConfig.WEAR)));
    }

    // ---- where its parts are ----

    /** The way the trailer faces, flat. */
    public Vec3 forward() {
        return fromYaw(getYRot());
    }

    private static Vec3 fromYaw(float yaw) {
        float radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
    }

    /** The kingpin, which sits on a tractor's fifth wheel. */
    public Vec3 kingpin() {
        return position().add(forward().scale(TrailerType.KINGPIN_FORWARD)).add(0.0, TrailerType.KINGPIN_HEIGHT, 0.0);
    }

    /** The middle of the back end, where cars drive onto a car hauler. */
    public Vec3 rear() {
        return position().add(forward().scale(-TrailerType.LENGTH / 2.0));
    }

    /** The tractor pulling it, if it's hitched. */
    @Nullable
    public CarEntity tractor() {
        return getVehicle() instanceof CarEntity car ? car : null;
    }

    /**
     * Puts the trailer behind {@code tractor}: kingpin on the fifth wheel, pointing from where its axles were toward
     * the fifth wheel. Called by the tractor every tick while hitched, on the server and on every client.
     */
    public void followTractor(CarEntity tractor) {
        Vec3 hitch = tractor.fifthWheel();
        if (axle == null) {
            Vec3 forward = forward();
            axle = new Vec3(getX() - forward.x * TrailerType.AXLE_BACK, 0.0, getZ() - forward.z * TrailerType.AXLE_BACK);
        }
        double dx = hitch.x - axle.x;
        double dz = hitch.z - axle.z;
        float yaw = dx * dx + dz * dz < 1.0E-6 ? getYRot() : (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        // No folding back on itself: a trailer can only swing so far round. Getting there (usually backing up
        // badly) jackknifes it: it slams into the cab.
        float relative = Mth.wrapDegrees(yaw - tractor.getYRot());
        boolean folded = Math.abs(relative) > TrailerType.MAX_ARTICULATION;
        if (folded) {
            yaw = tractor.getYRot() + Math.copySign(TrailerType.MAX_ARTICULATION, relative);
        }
        yaw = getYRot() + Mth.wrapDegrees(yaw - getYRot()); // keep turning smoothly, never jumping by 360
        Vec3 direction = fromYaw(yaw);
        double kingpinToAxle = TrailerType.KINGPIN_FORWARD + TrailerType.AXLE_BACK;
        axle = new Vec3(hitch.x - direction.x * kingpinToAxle, 0.0, hitch.z - direction.z * kingpinToAxle);
        Vec3 before = position();
        setYRot(yaw);
        setPos(hitch.x - direction.x * TrailerType.KINGPIN_FORWARD, hitch.y - TrailerType.KINGPIN_HEIGHT,
                hitch.z - direction.z * TrailerType.KINGPIN_FORWARD);
        updateParts();
        Vec3 moved = position().subtract(before);
        if (level().isClientSide) {
            spinWheels(moved);
        } else {
            double distance = moved.horizontalDistance();
            wearFromTowing(distance);
            if (folded && !jackknifed && distance > 0.03 && distance < 8.0) {
                crash(CarCondition.CRASH_THRESHOLD + 0.1 + distance * 2.0, true);
            }
            jackknifed = folded;
        }
    }

    private void spinWheels(Vec3 moved) {
        if (moved.lengthSqr() < 64.0) { // not a teleport
            wheelAngle += (float) (moved.dot(forward()) / TrailerType.WHEEL_RADIUS);
        }
    }

    private void updateParts() {
        Vec3 forward = forward();
        for (int i = 0; i < parts.length; i++) {
            double along = i == 0 ? PART_OFFSET : -PART_OFFSET;
            TrailerPart part = parts[i];
            part.xo = part.getX();
            part.yo = part.getY();
            part.zo = part.getZ();
            part.xOld = part.xo;
            part.yOld = part.yo;
            part.zOld = part.zo;
            part.setPos(getX() + forward.x * along, getY(), getZ() + forward.z * along);
        }
    }

    // ---- ticking ----

    @Override
    public void tick() {
        if (flashTicks > 0) {
            flashTicks--;
        }
        if (getHurtTime() > 0) {
            setHurtTime(getHurtTime() - 1);
        }
        if (getDamage() > 0.0F) {
            setDamage(getDamage() - 1.0F);
        }
        super.tick();
        wheelAngleO = wheelAngle;
        if (!isPassenger()) {
            axle = null;
            if (level().isClientSide) {
                tickLerp();
            } else {
                // Parked on its legs: it only falls.
                double vy = isNoGravity() || onGround() ? 0.0 : (getDeltaMovement().y - 0.08) * 0.98;
                setDeltaMovement(0.0, vy, 0.0);
                if (vy != 0.0) {
                    move(MoverType.SELF, getDeltaMovement());
                }
            }
            updateParts();
            if (level().isClientSide) {
                spinWheels(position().subtract(xo, yo, zo));
            }
        }
    }

    private void tickLerp() {
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, getXRot());
            lerpSteps--;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 5;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? (float) lerpYRot : getYRot();
    }

    public float wheelAngle(float partialTick) {
        return Mth.lerp(partialTick, wheelAngleO, wheelAngle);
    }

    /** Lit while it's hitched to a tractor with its engine running. */
    public boolean lightsOn() {
        CarEntity tractor = tractor();
        return (tractor != null && (tractor.isEngineRunning() || tractor.lightsFlashing())) || (flashTicks > 0 && (flashTicks / 3) % 2 == 1);
    }

    // ---- cars riding a car hauler ----

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return type.carSlots > 0 && passenger instanceof CarEntity car && car.carType().fitsOnHauler() && cars().size() < type.carSlots;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!(passenger instanceof CarEntity car)) {
            super.positionRider(passenger, callback);
            return;
        }
        int slot = Math.max(0, cars().indexOf(car));
        Vec3 at = position().add(forward().scale(TrailerType.carSlotForward(slot))).add(0.0, TrailerType.DECK_HEIGHT / 16.0, 0.0);
        callback.accept(passenger, at.x, at.y, at.z);
        car.setYRot(getYRot());
    }

    @Nullable
    @Override
    public net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return null;
    }

    // ---- interaction ----

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        InteractionResult keyed = CarKeys.useOn(this, player, hand);
        if (keyed != null) {
            return keyed;
        }
        if (player.isSecondaryUseActive() && held.isEmpty()) {
            if (owner() == null || !(canManage(player) || CarKeys.holds(player, lockCode()))) {
                return refuse(player, Component.translatable("trailer.reikocars.not_yours", ownerName));
            }
            if (player instanceof ServerPlayer serverPlayer) {
                toggleLock(serverPlayer);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.getItem() instanceof DyeItem dye) {
            if (!canManage(player)) {
                return refuse(player, Component.translatable("trailer.reikocars.not_yours", ownerName));
            }
            int rgb = dye.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF;
            if (rgb != color()) {
                if (!level().isClientSide) {
                    setColor(rgb);
                    held.consume(1, player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (!canBeUsedBy(player)) {
            return refuse(player, Component.translatable("trailer.reikocars.locked"));
        }
        switch (type) {
            case BOX_TRAILER -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.sixRows(id, inventory, cargo), getDisplayName()));
                }
            }
            case TANKER_TRAILER -> {
                if (FluidUtil.getFluidHandler(held).isPresent()) {
                    if (!level().isClientSide && !FluidUtil.interactWithFluidHandler(player, hand, tank)) {
                        player.displayClientMessage(Component.translatable("trailer.reikocars.tank_wont_take"), true);
                    }
                } else if (level().isClientSide) {
                    openTankerScreen.accept(this);
                }
            }
            case CAR_HAULER -> {
                if (!level().isClientSide) {
                    player.displayClientMessage(Component.translatable("trailer.reikocars.hauler_help", cars().size(), type.carSlots), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private InteractionResult refuse(Player player, Component message) {
        if (!level().isClientSide) {
            player.displayClientMessage(message, true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // ---- physical properties ----

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    /** Not solid: players and cars pass through it, so a tractor never snags on its own trailer. */
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        fallDistance = 0.0F;
    }

    /** It's far longer than its hitbox: don't stop drawing it while any of it could be on screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(TrailerType.LENGTH / 2.0, 0.5, TrailerType.LENGTH / 2.0);
    }

    // ---- damage and dropping ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        if (!(source.getEntity() instanceof Player player)) {
            return false;
        }
        if (!canManage(player)) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.not_yours", ownerName), true);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void destroy(DamageSource source) {
        kill();
        if (level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            spawnAtLocation(toItem());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy() && type.cargoRows > 0) {
            Containers.dropContents(level(), this, cargo);
        }
        super.remove(reason);
    }

    /** This trailer as an item, keeping its paint, name, wear and (for a tanker) load. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(getDropItem());
        CarItem.withCondition(stack, condition);
        if (lockCode() != 0L) {
            stack.set(ModDataComponents.LOCK.get(), lockCode()); // so the keys still fit when it's put down again
        }
        if (color() != type.defaultColor) {
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color(), false));
        }
        if (!tank.isEmpty() && type.tankBuckets > 0) {
            stack.set(ModDataComponents.TANK_FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
        }
        if (hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, getCustomName());
        }
        return stack;
    }

    /** Sets up a trailer just put down from {@code stack} by {@code player}. */
    public void setUpFrom(ItemStack stack, Player player) {
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        setColor(dyed != null ? dyed.rgb() : type.defaultColor);
        SimpleFluidContent fluid = stack.get(ModDataComponents.TANK_FLUID.get());
        if (fluid != null && type.tankBuckets > 0) {
            tank.setFluid(fluid.copy());
            syncFluid();
        }
        setCondition(CarItem.conditionOf(stack));
        setLockCode(stack.getOrDefault(ModDataComponents.LOCK.get(), 0L));
        setOwner(player);
        setLocked(CarsConfig.get(CarsConfig.LOCK_NEW_CARS));
    }

    @Override
    protected Item getDropItem() {
        return ModItems.trailer(type);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }

    // ---- saving ----

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Color", color());
        tag.putBoolean("Locked", isLocked());
        if (owner() != null) {
            tag.putUUID("Owner", owner());
            tag.putString("OwnerName", ownerName);
        }
        if (lockCode() != 0L) {
            tag.putLong("LockCode", lockCode());
        }
        if (type.cargoRows > 0) {
            tag.put("Cargo", cargo.createTag(registryAccess()));
        }
        if (type.tankBuckets > 0) {
            tag.put("Tank", tank.writeToNBT(registryAccess(), new CompoundTag()));
        }
        if (!condition.isNew()) {
            CarCondition.CODEC.encodeStart(NbtOps.INSTANCE, condition).result().ifPresent(saved -> tag.put("Condition", saved));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Color", Tag.TAG_INT)) {
            setColor(tag.getInt("Color"));
        }
        setLocked(tag.getBoolean("Locked"));
        entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        ownerName = tag.getString("OwnerName");
        setLockCode(tag.getLong("LockCode"));
        if (type.cargoRows > 0 && tag.contains("Cargo", Tag.TAG_LIST)) {
            cargo.fromTag(tag.getList("Cargo", Tag.TAG_COMPOUND), registryAccess());
        }
        if (type.tankBuckets > 0 && tag.contains("Tank", Tag.TAG_COMPOUND)) {
            tank.readFromNBT(registryAccess(), tag.getCompound("Tank"));
            syncFluid();
        }
        condition = tag.contains("Condition") ? CarCondition.CODEC.parse(NbtOps.INSTANCE, tag.get("Condition")).result().orElse(CarCondition.NEW)
                : CarCondition.NEW;
        entityData.set(DATA_CONDITION, condition);
    }
}
