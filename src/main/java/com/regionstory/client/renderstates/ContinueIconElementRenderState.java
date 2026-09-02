package com.regionstory.client.renderstates;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record ContinueIconElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, int color, @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds) implements SimpleGuiElementRenderState {
    public ContinueIconElementRenderState(Matrix3x2fc pose, int color, @Nullable ScreenRect scissorArea) {
        this(RenderPipelines.GUI, TextureSetup.empty(), pose, color, scissorArea, createBounds(pose, scissorArea));
    }

    public void setupVertices(VertexConsumer vertices) {
        float offset = (float) Math.sin(System.currentTimeMillis() / 150d) + 1;

        vertices.vertex(pose, 0f, 5f).color(color | 0x90000000);
        vertices.vertex(pose, 0f, 4f).color(color | 0x90000000);
        vertices.vertex(pose, -4f, 0f).color(color | 0x90000000);
        vertices.vertex(pose, -5f, 0f).color(color | 0x90000000);

        vertices.vertex(pose, 0f, 8f).color(0x00ffffff);
        vertices.vertex(pose, 0f, 5f).color(color | 0x20000000);
        vertices.vertex(pose, -5f, 0f).color(color | 0x20000000);
        vertices.vertex(pose, -8f, 0f).color(0x00ffffff);

        vertices.vertex(pose, 0f, 4f).color(color | 0x90000000);
        vertices.vertex(pose, 0f, 5f).color(color | 0x90000000);
        vertices.vertex(pose, 5f, 0f).color(color | 0x90000000);
        vertices.vertex(pose, 4f, 0f).color(color | 0x90000000);

        vertices.vertex(pose, 0f, 5f).color(color | 0x20000000);
        vertices.vertex(pose, 0f, 8f).color(0x00ffffff);
        vertices.vertex(pose, 8f, 0f).color(0x00ffffff);
        vertices.vertex(pose, 5f, 0f).color(color | 0x20000000);

        vertices.vertex(pose, 0f, -4f).color(color | 0x90000000);
        vertices.vertex(pose, 0f, -5f).color(color | 0x90000000);
        vertices.vertex(pose, -5f, 0f).color(color | 0x90000000);
        vertices.vertex(pose, -4f, 0f).color(color | 0x90000000);

        vertices.vertex(pose, 0f, -5f).color(color | 0x20000000);
        vertices.vertex(pose, 0f, -8f).color(0x00ffffff);
        vertices.vertex(pose, -8f, 0f).color(0x00ffffff);
        vertices.vertex(pose, -5f, 0f).color(color | 0x20000000);

        vertices.vertex(pose, 0f, -5f).color(color | 0x90000000);
        vertices.vertex(pose, 0f, -4f).color(color | 0x90000000);
        vertices.vertex(pose, 4f, 0f).color(color | 0x90000000);
        vertices.vertex(pose, 5f, 0f).color(color | 0x90000000);

        vertices.vertex(pose, 0f, -8f).color(0x00ffffff);
        vertices.vertex(pose, 0f, -5f).color(color | 0x20000000);
        vertices.vertex(pose, 5f, 0f).color(color | 0x20000000);
        vertices.vertex(pose, 8f, 0f).color(0x00ffffff);

        vertices.vertex(pose, 0, 4).color(color | 0x20000000);
        vertices.vertex(pose, 4, 0).color(color | 0x20000000);
        vertices.vertex(pose, 0, -4).color(color | 0x20000000);
        vertices.vertex(pose, -4, 0).color(color | 0x20000000);

        vertices.vertex(pose, 2.8f, offset).color(color | 0xff000000);
        vertices.vertex(pose, -2.8f, offset).color(color | 0xff000000);
        vertices.vertex(pose, 0f, offset + 2.8f).color(color | 0xff000000);
        vertices.vertex(pose, 2.8f, offset).color(color | 0xff000000);
    }

    private static @Nullable ScreenRect createBounds(Matrix3x2fc pose, @Nullable ScreenRect scissorArea) {
        ScreenRect screenRect = (new ScreenRect(-10, -10, 20, 20)).transformEachVertex(pose);
        return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
    }
}
