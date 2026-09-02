package com.regionstory.client.ui;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 自定义 GUI Shader 的注册和提交入口，面板本身不再走原版纹理矩形。 */
public final class RegionStoryPipelineRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("RegionStory/GUI");
    private static final Identifier PANEL_VERTEX = Identifier.of("regionstory", "core/regionstory_panel");
    private static final Identifier PANEL_FRAGMENT = Identifier.of("regionstory", "core/regionstory_panel");
    private static final Identifier SYMBOL_VERTEX = Identifier.of("regionstory", "core/regionstory_symbol");
    private static final Identifier SYMBOL_FRAGMENT = Identifier.of("regionstory", "core/regionstory_symbol");

    public static final RenderPipeline OPEN_FADE_PANEL = registerPanel(
            "pipeline/regionstory_open_fade_panel", "REGIONSTORY_OPEN_FADE");
    public static final RenderPipeline CAPSULE_PANEL = registerPanel(
            "pipeline/regionstory_capsule_panel", "REGIONSTORY_CAPSULE");
    public static final RenderPipeline DIALOGUE_BAND = registerPanel(
            "pipeline/regionstory_dialogue_band", "REGIONSTORY_BAND");
    public static final RenderPipeline SYMBOL = registerSymbol();

    private RegionStoryPipelineRenderer() {
    }

    public static boolean available() {
        return OPEN_FADE_PANEL != null && CAPSULE_PANEL != null
                && DIALOGUE_BAND != null && SYMBOL != null;
    }

    private static RenderPipeline registerPanel(String location, String define) {
        try {
            // 每种形状共享一套顶点结构，通过 Shader define 选择 SDF 分支。
            return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.of("regionstory", location))
                    .withVertexShader(PANEL_VERTEX)
                    .withFragmentShader(PANEL_FRAGMENT)
                    .withShaderDefine(define)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withDepthWrite(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                            VertexFormat.DrawMode.QUADS)
                    .build());
        } catch (Throwable throwable) {
            LOGGER.error("Unable to register RegionStory panel pipeline {}", location, throwable);
            return null;
        }
    }

    private static RenderPipeline registerSymbol() {
        try {
            return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.of("regionstory", "pipeline/regionstory_symbol"))
                    .withVertexShader(SYMBOL_VERTEX)
                    .withFragmentShader(SYMBOL_FRAGMENT)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withDepthWrite(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                            VertexFormat.DrawMode.QUADS)
                    .build());
        } catch (Throwable throwable) {
            LOGGER.error("Unable to register RegionStory symbol pipeline", throwable);
            return null;
        }
    }

    public static void drawOpenFadePanel(DrawContext context, float x, float y,
                                         float width, float height, int color) {
        if (OPEN_FADE_PANEL == null || width <= 0.0F || height <= 0.0F) return;
        context.state.addSimpleElement(RegionStoryPanelRenderState.of(
                OPEN_FADE_PANEL, context.getMatrices(), x, y, width, height,
                1.5F, Math.min(3.4F,
                        Math.max(1.4F, width / Math.max(1.0F, height) * 0.32F)), color));
    }

    public static void drawCapsule(DrawContext context, float x, float y,
                                   float width, float height, int color) {
        if (CAPSULE_PANEL == null || width <= 0.0F || height <= 0.0F) return;
        context.state.addSimpleElement(RegionStoryPanelRenderState.of(
                CAPSULE_PANEL, context.getMatrices(), x, y, width, height,
                2.0F, 0.0F, color));
    }

    public static void drawDialogueBand(DrawContext context, float x, float y,
                                        float width, float height, int color) {
        if (DIALOGUE_BAND == null || width <= 0.0F || height <= 0.0F) return;
        context.state.addSimpleElement(RegionStoryPanelRenderState.of(
                DIALOGUE_BAND, context.getMatrices(), x, y, width, height,
                0.0F, 0.0F, color));
    }

    public static void drawSymbol(DrawContext context, float x, float y, float size,
                                  int symbol, int color) {
        drawSymbolRect(context, x, y, size, size, symbol, color);
    }

    public static void drawSymbolRect(DrawContext context, float x, float y,
                                      float width, float height, int symbol, int color) {
        if (SYMBOL == null || width <= 0.0F || height <= 0.0F) return;
        context.state.addSimpleElement(RegionStorySymbolRenderState.of(
                SYMBOL, context.getMatrices(), x, y, width, height, symbol, color));
    }
}
