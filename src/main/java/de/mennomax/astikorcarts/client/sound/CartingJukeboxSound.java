package de.mennomax.astikorcarts.client.sound;

import de.mennomax.astikorcarts.entity.SupplyCartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.util.SoundCategory;

public class CartingJukeboxSound extends TickableSound {
    private final SupplyCartEntity cart;

    private final MusicDiscItem disc;

    public CartingJukeboxSound(final SupplyCartEntity cart, final MusicDiscItem disc) {
        super(disc.getSound(), SoundCategory.RECORDS);
        this.cart = cart;
        this.disc = disc;
    }

    @Override
    public void tick() {
        if (this.cart.isAlive() && getDisc(this.cart.getDisc()) == this.disc) {
            this.x = (float) this.cart.getX();
            this.y = (float) this.cart.getY();
            this.z = (float) this.cart.getZ();
        } else {
            this.stop();
        }
    }

    public static void play(final SupplyCartEntity e, final ItemStack stack) {
        final MusicDiscItem disc = getDisc(stack);
        final Minecraft mc = Minecraft.getInstance();
        final ClientPlayerEntity player = mc.player;
        if (player == null) return;
        mc.getSoundManager().play(new CartingJukeboxSound(e, disc));
        if (e.distanceToSqr(player) < 64.0D * 64.0D) {
            mc.gui.setNowPlaying(disc.getDisplayName());
        }
    }

    public static MusicDiscItem getDisc(final ItemStack stack) {
        final Item item = stack.getItem();
        return (MusicDiscItem) (item instanceof MusicDiscItem ? item : Items.MUSIC_DISC_11);
    }
}
