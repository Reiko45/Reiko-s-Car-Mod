package com.reiko.cars.trailer;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.registry.ModSounds;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
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
 */
public class TrailerEntity extends VehicleEntity {
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LOCKED = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_FLUID = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLUID_AMOUNT = SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);

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
    /** Where the axles are, on the ground, while hitched. Null until the trailer has been placed behind a tractor. */
    @Nullable
    private Vec3 axle;

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
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_FLUID, 0);
        builder.define(DATA_FLUID_AMOUNT, 0);
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

    /** Who may open, hitch or load it: anyone unless it's locked, and then its owner and admins. */
    public boolean canBeUsedBy(Player player) {
        return !isLocked() || owner() == null || isOwner(player) || player.hasPermissions(2);
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
        // No folding back on itself: a trailer can only swing so far round.
        float relative = Mth.wrapDegrees(yaw - tractor.getYRot());
        if (Math.abs(relative) > TrailerType.MAX_ARTICULATION) {
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
        if (level().isClientSide) {
            spinWheels(position().subtract(before));
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
        return tractor != null && tractor.isEngineRunning();
    }

    // ---- cars riding a car hauler ----

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return type.carSlots > 0 && passenger instanceof CarEntity car && !car.carType().isTractor() && cars().size() < type.carSlots;
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
        if (player.isSecondaryUseActive() && held.isEmpty()) {
            if (owner() == null || !canManage(player)) {
                return refuse(player, Component.translatable("trailer.reikocars.not_yours", ownerName));
            }
            if (!level().isClientSide) {
                setLocked(!isLocked());
                playSound(ModSounds.LOCK.get(), 0.7F, isLocked() ? 1.0F : 1.3F);
                player.displayClientMessage(Component.translatable(isLocked() ? "trailer.reikocars.now_locked" : "trailer.reikocars.now_unlocked"), true);
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

    /** This trailer as an item, keeping its paint, name and (for a tanker) load. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(getDropItem());
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
        if (type.cargoRows > 0) {
            tag.put("Cargo", cargo.createTag(registryAccess()));
        }
        if (type.tankBuckets > 0) {
            tag.put("Tank", tank.writeToNBT(registryAccess(), new CompoundTag()));
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
        if (type.cargoRows > 0 && tag.contains("Cargo", Tag.TAG_LIST)) {
            cargo.fromTag(tag.getList("Cargo", Tag.TAG_COMPOUND), registryAccess());
        }
        if (type.tankBuckets > 0 && tag.contains("Tank", Tag.TAG_COMPOUND)) {
            tank.readFromNBT(registryAccess(), tag.getCompound("Tank"));
            syncFluid();
        }
    }
}
