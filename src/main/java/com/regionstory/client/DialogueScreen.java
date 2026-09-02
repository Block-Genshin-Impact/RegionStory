package com.regionstory.client;

import com.regionstory.RegionStoryMod;
import com.regionstory.client.ui.RegionStoryUiMetrics;
import com.regionstory.client.ui.RegionStoryPipelineRenderer;
import com.regionstory.data.DialogueDefinition;
import com.tp4.genshinlib.client.GILText;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Dialogue screen. Shapes are shader-rendered; Minecraft TextRenderer remains the font backend. */
public final class DialogueScreen extends Screen {
    private final boolean hudVisible;
    private DialogueDefinition dialogue;
    private String entryId;
    private int introTicks = 0;
    private boolean transitionStarted;
    private boolean serverClosing;
    private int selectedOption = -1;
    private int selectedOptionTicks;
    public int keyboardSelectedOption = 0;
    private int hoveredOption = -1;
    private int hoverPulseTicks;
    public final List<int[]> optionRects = new ArrayList<>();
    private boolean typingAnimation = true;
    private long typingStartTime;

    public DialogueScreen(DialogueDefinition dialogue, String entryId) {
        super(Text.literal("RegionStory"));
        this.dialogue = dialogue;
        this.entryId = entryId;
        this.hudVisible = MinecraftClient.getInstance().options.hudHidden;
        MinecraftClient.getInstance().options.hudHidden = true;
        this.typingStartTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        if (!transitionStarted) {
            CameraTransitionController.beginEnter(client);
            transitionStarted = true;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Keep the world clear; no vanilla blur or opaque Screen background is used. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!typingAnimation) {
            introTicks = Math.min(RegionStoryUiMetrics.INTRO_TICKS, introTicks + 1);
        }
        if (hoveredOption >= 0) {
            hoverPulseTicks = (hoverPulseTicks + 1) % RegionStoryUiMetrics.HOVER_PULSE_PERIOD;
        } else {
            hoverPulseTicks = 0;
        }
        if (selectedOption >= 0) {
            selectedOptionTicks++;
            if (selectedOptionTicks > RegionStoryUiMetrics.OPTION_CLICK_FEEDBACK_TICKS) {
                int option = selectedOption;
                selectedOption = -1;
                selectedOptionTicks = 0;
                ClientPlayNetworkingBridge.choose(dialogue.id, entryId, option);
            }
        }
    }

    public void applyEntry(DialogueDefinition dialogue, String entryId) {
        typingAnimation = true;
        typingStartTime = System.currentTimeMillis();
        if (dialogue != null && dialogue.entry(entryId) != null) {
            this.dialogue = dialogue;
            this.entryId = entryId;
            this.selectedOption = -1;
            this.selectedOptionTicks = 0;
            this.hoveredOption = -1;
            this.keyboardSelectedOption = 0;
            this.hoverPulseTicks = 0;
            this.introTicks = 0;
            this.typingStartTime = System.currentTimeMillis();
        }
    }

    public void closeFromServer() {
        serverClosing = true;
        CameraTransitionController.beginExit(client);
        client.setScreen(null);
    }

    @Override
    public void removed() {
        MinecraftClient.getInstance().options.hudHidden = hudVisible;
        super.removed();
        if (!serverClosing) {
            CameraTransitionController.beginExit(client);
            ClientPlayNetworkingBridge.close(dialogue.id);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Shader 是 UI 的唯一面板后端；注册失败时安全停用 RegionStory 面板，避免回退到原版矩形。
        if (!RegionStoryPipelineRenderer.available()) return;

        DialogueDefinition.Entry entry = dialogue.entry(entryId);
        if (entry == null) return;

        List<String> dialogueLines = wrap(entry.text(), Math.max(80,
                width - RegionStoryUiMetrics.DIALOGUE_SIDE_PADDING * 2));
        int lineCount = Math.max(1, dialogueLines.size());
        int bandHeight = MathHelper.clamp(
                RegionStoryUiMetrics.DIALOGUE_BASE_HEIGHT
                        + Math.max(0, lineCount - 1) * RegionStoryUiMetrics.BODY_LINE_HEIGHT,
                RegionStoryUiMetrics.DIALOGUE_BASE_HEIGHT,
                RegionStoryUiMetrics.DIALOGUE_MAX_HEIGHT);
        int bandTop = height - bandHeight;
        context.drawTexturedQuad(Identifier.of("regionstory", "textures/gui/dialogue_background.png"), 0, height - bandHeight, width, height, 0, 1, 0, 1);
        int centerX = width / 2;
        int speakerY = bandTop + 12;
        GILText.textRender(context, entry.speaker(), centerX, speakerY).color(0xffffd34f).center().scale(1.3f).render();

        int lineY = speakerY + 14;
        if (entry.speakerTitle() != null && !entry.speakerTitle().isBlank()) {
            drawTitleRule(context, centerX, lineY + 4, entry.speakerTitle());
            GILText.textRender(context, entry.speaker(), centerX, lineY).color(0xffe9b94f).center().render();
            lineY += 12;
        }

        int continuationY = bandTop + bandHeight - 15;
        int bodyHeight = dialogueLines.size() * RegionStoryUiMetrics.BODY_LINE_HEIGHT;
        int bodyY = Math.min(lineY + 2, continuationY - bodyHeight - 20);

        int typingCount = (int) (System.currentTimeMillis() - typingStartTime) / RegionStoryUiMetrics.TYPING_SPEED_MILLISECOND;

        for (String line : dialogueLines) {
            if (typingCount >= line.length() || !typingAnimation) {
                GILText.textRender(context, line, centerX, bodyY).color(0xfff7f7f2).scale(1.3f).center().render();
            } else if (0 < typingCount) {
                GILText.textRender(context, line.substring(0, typingCount), centerX, bodyY).color(0xfff7f7f2).scale(1.3f).center(line).render();
            }
            typingCount -= line.length();
            bodyY += RegionStoryUiMetrics.BODY_LINE_HEIGHT;
        }
        if (typingCount >= 0) {
            typingAnimation = false;
        }
        if (!typingAnimation) {
            float intro = MathHelper.clamp(introTicks / (float) RegionStoryUiMetrics.INTRO_TICKS, 0.0F, 1.0F);
            optionRects.clear();
            if (entry.options().isEmpty()) {
                GILText.textRender(context, "◇", centerX - 6, continuationY - 6).color(0xffffc52e).scale(1.5f).outline(false).render();
                GILText.textRender(context, "◢", centerX, (float) (continuationY - 3.5 + Math.sin(System.currentTimeMillis() / 200d))).color(0xffffc52e).scale(0.7f).outline(false).rotate(45).render();

                return;
            }

            int optionX = MathHelper.clamp(
                    Math.round(width * RegionStoryUiMetrics.OPTION_ANCHOR_X),
                    RegionStoryUiMetrics.PANEL_MARGIN,
                    Math.max(RegionStoryUiMetrics.PANEL_MARGIN,
                            width - RegionStoryUiMetrics.PANEL_MARGIN - 1)) + Math.round((1.0F - intro) * 12.0F);
            int availableOptionWidth = Math.max(1,
                    width - optionX - RegionStoryUiMetrics.PANEL_MARGIN);
            int optionW = Math.min(availableOptionWidth,
                    Math.max(RegionStoryUiMetrics.OPTION_MIN_WIDTH, Math.round(width * 0.30F)));
            List<Integer> optionHeights = new ArrayList<>();
            int optionTotalHeight = 0;
            int optionTextWidth = Math.max(32, Math.round(
                    Math.max(32, optionW - 42) / RegionStoryUiMetrics.OPTION_TEXT_SCALE));
            for (DialogueDefinition.Option option : entry.options()) {
                int lines = Math.max(1, wrap(option.text(), optionTextWidth).size());
                int optionHeight = Math.max(RegionStoryUiMetrics.OPTION_HEIGHT,
                        lines * RegionStoryUiMetrics.OPTION_LINE_HEIGHT + 6);
                optionHeights.add(optionHeight);
                optionTotalHeight += optionHeight;
            }
            optionTotalHeight += Math.max(0,
                    (entry.options().size() - 1) * RegionStoryUiMetrics.OPTION_GAP);

            int optionY = Math.max(0, bandTop - optionTotalHeight);

            int optionStartY = optionY;
            int activeHover = -1;
            for (int i = 0; i < entry.options().size(); i++) {
                int optionHeight = optionHeights.get(i);
                if (mouseX >= optionX && mouseX <= optionX + optionW
                        && mouseY >= optionY && mouseY <= optionY + optionHeight) {
                    activeHover = i;
                }
                optionY += optionHeight + RegionStoryUiMetrics.OPTION_GAP;
            }
            if (activeHover != hoveredOption) {
                hoveredOption = activeHover;
                hoverPulseTicks = 0;
            }

            optionY = optionStartY;
            for (int i = 0; i < entry.options().size(); i++) {
                DialogueDefinition.Option option = entry.options().get(i);
                int optionHeight = optionHeights.get(i);
                optionRects.add(new int[]{optionX, optionY, optionW, optionHeight});

                boolean hover = i == hoveredOption || i == keyboardSelectedOption;
                // 从亮态开始，经暗态再回到亮态，循环提示当前可选项。
                float hoverPulse = hover
                        ? 0.25F + 0.75F * (0.5F + 0.5F * (float) Math.cos(
                        (hoverPulseTicks / (float) RegionStoryUiMetrics.HOVER_PULSE_PERIOD)
                                * Math.PI * 2.0))
                        : 0.0F;
                float clickPulse = i == selectedOption
                        ? RegionStoryUi.clickPulse(selectedOptionTicks,
                        RegionStoryUiMetrics.OPTION_CLICK_FEEDBACK_TICKS) : 0.0F;

                RegionStoryUi.drawOpenFadePanel(context, optionX, optionY, optionW, optionHeight, hover);

                if (hover) {
                    if (i == keyboardSelectedOption) {
                        int keyX = optionX - 22;
                        int keyY = optionY + 6;
                        int keyTextHeight = Math.max(1, Math.round(client.textRenderer.fontHeight
                                * RegionStoryUiMetrics.HINT_KEY_TEXT_SCALE));
                        context.drawTexturedQuad(Identifier.of("regionstory", "textures/gui/hint_key.png"), keyX - 4, (int) ((keyTextHeight + 2) * 0.1f + keyY - 2), keyX + keyTextHeight, (int) ((keyTextHeight + 2) * 0.9f + keyY),0, 1, 0, 1);
                        GILText.textRender(context, "F", keyX, keyY).scale(RegionStoryUiMetrics.HINT_KEY_TEXT_SCALE).color(0xff000000).outline(false).render();
                    }
                    GILText.textRender(context, "◢", (float) (optionX - 10 + Math.sin(System.currentTimeMillis() / 200d) - 1), optionY + 11).scale(0.8f).rotate(-45).outline(false).render();
                }

                RegionStoryUi.drawIcon(context, client, option.icon(),
                        optionX + 7, optionY + (optionHeight - RegionStoryUiMetrics.OPTION_ICON_SIZE) / 2,
                        RegionStoryUiMetrics.OPTION_ICON_SIZE,
                        RegionStoryUi.blend(0xFFF8FAFC, 0xFFFFF5B8,
                                Math.max(hoverPulse, clickPulse)));

                List<String> optionLines = wrap(option.text(), optionTextWidth);
                int textLineHeight = Math.max(1, Math.round(
                        textRenderer.fontHeight * RegionStoryUiMetrics.OPTION_TEXT_SCALE));
                int textBlockHeight = optionLines.size() * textLineHeight;
                float textY = optionY + (optionHeight - textBlockHeight) / 2f;
                for (String line : optionLines) {
                    GILText.textRender(context, line, optionX + 24, textY).color(RegionStoryUi.blend(hover ? 0xFFFFFFFF : 0xFFF8F8F8, 0xFFFFF8C8, clickPulse)).scale(RegionStoryUiMetrics.OPTION_TEXT_SCALE).render();
                    textY += textLineHeight;
                }
                optionY += optionHeight + RegionStoryUiMetrics.OPTION_GAP;
            }
        }
    }

    private void drawTitleRule(DrawContext context, int centerX, int y, String title) {
        int titleWidth = GILText.width(title);
        int gap = titleWidth / 2 + 13;
        int halfRule = Math.max(44, Math.min(150, gap + 38));
        int color = 0xD9D8A54A;
        int softColor = 0x8FD8A54A;
        RegionStoryUi.drawRule(context, centerX - halfRule, y, Math.max(8, halfRule - gap), 2, color);
        RegionStoryUi.drawRule(context, centerX + gap, y, Math.max(8, halfRule - gap), 2, color);
        RegionStoryUi.drawRule(context, centerX - halfRule, y + 3,
                Math.max(8, halfRule - gap - 6), 1, softColor);
        RegionStoryUi.drawRule(context, centerX + gap + 6, y + 3,
                Math.max(8, halfRule - gap - 6), 1, softColor);
    }

    private List<String> wrap(String value, int maxWidth) {
        String text = value == null ? "" : value;
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int limit = Math.max(1, maxWidth);
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                lines.add(line.toString());
                line.setLength(0);
                continue;
            }
            String candidate = line + String.valueOf(character);
            if (!line.isEmpty() && GILText.width(candidate) * RegionStoryUiMetrics.OPTION_TEXT_SCALE > limit) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(character);
        }
        if (!line.isEmpty() || lines.isEmpty()) lines.add(line.toString());
        return lines;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (selectedOption >= 0) return true;

        if (typingAnimation) {
            typingAnimation = false;
            return true;
        }

        for (int i = 0; i < optionRects.size(); i++) {
            int[] rect = optionRects.get(i);
            if (click.x() >= rect[0] && click.x() <= rect[0] + rect[2]
                    && click.y() >= rect[1] && click.y() <= rect[1] + rect[3]) {
                selectedOption = i;
                selectedOptionTicks = 0;
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_ITEM_PICKUP);
                }
                return true;
            }
        }
        DialogueDefinition.Entry entry = dialogue.entry(entryId);
        if (entry != null && entry.options().isEmpty()) {
            ClientPlayNetworkingBridge.advance(dialogue.id, entryId);
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_ITEM_PICKUP);
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == 256) {
            ClientPlayNetworkingBridge.close(dialogue.id);
            return true;
        }
        if (input.getKeycode() == 32 || input.getKeycode() == 70) {  // F或空格
            if (typingAnimation) {
                typingAnimation = false;
                return true;
            }
        }
        if (!optionRects.isEmpty()) {
            if (input.getKeycode() == 70) {
                selectedOption = keyboardSelectedOption;
                selectedOptionTicks = 0;
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_ITEM_PICKUP);
                }
                return true;
            } else if (input.getKeycode() == 83) {
                keyboardSelectedOption ++;
                if (keyboardSelectedOption >= optionRects.size()) {
                    keyboardSelectedOption = 0;
                }
            } else if (input.getKeycode() == 87) {
                keyboardSelectedOption --;
                if (keyboardSelectedOption < 0) {
                    keyboardSelectedOption = optionRects.size() - 1;
                }
            }
        }
        if (input.getKeycode() == 32 || input.getKeycode() == 70) {  // F或空格
            DialogueDefinition.Entry entry = dialogue.entry(entryId);
            if (entry != null && entry.options().isEmpty()) {
                ClientPlayNetworkingBridge.advance(dialogue.id, entryId);
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_ITEM_PICKUP);
                }
            }
            return true;
        }
        return true;
    }

    private static final class ClientPlayNetworkingBridge {
        private static void advance(String dialogueId, String entryId) {
            ClientPlayNetworking.send(new RegionStoryMod.AdvanceDialoguePayload(dialogueId, entryId));
        }

        private static void choose(String dialogueId, String entryId, int optionIndex) {
            ClientPlayNetworking.send(new RegionStoryMod.SelectOptionPayload(dialogueId, entryId, optionIndex));
        }

        private static void close(String dialogueId) {
            ClientPlayNetworking.send(new RegionStoryMod.CloseDialoguePayload(dialogueId));
        }
    }
}
