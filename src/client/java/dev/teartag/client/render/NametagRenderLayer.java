package dev.teartag.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.teartag.client.ClientNametagState;
import dev.teartag.client.ClientNametagStore;
import dev.teartag.client.ClientFeedback;
import dev.teartag.state.NametagVisualStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public final class NametagRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier PLATE_TEXTURE = texture("plate_mask");
    private static final Identifier[] DETAIL_TEXTURES = {
        texture("detail_0"), texture("detail_1"), texture("detail_2"),
        texture("detail_3"), texture("detail_4"), texture("detail_torn")
    };

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
            int stage = NametagVisualStage.fromProgress(tag.tears(), tag.requiredTears(), false);
            submitPlate(poseStack, collector, light, width, height, tag.paperColor(), tag.borderColor(), stage);
            submitText(poseStack, collector, light, tag, width, height);
        } else {
            poseStack.pushPose();
            poseStack.translate(0.0F, animation * animation * 1.2F, animation * 0.8F);
            poseStack.rotateAround(Axis.ZP.rotationDegrees(animation * 70.0F), 0.0F, 0.0F, 0.0F);
            submitPlate(poseStack, collector, light, width, height, tag.paperColor(), tag.borderColor(), NametagVisualStage.TORN);
            submitText(poseStack, collector, light, tag, width, height);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void submitPlate(PoseStack stack, SubmitNodeCollector collector, int light, float width, float height,
                                    int paperColor, int borderColor, int stage) {
        collector.order(0).submitCustomGeometry(stack, RenderTypes.entityCutout(PLATE_TEXTURE),
            (pose, vertices) -> texturedQuad(vertices, pose, width, height, 0.0F, paperColor, light));
        collector.order(1).submitCustomGeometry(stack, RenderTypes.entityTranslucent(DETAIL_TEXTURES[stage]),
            (pose, vertices) -> texturedQuad(vertices, pose, width, height, 0.001F, borderColor, light));
    }

    private static void submitText(PoseStack stack, SubmitNodeCollector collector, int light, ClientNametagState tag, float width, float height) {
        Font font = Minecraft.getInstance().font;
        float pixelsPerBlock = 1.0F / tag.textScale();
        int maxWidth = Math.max(1, (int) (width * pixelsPerBlock * 0.78F));
        var lines = font.split(tag.text(), maxWidth);
        int maxLines = tag.maxTextLines();
        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);
        float contentHeight = lines.size() * font.lineHeight;
        float scale = Math.min(tag.textScale(), height * 0.55F / Math.max(1.0F, contentHeight));
        stack.pushPose();
        stack.translate(0.0F, -contentHeight * scale / 2.0F, 0.006F);
        stack.rotateAround(Axis.XP.rotationDegrees(180.0F), 0.0F, 0.0F, 0.0F);
        stack.scale(-scale, -scale, scale);
        float y = 0;
        for (FormattedCharSequence line : lines) {
            float x = -font.width(line) / 2.0F;
            collector.order(2).submitText(stack, x, y, line, false, Font.DisplayMode.POLYGON_OFFSET, light, tag.textColor(), 0, 0);
            y += font.lineHeight;
        }
        stack.popPose();
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("teartag", "textures/nametag/" + name + ".png");
    }

    private static void texturedQuad(VertexConsumer vertices, PoseStack.Pose pose, float width, float height, float z, int color, int light) {
        float x1 = -width / 2.0F;
        float y1 = -height / 2.0F;
        float x2 = width / 2.0F;
        float y2 = height / 2.0F;
        vertex(vertices, pose, x1, y1, z, 0.0F, 1.0F, color, light);
        vertex(vertices, pose, x2, y1, z, 1.0F, 1.0F, color, light);
        vertex(vertices, pose, x2, y2, z, 1.0F, 0.0F, color, light);
        vertex(vertices, pose, x1, y2, z, 0.0F, 0.0F, color, light);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int color, int light) {
        vertices.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
