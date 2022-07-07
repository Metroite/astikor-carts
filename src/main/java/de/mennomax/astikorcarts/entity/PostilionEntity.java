package de.mennomax.astikorcarts.entity;

import de.mennomax.astikorcarts.world.AstikorWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.network.IPacket;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Optional;

public final class PostilionEntity extends DummyLivingEntity {
    public PostilionEntity(final EntityType<? extends PostilionEntity> type, final World world) {
        super(type, world);
    }

    @Override
    public double getMyRidingOffset() {
        return 0.125D;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            final LivingEntity coachman = this.getCoachman();
            if (coachman != null) {
                this.yRot = coachman.yRot;
                this.yRotO = this.yRot;
                this.xRot = coachman.xRot * 0.5F;
                this.zza = coachman.zza;
                this.xxa = 0.0F;
            } else {
                this.remove();
            }
        }
    }

    @Nullable
    private LivingEntity getCoachman() {
        final Entity mount = this.getVehicle();
        if (mount != null) {
            return AstikorWorld.get(this.level).map(m -> m.getDrawn(mount)).orElse(Optional.empty())
                .map(AbstractDrawnEntity::getControllingPassenger).orElse(null);
        }
        return null;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
