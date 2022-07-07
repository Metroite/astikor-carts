package de.mennomax.astikorcarts.item;

import de.mennomax.astikorcarts.AstikorCarts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import net.minecraft.item.Item.Properties;

public final class CartItem extends Item {
    public CartItem(final Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(final World world, final PlayerEntity player, final Hand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final RayTraceResult result = getPlayerPOVHitResult(world, player, FluidMode.ANY);
        if (result.getType() == Type.MISS) {
            return ActionResult.pass(stack);
        } else {
            final Vector3d lookVec = player.getViewVector(1.0F);
            final List<Entity> list = world.getEntities(player, player.getBoundingBox().expandTowards(lookVec.scale(5.0D)).inflate(5.0D), EntityPredicates.NO_SPECTATORS.and(Entity::isPickable));
            if (!list.isEmpty()) {
                final Vector3d eyePos = player.getEyePosition(1.0F);
                for (final Entity entity : list) {
                    final AxisAlignedBB axisalignedbb = entity.getBoundingBox().inflate(entity.getPickRadius());
                    if (axisalignedbb.contains(eyePos)) {
                        return ActionResult.pass(stack);
                    }
                }
            }

            if (result.getType() == Type.BLOCK) {
                final EntityType<?> type = ForgeRegistries.ENTITIES.getValue(this.getRegistryName());
                if (type == null) {
                    return ActionResult.pass(stack);
                }
                final Entity cart = type.create(world);
                if (cart == null) {
                    return ActionResult.pass(stack);
                }
                cart.setPos(result.getLocation().x, result.getLocation().y, result.getLocation().z);
                cart.yRot = (player.yRot + 180) % 360;
                if (!world.noCollision(cart, cart.getBoundingBox().inflate(0.1F, -0.1F, 0.1F))) {
                    return ActionResult.fail(stack);
                } else {
                    if (!world.isClientSide()) {
                        world.addFreshEntity(cart);
                        world.playSound(null, cart.getX(), cart.getY(), cart.getZ(), AstikorCarts.SoundEvents.CART_PLACED.get(), SoundCategory.BLOCKS, 0.75F, 0.8F);
                    }
                    if (!player.abilities.instabuild) {
                        stack.shrink(1);
                    }
                    player.awardStat(Stats.ITEM_USED.get(this));
                    return ActionResult.success(stack);
                }
            } else {
                return ActionResult.pass(stack);
            }
        }
    }
}
