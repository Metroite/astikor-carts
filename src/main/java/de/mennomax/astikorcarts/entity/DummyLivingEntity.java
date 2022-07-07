package de.mennomax.astikorcarts.entity;

import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.HandSide;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Collections;

public abstract class DummyLivingEntity extends LivingEntity {
    protected DummyLivingEntity(final EntityType<? extends LivingEntity> type, final World world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.setSilent(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setInvisible(true);
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(final EquipmentSlotType slotIn) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(final EquipmentSlotType slotIn, final ItemStack stack) {
    }

    @Override
    public HandSide getMainArm() {
        return HandSide.RIGHT;
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isEffectiveAi() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected boolean isMovementNoisy() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean attackable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean canAttackType(final EntityType<?> type) {
        return false;
    }

    @Override
    public boolean canAttack(final LivingEntity living) {
        return false;
    }

    @Override
    public boolean canBeAffected(final EffectInstance effect) {
        return false;
    }

    @Override
    public void kill() {
        this.remove();
    }

    @Override
    public void thunderHit(final ServerWorld world, final LightningBoltEntity bolt) {
    }

    @Override
    protected void doPush(final Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    @Override
    public boolean addEffect(final EffectInstance effect) {
        return false;
    }

    @Override
    protected void updateInvisibilityStatus() {
        this.setInvisible(true);
    }
}
