package com.regionstory.client;

import com.regionstory.client.ui.RegionStoryPipelineRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** Shared text and shader drawing helpers for the client UI. */
public final class RegionStoryUi {
    private static final int SYMBOL_DIAMOND = 1;
    private static final int SYMBOL_ARROW = 2;
    private static final int SYMBOL_CHAT = 3;
    private static final int SYMBOL_STAR = 4;
    private static final int SYMBOL_CIRCLE = 5;
    private static final int SYMBOL_MAP_PIN = 6;
    private static final int SYMBOL_EXIT = 7;
    private static final int SYMBOL_COMPASS = 8;
    private static final int SYMBOL_RULE = 9;
    private static final int SYMBOL_CHAT_DOTS = 10;

    private RegionStoryUi() {
    }

    /** Returns one complete original -> highlight -> original click pulse. */
    public static float clickPulse(int ticks, int duration) {
        if (ticks < 0 || duration <= 0 || ticks > duration) return 0.0F;
        float progress = ticks / (float) duration;
        return (float) Math.sin(Math.PI * progress);
    }

    /** Blends ARGB values while keeping the shader API color-only. */
    public static int blend(int from, int to, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int a = Math.round(((from >>> 24) & 0xFF) * (1.0F - t) + ((to >>> 24) & 0xFF) * t);
        int r = Math.round(((from >>> 16) & 0xFF) * (1.0F - t) + ((to >>> 16) & 0xFF) * t);
        int g = Math.round(((from >>> 8) & 0xFF) * (1.0F - t) + ((to >>> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1.0F - t) + (to & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Open-ended panel: left semicircle, flat body, transparent right fade. */
    public static void drawOpenFadePanel(DrawContext context, int x, int y, int w, int h, boolean highlight) {
        context.drawTexturedQuad(Identifier.of("regionstory", "textures/gui/fade_panel_top.png"), x, y, (int) (x+h*0.5f), y+h,0, 1, 0, 1);
        context.drawTexturedQuad(Identifier.of("regionstory", "textures/gui/fade_panel.png"), (int) (x+h*0.5f), y, (int) (x+w-h*0.5f), y+h,0, 1, 0, 1);
        if (highlight) {
            int breathAlpha = Math.clamp((int) (((Math.sin(System.currentTimeMillis() / 200d) + 1.0) / 2.0) * 255), 0, 255);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("regionstory", "textures/gui/fade_panel_top_highlight.png"), x, y, 0f, 0f, (int) (h * 0.5f), h, (int) (h * 0.5f), h, breathAlpha << 24 | 0x00FFFFFF);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("regionstory", "textures/gui/fade_panel_highlight.png"), (int) (h * 0.5f) + x, y, 0f, 0f, w -h, h, w - h, h, breathAlpha << 24 | 0x00FFFFFF);
        }
    }


    public static void drawRule(DrawContext context, int x, int y, int width, int height,
                                int color) {
        RegionStoryPipelineRenderer.drawSymbolRect(context, x, y, width, height,
                SYMBOL_RULE, color);
    }

    /** Draws the reference chat icon without a raster asset. */
    public static void drawReferenceChatIcon(DrawContext context, int x, int y, int size) {
        int slot = Math.max(12, size);
        RegionStoryPipelineRenderer.drawSymbol(context, x, y, slot, SYMBOL_CHAT, 0xFFF6F7F8);
        int dotSize = Math.max(2, slot / 7);
        int dotY = y + Math.round(slot * 0.43F);
        int gap = Math.max(2, slot / 8);
        int firstX = x + (slot - dotSize * 3 - gap * 2) / 2;
        for (int i = 0; i < 3; i++) {
            RegionStoryPipelineRenderer.drawSymbol(context, firstX + i * (dotSize + gap),
                    dotY, dotSize, SYMBOL_CHAT_DOTS, 0xFF67717A);
        }
    }

    /**
     * External PNG icons remain supported for data packs. Built-in icons are shader symbols.
     */
    public static void drawIcon(DrawContext context, MinecraftClient client, String icon,
                                int x, int y, int size, int color) {
        String key = icon == null ? "" : icon.trim().toLowerCase(Locale.ROOT);
        if (key.contains(":") && !key.startsWith("regionstory:icon/")) {
            try {
                Identifier texture = Identifier.of(icon);
                Identifier resource = texture.withPath(path -> {
                    String normalized = path;
                    if (!normalized.startsWith("textures/")) normalized = "textures/" + normalized;
                    if (!normalized.endsWith(".png")) normalized += ".png";
                    return normalized;
                });
                if (client.getResourceManager().getResource(resource).isPresent()) {
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, resource, x, y,
                            0.0F, 0.0F, size, size, size, size);
                    return;
                }
            } catch (Exception ignored) {
                // Invalid external IDs fall back to a built-in symbol.
            }
        }

        String symbol = key.startsWith("regionstory:icon/")
                ? key.substring("regionstory:icon/".length()) : key;
        int slot = Math.max(12, size);
        int backdrop = 0x66333E4D;
        RegionStoryPipelineRenderer.drawSymbol(context, x, y, slot, SYMBOL_CIRCLE, backdrop);
        switch (symbol) {
            case "star", "spark", "quest" ->
                    RegionStoryPipelineRenderer.drawSymbol(context, x, y, slot, SYMBOL_STAR, color);
            case "compass", "explore" ->
                    RegionStoryPipelineRenderer.drawSymbol(context, x, y, slot, SYMBOL_COMPASS, color);
            case "map", "location" ->
                    RegionStoryPipelineRenderer.drawSymbol(context, x, y, slot, SYMBOL_MAP_PIN, color);
            case "exit", "leave" ->
                    RegionStoryPipelineRenderer.drawSymbol(context, x, y, slot, SYMBOL_EXIT, color);
            default -> drawReferenceChatIcon(context, x, y, slot);
        }
    }
}
