package de.mennomax.astikorcarts.client.renderer.entity.model;

import de.mennomax.astikorcarts.entity.PlowEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class PlowModel extends CartModel<PlowEntity> {
    private final ModelPart plow;

    public PlowModel() {
        super(64, 64);
        plow = LayerDefinition.create(createBodyLayer(), 64, 64).bakeRoot().getChild("axis");
    }

    public static MeshDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition axis = partdefinition.addOrReplaceChild("axis", CubeListBuilder.create()
                .addBox(-12.5F, -1.0F, -1.0F, 25, 2, 2), PartPose.ZERO);

        PartDefinition triangle0 = axis.addOrReplaceChild("triangle0", CubeListBuilder.create().texOffs(0, 4)
                .addBox(-7.5F, -9.0F, 0.0F, 15, 2, 2), PartPose.ZERO);

        PartDefinition triangle1 = axis.addOrReplaceChild("triangle1", CubeListBuilder.create().texOffs(0, 11)
                .addBox(-5.0F, -9.0F, 0.5F, 2, 14, 2), PartPose.rotation(0F, 0F, -0.175F));

        PartDefinition triangle2 = axis.addOrReplaceChild("triangle2", CubeListBuilder.create().texOffs(0, 11)
                .addBox(3.0F, -9.0F, 0.5F, 2, 14, 2).mirror(), PartPose.rotation(0F, 0F, 0.175F));

        PartDefinition shaft = axis.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(0, 8)
                .addBox(0.0F, 0.0F, -8.0F, 20, 2, 1)
                .addBox(0.0F, 0.0F, 7.0F, 20, 2, 1), PartPose.rotation(0F, 0F, -0.07F));

        PartDefinition shaftConnector = axis.addOrReplaceChild("shaft_connector", CubeListBuilder.create().texOffs(0, 27)
                .addBox(-16.0F, 0.0F, -8.0F, 16, 2, 1)
                .addBox(-16.0F, 0.0F, 7.0F, 16, 2, 1), PartPose.rotation(0F, 0F, -0.26F));

        PartDefinition shafts = shaft.addOrReplaceChild("shafts", CubeListBuilder.create().texOffs(0, 27), PartPose.offsetAndRotation(0.0F, 0.0F, -14.0F, 0F, (float) Math.PI / 2.0F, 0F));

        for (int i = 0; i < 3; i++) {
            PartDefinition plowShaftLower = shafts.addOrReplaceChild("plow_shaft_lower"+i, CubeListBuilder.create().texOffs(42, 4)
                    .addBox(-1.0F, -0.7F, -0.7F, 2, 10, 2), PartPose.offsetAndRotation(0.0F, 28.0F, -1.0F, (float) Math.PI / 4.0F, 0F, 0F));

            PartDefinition plowShaftUpper = plowShaftLower.addOrReplaceChild("plow_shaft_upper"+i, CubeListBuilder.create().texOffs(56, 0)
                    .addBox(-1.0F, -2.0F, -2.0F, 2, 30, 2), PartPose.offsetAndRotation(-3.0F + 3 * i, -7.0F, 0.0F, 0F, -0.523599F + (float) Math.PI / 6.0F * i, 0F));
        }

        PartDefinition plowHandle = shafts.getChild("plow_shaft_lower1").getChild("plow_shaft_upper1").addOrReplaceChild("plow_handle", CubeListBuilder.create().texOffs(50, 4)
                .addBox(-0.5F, 0.0F, -0.5F, 1, 18, 1), PartPose.offsetAndRotation(0.0F, 33.0F, 5.0F, (float) Math.PI / 2.0F, 0F, 0F));

        PartDefinition plowHandleGrip = shafts.getChild("plow_shaft_lower1").getChild("plow_shaft_upper1").addOrReplaceChild("plow_handle_grip", CubeListBuilder.create().texOffs(50, 23)
                .addBox(-0.5F, 0.0F, -1.0F, 1, 5, 1), PartPose.offsetAndRotation(0.0F, 32.8F, 21.0F, (float) Math.PI / 4.0F, 0F, 0F));

        return meshdefinition;
    }

    public ModelPart getShaft(final int original) {
        return this.plow.getChild("plow_shaft_lower"+original);
    }


    @Override
    public void setupAnim(final PlowEntity entity, final float delta, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float pitch) {
        super.setupAnim(entity, delta, limbSwingAmount, ageInTicks, netHeadYaw, pitch);
        for (int i = 0; i < 3; i++) {
            ModelPart part = this.plow.getChild("plow_shaft_upper"+i);
            part.xRot = (float) (entity.getPlowing() ? Math.PI / 4.0D - Math.toRadians(pitch) : Math.PI / 2.5D);
        }
    }
}
