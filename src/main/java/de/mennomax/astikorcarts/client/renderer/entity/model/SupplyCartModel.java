package de.mennomax.astikorcarts.client.renderer.entity.model;

import de.mennomax.astikorcarts.entity.SupplyCartEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class SupplyCartModel extends CartModel<SupplyCartEntity> {
    private final ModelPart cart;

    public SupplyCartModel() {
        super(64, 64);
        cart = LayerDefinition.create(createBodyLayer(), 64, 64).bakeRoot().getChild("board_bottom");
    }

    public static MeshDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition boardBottom = partdefinition.addOrReplaceChild("board_bottom", CubeListBuilder.create()
                .addBox(-15.5F, -11.0F, -2.0F, 29, 22, 1), PartPose.rotation((float) Math.PI / -2.0F, (float) Math.PI / -2.0F, 0F));

        PartDefinition axis = boardBottom.addOrReplaceChild("axis", CubeListBuilder.create().texOffs(0, 23)
                .addBox(-12.5F, -1.0F, -1.0F, 25, 2, 2), PartPose.ZERO);

        PartDefinition shaft = boardBottom.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(0, 31)
                .addBox(0.0F, -2.5F, -8.0F, 20, 2, 1)
                .addBox(0.0F, -2.5F, 7.0F, 20, 2, 1), PartPose.offsetAndRotation(0.0F, -5.0F, -15.0F, 0F, (float) Math.PI / 2.0F, 0F));

        PartDefinition boardFront = boardBottom.addOrReplaceChild("board_front", CubeListBuilder.create().texOffs(0, 34)
                .addBox(-12.0F, -12.0F, -15.5F, 24, 10, 1), PartPose.ZERO);

        PartDefinition boardsSide0 = boardBottom.addOrReplaceChild("boards_side0", CubeListBuilder.create().texOffs(0, 27)
                .addBox(-13.5F, -7.0F, 0.0F, 28, 3, 1), PartPose.offsetAndRotation(-11.0F, -5.0F, -1.0F, 0F, (float) Math.PI / -2.0F, 0F));

        PartDefinition boardsSide1 = boardBottom.addOrReplaceChild("boards_side1", CubeListBuilder.create().texOffs(0, 27)
                .addBox(-14.5F, -7.0F, 0.0F, 28, 3, 1), PartPose.offsetAndRotation(11.0F, -5.0F, -1.0F, 0F, (float) Math.PI / 2.0F, 0F));

        PartDefinition boardsSide2 = boardBottom.addOrReplaceChild("boards_side2", CubeListBuilder.create().texOffs(0, 27)
                .addBox(-13.5F, -2.0F, 0.0F, 28, 3, 1), PartPose.offsetAndRotation(-11.0F, -5.0F, -1.0F, 0F, (float) Math.PI / -2.0F, 0F));

        PartDefinition boardsSide3 = boardBottom.addOrReplaceChild("boards_side3", CubeListBuilder.create().texOffs(0, 27)
                .addBox(-14.5F, -2.0F, 0.0F, 28, 3, 1), PartPose.offsetAndRotation(11.0F, -5.0F, -1.0F, 0F, (float) Math.PI / 2.0F, 0F));

        PartDefinition boardsRear0 = boardBottom.addOrReplaceChild("boards_rear0", CubeListBuilder.create().texOffs(50,31)
                .addBox(10.0F, -12.0F, 13.5F, 2, 11, 1), PartPose.ZERO);

        PartDefinition boardsRear1 = boardBottom.addOrReplaceChild("boards_rear1", CubeListBuilder.create().texOffs(50,31)
                .addBox(-12.0F, -12.0F, 13.5F, 2, 11, 1), PartPose.ZERO);

        PartDefinition flowerBasket = boardBottom.addOrReplaceChild("flower_basker", CubeListBuilder.create().texOffs(-17, 45)
                .addBox(-8.0F, -6.0F, -11.5F, 16.0F, 1.0F, 17.0F).texOffs(16, 45)
                .addBox(-10.0F, -7.0F, 5.5F, 20.0F, 5.0F, 2.0F), PartPose.ZERO);

        PartDefinition frontSide = flowerBasket.addOrReplaceChild("front_side", CubeListBuilder.create().texOffs(16, 45)
                .addBox(-10.0F, -7.0F, 11.5F, 20.0F, 5.0F, 2.0F),PartPose.rotation(0F, (float) Math.PI, 0F));

        PartDefinition leftSide = flowerBasket.addOrReplaceChild("left_side", CubeListBuilder.create().texOffs(16, 52)
                .addBox(-5.5F, -7.0F, 8.0F, 17.0F, 5.0F, 2.0F),PartPose.rotation(0F, (float) Math.PI / 2.0F, 0F));

        PartDefinition rightSide = flowerBasket.addOrReplaceChild("right_side", CubeListBuilder.create().texOffs(16, 52)
                .addBox(-11.5F, -7.0F, 8.0F, 17.0F, 5.0F, 2.0F),PartPose.rotation(0F, (float) -Math.PI / 2.0F, 0F));

        return meshdefinition;
    }

    public ModelPart getFlowerBasket() {
        return this.cart.getChild("flower_basket");
    }
}
