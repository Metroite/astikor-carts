package de.mennomax.astikorcarts.client.renderer.entity.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import de.mennomax.astikorcarts.entity.AbstractDrawnEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

public abstract class CartModel<T extends AbstractDrawnEntity> extends EntityModel<T> {
    protected final ModelRenderer body;

    protected final ModelRenderer leftWheel;

    protected final ModelRenderer rightWheel;

    protected CartModel(final int textureWidth, final int textureHeight) {
        this.texWidth = textureWidth;
        this.texHeight = textureHeight;

        this.body = new ModelRenderer(this);
        this.body.setPos(0.0F, -11.0F, 1.0F);
        this.leftWheel = new ModelRenderer(this, 46, 60);
        this.leftWheel.setPos(14.5F, -11.0F, 1.0F);
        this.leftWheel.addBox(-2.0F, -1.0F, -1.0F, 2, 2, 2);
        for (int i = 0; i < 8; i++) {
            final ModelRenderer rim = new ModelRenderer(this, 58, 54);
            rim.addBox(-2.0F, -4.5F, 9.86F, 2, 9, 1);
            rim.xRot = i * (float) Math.PI / 4.0F;
            this.leftWheel.addChild(rim);

            final ModelRenderer spoke = new ModelRenderer(this, 54, 54);
            spoke.addBox(-1.5F, 1.0F, -0.5F, 1, 9, 1);
            spoke.xRot = i * (float) Math.PI / 4.0F;
            this.leftWheel.addChild(spoke);
        }

        this.rightWheel = new ModelRenderer(this, 46, 60);
        this.rightWheel.setPos(-14.5F, -11.0F, 1.0F);
        this.rightWheel.addBox(0.0F, -1.0F, -1.0F, 2, 2, 2);
        for (int i = 0; i < 8; i++) {
            final ModelRenderer rim = new ModelRenderer(this, 58, 54);
            rim.addBox(0.0F, -4.5F, 9.86F, 2, 9, 1);
            rim.xRot = i * (float) Math.PI / 4.0F;
            this.rightWheel.addChild(rim);

            final ModelRenderer spoke = new ModelRenderer(this, 54, 54);
            spoke.addBox(0.5F, 1.0F, -0.5F, 1, 9, 1);
            spoke.xRot = i * (float) Math.PI / 4.0F;
            this.rightWheel.addChild(spoke);
        }
    }

    public ModelRenderer getBody() {
        return this.body;
    }

    public ModelRenderer getWheel() {
        return this.rightWheel;
    }

    @Override
    public void renderToBuffer(final MatrixStack stack, final IVertexBuilder buf, final int packedLight, final int packedOverlay, final float red, final float green, final float blue, final float alpha) {
        this.body.render(stack, buf, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftWheel.render(stack, buf, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightWheel.render(stack, buf, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(final T entity, final float delta, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float pitch) {
        this.body.xRot = (float) Math.toRadians(pitch);
        this.rightWheel.xRot = (float) (entity.getWheelRotation(0) + entity.getWheelRotationIncrement(0) * delta);
        this.leftWheel.xRot = (float) (entity.getWheelRotation(1) + entity.getWheelRotationIncrement(1) * delta);
        final float time = entity.getTimeSinceHit() - delta;
        final float rot;
        if (time > 0.0F) {
            final float damage = Math.max(entity.getDamageTaken() - delta, 0.0F);
            rot = (float) Math.toRadians(MathHelper.sin(time) * time * damage / 40.0F * -entity.getForwardDirection());
        } else {
            rot = 0.0F;
        }
        this.rightWheel.zRot = rot;
        this.leftWheel.zRot = rot;
    }
}
