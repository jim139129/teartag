package dev.teartag.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.teartag.client.ClientNametagState;
import dev.teartag.client.ClientNametagStore;
import dev.teartag.client.ClientFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.FormattedCharSequence;

public final class NametagRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public NametagRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState renderState, float yRot, float xRot) {
        ClientNametagState tag = ClientNametagStore.byEntity(renderState.id);
        if (tag == null || !tag.enabled() || renderState.isInvisible || renderState.isSpectator) return;
        float animation = ClientFeedback.animationProgress(renderState.id);
        if (tag.eliminated() && animation < 0.0F) return;

        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().body.translateAndRotate(poseStack);
        float width = (float) tag.width();
        float height = (float) tag.height();
        // LivingEntityRenderer uses a 0.9375 player scale and places model Y=0 at 1.501 blocks.
        poseStack.translate(0.0F, 1.501F - (float) tag.verticalOffset() / 0.9375F,
            (float) tag.backOffset() / 0.9375F + 0.002F);

        if (animation < 0.0F) {
            submitPlate(poseStack, collector, light, width, height, tag.paperColor(), tag.borderColor(), tag.tears(), tag.requiredTears());
            submitText(poseStack, collector, light, tag, width, height);
        } else {
            poseStack.pushPose();
            poseStack.translate(0.0F, animation * animation * 1.2F, animation * 0.8F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(animation * 70.0F));
            submitPlate(poseStack, collector, light, width, height, tag.paperColor(), tag.borderColor(), tag.requiredTears(), tag.requiredTears());
            submitText(poseStack, collector, light, tag, width, height);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void submitPlate(PoseStack stack, SubmitNodeCollector collector, int light, float width, float height,
                                    int paperColor, int borderColor, int tears, int required) {
        float border = 0.018F;
        collector.submitCustomGeometry(stack, RenderTypes.textBackground(), (pose, vertices) -> quad(vertices, pose, -width / 2 - border, -height / 2 - border, width / 2 + border, height / 2 + border, 0, borderColor, light));
        collector.submitCustomGeometry(stack, RenderTypes.textBackground(), (pose, vertices) -> quad(vertices, pose, -width / 2, -height / 2, width / 2, height / 2, -0.001F, paperColor, light));
        if (tears > 0) {
            float progress = Math.min(1.0F, tears / (float) Math.max(1, required));
            int tearColor = 0xFF9A3328;
            for (int i = 0; i < tears; i++) {
                float x = -width / 2 + width * (i + 0.5F) / Math.max(1, required);
                float depth = height * progress * (0.25F + 0.12F * (i % 3));
                final float fx = x;
                final float fd = depth;
                collector.submitCustomGeometry(stack, RenderTypes.textBackground(), (pose, vertices) -> quad(vertices, pose, fx - 0.012F, -height / 2, fx + 0.012F, -height / 2 + fd, -0.004F, tearColor, light));
            }
        }
    }

    private static void submitText(PoseStack stack, SubmitNodeCollector collector, int light, ClientNametagState tag, float width, float height) {
        Font font = Minecraft.getInstance().font;
        float pixelsPerBlock = 1.0F / tag.textScale();
        int maxWidth = Math.max(1, (int) (width * pixelsPerBlock * 0.82F));
        var lines = font.split(tag.text(), maxWidth);
        int maxLines = tag.maxTextLines();
        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);
        float contentHeight = lines.size() * font.lineHeight;
        float scale = Math.min(tag.textScale(), height * 0.68F / Math.max(1.0F, contentHeight));
        stack.pushPose();
        stack.translate(0.0F, -contentHeight * scale / 2.0F, -0.006F);
        stack.scale(-scale, -scale, scale);
        float y = 0;
        for (FormattedCharSequence line : lines) {
            float x = -font.width(line) / 2.0F;
            collector.order(2).submitText(stack, x, y, line, false, Font.DisplayMode.POLYGON_OFFSET, light, 0xFF202020, 0, 0);
            y += font.lineHeight;
        }
        stack.popPose();
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer v, PoseStack.Pose pose, float x1, float y1, float x2, float y2, float z, int color, int light) {
        v.addVertex(pose, x1, y1, z).setColor(color).setLight(light);
        v.addVertex(pose, x2, y1, z).setColor(color).setLight(light);
        v.addVertex(pose, x2, y2, z).setColor(color).setLight(light);
        v.addVertex(pose, x1, y2, z).setColor(color).setLight(light);
    }
}
