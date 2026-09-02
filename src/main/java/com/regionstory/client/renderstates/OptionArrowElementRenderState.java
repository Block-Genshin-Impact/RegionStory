package com.regionstory.client.renderstates;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record OptionArrowElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, int color, @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds) implements SimpleGuiElementRenderState {
    public OptionArrowElementRenderState(Matrix3x2fc pose, @Nullable ScreenRect scissorArea) {
        this(RenderPipelines.GUI, TextureSetup.empty(), pose, 0xffffffff, scissorArea, createBounds(pose, scissorArea));
    }

    @Override
    public void setupVertices(VertexConsumer vertices) {
        float offset = (float) Math.sin(System.currentTimeMillis() / 150d) * 2 - 10;

        vertices.vertex(pose, offset, 5f).color(color);
        vertices.vertex(pose, offset + 5f, 0f).color(color);
        vertices.vertex(pose, offset, -5f).color(color);
        vertices.vertex(pose, offset, 5f).color(color);
    }

    private static @Nullable ScreenRect createBounds(Matrix3x2fc pose, @Nullable ScreenRect scissorArea) {
        ScreenRect screenRect = (new ScreenRect(-10, -10, 20, 20)).transformEachVertex(pose);
        return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
    }
}
