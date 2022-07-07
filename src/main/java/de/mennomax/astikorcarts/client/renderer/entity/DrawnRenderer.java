package de.mennomax.astikorcarts.client.renderer.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import de.mennomax.astikorcarts.entity.AbstractDrawnEntity;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public abstract class DrawnRenderer<T extends AbstractDrawnEntity, M extends EntityModel<T>> extends EntityRenderer<T> {
    protected M model;

    protected DrawnRenderer(final EntityRendererManager renderManager, final M model) {
        super(renderManager);
        this.model = model;
    }

    @Override
    public void render(final T entity, final float yaw, final float delta, final MatrixStack stack, final IRenderTypeBuffer source, final int packedLight) {
        stack.pushPose();
        final AbstractDrawnEntity.RenderInfo info = entity.getInfo(delta);
        this.setupRotation(entity, info.getYaw(), delta, stack);

        this.model.setupAnim(entity, delta, 0.0F, 0.0F, 0.0F, info.getPitch());
        final IVertexBuilder buf = source.getBuffer(this.model.renderType(this.getTextureLocation(entity)));
        this.model.renderToBuffer(stack, buf, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.renderContents(entity, delta, stack, source, packedLight);

        stack.popPose();
        super.render(entity, info.getYaw(), delta, stack, source, packedLight);
    }

    protected void renderContents(final T entity, final float delta, final MatrixStack stack, final IRenderTypeBuffer source, final int packedLight) {
    }

    public void setupRotation(final T entity, final float entityYaw, final float delta, final MatrixStack stack) {
        stack.mulPose(Vector3f.YP.rotationDegrees(180.0F - entityYaw));
        final float time = entity.getTimeSinceHit() - delta;
        if (time > 0.0F) {
            final double center = 1.2D;
            stack.translate(0.0D, center, 0.0D);
            final float damage = Math.max(entity.getDamageTaken() - delta, 0.0F);
            final float angle = MathHelper.sin(time) * time * damage / 60.0F;
            stack.mulPose(Vector3f.ZP.rotationDegrees(angle * entity.getForwardDirection()));
            stack.translate(0.0D, -center, 0.0D);
            stack.translate(0.0D, angle / 32.0F, 0.0D);
        }
        stack.scale(-1.0F, -1.0F, 1.0F);
    }

    private static final Field CHILD_MODELS = ObfuscationReflectionHelper.findField(ModelRenderer.class, "children");

    @SuppressWarnings("unchecked")
    protected void attach(final ModelRenderer bone, final ModelRenderer attachment, final Consumer<MatrixStack> function, final MatrixStack stack) {
        stack.pushPose();
        bone.translateAndRotate(stack);
        if (bone == attachment) {
            function.accept(stack);
        } else {
            final ObjectList<ModelRenderer> childModels;
            try {
                childModels = (ObjectList<ModelRenderer>) CHILD_MODELS.get(bone);
            } catch (final IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            for (final ModelRenderer child : childModels) {
                this.attach(child, attachment, function, stack);
            }
        }
        stack.popPose();
    }
}
