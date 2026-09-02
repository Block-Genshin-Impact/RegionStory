package com.regionstory.client;

import com.regionstory.RegionStoryMod;
import com.regionstory.client.renderstates.OptionArrowElementRenderState;
import com.tp4.genshinlib.client.GILText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public class DialogueRegionHint {
    public static final int OPTION_HEIGHT = 24;
    public static final int OPTION_GAP = 5;
    public static final float OPTION_TEXT_SCALE = 1.3f;
    public static final float OPTION_ANCHOR_X = 0.7f;

    public static void render(DrawContext context, RenderTickCounter ignoreTickCounter) {
        float y = MinecraftClient.getInstance().getWindow().getScaledHeight() / 2f;
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int index = 0;
        for (String hint : RegionStoryClient.currentRegion) {
            renderOption(context, (int) ((1 - scale(1 - DialogueRegionHint.OPTION_ANCHOR_X)) * width), (int) y, RegionStoryClient.currentPrompt.get(hint), RegionStoryClient.currentIcon.get(hint), false, index == RegionStoryClient.selectedRegionIndex);
            y += scale(OPTION_HEIGHT + OPTION_GAP);
            index++;
        }
    }

    public static void renderOption(DrawContext context, int x, int y, String text, String icon, boolean mouseHover, boolean keyboardSelected) {
        int h = (int) scale(OPTION_HEIGHT);
        int w = (int) (scale(0.95 - OPTION_ANCHOR_X) * MinecraftClient.getInstance().getWindow().getScaledWidth());
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.drawTexturedQuad(Identifier.of(RegionStoryMod.MOD_ID, "textures/gui/fade_panel_top.png"), 0, 0, (int) (h*0.5f), h,0, 1, 0, 1);
        context.drawTexturedQuad(Identifier.of(RegionStoryMod.MOD_ID, "textures/gui/fade_panel.png"), (int) (h*0.5f), 0, (int) (w-h*0.5f), h,0, 1, 0, 1);
        ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
        if (manager.getResource(Identifier.of(icon)).isPresent()) {
            context.drawTexturedQuad(Identifier.of(icon), 4, 4, h - 4, h - 4, 0, 1, 0, 1);
        } else if (manager.getResource(Identifier.of(icon + ".png")).isPresent()) {
            context.drawTexturedQuad(Identifier.of(icon + ".png"), 4, 4, h - 4, h - 4, 0, 1, 0, 1);
        } else {
            context.drawTexturedQuad(Identifier.of(RegionStoryMod.MOD_ID, "textures/icon/dialogue.png"), 4, 4, h - 4, h - 4, 0, 1, 0, 1);
        }
        if (mouseHover || keyboardSelected) {
            int breathAlpha = Math.clamp((int) (((Math.sin(System.currentTimeMillis() / 200d) + 1.0) / 2.0) * 255), 0, 255);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, Identifier.of(RegionStoryMod.MOD_ID, "textures/gui/fade_panel_top_highlight.png"), 0, 0, 0f, 0f, (int) (h * 0.5f), h, (int) (h * 0.5f), h, breathAlpha << 24 | 0x00FFFFFF);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, Identifier.of(RegionStoryMod.MOD_ID, "textures/gui/fade_panel_highlight.png"), (int) (h * 0.5f), 0, 0f, 0f, w -h, h, w - h, h, breathAlpha << 24 | 0x00FFFFFF);
            context.getMatrices().translate(0, h / 2f);
            context.getMatrices().scale(RegionStoryClient.config.scale * 0.75f);
            context.state.addSimpleElement(new OptionArrowElementRenderState(new Matrix3x2f(context.getMatrices()), context.scissorStack.peekLast()));
        }
        context.getMatrices().popMatrix();
        GILText.textRender(context, text, x + scale(OPTION_HEIGHT), y + scale(OPTION_HEIGHT - MinecraftClient.getInstance().textRenderer.fontHeight * OPTION_TEXT_SCALE) / 2).scale(scale(OPTION_TEXT_SCALE)).render();
        if (keyboardSelected) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x - h * 1.2f, y + h * 0.225f);
            context.getMatrices().scale(0.6f, 0.55f);
            context.drawTexturedQuad(Identifier.of(RegionStoryMod.MOD_ID, "textures/gui/hint_key.png"), 0, 0, h, h, 0, 1, 0, 1);
            GILText.textRender(context, "F", h / 2f, h / 2f - scale(MinecraftClient.getInstance().textRenderer.fontHeight) / 1.1f).color(0xff000000).center().scale(scale(1.83f), scale(2f)).render();
            context.getMatrices().popMatrix();
        }
    }

    private static float scale(double number) {return (float) (RegionStoryClient.config.scale * number);}

}
