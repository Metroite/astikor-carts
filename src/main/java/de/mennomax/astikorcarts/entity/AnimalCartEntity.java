package de.mennomax.astikorcarts.entity;

import de.mennomax.astikorcarts.AstikorCarts;
import de.mennomax.astikorcarts.config.AstikorCartsConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class AnimalCartEntity extends AbstractDrawnEntity {
    public AnimalCartEntity(final EntityType<? extends Entity> entityTypeIn, final World worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected AstikorCartsConfig.CartConfig getConfig() {
        return AstikorCartsConfig.get().animalCart;
    }

    @Override
    public void tick() {
        super.tick();
        final Entity coachman = this.getControllingPassenger();
        final Entity pulling = this.getPulling();
        if (pulling != null && coachman != null && pulling.getControllingPassenger() == null) {
            final PostilionEntity postilion = AstikorCarts.EntityTypes.POSTILION.get().create(this.level);
            if (postilion != null) {
                postilion.absMoveTo(pulling.getX(), pulling.getY(), pulling.getZ(), coachman.yRot, coachman.xRot);
                if (postilion.startRiding(pulling)) {
                    this.level.addFreshEntity(postilion);
                } else {
                    postilion.remove();
                }
            }
        }
    }

    @Override
    public ActionResultType interact(final PlayerEntity player, final Hand hand) {
        if (player.isSecondaryUseActive()) {
            if (!this.level.isClientSide) {
                for (final Entity entity : this.getPassengers()) {
                    if (!(entity instanceof PlayerEntity)) {
                        entity.stopRiding();
                    }
                }
            }
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        } else if (this.getPulling() != player) {
            if (!this.canAddPassenger(player)) {
                return ActionResultType.PASS;
            }
            if (!this.level.isClientSide) {
                return player.startRiding(this) ? ActionResultType.CONSUME : ActionResultType.PASS;
            }
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public void push(final Entity entityIn) {
        if (!entityIn.hasPassenger(this)) {
            if (!this.level.isClientSide && this.getPulling() != entityIn && this.getControllingPassenger() == null && this.getPassengers().size() < 2 && !entityIn.isPassenger() && entityIn.getBbWidth() < this.getBbWidth() && entityIn instanceof LivingEntity
                && !(entityIn instanceof WaterMobEntity) && !(entityIn instanceof PlayerEntity)) {
                entityIn.startRiding(this);
            } else {
                super.push(entityIn);
            }
        }
    }

    @Override
    protected boolean canAddPassenger(final Entity passenger) {
        return this.getPassengers().size() < 2;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 11.0D / 16.0D;
    }

    @Override
    public void positionRider(final Entity passenger) {
        if (this.hasPassenger(passenger)) {
            double f = -0.1D;

            if (this.getPassengers().size() > 1) {
                f = this.getPassengers().indexOf(passenger) == 0 ? 0.2D : -0.6D;

                if (passenger instanceof AnimalEntity) {
                    f += 0.2D;
                }
            }

            final Vector3d forward = this.getLookAngle();
            final Vector3d origin = new Vector3d(0.0D, this.getPassengersRidingOffset(), 1.0D / 16.0D);
            final Vector3d pos = origin.add(forward.scale(f + MathHelper.sin((float) Math.toRadians(this.xRot)) * 0.7D));
            passenger.setPos(this.getX() + pos.x, this.getY() + pos.y + passenger.getMyRidingOffset(), this.getZ() + pos.z);
            passenger.setYBodyRot(this.yRot);
            final float f2 = MathHelper.wrapDegrees(passenger.yRot - this.yRot);
            final float f1 = MathHelper.clamp(f2, -105.0F, 105.0F);
            passenger.yRotO += f1 - f2;
            passenger.yRot += f1 - f2;
            passenger.setYHeadRot(passenger.yRot);
            if (passenger instanceof AnimalEntity && this.getPassengers().size() > 1) {
                final int j = passenger.getId() % 2 == 0 ? 90 : 270;
                passenger.setYBodyRot(((AnimalEntity) passenger).yBodyRot + j);
                passenger.setYHeadRot(passenger.getYHeadRot() + j);
            }
        }
    }

    @Override
    public Item getCartItem() {
        return AstikorCarts.Items.ANIMAL_CART.get();
    }
}
