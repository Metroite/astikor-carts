package de.mennomax.astikorcarts.client.renderer.entity.model;

import de.mennomax.astikorcarts.entity.AnimalCartEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class AnimalCartModel extends CartModel<AnimalCartEntity> {
    private final ModelPart cart;

    public AnimalCartModel() {
        super(64, 64);
        MeshDefinition cart = CartModel.createBodyLayer();
        LayerDefinition animalCart = createBodyLayer(cart);
        this.cart = animalCart.bakeRoot().getChild("cart");
    }

    public static LayerDefinition createBodyLayer(MeshDefinition cart) {
        PartDefinition root = cart.getRoot();

        PartDefinition axis = root.addOrReplaceChild("axis", CubeListBuilder.create().texOffs(0, 21)
                .addBox(-12.5F, -1.0F, -1.0F, 25, 2, 2), PartPose.offset(0.0F, -11.0F, 1.0F));

        PartDefinition cartBase = axis.addOrReplaceChild("cart_base", CubeListBuilder.create()
                .addBox(-15.5F, -10.0F, -2.0F, 29, 20, 1), PartPose.rotation(0F, (float) -Math.PI / 2.0F, (float) -Math.PI / 2.0F));

        PartDefinition shaft = cartBase.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(0, 25)
                .addBox(0.0F, -0.5F, -8.0F, 20, 2, 1)
                .addBox(0.0F, -0.5F, 7.0F, 20, 2, 1), PartPose.offsetAndRotation(0.0F, -5.0F, -15.0F, 0F, 0F, (float) Math.PI / 2.0F));

        PartDefinition boardLeft = shaft.addOrReplaceChild("boardLeft", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-10.0F, -14.5F, 9F, 8, 31, 2), PartPose.rotation(0F, (float) -Math.PI / 2.0F, (float) Math.PI / 2.0F));

        PartDefinition boardRight = boardLeft.addOrReplaceChild("board_right", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-10.0F, -14.5F, -11F, 8, 31, 2), PartPose.rotation(0F, (float) -Math.PI / 2.0F, (float) Math.PI / 2.0F));

        PartDefinition boardBack = boardRight.addOrReplaceChild("board_back", CubeListBuilder.create().texOffs(20, 28)
                .addBox(-9F, -10.0F, 12.5F, 18, 8, 2), PartPose.ZERO);

        PartDefinition boardFront = boardBack.addOrReplaceChild("board_front", CubeListBuilder.create().texOffs(20, 28)
                .addBox(-9F, -10.0F, -16.5F, 18, 8, 2), PartPose.ZERO);

        return LayerDefinition.create(cart, 128, 64);
    }
}
