package de.mennomax.astikorcarts.inventory.container;

import de.mennomax.astikorcarts.entity.AbstractDrawnInventoryEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public abstract class CartContainer extends Container {
    protected final ItemStackHandler cartInv;

    protected final AbstractDrawnInventoryEntity cart;

    public CartContainer(final ContainerType<?> type, final int id, final AbstractDrawnInventoryEntity cart) {
        super(type, id);
        this.cart = cart;
        this.cartInv = cart.inventory;
    }

    @Override
    public boolean stillValid(final PlayerEntity playerIn) {
        return this.cart.isAlive() && this.cart.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public ItemStack quickMoveStack(final PlayerEntity playerIn, final int index) {
        final ItemStack itemstack = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack itemstack1 = slot.getItem();
            if (index < this.cartInv.getSlots()) {
                if (!this.moveItemStackTo(itemstack1, this.cartInv.getSlots(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.cartInv.getSlots(), false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void removed(final PlayerEntity playerIn) {
        super.removed(playerIn);
    }
}
