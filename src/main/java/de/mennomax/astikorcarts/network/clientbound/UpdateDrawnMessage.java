package de.mennomax.astikorcarts.network.clientbound;

import de.mennomax.astikorcarts.entity.AbstractDrawnEntity;
import de.mennomax.astikorcarts.network.ClientMessageContext;
import de.mennomax.astikorcarts.network.Message;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import java.util.function.BiConsumer;

public final class UpdateDrawnMessage implements Message {
    private int pullingId;

    private int cartId;

    public UpdateDrawnMessage() {
    }

    public UpdateDrawnMessage(final int pullingId, final int cartId) {
        this.pullingId = pullingId;
        this.cartId = cartId;
    }

    @Override
    public void encode(final PacketBuffer buf) {
        buf.writeVarInt(this.pullingId);
        buf.writeVarInt(this.cartId);
    }

    @Override
    public void decode(final PacketBuffer buf) {
        this.pullingId = buf.readVarInt();
        this.cartId = buf.readVarInt();
    }

    public static final class Handler implements BiConsumer<UpdateDrawnMessage, ClientMessageContext> {
        @Override
        public void accept(final UpdateDrawnMessage msg, final ClientMessageContext ctx) {
            final World world = ctx.getWorld();
            final Entity e = world.getEntity(msg.cartId);
            if (e instanceof AbstractDrawnEntity) {
                if (msg.pullingId < 0) {
                    ((AbstractDrawnEntity) e).setPulling(null);
                } else {
                    ((AbstractDrawnEntity) e).setPulling(world.getEntity(msg.pullingId));
                }
            }
        }
    }
}
