package de.mennomax.astikorcarts.entity;

import de.mennomax.astikorcarts.AstikorCarts;
import de.mennomax.astikorcarts.config.AstikorCartsConfig;
import de.mennomax.astikorcarts.network.clientbound.UpdateDrawnMessage;
import de.mennomax.astikorcarts.util.CartWheel;
import de.mennomax.astikorcarts.world.AstikorWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.IEquipable;
import net.minecraft.entity.IRideable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.HandSide;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.TransportationHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class AbstractDrawnEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final DataParameter<Integer> TIME_SINCE_HIT = EntityDataManager.defineId(AbstractDrawnEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> FORWARD_DIRECTION = EntityDataManager.defineId(AbstractDrawnEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> DAMAGE_TAKEN = EntityDataManager.defineId(AbstractDrawnEntity.class, DataSerializers.FLOAT);
    private static final UUID PULL_SLOWLY_MODIFIER_UUID = UUID.fromString("49B0E52E-48F2-4D89-BED7-4F5DF26F1263");
    private static final UUID PULL_MODIFIER_UUID = UUID.fromString("BA594616-5BE3-46C6-8B40-7D0230C64B77");
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYaw;
    private double lerpPitch;
    protected List<CartWheel> wheels;
    private int pullingId = -1;
    private UUID pullingUUID = null;
    protected double spacing = 1.7D;
    public Entity pulling;
    protected AbstractDrawnEntity drawn;

    public AbstractDrawnEntity(final EntityType<? extends Entity> entityTypeIn, final World worldIn) {
        super(entityTypeIn, worldIn);
        this.maxUpStep = 1.2F;
        this.blocksBuilding = true;
        this.initWheels();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public AxisAlignedBB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(3.0D, 3.0D, 3.0D);
    }

    @Override
    public void tick() {
        if (this.getTimeSinceHit() > 0) {
            this.setTimeSinceHit(this.getTimeSinceHit() - 1);
        }
        if (!this.isNoGravity()) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y - 0.08D, 0.0D);
        }
        if (this.getDamageTaken() > 0.0F) {
            this.setDamageTaken(this.getDamageTaken() - 1.0F);
        }
        super.tick();
        this.tickLerp();
        if (this.pulling == null) {
            this.xRot = 25.0F;
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.attemptReattach();
        }
        for (final Entity entity : this.level.getEntities(this, this.getBoundingBox(), EntityPredicates.pushableBy(this))) {
            this.push(entity);
        }
    }

    /**
     * This method is called for every cart that is being pulled by another entity
     * after all other
     * entities have been ticked to ensure that the cart always behaves the same
     * when being pulled. (unless this cart is being pulled by another cart)
     */
    public void pulledTick() {
        if (this.pulling == null) {
            return;
        }
        Vector3d targetVec = this.getRelativeTargetVec(1.0F);
        this.handleRotation(targetVec);
        while (this.yRot - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }
        while (this.yRot - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
        if (this.pulling.isOnGround()) {
            targetVec = new Vector3d(targetVec.x, 0.0D, targetVec.z);
        }
        final double targetVecLength = targetVec.length();
        final double r = 0.2D;
        final double relativeSpacing = Math.max(this.spacing + 0.5D * this.pulling.getBbWidth(), 1.0D);
        final double diff = targetVecLength - relativeSpacing;
        final Vector3d move;
        if (Math.abs(diff) < r) {
            move = this.getDeltaMovement();
        } else {
            move = this.getDeltaMovement().add(targetVec.subtract(targetVec.normalize().scale(relativeSpacing + r * Math.signum(diff))));
        }
        this.onGround = true;
        final double startX = this.getX();
        final double startY = this.getY();
        final double startZ = this.getZ();
        this.move(MoverType.SELF, move);
        if (!this.isAlive()) {
            return;
        }
        this.addStats(this.getX() - startX, this.getY() - startY, this.getZ() - startZ);
        if (this.level.isClientSide) {
            for (final CartWheel wheel : this.wheels) {
                wheel.tick();
            }
        } else {
            targetVec = this.getRelativeTargetVec(1.0F);
            if (targetVec.length() > relativeSpacing + 1.0D) {
                this.setPulling(null);
            }
        }
        this.updatePassengers();
        if (this.drawn != null) {
            this.drawn.pulledTick();
        }
    }

    private void addStats(final double x, final double y, final double z) {
        if (!this.level.isClientSide) {
            final int cm = Math.round(MathHelper.sqrt(x * x + y * y + z * z) * 100.0F);
            if (cm > 0) {
                for (final Entity passenger : this.getPassengers()) {
                    if (passenger instanceof PlayerEntity) {
                        ((PlayerEntity) passenger).awardStat(AstikorCarts.Stats.CART_ONE_CM, cm);
                    }
                }
            }
        }
    }

    public void initWheels() {
        this.wheels = Arrays.asList(new CartWheel(this, 0.9F), new CartWheel(this, -0.9F));
    }

    /**
     * @return Whether the currently pulling entity should stop pulling this cart.
     */
    public boolean shouldRemovePulling() {
        if (this.horizontalCollision) {
            this.position().add(0.0D, this.getEyeHeight(), 0.0D);
            final Vector3d start = new Vector3d(this.getX(), this.getY() + this.getBbHeight(), this.getZ());
            final Vector3d end = new Vector3d(this.pulling.getX(), this.pulling.getY() + this.pulling.getBbHeight() / 2, this.pulling.getZ());
            final RayTraceResult result = this.level.clip(new RayTraceContext(start, end, BlockMode.COLLIDER, FluidMode.NONE, this));
            return result.getType() == Type.BLOCK;
        }
        return false;
    }

    public void updatePassengers() {
        for (final Entity passenger : this.getPassengers()) {
            this.positionRider(passenger);
        }
    }

    @Nullable
    public Entity getPulling() {
        return this.pulling;
    }

    /**
     * Attaches the cart to an entity so that the cart follows it.
     *
     * @param entityIn new pulling entity
     */
    public void setPulling(final Entity entityIn) {
        if (!this.level.isClientSide) {
            if (this.canBePulledBy(entityIn)) {
                if (entityIn == null) {
                    if (this.pulling instanceof LivingEntity) {
                        final ModifiableAttributeInstance attr = ((LivingEntity) this.pulling).getAttribute(Attributes.MOVEMENT_SPEED);
                        if (attr != null) {
                            attr.removeModifier(PULL_SLOWLY_MODIFIER_UUID);
                            attr.removeModifier(PULL_MODIFIER_UUID);
                        }
                    } else if (this.pulling instanceof AbstractDrawnEntity) {
                        ((AbstractDrawnEntity) this.pulling).drawn = null;
                    }
                    AstikorCarts.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new UpdateDrawnMessage(-1, this.getId()));
                    this.pullingUUID = null;
                    if (this.tickCount > 20) {
                        this.playDetachSound();
                    }
                } else {
                    if (entityIn instanceof LivingEntity && this.getConfig().pullSpeed.get() != 0.0D) {
                        final ModifiableAttributeInstance attr = ((LivingEntity) entityIn).getAttribute(Attributes.MOVEMENT_SPEED);
                        if (attr != null && attr.getModifier(PULL_MODIFIER_UUID) == null) {
                            attr.addTransientModifier(new AttributeModifier(
                                PULL_MODIFIER_UUID,
                                "Pull modifier",
                                this.getConfig().pullSpeed.get(),
                                AttributeModifier.Operation.MULTIPLY_TOTAL
                            ));
                        }
                    }
                    if (entityIn instanceof MobEntity) {
                        ((MobEntity) entityIn).getNavigation().stop();
                    }
                    AstikorCarts.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new UpdateDrawnMessage(entityIn.getId(), this.getId()));
                    this.pullingUUID = entityIn.getUUID();
                    if (this.tickCount > 20) {
                        this.playAttachSound();
                    }
                }
                if (entityIn instanceof AbstractDrawnEntity) {
                    ((AbstractDrawnEntity) entityIn).drawn = this;
                }
                this.pulling = entityIn;
                AstikorWorld.get(this.level).ifPresent(w -> w.addPulling(this));

            }
        } else {
            if (entityIn == null) {
                this.pullingId = -1;
                for (final CartWheel wheel : this.wheels) {
                    wheel.clearIncrement();
                }
                if (this.pulling instanceof AbstractDrawnEntity) {
                    ((AbstractDrawnEntity) this.pulling).drawn = null;
                }
            } else {
                this.pullingId = entityIn.getId();
                if (entityIn instanceof AbstractDrawnEntity) {
                    ((AbstractDrawnEntity) entityIn).drawn = this;
                }
            }
            this.pulling = entityIn;
            AstikorWorld.get(this.level).ifPresent(w -> w.addPulling(this));
        }
    }

    private void playAttachSound() {
        this.playSound(AstikorCarts.SoundEvents.CART_ATTACHED.get(), 0.2F, 1.0F);
    }

    private void playDetachSound() {
        this.playSound(AstikorCarts.SoundEvents.CART_DETACHED.get(), 0.2F, 1.0F);
    }

    /**
     * Attempts to reattach the cart to the last pulling entity.
     */
    private void attemptReattach() {
        if (this.level.isClientSide) {
            if (this.pullingId != -1) {
                final Entity entity = this.level.getEntity(this.pullingId);
                if (entity != null && entity.isAlive()) {
                    this.setPulling(entity);
                }
            }
        } else {
            if (this.pullingUUID != null) {
                final Entity entity = ((ServerWorld) this.level).getEntity(this.pullingUUID);
                if (entity != null && entity.isAlive()) {
                    this.setPulling(entity);
                }
            }
        }
    }

    public boolean shouldStopPulledTick() {
        if (!this.isAlive() || this.getPulling() == null || !this.getPulling().isAlive() || this.getPulling().isPassenger()) {
            if (this.pulling != null && this.pulling instanceof PlayerEntity) {
                this.setPulling(null);
            } else {
                this.pulling = null;
            }
            return true;
        } else if (!this.level.isClientSide && this.shouldRemovePulling()) {
            this.setPulling(null);
            return true;
        }
        return false;
    }

    /**
     * @return The position this cart should always face and travel towards.
     * Relative to the cart position.
     * @param delta
     */
    public Vector3d getRelativeTargetVec(final float delta) {
        final double x;
        final double y;
        final double z;
        if (delta == 1.0F) {
            x = this.pulling.getX() - this.getX();
            y = this.pulling.getY() - this.getY();
            z = this.pulling.getZ() - this.getZ();
        } else {
            x = MathHelper.lerp(delta, this.pulling.xOld, this.pulling.getX()) - MathHelper.lerp(delta, this.xOld, this.getX());
            y = MathHelper.lerp(delta, this.pulling.yOld, this.pulling.getY()) - MathHelper.lerp(delta, this.yOld, this.getY());
            z = MathHelper.lerp(delta, this.pulling.zOld, this.pulling.getZ()) - MathHelper.lerp(delta, this.zOld, this.getZ());
        }
        final float yaw = (float) Math.toRadians(this.pulling.yRot);
        final float nx = -MathHelper.sin(yaw);
        final float nz = MathHelper.cos(yaw);
        final double r = 0.2D;
        return new Vector3d(x + nx * r, y, z + nz * r);
    }

    /**
     * Handles the rotation of this cart and its components.
     *
     * @param target
     */
    public void handleRotation(final Vector3d target) {
        this.yRot = getYaw(target);
        this.xRot = getPitch(target);
    }

    public static float getYaw(final Vector3d vec) {
        return MathHelper.wrapDegrees((float) Math.toDegrees(-MathHelper.atan2(vec.x, vec.z)));
    }

    public static float getPitch(final Vector3d vec) {
        return MathHelper.wrapDegrees((float) Math.toDegrees(-MathHelper.atan2(vec.y, MathHelper.sqrt(vec.x * vec.x + vec.z * vec.z))));
    }

    public double getWheelRotation(final int wheel) {
        return this.wheels.get(wheel).getRotation();
    }

    public double getWheelRotationIncrement(final int wheel) {
        return this.wheels.get(wheel).getRotationIncrement();
    }

    public abstract Item getCartItem();

    /**
     * Returns true if the passed in entity is allowed to pull this cart.
     *
     * @param entityIn
     */
    protected boolean canBePulledBy(final Entity entityIn) {
        if (this.level.isClientSide) {
            return true;
        }
        if (entityIn == null) {
            return true;
        }
        return (this.pulling == null || !this.pulling.isAlive()) && !this.hasPassenger(entityIn) && this.canPull(entityIn);
    }

    private boolean canPull(final Entity entity) {
        // saddleable
        if (entity instanceof IEquipable && !((IEquipable) entity).isSaddleable()) return false;
        if (entity instanceof TameableEntity && !((TameableEntity) entity).isTame()) return false;
        final ArrayList<String> allowed = this.getConfig().pullAnimals.get();
        if (allowed.isEmpty()) {
            return entity instanceof PlayerEntity ||
                // real semantics = can wear saddle and not steered by item
                entity instanceof IEquipable && !(entity instanceof IRideable);
        }
        return allowed.contains(EntityType.getKey(entity.getType()).toString());
    }

    protected abstract AstikorCartsConfig.CartConfig getConfig();

    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level.isClientSide && this.isAlive()) {
            if (source == DamageSource.CACTUS) {
                return false;
            }
            if (source instanceof IndirectEntityDamageSource && source.getEntity() != null && this.hasPassenger(source.getEntity())) {
                return false;
            }
            this.setForwardDirection(-this.getForwardDirection());
            this.setTimeSinceHit(10);
            this.setDamageTaken(this.getDamageTaken() + amount * 10.0F);
            final boolean flag = source.getEntity() instanceof PlayerEntity && ((PlayerEntity) source.getEntity()).abilities.instabuild;
            if (flag || this.getDamageTaken() > 40.0F) {
                this.onDestroyed(source, flag);
                this.setPulling(null);
                this.remove();
            }
            return true;
        }
        return false;
    }

    /**
     * Called when the cart has been destroyed by a creative player or the carts
     * health hit 0.
     *
     * @param source
     * @param byCreativePlayer
     */
    public void onDestroyed(final DamageSource source, final boolean byCreativePlayer) {
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            if (!byCreativePlayer) {
                this.spawnAtLocation(this.getCartItem());
            }
            this.onDestroyedAndDoDrops(source);
        }
    }

    /**
     * This method is called from {@link #onDestroyed(DamageSource, boolean)} if the
     * GameRules allow entities to drop items.
     *
     * @param source
     */
    public void onDestroyedAndDoDrops(final DamageSource source) {

    }

    private void tickLerp() {
        if (this.lerpSteps > 0) {
            final double dx = (this.lerpX - this.getX()) / this.lerpSteps;
            final double dy = (this.lerpY - this.getY()) / this.lerpSteps;
            final double dz = (this.lerpZ - this.getZ()) / this.lerpSteps;
            this.yRot = (float) (this.yRot + MathHelper.wrapDegrees(this.lerpYaw - this.yRot) / this.lerpSteps);
            this.xRot = (float) (this.xRot + (this.lerpPitch - this.xRot) / this.lerpSteps);
            this.lerpSteps--;
            this.onGround = true;
            this.move(MoverType.SELF, new Vector3d(dx, dy, dz));
            this.setRot(this.yRot, this.xRot);
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void lerpTo(final double x, final double y, final double z, final float yaw, final float pitch, final int posRotationIncrements, final boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpPitch = pitch;
        this.lerpSteps = posRotationIncrements;
    }

    @Override
    protected void addPassenger(final Entity passenger) {
        super.addPassenger(passenger);
        if (this.isControlledByLocalInstance() && this.lerpSteps > 0) {
            this.lerpSteps = 0;
            this.absMoveTo(this.lerpX, this.lerpY, this.lerpZ, (float) this.lerpYaw, (float) this.lerpPitch);
        }
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        final List<Entity> passengers = this.getPassengers();
        if (passengers.isEmpty()) {
            return null;
        }
        final Entity first = passengers.get(0);
        if (first instanceof AnimalEntity || !(first instanceof LivingEntity)) {
            return null;
        }
        return (LivingEntity) first;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return false;
    }

    @Override
    public Vector3d getDismountLocationForPassenger(final LivingEntity rider) {
        for (final float angle : rider.getMainArm() == HandSide.RIGHT ? new float[] { 90.0F, -90.0F } : new float[] { -90.0F, 90.0F }) {
            final Vector3d pos = this.dismount(getCollisionHorizontalEscapeVector(this.getBbWidth(), rider.getBbWidth(), this.yRot + angle), rider);
            if (pos != null) return pos;
        }
        return this.position();
    }

    private Vector3d dismount(final Vector3d dir, LivingEntity rider) {
        final double x = this.getX() + dir.x;
        final double y = this.getBoundingBox().minY;
        final double z = this.getZ() + dir.z;
        final double limit = this.getBoundingBox().maxY + 0.75D;
        final BlockPos.Mutable blockPos = new BlockPos.Mutable();
        for (final Pose pose : rider.getDismountPoses()) {
            blockPos.set(x, y, z);
            while (blockPos.getY() < limit) {
                final double ground = this.level.getBlockFloorHeight(blockPos);
                if (blockPos.getY() + ground > limit) break;
                if (TransportationHelper.isBlockFloorValid(ground)) {
                    final Vector3d pos = new Vector3d(x, blockPos.getY() + ground, z);
                    if (TransportationHelper.canDismountTo(this.level, rider, rider.getLocalBoundsForPose(pose).move(pos))) {
                        rider.setPose(pose);
                        return pos;
                    }
                }
                blockPos.move(Direction.UP);
            }
        }
        return null;
    }

    public void setDamageTaken(final float damageTaken) {
        this.entityData.set(DAMAGE_TAKEN, damageTaken);
    }

    public float getDamageTaken() {
        return this.entityData.get(DAMAGE_TAKEN);
    }

    public void setTimeSinceHit(final int timeSinceHit) {
        this.entityData.set(TIME_SINCE_HIT, timeSinceHit);
    }

    public int getTimeSinceHit() {
        return this.entityData.get(TIME_SINCE_HIT);
    }

    public void setForwardDirection(final int forwardDirection) {
        this.entityData.set(FORWARD_DIRECTION, forwardDirection);
    }

    public int getForwardDirection() {
        return this.entityData.get(FORWARD_DIRECTION);
    }

    @Override
    public ItemStack getPickedResult(final RayTraceResult target) {
        return new ItemStack(this.getCartItem());
    }

    @Override
    public void writeSpawnData(final PacketBuffer buffer) {
        buffer.writeInt(this.pulling != null ? this.pulling.getId() : -1);
    }

    @Override
    public void readSpawnData(final PacketBuffer additionalData) {
        this.pullingId = additionalData.readInt();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TIME_SINCE_HIT, 0);
        this.entityData.define(FORWARD_DIRECTION, 1);
        this.entityData.define(DAMAGE_TAKEN, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundNBT compound) {
        if (compound.hasUUID("PullingUUID")) {
            this.pullingUUID = compound.getUUID("PullingUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(final CompoundNBT compound) {
        if (this.pulling != null) {
            compound.putUUID("PullingUUID", this.pullingUUID);
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public RenderInfo getInfo(final float delta) {
        return new RenderInfo(delta);
    }

    public void toggleSlow() {
        final Entity pulling = this.pulling;
        if (!(pulling instanceof LivingEntity)) return;
        final ModifiableAttributeInstance speed = ((LivingEntity) pulling).getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        final AttributeModifier modifier = speed.getModifier(PULL_SLOWLY_MODIFIER_UUID);
        if (modifier == null) {
            speed.addTransientModifier(new AttributeModifier(
                PULL_SLOWLY_MODIFIER_UUID,
                "Pull slowly modifier",
                this.getConfig().slowSpeed.get(),
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        } else {
            speed.removeModifier(modifier);
        }
    }

    public class RenderInfo {
        final float delta;
        Vector3d target;
        float yaw = Float.NaN;
        float pitch = Float.NaN;

        public RenderInfo(final float delta) {
            this.delta = delta;
        }

        public Vector3d getTarget() {
            if (this.target == null) {
                if (AbstractDrawnEntity.this.pulling == null) {
                    this.target = AbstractDrawnEntity.this.getViewVector(this.delta);
                } else {
                    this.target = AbstractDrawnEntity.this.getRelativeTargetVec(this.delta);
                }
            }
            return this.target;
        }

        public float getYaw() {
            if (Float.isNaN(this.yaw)) {
                if (AbstractDrawnEntity.this.pulling == null) {
                    this.yaw = MathHelper.lerp(this.delta, AbstractDrawnEntity.this.yRotO, AbstractDrawnEntity.this.yRot);
                } else {
                    this.yaw = AbstractDrawnEntity.getYaw(this.getTarget());
                }
            }
            return this.yaw;
        }

        public float getPitch() {
            if (Float.isNaN(this.pitch)) {
                if (AbstractDrawnEntity.this.pulling == null) {
                    this.pitch = MathHelper.lerp(this.delta, AbstractDrawnEntity.this.xRotO, AbstractDrawnEntity.this.xRot);
                } else {
                    this.pitch = AbstractDrawnEntity.getPitch(this.target);
                }
            }
            return this.pitch;
        }
    }
}
