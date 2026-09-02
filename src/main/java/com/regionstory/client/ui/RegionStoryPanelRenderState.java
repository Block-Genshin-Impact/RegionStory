package com.regionstory.client.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** 无纹理四边形：通过 UV0/UV1 把面板尺寸和渐隐参数传给自定义 Shader。 */
public final class RegionStoryPanelRenderState implements SimpleGuiElementRenderState {
    private final RenderPipeline pipeline;
    private final Matrix3x2f pose;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float padding;
    private final float fadeLength;
    private final int color;
    private final ScreenRect bounds;

    private RegionStoryPanelRenderState(RenderPipeline pipeline, Matrix3x2fc pose,
                                        float x, float y, float width, float height,
                                        float padding, float fadeLength, int color) {
        this.pipeline = pipeline;
        this.pose = new Matrix3x2f(pose);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.padding = Math.max(0.0F, padding);
        this.fadeLength = Math.max(0.0F, fadeLength);
        this.color = color;
        this.bounds = new ScreenRect(
                Math.round(x - this.padding),
                Math.round(y - this.padding),
                Math.max(1, Math.round(width + this.padding * 2.0F)),
                Math.max(1, Math.round(height + this.padding * 2.0F)))
                .transform(this.pose);
    }

    public static RegionStoryPanelRenderState of(RenderPipeline pipeline,
                                                  Matrix3x2fc pose,
                                                  float x, float y, float width, float height,
                                                  float padding, float fadeLength, int color) {
        return new RegionStoryPanelRenderState(pipeline, pose, x, y, width, height,
                padding, fadeLength, color);
    }

    @Override
    public void setupVertices(VertexConsumer consumer) {
        float safeHeight = Math.max(1.0F, height);
        float aspect = Math.max(1.0F, width / safeHeight);
        float pad = padding / safeHeight;
        int encodedAspect = Math.max(1, Math.round(aspect * 1000.0F));
        int encodedFadeLength = Math.max(0, Math.round(fadeLength * 1000.0F));

        float left = x - padding;
        float right = x + width + padding;
        float top = y - padding;
        float bottom = y + height + padding;

        // UV0 只是以面板高度为单位的局部坐标，不会触发纹理采样。
        consumer.vertex(pose, left, top)
                .texture(-pad, -pad)
                .color(color)
                .overlay(encodedAspect, encodedFadeLength)
                .light(0, 0)
                .normal(0.0F, 0.0F, 1.0F);
        consumer.vertex(pose, left, bottom)
                .texture(-pad, 1.0F + pad)
                .color(color)
                .overlay(encodedAspect, encodedFadeLength)
                .light(0, 0)
                .normal(0.0F, 0.0F, 1.0F);
        consumer.vertex(pose, right, bottom)
                .texture(aspect + pad, 1.0F + pad)
                .color(color)
                .overlay(encodedAspect, encodedFadeLength)
                .light(0, 0)
                .normal(0.0F, 0.0F, 1.0F);
        consumer.vertex(pose, right, top)
                .texture(aspect + pad, -pad)
                .color(color)
                .overlay(encodedAspect, encodedFadeLength)
                .light(0, 0)
                .normal(0.0F, 0.0F, 1.0F);
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    @Override
    public ScreenRect scissorArea() {
        return null;
    }

    @Override
    public ScreenRect bounds() {
        return bounds;
    }
}
