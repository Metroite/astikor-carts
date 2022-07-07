package de.mennomax.astikorcarts.entity;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonParseException;
import de.mennomax.astikorcarts.AstikorCarts;
import de.mennomax.astikorcarts.client.sound.CartingJukeboxSound;
import de.mennomax.astikorcarts.config.AstikorCartsConfig;
import de.mennomax.astikorcarts.inventory.container.SupplyCartContainer;
import de.mennomax.astikorcarts.util.CartItemStackHandler;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public final class SupplyCartEntity extends AbstractDrawnInventoryEntity {
    private static final ImmutableList<DataParameter<ItemStack>> CARGO = ImmutableList.of(
        EntityDataManager.defineId(SupplyCartEntity.class, DataSerializers.ITEM_STACK),
        EntityDataManager.defineId(SupplyCartEntity.class, DataSerializers.ITEM_STACK),
        EntityDataManager.defineId(SupplyCartEntity.class, DataSerializers.ITEM_STACK),
        EntityDataManager.defineId(SupplyCartEntity.class, DataSerializers.ITEM_STACK));

    public SupplyCartEntity(final EntityType<? extends Entity> type, final World world) {
        super(type, world);
    }

    @Override
    protected AstikorCartsConfig.CartConfig getConfig() {
        return AstikorCartsConfig.get().supplyCart;
    }

    @Override
    protected ItemStackHandler initInventory() {
        return new CartItemStackHandler<SupplyCartEntity>(54, this) {
            @Override
            protected void onLoad() {
                super.onLoad();
                this.onContentsChanged(0);
            }

            @Override
            protected void onContentsChanged(final int slot) {
                final Object2IntMap<Item> totals = new Object2IntLinkedOpenHashMap<>();
                final Object2ObjectMap<Item, ItemStack> stacks = new Object2ObjectOpenHashMap<>();
                for (int i = 0; i < this.getSlots(); i++) {
                    final ItemStack stack = this.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        totals.mergeInt(stack.getItem(), 1, Integer::sum);
                        stacks.putIfAbsent(stack.getItem(), stack);
                    }
                }
                final Iterator<Object2IntMap.Entry<Item>> topTotals = totals.object2IntEntrySet().stream()
                    .sorted(Comparator.<Object2IntMap.Entry<Item>>comparingInt(e -> e.getKey() instanceof BlockItem ? 0 : 1)
                        .thenComparingInt(e -> -e.getIntValue()))
                    .limit(CARGO.size()).iterator();
                final ItemStack[] items = new ItemStack[CARGO.size()];
                Arrays.fill(items, ItemStack.EMPTY);
                final int forth = this.getSlots() / CARGO.size();
                for (int pos = 0; topTotals.hasNext() && pos < CARGO.size(); ) {
                    final Object2IntMap.Entry<Item> entry = topTotals.next();
                    final int count = Math.max(1, (entry.getIntValue() + forth / 2) / forth);
                    for (int n = 1; n <= count && pos < CARGO.size(); n++) {
                        final ItemStack stack = stacks.getOrDefault(entry.getKey(), ItemStack.EMPTY).copy();
                        stack.setCount(Math.min(stack.getMaxStackSize(), entry.getIntValue() / n));
                        items[pos++] = stack;
                    }
                }
                for (int i = 0; i < CARGO.size(); i++) {
                    this.cart.getEntityData().set(CARGO.get(i), items[i]);
                }
            }
        };
    }

    @Override
    public ActionResultType interact(final PlayerEntity player, final Hand hand) {
        if (player.isSecondaryUseActive()) {
            this.openContainer(player);
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        final ItemStack held = player.getItemInHand(hand);
        if (this.hasJukebox()) {
            if (this.level.isClientSide) return ActionResultType.SUCCESS;
            if (held.getItem() instanceof MusicDiscItem && this.insertDisc(player, held) || this.ejectDisc(player)) {
                return ActionResultType.CONSUME;
            } else {
                return ActionResultType.FAIL;
            }
        }
        if (this.isVehicle()) {
            return ActionResultType.PASS;
        }
        if (!this.level.isClientSide) {
            return player.startRiding(this) ? ActionResultType.CONSUME : ActionResultType.PASS;
        }
        return ActionResultType.SUCCESS;
    }

    private boolean insertDisc(final PlayerEntity player, final ItemStack held) {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            final ItemStack stack = this.inventory.getStackInSlot(i);
            if (DiscTag.insert(stack, held)) {
                this.inventory.setStackInSlot(i, stack);
                ((ServerWorld) this.level).getChunkSource().broadcast(this, new SEntityMetadataPacket(this.getId(), this.entityData, false));
                this.level.broadcastEntityEvent(this, (byte) 5);
                if (!player.abilities.instabuild) held.shrink(1);
                return true;
            }
        }
        return false;
    }

    private boolean ejectDisc(final PlayerEntity player) {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            final ItemStack stack = this.inventory.getStackInSlot(i);
            final DiscTag record = DiscTag.get(stack);
            if (record.eject(player)) {
                this.inventory.setStackInSlot(i, stack);
                return true;
            }
        }
        return false;
    }

    public boolean hasJukebox() {
        for (final DataParameter<ItemStack> slot : CARGO) {
            final ItemStack cargo = this.entityData.get(slot);
            if (cargo.getItem() == Items.JUKEBOX) return true;
        }
        return false;
    }

    public ItemStack getDisc() {
        for (final DataParameter<ItemStack> slot : CARGO) {
            final ItemStack disc = DiscTag.get(this.entityData.get(slot)).disc;
            if (!disc.isEmpty()) return disc;
        }
        return ItemStack.EMPTY;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void handleEntityEvent(final byte id) {
        if (id == 5) {
            for (final DataParameter<ItemStack> slot : CARGO) {
                final ItemStack disc = DiscTag.get(this.entityData.get(slot)).disc;
                if (!disc.isEmpty()) {
                    CartingJukeboxSound.play(this, disc);
                    break;
                }
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 11.0D / 16.0D;
    }

    @Override
    public void positionRider(final Entity passenger) {
        if (this.hasPassenger(passenger)) {
            final Vector3d forward = this.getLookAngle();
            final Vector3d origin = new Vector3d(0.0D, this.getPassengersRidingOffset(), 1.0D / 16.0D);
            final Vector3d pos = origin.add(forward.scale(-0.68D));
            passenger.setPos(this.getX() + pos.x, this.getY() + pos.y - 0.1D + passenger.getMyRidingOffset(), this.getZ() + pos.z);
            passenger.setYBodyRot(this.yRot + 180.0F);
            final float f2 = MathHelper.wrapDegrees(passenger.yRot - this.yRot + 180.0F);
            final float f1 = MathHelper.clamp(f2, -105.0F, 105.0F);
            passenger.yRotO += f1 - f2;
            passenger.yRot += f1 - f2;
            passenger.setYHeadRot(passenger.yRot);
        }
    }

    public NonNullList<ItemStack> getCargo() {
        final NonNullList<ItemStack> cargo = NonNullList.withSize(CARGO.size(), ItemStack.EMPTY);
        for (int i = 0; i < CARGO.size(); i++) {
            cargo.set(i, this.entityData.get(CARGO.get(i)));
        }
        return cargo;
    }

    @Override
    public Item getCartItem() {
        return AstikorCarts.Items.SUPPLY_CART.get();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        for (final DataParameter<ItemStack> parameter : CARGO) {
            this.entityData.define(parameter, ItemStack.EMPTY);
        }
    }

    public void openContainer(final PlayerEntity player) {
        if (!this.level.isClientSide) {
            player.openMenu(new SimpleNamedContainerProvider((id, inv, plyr) -> {
                return new SupplyCartContainer(id, inv, this);
            }, this.getDisplayName()));
        }
    }

    static class DiscTag {
        static final DiscTag EMPTY = new DiscTag(ItemStack.EMPTY, new CompoundNBT(), new CompoundNBT(), ItemStack.EMPTY);

        final ItemStack stack;
        final CompoundNBT nbt, tag;
        final ItemStack disc;

        DiscTag(final ItemStack stack, final CompoundNBT nbt, final CompoundNBT tag, final ItemStack disc) {
            this.stack = stack;
            this.nbt = nbt;
            this.tag = tag;
            this.disc = disc;
        }

        boolean isEmpty() {
            return this.stack.isEmpty();
        }

        boolean eject(final PlayerEntity player) {
            if (this.isEmpty()) return false;
            this.tag.remove("RecordItem");
            if (this.tag.isEmpty()) this.nbt.remove("BlockEntityTag");
            if (this.nbt.contains("display", Constants.NBT.TAG_COMPOUND)) {
                final CompoundNBT display = this.nbt.getCompound("display");
                if (display.contains("Lore", Constants.NBT.TAG_LIST)) {
                    final ListNBT lore = display.getList("Lore", Constants.NBT.TAG_STRING);
                    final String descKey = this.disc.getDescriptionId() + ".desc";
                    for (int i = lore.size(); i --> 0; ) {
                        final String s = lore.getString(i);
                        final ITextComponent component;
                        try {
                            component = ITextComponent.Serializer.fromJson(s);
                        } catch (final JsonParseException ignored) {
                            continue;
                        }
                        if (component instanceof TranslationTextComponent && descKey.equals(((TranslationTextComponent) component).getKey())) {
                            lore.remove(i);
                        }
                    }
                }
            }
            if (this.nbt.isEmpty()) this.stack.setTag(null);
            ItemHandlerHelper.giveItemToPlayer(player, this.disc, player.inventory.selected);
            return true;
        }

        static DiscTag get(final ItemStack stack) {
            if (stack.getItem() != Items.JUKEBOX) return EMPTY;
            final CompoundNBT nbt = stack.getTag();
            if (nbt == null || !nbt.contains("BlockEntityTag", Constants.NBT.TAG_COMPOUND)) return EMPTY;
            final CompoundNBT tag = nbt.getCompound("BlockEntityTag");
            if (!tag.contains("RecordItem", Constants.NBT.TAG_COMPOUND)) return EMPTY;
            return new DiscTag(stack, nbt, tag, ItemStack.of(tag.getCompound("RecordItem")));
        }

        static boolean insert(final ItemStack stack, final ItemStack disc) {
            if (stack.getItem() != Items.JUKEBOX) return false;
            final CompoundNBT tag = stack.getOrCreateTagElement("BlockEntityTag");
            if (tag.contains("RecordItem", Constants.NBT.TAG_COMPOUND)) return false;
            tag.put("RecordItem", disc.save(new CompoundNBT()));
            final CompoundNBT display = stack.getOrCreateTagElement("display");
            final ListNBT lore = display.getList("Lore", Constants.NBT.TAG_STRING);
            lore.add(StringNBT.valueOf(ITextComponent.Serializer.toJson(new TranslationTextComponent(disc.getDescriptionId() + ".desc"))));
            display.put("Lore", lore);
            return true;
        }
    }
}
