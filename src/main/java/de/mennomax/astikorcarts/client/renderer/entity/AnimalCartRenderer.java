package de.mennomax.astikorcarts.client.renderer.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.mennomax.astikorcarts.AstikorCarts;
import de.mennomax.astikorcarts.client.renderer.entity.model.AnimalCartModel;
import de.mennomax.astikorcarts.entity.AnimalCartEntity;
import de.mennomax.astikorcarts.util.Mat4f;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public final class AnimalCartRenderer extends DrawnRenderer<AnimalCartEntity, AnimalCartModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(AstikorCarts.ID, "textures/entity/animal_cart.png");

    public AnimalCartRenderer(final EntityRendererProvider.Context context) {
        super(context, new AnimalCartModel());
        this.shadowRadius = 1.0F;
    }

    @Override
    public void render(final AnimalCartEntity entity, final float yaw, final float delta, final PoseStack stack, final MultiBufferSource source, final int packedLight) {
        super.render(entity, yaw, delta, stack, source, packedLight);
        /*final LivingEntity coachman = entity.getControllingPassenger();
        final Entity pulling = entity.getPulling();
        if (pulling instanceof HorseEntity && coachman instanceof PlayerEntity) {
            final HorseEntity horse = (HorseEntity) pulling;
            GlStateManager.pushMatrix();
            GlStateManager.translatef((float) -TileEntityRendererDispatcher.staticPlayerX, (float) -TileEntityRendererDispatcher.staticPlayerY, (float) -TileEntityRendererDispatcher.staticPlayerZ);
            final Mat4f cmv = this.modelView(coachman, delta);
            final Mat4f pmv = this.modelView(pulling, delta);
            this.horseTransform(pmv, horse, delta);
            final float strength = Math.min(MathHelper.lerp(delta, horse.prevLimbSwingAmount, horse.limbSwingAmount), 1.0F);
            final float swing = horse.limbSwing - horse.limbSwingAmount * (1.0F - delta);
            for (int side = -1; side <= 1; side += 2) {
                final Vec4f start = new Vec4f(side * 0.4F, 1.17F + MathHelper.cos((swing - 1.02F) * 0.4F * 2) * 0.05F * strength, -0.3F, 1.0F).transform(cmv);
                final Vec4f end = new Vec4f(-side * 0.175F, -0.5F, -0.3F, 1.0F).transform(pmv);
                this.renderCurve(start.x(), start.y(), start.z(), end.x(), end.y(), end.z());
            }
            GlStateManager.popMatrix();
        }*/
    }

    private void horseTransform(final Mat4f m, final Horse entity, final float delta) {
        final HorseModel<Horse> horseModel = ((HorseRenderer) this.entityRenderDispatcher.getRenderer(entity)).getModel();
        float strength = 0.0F;
        float swing = 0.0F;
        if (!entity.isPassenger() && entity.isAlive()) {
            strength = Mth.lerp(delta, entity.animationSpeedOld, entity.animationSpeed);
            swing = entity.animationPosition - entity.animationSpeed * (1.0F - delta);
            if (entity.isBaby()) {
                swing *= 3.0F;
            }
            if (strength > 1.0F) {
                strength = 1.0F;
            }
        }
        horseModel.prepareMobModel(entity, swing, strength, delta);
        final ModelPart head = ObfuscationReflectionHelper.getPrivateValue(HorseModel.class, horseModel, "headParts");
        final Mat4f tmp = new Mat4f();
        m.mul(tmp.makeScale(-1.0F, -1.0F, 1.0F));
        m.mul(tmp.makeScale(1.1F, 1.1F, 1.1F));
        m.mul(tmp.makeTranslation(0.0F, -1.501F, 0.0F));
        this.transform(m, head);
    }

    private void transform(final Mat4f m, final ModelPart bone) {
        final Mat4f tmp = new Mat4f();
        m.mul(tmp.makeTranslation(bone.x / 16.0F, bone.y / 16.0F, bone.z / 16.0F));
        if (bone.zRot != 0.0F) {
            m.mul(tmp.makeRotation(bone.zRot, 0.0F, 0.0F, 1.0F));
        }
        if (bone.yRot != 0.0F) {
            m.mul(tmp.makeRotation(bone.yRot, 0.0F, 1.0F, 0.0F));
        }
        if (bone.xRot != 0.0F) {
            m.mul(tmp.makeRotation(bone.xRot, 1.0F, 0.0F, 0.0F));
        }
    }

    private Mat4f modelView(final Entity entity, final float delta) {
        final Mat4f m = new Mat4f();
        m.makeTranslation(
            (float) Mth.lerp(delta, entity.xOld, entity.getX()),
            (float) Mth.lerp(delta, entity.yOld, entity.getY()),
            (float) Mth.lerp(delta, entity.zOld, entity.getZ()));
        final Mat4f r = new Mat4f();
        final float prevYaw, yaw;
        if (entity instanceof LivingEntity) {
            prevYaw = ((LivingEntity) entity).yBodyRotO;
            yaw = ((LivingEntity) entity).yBodyRot;
        } else {
            prevYaw = entity.yRotO;
            yaw = entity.getYRot();
        }
        r.makeRotation((float) Math.toRadians(180.0F - Mth.rotLerp(delta, prevYaw, yaw)), 0.0F, 1.0F, 0.0F);
        m.mul(r);
        return m;
    }

    private void renderCurve(final double x0, final double y0, final double z0, final double x1, final double y1, final double z1) {
        if (y0 > y1) {
            this.renderLead(x1, y1, z1, x0 - x1, y0 - y1, z0 - z1, 1);
        } else {
            this.renderLead(x0, y0, z0, x1 - x0, y1 - y0, z1 - z0, 0);
        }
    }

    private void renderLead(final double x, final double y, final double z, final double dx, final double dy, final double dz, final int offset) {
        final Tesselator tes = Tesselator.getInstance();
        final BufferBuilder buf = tes.getBuilder();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        final int n = 24;
        final double w = 0.025D * 2;
        final double m = Math.sqrt(dx * dx + dz * dz);
        final double nx = dx / m;
        final double nz = dz / m;
        final float r0 = 97.0F / 255.0F;
        final float g0 = 58.0F / 255.0F;
        final float b0 = 29.0F / 255.0F;
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= n; i++) {
            float r = r0;
            float g = g0;
            float b = b0;
            if ((i + offset) % 2 == 0) {
                r *= 0.7F;
                g *= 0.7F;
                b *= 0.7F;
            }
            final float t = (float) i / n;
            buf.vertex(x + dx * t, y + dy * (t * t + t) * 0.5D - w, z + dz * t).color(r, g, b, 1.0F).endVertex();
            buf.vertex(x + dx * t, y + dy * (t * t + t) * 0.5D + w, z + dz * t).color(r, g, b, 1.0F).endVertex();
        }
        tes.end();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= n; i++) {
            float r = r0;
            float g = g0;
            float b = b0;
            if ((i + offset) % 2 == 0) {
                r *= 0.7F;
                g *= 0.7F;
                b *= 0.7F;
            }
            final float t = (float) i / n;
            buf.vertex(x + dx * t + w * -nz, y + dy * (t * t + t) * 0.5D, z + dz * t + w * nx).color(r, g, b, 1.0F).endVertex();
            buf.vertex(x + dx * t + w * nz, y + dy * (t * t + t) * 0.5D, z + dz * t + w * -nx).color(r, g, b, 1.0F).endVertex();
        }
        tes.end();
        RenderSystem.enableTexture();
        RenderSystem.enableCull();
    }

    @Override
    public ResourceLocation getTextureLocation(final AnimalCartEntity entity) {
        return TEXTURE;
    }
}
