package de.mennomax.astikorcarts.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.mennomax.astikorcarts.entity.AbstractDrawnEntity;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import com.mojang.math.Vector3f;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public abstract class DrawnRenderer<T extends AbstractDrawnEntity, M extends EntityModel<T>> extends EntityRenderer<T> {
    protected M model;

    protected DrawnRenderer(final EntityRendererProvider.Context context, final M model) {
        super(context);
        this.model = model;
    }

    @Override
    public void render(final T entity, final float yaw, final float delta, final PoseStack stack, final MultiBufferSource source, final int packedLight) {
        stack.pushPose();
        final AbstractDrawnEntity.RenderInfo info = entity.getInfo(delta);
        this.setupRotation(entity, info.getYaw(), delta, stack);

        this.model.setupAnim(entity, delta, 0.0F, 0.0F, 0.0F, info.getPitch());
        final VertexConsumer buf = source.getBuffer(this.model.renderType(this.getTextureLocation(entity)));
        this.model.renderToBuffer(stack, buf, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.renderContents(entity, delta, stack, source, packedLight);

        stack.popPose();
        super.render(entity, info.getYaw(), delta, stack, source, packedLight);
    }

    protected void renderContents(final T entity, final float delta, final PoseStack stack, final MultiBufferSource source, final int packedLight) {
    }

    public void setupRotation(final T entity, final float entityYaw, final float delta, final PoseStack stack) {
        stack.mulPose(Vector3f.YP.rotationDegrees(180.0F - entityYaw));
        final float time = entity.getTimeSinceHit() - delta;
        if (time > 0.0F) {
            final double center = 1.2D;
            stack.translate(0.0D, center, 0.0D);
            final float damage = Math.max(entity.getDamageTaken() - delta, 0.0F);
            final float angle = Mth.sin(time) * time * damage / 60.0F;
            stack.mulPose(Vector3f.ZP.rotationDegrees(angle * entity.getForwardDirection()));
            stack.translate(0.0D, -center, 0.0D);
            stack.translate(0.0D, angle / 32.0F, 0.0D);
        }
        stack.scale(-1.0F, -1.0F, 1.0F);
    }

    private static final Field CHILD_MODELS = ObfuscationReflectionHelper.findField(ModelPart.class, "children");

    @SuppressWarnings("unchecked")
    protected void attach(final ModelPart bone, final ModelPart attachment, final Consumer<PoseStack> function, final PoseStack stack) {
        stack.pushPose();
        bone.translateAndRotate(stack);
        if (bone == attachment) {
            function.accept(stack);
        } else {
            final ObjectList<ModelPart> childModels;
            try {
                childModels = (ObjectList<ModelPart>) CHILD_MODELS.get(bone);
            } catch (final IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            for (final ModelPart child : childModels) {
                this.attach(child, attachment, function, stack);
            }
        }
        stack.popPose();
    }
}
