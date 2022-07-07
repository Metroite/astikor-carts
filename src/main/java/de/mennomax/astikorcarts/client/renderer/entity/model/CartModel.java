package de.mennomax.astikorcarts.client.renderer.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.mennomax.astikorcarts.entity.AbstractDrawnEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public abstract class CartModel<T extends AbstractDrawnEntity> extends EntityModel<T> {
    private final ModelPart cart;

    protected CartModel(final int textureWidth, final int textureHeight) {
        this.cart = LayerDefinition.create(createBodyLayer(), textureWidth, textureHeight).bakeRoot().getChild("cart");
    }

    public static MeshDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition cart = partdefinition.addOrReplaceChild("cart", CubeListBuilder.create(), PartPose.offset(0.0F, -11.0F, 1.0F));

        PartDefinition leftWheel = cart.addOrReplaceChild("left_wheel", CubeListBuilder.create().texOffs(46, 60)
                .addBox(-2.0F, -1.0F, -1.0F, 2, 2, 2), PartPose.offset(14.5F, -11.0F, 1.0F));

        for (int i = 0; i < 8; i++) {
            PartDefinition rim = leftWheel.addOrReplaceChild("rim"+i, CubeListBuilder.create().texOffs(58,54)
                    .addBox(-2.0F, -4.5F, 9.86F, 2, 9, 1), PartPose.rotation(0F, i * (float) Math.PI / 4.0F, 0F));

            PartDefinition spoke = leftWheel.addOrReplaceChild("spoke"+i, CubeListBuilder.create().texOffs(54, 54)
                    .addBox(-1.5F, 1.0F, -0.5F, 1, 9, 1), PartPose.rotation(0F, i * (float) Math.PI / 4.0F, 0F));
        }

        PartDefinition rightWheel = cart.addOrReplaceChild("right_wheel", CubeListBuilder.create().texOffs(46, 60)
                .addBox(0.0F, -1.0F, -1.0F, 2, 2, 2), PartPose.offset(-14.5F, -11.0F, 1.0F));

        for (int i = 0; i < 8; i++) {
            PartDefinition rim = rightWheel.addOrReplaceChild("rim"+i, CubeListBuilder.create().texOffs(58, 54)
                    .addBox(0.0F, -4.5F, 9.86F, 2, 9, 1), PartPose.rotation(0, i * (float) Math.PI / 4.0F, 0));

            PartDefinition spoke = rightWheel.addOrReplaceChild("spoke"+i, CubeListBuilder.create().texOffs(54, 54)
                    .addBox(0.5F, 1.0F, -0.5F, 1, 9, 1), PartPose.rotation(0F, i * (float) Math.PI / 4.0F, 0F));
        }

        return meshdefinition;
    }

    public ModelPart getBody() {
        return this.cart;
    }

    public ModelPart getWheel() {
        return this.cart.getChild("right_wheel");
    }

    @Override
    public void renderToBuffer(final PoseStack stack, final VertexConsumer buf, final int packedLight, final int packedOverlay, final float red, final float green, final float blue, final float alpha) {
        this.cart.render(stack, buf, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(final T entity, final float delta, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float pitch) {
        this.cart.xRot = (float) Math.toRadians(pitch);
        this.cart.getChild("right_wheel").xRot = (float) (entity.getWheelRotation(0) + entity.getWheelRotationIncrement(0) * delta);
        this.cart.getChild("left_wheel").xRot = (float) (entity.getWheelRotation(1) + entity.getWheelRotationIncrement(1) * delta);
        final float time = entity.getTimeSinceHit() - delta;
        final float rot;
        if (time > 0.0F) {
            final float damage = Math.max(entity.getDamageTaken() - delta, 0.0F);
            rot = (float) Math.toRadians(Mth.sin(time) * time * damage / 40.0F * -entity.getForwardDirection());
        } else {
            rot = 0.0F;
        }
        this.cart.getChild("right_wheel").zRot = rot;
        this.cart.getChild("left_wheel").zRot = rot;
    }
}
