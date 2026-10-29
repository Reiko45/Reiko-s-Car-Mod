package com.reiko.cars.car;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModDataSerializers;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.registry.ModSounds;
import com.reiko.cars.repair.Repairable;
import com.reiko.cars.trailer.TrailerEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A drivable car. Like a boat, the driver's own client works out where the car goes and the server accepts it, so
 * steering feels instant even with lag. The server burns fuel for the distance the car actually covered, hurts
 * whatever it runs over, and keeps the fuel, paint, owner and lock, which it sends to every client.
 *
 * <p>Fuel is stored as energy (BuildCraft MJ) rather than as a fluid, so stronger fuels go further. A full tank is
 * {@link CarType#tankBuckets} buckets of BuildCraft's own fuel of the car's kind: light fuel, or dense fuel for diesels.
 *
 * <p>A semi tractor pulls a {@link TrailerEntity}, which rides it as a passenger (without taking a seat), so it's
 * moved straight after the tractor every tick. A car can ride a car hauler the same way, and doesn't drive while it does.
 *
 * <p>Cars wear out as they're driven (see {@link CarCondition}). The server wears the oil, engine and tires by the
 * distance covered; the driver's client, which alone knows when the brakes are on and when the car hits something,
 * reports braking and crashes, and the server checks those against what the car could do. Worn parts change how the
 * car drives on the driver's client, from the condition the server sends with the rest of the car's data.
 */
public class CarEntity extends VehicleEntity implements Repairable {
    public static final ResourceKey<DamageType> RUN_OVER = ResourceKey.create(Registries.DAMAGE_TYPE, ReikoCars.id("car"));
    /** Grass, dirt, sand, snow and the like slow every car but the off-roader. */
    public static final TagKey<Block> OFF_ROAD = BlockTags.create(ReikoCars.id("off_road"));

    private static final EntityDataAccessor<Float> DATA_FUEL = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LOCKED = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<CarCondition> DATA_CONDITION = SynchedEntityData.defineId(CarEntity.class, ModDataSerializers.CONDITION);
    /** Wear smaller than this isn't sent to clients straight away (warning levels always are). */
    private static final float CONDITION_SYNC_STEP = 0.005F;
    /** The fewest ticks between a driver's wear reports, so a client can't flood the server. */
    private static final int REPORT_INTERVAL = 5;

    /** Fuel changes smaller than this (in MJ) aren't sent to clients straight away, so driving doesn't send a packet every tick. */
    private static final float FUEL_SYNC_STEP = 250;
    private static final int HORN_COOLDOWN = 12;
    /** How far a motorcycle leans at full lock and speed, in degrees. */
    private static final float MAX_LEAN = 32.0F;
    /** How far a parked motorcycle leans over on its kickstand, to its left, in degrees. */
    private static final float KICKSTAND_LEAN = 9.0F;

    private final CarType type;
    private final SimpleContainer bed;

    // Server-side state
    private double fuel;
    private String ownerName = "";
    @Nullable
    private Vec3 lastFuelPos;
    private int hornCooldown;
    private CarCondition condition = CarCondition.NEW;
    private int lastBrakeReport = -REPORT_INTERVAL;
    private int lastCrashReport = -REPORT_INTERVAL;

    // Driving state, on whichever side simulates the car
    private boolean inputForward;
    private boolean inputBack;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputBrake;
    /** Signed speed along the car's heading, in blocks per tick. */
    private double speed;
    /** -1 (full right) to 1 (full left), eased toward the keys being held. */
    private float steering;
    /** How much the car turned this tick, which turns its passengers with it. */
    private float deltaRotation;
    /** This tick's sideways shift from turning about a point behind the middle (see {@link CarType#pivotBack}). */
    private Vec3 pivotShift = Vec3.ZERO;

    // What the driver's client has to tell the server: speed taken off by the brakes, and by skidding on the
    // handbrake, in blocks per tick, and the hardest hit since the last report.
    private double brakeWork;
    private double skidWork;
    private double crashImpact;
    private int squealCooldown;

    // Lerping toward the server's position, on clients that don't drive it
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    // Client-side animation
    private double measuredSpeed;
    private float wheelAngle;
    private float wheelAngleO;
    private float steerAngle;
    private float steerAngleO;
    /** A motorcycle's lean, in degrees: positive to its right. Client-side, for drawing. */
    private float lean;
    private float leanO;

    public CarEntity(EntityType<? extends CarEntity> entityType, Level level, CarType type) {
        super(entityType, level);
        this.type = type;
        this.blocksBuilding = true;
        this.bed = new SimpleContainer(Math.max(1, type.storageRows) * 9) {
            @Override
            public boolean stillValid(Player player) {
                return CarEntity.this.isAlive() && player.distanceToSqr(CarEntity.this) < 64.0;
            }
        };
        this.entityData.set(DATA_COLOR, type.defaultColor);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUEL, 0.0F);
        builder.define(DATA_COLOR, 0xFFFFFF);
        builder.define(DATA_LOCKED, false);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_CONDITION, CarCondition.NEW);
    }

    public CarType carType() {
        return type;
    }

    // ---- fuel ----

    /** MJ in the tank. Exact on the server; on clients, accurate to within a few hundred MJ. */
    public double fuel() {
        return level().isClientSide ? entityData.get(DATA_FUEL) : fuel;
    }

    /** 0 to 1. */
    public float fuelFraction() {
        return (float) Mth.clamp(fuel() / type.tankCapacity(), 0.0, 1.0);
    }

    /** MJ of free space in the tank. */
    public double fuelRoom() {
        return Math.max(0.0, type.tankCapacity() - fuel());
    }

    public boolean hasFuel() {
        return fuel() > 0.0;
    }

    public void setFuel(double mj) {
        double clamped = Mth.clamp(mj, 0.0, type.tankCapacity());
        fuel = clamped;
        float synced = entityData.get(DATA_FUEL);
        if (Math.abs(synced - clamped) >= FUEL_SYNC_STEP || clamped == 0.0 || clamped >= type.tankCapacity()
                || (synced == 0.0F) != (clamped == 0.0)) {
            entityData.set(DATA_FUEL, (float) clamped);
        }
    }

    /** Adds fuel, up to a full tank. Returns the MJ that went in. */
    public double addFuel(double mj) {
        double added = Math.min(mj, fuelRoom());
        if (added > 0) {
            setFuel(fuel + added);
            entityData.set(DATA_FUEL, (float) fuel); // fills always show straight away
        }
        return added;
    }

    // ---- wear and tear ----

    /** The car's wear and tear. Exact on the server; on clients, to within half a percent. */
    @Override
    public CarCondition condition() {
        return level().isClientSide ? entityData.get(DATA_CONDITION) : condition;
    }

    /**
     * Sets the car's wear and tear, telling the driver when a part runs low or gives out. Server side.
     */
    @Override
    public void setCondition(CarCondition newCondition) {
        CarCondition before = condition;
        condition = newCondition;
        if (newCondition.differs(entityData.get(DATA_CONDITION), CONDITION_SYNC_STEP) || newCondition.isNew()) {
            entityData.set(DATA_CONDITION, newCondition);
        }
        if (level().isClientSide) {
            return;
        }
        Part brokeNow = CarsConfig.get(CarsConfig.BREAKDOWNS) ? newCondition.brokenPart() : null;
        if (brokeNow != null && before.brokenPart() == null && getControllingPassenger() != null) {
            playSound(ModSounds.BREAKDOWN.get(), 1.0F, 1.0F);
            tellDriver(Component.translatable("car.reikocars.broke_down." + brokeNow.id()));
            return;
        }
        for (Part part : Part.values()) {
            CarCondition.Level was = before.level(part);
            CarCondition.Level now = newCondition.level(part);
            if (now.ordinal() > was.ordinal() && now != CarCondition.Level.BROKEN) {
                tellDriver(Component.translatable("car.reikocars.part_" + now.name().toLowerCase(java.util.Locale.ROOT),
                        part.displayName(), CarCondition.percent(newCondition.get(part))));
            }
        }
    }

    /**
     * Whether a worn-out part keeps the car from running, or its trailer from being towed (unless the server has
     * breakdowns turned off).
     */
    public boolean isBrokenDown() {
        if (!CarsConfig.get(CarsConfig.BREAKDOWNS)) {
            return false;
        }
        TrailerEntity towed = trailer();
        return condition().brokenPart() != null || (towed != null && towed.isBrokenDown());
    }

    /** What's broken down, for the driver: one of the car's parts, or its trailer's. */
    private Component brokenDownMessage() {
        Part own = condition().brokenPart();
        if (own != null) {
            return Component.translatable("car.reikocars.broke_down." + own.id());
        }
        TrailerEntity towed = trailer();
        Part trailers = towed == null ? null : towed.condition().brokenPart();
        return Component.translatable("car.reikocars.trailer_broke_down." + (trailers == null ? Part.BODY : trailers).id());
    }

    /** A car has every part. */
    @Override
    public boolean hasPart(Part part) {
        return true;
    }

    @Override
    public int wheelCount() {
        return type.wheels;
    }

    /** Diesel trucks are heavy-duty. */
    @Override
    public boolean isHeavy() {
        return type.fuel() == FuelType.DIESEL;
    }

    @Override
    public Component kindName() {
        return Component.translatable("entity.reikocars." + type.id());
    }

    private void tellDriver(Component message) {
        if (getControllingPassenger() instanceof Player player) {
            player.displayClientMessage(message, true);
        }
    }

    /**
     * What the driver's client has seen since it last reported: braking, handbrake skids and the hardest crash, or
     * null if there's nothing to tell. Clears what it returns. A crash is reported straight away; braking every
     * second or so.
     */
    @Nullable
    public float[] takeWearReport() {
        boolean crash = crashImpact > CarCondition.CRASH_THRESHOLD;
        if (!crash && (tickCount % 20 != 0 || (brakeWork <= 0.0 && skidWork <= 0.0))) {
            return null;
        }
        float[] report = {(float) brakeWork, (float) skidWork, (float) (crash ? crashImpact : 0.0)};
        brakeWork = 0.0;
        skidWork = 0.0;
        crashImpact = 0.0;
        return report;
    }

    /**
     * Applies what the driver's client reported, within what the car could really have done. Server side.
     *
     * @param brakeSpeed speed the brakes took off, in blocks per tick, summed
     * @param skidSpeed speed taken off with the wheels locked
     * @param impact the hardest crash, in blocks per tick (0 for none)
     */
    public void applyWearReport(ServerPlayer from, float brakeSpeed, float skidSpeed, float impact) {
        if (getControllingPassenger() != from || isPassenger()) {
            return;
        }
        double rate = CarsConfig.get(CarsConfig.WEAR);
        double load = trailer() != null ? 1.4 : 1.0;
        if ((brakeSpeed > 0.0F || skidSpeed > 0.0F) && tickCount - lastBrakeReport >= REPORT_INTERVAL) {
            // At most what the brakes could take off in two seconds.
            double most = type.braking * 1.5 * 40;
            double braked = Mth.clamp(brakeSpeed, 0.0, most);
            double skidded = Mth.clamp(skidSpeed, 0.0, most);
            lastBrakeReport = tickCount;
            setCondition(condition.afterBraking(braked, skidded, load, rate));
            TrailerEntity towed = trailer();
            if (towed != null) {
                towed.brakeWear(braked, skidded);
            }
            if ((condition.brakes() < 0.25F || (towed != null && towed.condition().brakes() < 0.25F)) && braked > 0.1) {
                level().playSound(from, getX(), getY(), getZ(), ModSounds.BRAKE_SQUEAL.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
            }
        }
        if (impact > 0.0F && tickCount - lastCrashReport >= REPORT_INTERVAL) {
            lastCrashReport = tickCount;
            crash(Math.min(impact, type.topSpeed * CarsConfig.get(CarsConfig.SPEED) * 1.6));
        }
    }

    /** The car hit something at {@code impact} blocks per tick: dents its body (and its engine, if hard), with a bang. */
    public void crash(double impact) {
        if (impact <= CarCondition.CRASH_THRESHOLD) {
            return;
        }
        double damage = CarsConfig.get(CarsConfig.CRASH_DAMAGE);
        if (damage > 0.0) {
            setCondition(condition.afterCrash(impact, damage));
        }
        TrailerEntity towed = trailer();
        if (towed != null) {
            towed.crash(impact * TrailerEntity.CRASH_SHARE, false); // the load shoves the trailer into the cab
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.NEUTRAL,
                (float) Math.min(1.2, 0.3 + impact), 0.7F + (float) Math.min(0.3, impact * 0.2));
        if (level() instanceof ServerLevel server) {
            Vec3 nose = position().add(forward().scale(Math.signum(speed == 0 ? 1 : speed) * type.length * 0.45));
            server.sendParticles(ParticleTypes.CRIT, nose.x, getY() + type.height * 0.4, nose.z, (int) (6 + impact * 12),
                    type.width * 0.3, 0.2, type.width * 0.3, 0.2);
        }
    }

    // ---- paint, owner, lock ----

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

    /** Owners and admins can always get in. Anyone can get into an unlocked or unowned car, or join its owner. */
    public boolean canBeUsedBy(Player player) {
        if (!isLocked() || owner() == null || isOwner(player) || player.hasPermissions(2) || hasPassenger(player)) {
            return true;
        }
        return getPassengers().stream().anyMatch(passenger -> passenger instanceof Player p && isOwner(p));
    }

    /** Who may break the car to pick it up or repaint it: its owner, an admin, or anyone if nobody owns it. */
    public boolean canManage(Player player) {
        return owner() == null || isOwner(player) || player.hasPermissions(2);
    }

    // ---- interaction ----

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (type.storageRows == 0) {
                return InteractionResult.PASS;
            }
            if (!canBeUsedBy(player)) {
                return refuse(player, Component.translatable("car.reikocars.locked"));
            }
            if (player instanceof ServerPlayer serverPlayer) {
                openBed(serverPlayer);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.getItem() instanceof DyeItem dye) {
            if (!canManage(player)) {
                return refuse(player, Component.translatable("car.reikocars.not_yours", ownerName));
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
        if (CarsConfig.get(CarsConfig.BUCKET_FUELING) && FluidUtil.getFluidContained(held).filter(CarFuel::isFuel).isPresent()) {
            FuelType poured = FuelType.of(FluidUtil.getFluidContained(held).get());
            if (poured != carType().fuel()) {
                return refuse(player, Component.translatable("car.reikocars.wrong_fuel", carType().fuel().displayName(), poured.displayName()));
            }
            if (!level().isClientSide) {
                pourFrom(player, hand);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!canAddPassenger(player)) {
            return InteractionResult.PASS;
        }
        if (!canBeUsedBy(player)) {
            return refuse(player, Component.translatable("car.reikocars.locked"));
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult refuse(Player player, Component message) {
        if (!level().isClientSide) {
            player.displayClientMessage(message, true);
            playSound(ModSounds.LOCK.get(), 0.5F, 0.6F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Pours a bucket (or any fluid container) of fuel into the tank. */
    private void pourFrom(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        CarTank tank = new CarTank(this);
        FluidActionResult result = FluidUtil.tryEmptyContainer(held, tank, Integer.MAX_VALUE, player, true);
        if (result.isSuccess()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result.getResult()));
            player.displayClientMessage(Component.translatable("car.reikocars.fuel_level", Math.round(fuelFraction() * 100)), true);
        } else {
            player.displayClientMessage(Component.translatable(fuelRoom() <= 0 ? "car.reikocars.tank_full" : "car.reikocars.no_room_for_bucket"), true);
        }
    }

    private void openBed(ServerPlayer player) {
        Component title = Component.translatable(type.storageKey());
        int rows = type.storageRows;
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> switch (rows) {
            case 1 -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, bed, 1);
            case 2 -> new ChestMenu(MenuType.GENERIC_9x2, id, inventory, bed, 2);
            case 4 -> new ChestMenu(MenuType.GENERIC_9x4, id, inventory, bed, 4);
            case 5 -> new ChestMenu(MenuType.GENERIC_9x5, id, inventory, bed, 5);
            case 6 -> ChestMenu.sixRows(id, inventory, bed);
            default -> ChestMenu.threeRows(id, inventory, bed);
        }, title));
    }

    /** The truck bed's items, or an empty container for cars without one. */
    public SimpleContainer bed() {
        return bed;
    }

    /** The horn, sent from the driver's key. */
    public void honk() {
        if (hornCooldown <= 0) {
            hornCooldown = HORN_COOLDOWN;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.HORN.get(), SoundSource.NEUTRAL, 2.0F,
                    type.hornPitch());
        }
    }

    /** Locks or unlocks the car for its owner. Returns false, and changes nothing, for anyone else. */
    public boolean toggleLock(ServerPlayer player) {
        if (!canManage(player) || owner() == null) {
            return false;
        }
        setLocked(!isLocked());
        TrailerEntity trailer = trailer();
        if (trailer != null && trailer.canManage(player)) {
            trailer.setLocked(isLocked()); // the rig locks as one
        }
        playSound(ModSounds.LOCK.get(), 0.7F, isLocked() ? 1.0F : 1.3F);
        player.displayClientMessage(Component.translatable(isLocked() ? "car.reikocars.now_locked" : "car.reikocars.now_unlocked"), true);
        return true;
    }

    // ---- passengers ----

    /** The passengers in seats: everyone but the trailer. */
    private List<Entity> riders() {
        return getPassengers().stream().filter(passenger -> !(passenger instanceof TrailerEntity)).toList();
    }

    /** The trailer this tractor is pulling, if any. */
    @Nullable
    public TrailerEntity trailer() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof TrailerEntity trailer) {
                return trailer;
            }
        }
        return null;
    }

    /** Where a trailer's kingpin sits: the top of the fifth wheel, behind the cab. */
    public Vec3 fifthWheel() {
        return position().add(type.fifthWheelOffset().yRot(-getYRot() * Mth.DEG_TO_RAD));
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof TrailerEntity) {
            return type.isTractor() && trailer() == null;
        }
        return riders().size() < type.seatCount();
    }

    /** The driver: whoever's in the first seat. */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        List<Entity> riders = riders();
        return !riders.isEmpty() && riders.get(0) instanceof Player player ? player : null;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        boolean hadDriver = getControllingPassenger() != null;
        super.addPassenger(passenger);
        if (!level().isClientSide && !hadDriver && getControllingPassenger() != null && hasFuel()) {
            if (isBrokenDown()) {
                playSound(ModSounds.BREAKDOWN.get(), 0.6F, 1.2F); // turns over, won't start
                tellDriver(brokenDownMessage());
            } else {
                playSound(ModSounds.ENGINE_START.get(), 1.0F, 1.0F);
            }
        }
        if (level().isClientSide && passenger instanceof Player player && player.isLocalPlayer()) {
            // Getting in, you face the windscreen (your client owns your view, so it's set here).
            player.setYRot(getYRot());
            player.yRotO = getYRot();
            player.setYHeadRot(getYRot());
            player.setXRot(Math.max(player.getXRot(), 5.0F));
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        int seat = Math.max(0, riders().indexOf(entity));
        return type.seatOffset(seat).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (passenger instanceof TrailerEntity trailer) {
            trailer.followTractor(this);
            return;
        }
        super.positionRider(passenger, callback);
        if (level().isClientSide) {
            passenger.setYRot(passenger.getYRot() + deltaRotation);
            passenger.setYHeadRot(passenger.getYHeadRot() + deltaRotation);
        }
        clampRotation(passenger);
    }

    /** Passengers face roughly forward, but can look over their shoulder. */
    protected void clampRotation(Entity passenger) {
        passenger.setYBodyRot(getYRot());
        float relative = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = Mth.clamp(relative, -120.0F, 120.0F);
        passenger.yRotO += clamped - relative;
        passenger.setYRot(passenger.getYRot() + clamped - relative);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        clampRotation(passenger);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Out of the door on the passenger's own side, then the other side, then anywhere around the car.
        int seat = riders().indexOf(passenger);
        double side = seat >= 0 && type.seatOffset(seat).x < 0 ? -1.0 : 1.0;
        double reach = type.width / 2.0 + passenger.getBbWidth() / 2.0 + 0.2;
        for (double sideSign : new double[] {side, -side}) {
            Vec3 offset = new Vec3(sideSign * reach, 0.0, 0.0).yRot(-getYRot() * Mth.DEG_TO_RAD);
            BlockPos pos = BlockPos.containing(getX() + offset.x, getBoundingBox().maxY, getZ() + offset.z);
            for (BlockPos candidate : new BlockPos[] {pos, pos.below(), pos.below(2)}) {
                double floor = level().getBlockFloorHeight(candidate);
                if (!DismountHelper.isBlockFloorValid(floor)) {
                    continue;
                }
                Vec3 spot = new Vec3(getX() + offset.x, candidate.getY() + floor, getZ() + offset.z);
                for (Pose pose : passenger.getDismountPoses()) {
                    if (DismountHelper.canDismountTo(level(), spot, passenger, pose)) {
                        passenger.setPose(pose);
                        return spot;
                    }
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // ---- input (set by the driver's client) ----

    public void setInput(boolean forward, boolean back, boolean left, boolean right, boolean brake) {
        this.inputForward = forward;
        this.inputBack = back;
        this.inputLeft = left;
        this.inputRight = right;
        this.inputBrake = brake;
    }

    // ---- ticking ----

    @Override
    public void tick() {
        if (getHurtTime() > 0) {
            setHurtTime(getHurtTime() - 1);
        }
        if (getDamage() > 0.0F) {
            setDamage(getDamage() - 1.0F);
        }
        float yawBefore = getYRot();
        super.tick();
        tickLerp();
        if (isPassenger()) {
            stopDead(); // carried on a car hauler: the hauler moves it
        } else if (isControlledByLocalInstance()) {
            if (getControllingPassenger() == null) {
                setInput(false, false, false, false, false);
            }
            drive();
            Vec3 before = position();
            boolean endsWereClear = !endsBlocked();
            // The pivot shift moves the car but isn't speed: it mustn't carry over into next tick's velocity.
            move(MoverType.SELF, getDeltaMovement().add(pivotShift));
            if (endsWereClear && endsBlocked()) {
                // A long vehicle's nose or tail ran into a wall: stop it there, as its hitbox alone wouldn't.
                noteImpact(Math.abs(speed));
                setPos(before.x, getY(), before.z);
                setYRot(yawBefore);
                speed = 0.0;
                setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
            }
            afterMove(before);
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        deltaRotation = Mth.wrapDegrees(getYRot() - yawBefore);
        if (level().isClientSide) {
            clientTick();
        } else {
            serverTick();
        }
        checkInsideBlocks();
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, lerpXRot);
            lerpSteps--;
        }
    }

    /** Works out this tick's speed, heading and velocity from the driver's keys and the ground under the car. */
    private void drive() {
        double speedScale = CarsConfig.get(CarsConfig.SPEED);
        BlockState ground = level().getBlockState(getBlockPosBelowThatAffectsMyMovement());
        float friction = ground.getFriction(level(), getBlockPosBelowThatAffectsMyMovement(), this);
        double grip = onGround() ? Mth.clamp((1.0 - friction) / 0.4, 0.1, 1.0) : 0.0;
        CarCondition wear = condition();
        double tires = wear.grip();
        double top = type.topSpeed * speedScale * surfaceFactor() * wear.power() * wear.drag();
        if (isInWater()) {
            top = Math.min(top, 0.1);
        }
        double accel = type.acceleration * speedScale * wear.power() * tires;
        double brakes = type.braking * wear.braking() * tires;
        TrailerEntity towed = trailer();
        if (towed != null) {
            top *= 0.9; // pulling a trailer: slower, and slower to get going
            accel *= 0.7;
            // A trailer's worn brakes and bald tires don't help stop the rig; a battered one drags.
            CarCondition trailerWear = towed.condition();
            brakes *= 0.4 + 0.6 * trailerWear.braking() * trailerWear.grip();
            top *= 0.5 + 0.5 * trailerWear.drag();
        }
        boolean engine = getControllingPassenger() != null && hasFuel() && !isBrokenDown();
        double before = speed;
        boolean braking = onGround() && ((inputForward && speed < 0) || (inputBack && speed > 0.001) || inputBrake);

        if (onGround()) {
            if (inputForward && engine) {
                speed = speed < 0 ? approach(speed, 0, brakes * grip) : speed + accel * grip * (1.0 - 0.5 * speed / top);
            } else if (inputBack) {
                if (speed > 0.001) {
                    speed = approach(speed, 0, brakes * grip);
                } else if (engine) {
                    speed = Math.max(-type.reverseSpeed * speedScale, speed - accel * 0.8 * grip);
                }
            } else {
                speed = approach(speed * 0.992, 0, 0.003 * Math.max(grip, 0.3));
            }
            if (inputBrake) {
                speed = approach(speed, 0, brakes * 1.5 * grip);
            }
            if (Math.abs(speed) > top) {
                speed = approach(speed, Math.copySign(top, speed), 0.03);
            }
        } else {
            speed *= 0.995;
        }
        if (braking && level().isClientSide) {
            noteBraking(Math.abs(before) - Math.abs(speed), wear);
        }

        float steerTarget = (inputLeft ? 1.0F : 0.0F) - (inputRight ? 1.0F : 0.0F);
        steering += (steerTarget - steering) * 0.3F;
        double pace = Math.abs(speed);
        double turnFactor = Mth.clamp(pace / 0.15, 0.0, 1.0) * (1.0 - 0.35 * Math.min(1.0, pace / type.topSpeed));
        float turn = (float) (steering * type.turnRate * turnFactor * Math.signum(speed) * (onGround() ? 1.0 : 0.2)
                * (0.75 + 0.25 * tires)); // bald tires don't bite into the turn
        Vec3 pivotBefore = forward().scale(-type.pivotBack());
        setYRot(getYRot() - turn);
        pivotShift = type.pivotBack() > 0.0 ? pivotBefore.subtract(forward().scale(-type.pivotBack())) : Vec3.ZERO;

        Vec3 forward = forward();
        Vec3 current = getDeltaMovement();
        double vy = isNoGravity() ? 0.0 : (current.y - 0.08) * 0.98;
        if (isInWater()) {
            vy *= 0.6;
        }
        // On ice the car keeps sliding the way it was going, instead of following its wheels.
        double vx = Mth.lerp(onGround() ? grip : 0.02, current.x, forward.x * speed);
        double vz = Mth.lerp(onGround() ? grip : 0.02, current.z, forward.z * speed);
        setDeltaMovement(vx, vy, vz);
    }

    /** Counts speed the brakes took off, for the wear report, and squeals if they're worn. Driver's client only. */
    private void noteBraking(double lost, CarCondition wear) {
        if (lost <= 0.0) {
            return;
        }
        brakeWork += lost;
        if (inputBrake) {
            skidWork += lost;
        }
        if (squealCooldown > 0) {
            squealCooldown--;
        } else if ((wear.brakes() < 0.25F || (trailer() != null && trailer().condition().brakes() < 0.25F)) && Math.abs(speed) > 0.15) {
            squealCooldown = 30;
            level().playLocalSound(getX(), getY(), getZ(), ModSounds.BRAKE_SQUEAL.get(), SoundSource.NEUTRAL, 0.8F, 1.0F, false);
        }
    }

    /** The car ran into something this tick at {@code impact} blocks per tick: reported to the server. */
    private void noteImpact(double impact) {
        if (level().isClientSide && getControllingPassenger() instanceof Player) {
            crashImpact = Math.max(crashImpact, impact);
        }
    }

    /** Stops the car where it is, for when it's carried or moved. */
    public void stopDead() {
        speed = 0.0;
        setDeltaMovement(Vec3.ZERO);
    }

    /**
     * Whether a long vehicle's nose or tail is inside a wall. Its hitbox is square, as every entity's is, so for the
     * semi it stops well short of the bumpers; these boxes cover the ends, from a block up (so it still climbs
     * curbs) to its roof. Ordinary cars are short enough that their hitbox does the job.
     */
    private boolean endsBlocked() {
        if (type.length < type.width + 2.0F) {
            return false;
        }
        double half = type.width * 0.45;
        double reach = type.length / 2.0 - half;
        Vec3 forward = forward();
        for (double sign : new double[] {1.0, -1.0}) {
            double x = getX() + forward.x * reach * sign;
            double z = getZ() + forward.z * reach * sign;
            AABB end = new AABB(x - half, getY() + maxUpStep() + 0.01, z - half, x + half, getY() + type.height, z + half);
            if (!level().noBlockCollision(this, end)) {
                return true;
            }
        }
        return false;
    }

    /** Hitting a wall stops the car instead of leaving it pushing forever at full speed. */
    private void afterMove(Vec3 before) {
        if (horizontalCollision) {
            double along = position().subtract(before).dot(forward());
            if (Math.abs(along) < Math.abs(speed)) {
                noteImpact(Math.abs(speed) - Math.abs(along));
                speed = along;
            }
        }
    }

    private double surfaceFactor() {
        if (type.offRoad) {
            return 1.0;
        }
        return isOffRoad() ? CarsConfig.get(CarsConfig.OFF_ROAD_SPEED) : 1.0;
    }

    /** On grass, dirt, sand, snow or the like, rather than a road. */
    private boolean isOffRoad() {
        return level().getBlockState(getOnPos()).is(OFF_ROAD) || level().getBlockState(blockPosition()).is(OFF_ROAD);
    }

    private static double approach(double value, double target, double step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    /** The way the car faces, flat. */
    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    private void serverTick() {
        if (hornCooldown > 0) {
            hornCooldown--;
        }
        if (isPassenger()) {
            lastFuelPos = null; // being carried, not driven: no fuel burned, nobody run over
            return;
        }
        Vec3 pos = position();
        double moved = lastFuelPos == null ? 0.0 : Math.sqrt(Mth.square(pos.x - lastFuelPos.x) + Mth.square(pos.z - lastFuelPos.z));
        if (moved > 8.0) {
            moved = 0.0; // teleported, not driven
        }
        lastFuelPos = pos;
        LivingEntity driver = getControllingPassenger();
        double load = trailer() != null ? 1.4 : 1.0;
        if (driver != null && fuel > 0 && !isBrokenDown()) {
            double burn = moved * type.fuelPerBlock * load * CarsConfig.get(CarsConfig.FUEL_USE) * condition.fuelUse()
                    + CarsConfig.get(CarsConfig.IDLE_FUEL_USE);
            setFuel(fuel - burn);
            if (fuel <= 0 && driver instanceof Player player) {
                player.displayClientMessage(Component.translatable("car.reikocars.out_of_fuel"), true);
            }
        }
        if (driver != null && moved > 0.0) {
            double tires = isOffRoad() ? (type.offRoad ? 1.5 : 2.5) : 1.0;
            setCondition(condition.afterDriving(moved, tires, load, CarsConfig.get(CarsConfig.WEAR)));
        }
        if (moved > 0.3) {
            runOver(driver, moved);
        }
    }

    /** Hurts and shoves mobs in the car's way, scaled by how fast it's going. Players only with PvP on. */
    private void runOver(@Nullable LivingEntity driver, double moved) {
        double perSpeed = CarsConfig.get(CarsConfig.RUN_OVER_DAMAGE);
        if (perSpeed <= 0) {
            return;
        }
        Vec3 heading = forward().scale(Math.signum(speedAlong(moved)));
        AABB box = getBoundingBox().inflate(0.1, 0.0, 0.1).expandTowards(heading.scale(0.6));
        List<LivingEntity> hit = level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive()
                && !entity.isPassengerOfSameVehicle(this) && entity.getVehicle() != this && !entity.isSpectator());
        if (hit.isEmpty()) {
            return;
        }
        DamageSource source = level().damageSources().source(RUN_OVER, this, driver);
        for (LivingEntity entity : hit) {
            if (entity instanceof Player player && (player.isCreative() || level().getServer() == null
                    || !level().getServer().isPvpAllowed())) {
                continue;
            }
            if (driver != null && entity instanceof OwnableEntity pet && driver.getUUID().equals(pet.getOwnerUUID())) {
                continue;
            }
            if (entity.hurt(source, (float) (moved * perSpeed))) {
                entity.push(heading.x * moved * 0.8, 0.25 + moved * 0.2, heading.z * moved * 0.8);
                entity.hurtMarked = true;
                setCondition(condition.afterHitting(moved, CarsConfig.get(CarsConfig.CRASH_DAMAGE)));
            }
        }
    }

    /** Whether this tick's movement went forward (positive) or backward (negative). */
    private double speedAlong(double moved) {
        Vec3 delta = position().subtract(xo, yo, zo);
        double along = delta.dot(forward());
        return along == 0.0 ? moved : along;
    }

    private void clientTick() {
        Vec3 delta = position().subtract(xo, yo, zo);
        measuredSpeed = delta.dot(forward());
        wheelAngleO = wheelAngle;
        wheelAngle += (float) (measuredSpeed / type.wheelRadius());
        steerAngleO = steerAngle;
        float target;
        if (isControlledByLocalInstance()) {
            target = steering * 0.5F;
        } else {
            // Other players' cars: guess the steering from how fast the car is turning.
            target = Math.abs(measuredSpeed) < 0.01 ? steerAngle : (float) Mth.clamp(-deltaRotation / type.turnRate * 0.5F, -0.5F, 0.5F);
        }
        steerAngle += (target - steerAngle) * 0.4F;
        leanO = lean;
        if (type.isBike()) {
            // Into the turn, more the faster it's going; parked with nobody on, over on its kickstand.
            float leanTarget = riders().isEmpty() ? -KICKSTAND_LEAN
                    : -steerAngle / 0.5F * MAX_LEAN * (float) Math.min(1.0, Math.abs(measuredSpeed) / 0.35);
            lean += (leanTarget - lean) * 0.25F;
        }

        engineSmoke();
        if (isEngineRunning() && tickCount % 3 == 0) {
            if (!type.exhaustStacks().isEmpty()) {
                // Out of the tops of its exhaust stacks (the semi's behind the cab, the tractor's on the hood).
                for (Vec3 local : type.exhaustStacks()) {
                    Vec3 stack = local.yRot(-getYRot() * Mth.DEG_TO_RAD);
                    level().addParticle(ParticleTypes.SMOKE, getX() + stack.x, getY() + stack.y, getZ() + stack.z, 0.0, 0.06, 0.0);
                }
            } else {
                Vec3 tail = forward().scale(-type.length / 2.0 - 0.1);
                Vec3 side = new Vec3(type.isBike() ? -0.25 : -0.35, 0, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
                level().addParticle(ParticleTypes.SMOKE, getX() + tail.x + side.x, getY() + 0.3, getZ() + tail.z + side.z,
                        tail.x * 0.02, 0.02, tail.z * 0.02);
            }
        }
    }

    /**
     * A tired engine smokes from under the bonnet while it runs, more as it gets worse; one run dry of oil smokes
     * too, and so does a broken-down one with somebody at the wheel.
     */
    private void engineSmoke() {
        CarCondition wear = condition();
        boolean running = isEngineRunning();
        boolean dead = getControllingPassenger() != null && isBrokenDown() && wear.engine() <= 0.0F;
        if (!(running && (wear.engine() < 0.3F || wear.oil() <= 0.0F)) && !dead) {
            return;
        }
        boolean bad = wear.engine() < 0.1F;
        if (tickCount % (dead ? 8 : bad ? 2 : 5) != 0) {
            return;
        }
        // Over the engine: under the bonnet, the bus's at its back, a motorcycle's low in the middle.
        double along = type.isBike() ? 0.05 : type == CarType.CITY_BUS ? -type.length * 0.42 : type.length * 0.32;
        Vec3 at = forward().scale(along);
        double up = type.isBike() ? 0.55 : Math.min(type.height * 0.55, 1.4);
        var random = level().getRandom();
        level().addParticle(bad || dead ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                getX() + at.x + (random.nextDouble() - 0.5) * type.width * 0.5, getY() + up,
                getZ() + at.z + (random.nextDouble() - 0.5) * type.width * 0.5, 0.0, 0.04 + random.nextDouble() * 0.03, 0.0);
    }

    /** Someone's driving, there's fuel and nothing's worn out. */
    public boolean isEngineRunning() {
        return getControllingPassenger() != null && hasFuel() && !isBrokenDown();
    }

    /** Speed along the heading in blocks per tick, as seen on this client. */
    public double displaySpeed() {
        return measuredSpeed;
    }

    public float wheelAngle(float partialTick) {
        return Mth.lerp(partialTick, wheelAngleO, wheelAngle);
    }

    /** A motorcycle's lean in degrees, positive to its right; 0 for a car. */
    public float lean(float partialTick) {
        return Mth.lerp(partialTick, leanO, lean);
    }

    /** Front wheels' steering angle in radians, positive to the left. */
    public float steerAngle(float partialTick) {
        return Mth.lerp(partialTick, steerAngleO, steerAngle);
    }

    // ---- syncing position with the server ----

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
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
    public float lerpTargetXRot() {
        return lerpSteps > 0 ? (float) lerpXRot : getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? (float) lerpYRot : getYRot();
    }

    // ---- physical properties ----

    /** Climbs a full block, so curbs, slabs and stairs don't stop it. */
    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /** The semi is longer than its hitbox: keep drawing it while its ends could be on screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(type.length / 2.0, 0.5, type.length / 2.0);
    }

    /** Solid to other vehicles; drives through mobs (and hurts them) instead of stopping at them. */
    @Override
    public boolean canCollideWith(Entity entity) {
        return entity.canBeCollidedWith() && !isPassengerOfSameVehicle(entity);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        fallDistance = 0.0F; // cars land on their suspension; the people inside don't take the fall either
    }

    // ---- damage and dropping ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        if (!(source.getEntity() instanceof Player player)) {
            return false; // only players can break cars: no losing one to a creeper or lava
        }
        if (!canManage(player)) {
            player.displayClientMessage(Component.translatable("car.reikocars.not_yours", ownerName), true);
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
        if (!level().isClientSide && reason.shouldDestroy() && type.storageRows > 0) {
            Containers.dropContents(level(), this, bed);
        }
        super.remove(reason);
    }

    /** This car as an item, keeping its fuel, wear, paint and name. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(getDropItem());
        CarItem.withFuel(stack, fuel());
        CarItem.withCondition(stack, condition());
        if (color() != type.defaultColor) {
            stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new DyedItemColor(color(), false));
        }
        if (hasCustomName()) {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, getCustomName());
        }
        return stack;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.car(type);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }

    // ---- saving ----

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("Fuel", fuel);
        tag.putInt("Color", color());
        tag.putBoolean("Locked", isLocked());
        if (owner() != null) {
            tag.putUUID("Owner", owner());
            tag.putString("OwnerName", ownerName);
        }
        if (type.storageRows > 0) {
            tag.put("Bed", bed.createTag(registryAccess()));
        }
        if (!condition.isNew()) {
            CarCondition.CODEC.encodeStart(NbtOps.INSTANCE, condition).result().ifPresent(saved -> tag.put("Condition", saved));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setFuel(tag.getDouble("Fuel"));
        entityData.set(DATA_FUEL, (float) fuel);
        if (tag.contains("Color", Tag.TAG_INT)) {
            setColor(tag.getInt("Color"));
        }
        setLocked(tag.getBoolean("Locked"));
        entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        ownerName = tag.getString("OwnerName");
        if (type.storageRows > 0 && tag.contains("Bed", Tag.TAG_LIST)) {
            bed.fromTag(tag.getList("Bed", Tag.TAG_COMPOUND), registryAccess());
        }
        condition = tag.contains("Condition") ? CarCondition.CODEC.parse(NbtOps.INSTANCE, tag.get("Condition")).result().orElse(CarCondition.NEW)
                : CarCondition.NEW;
        entityData.set(DATA_CONDITION, condition);
    }
}
