package com.regionstory.client.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** 使用自定义 Shader 绘制菱形、箭头、聊天气泡等简单图标。 */
public final class RegionStorySymbolRenderState implements SimpleGuiElementRenderState {
    private final RenderPipeline pipeline;
    private final Matrix3x2f pose;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final int symbol;
    private final int color;
    private final ScreenRect bounds;

    private RegionStorySymbolRenderState(RenderPipeline pipeline, Matrix3x2fc pose,
                                         float x, float y, float width, float height,
                                         int symbol, int color) {
        this.pipeline = pipeline;
        this.pose = new Matrix3x2f(pose);
        this.x = x;
        this.y = y;
        this.width = Math.max(1.0F, width);
        this.height = Math.max(1.0F, height);
        this.symbol = symbol;
        this.color = color;
        this.bounds = new ScreenRect(Math.round(x), Math.round(y),
                Math.max(1, Math.round(this.width)), Math.max(1, Math.round(this.height)))
                .transform(this.pose);
    }

    public static RegionStorySymbolRenderState of(RenderPipeline pipeline, Matrix3x2fc pose,
                                                  float x, float y, float size,
                                                  int symbol, int color) {
        return of(pipeline, pose, x, y, size, size, symbol, color);
    }

    public static RegionStorySymbolRenderState of(RenderPipeline pipeline, Matrix3x2fc pose,
                                                  float x, float y, float width, float height,
                                                  int symbol, int color) {
        return new RegionStorySymbolRenderState(pipeline, pose, x, y, width, height,
                symbol, color);
    }

    @Override
    public void setupVertices(VertexConsumer consumer) {
        float right = x + width;
        float bottom = y + height;
        consumer.vertex(pose, x, y).texture(0.0F, 0.0F).color(color)
                .overlay(symbol, 0).light(0, 0).normal(0.0F, 0.0F, 1.0F);
        consumer.vertex(pose, x, bottom).texture(0.0F, 1.0F).color(color)
                .overlay(symbol, 0).light(0, 0).normal(0.0F, 0.0F, 1.0F);
        consumer.vertex(pose, right, bottom).texture(1.0F, 1.0F).color(color)
                .overlay(symbol, 0).light(0, 0).normal(0.0F, 0.0F, 1.0F);
        consumer.vertex(pose, right, y).texture(1.0F, 0.0F).color(color)
                .overlay(symbol, 0).light(0, 0).normal(0.0F, 0.0F, 1.0F);
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
