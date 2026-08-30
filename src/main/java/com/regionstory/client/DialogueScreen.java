package com.regionstory.client;

import com.regionstory.RegionStoryMod;
import com.regionstory.client.renderstates.ContinueIconElementRenderState;
import com.regionstory.data.DialogueDefinition;
import com.tp4.genshinlib.client.GILButton;
import com.tp4.genshinlib.client.GILText;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

public final class DialogueScreen extends Screen {
    private static final int TYPING_SPEED_MILLISECOND = 30;
    private static final int BODY_LINE_HEIGHT = 12;
    private static final int DIALOGUE_BASE_HEIGHT = 88;
    private static final float DIALOGUE_MIN_HEIGHT = 0.35f;
    private static final int DIALOGUE_SIDE_PADDING = 60;
    private static final int HOVER_DIAMOND_Y = 10;
    private static final float OPTION_DELTA = 0.2f;
    private static final float OPTION_TRANSITION = 10;
    private static final int HISTORY_BACKGROUND_ALPHA = 240;
    private static final float HISTORY_BACKGROUND_ALPHA_DELTA = 0.4f;

    private static final int K_W = 87;
    private static final int K_S = 83;
    private static final int K_F = 70;
    private static final int K_SPACE = 32;
    private static final int K_ESC = 256;

    private float option_offset = 0;
    private float history_background_alpha = 0;

    private int mouseHoveredOption = -1;
    private int keyboardSelectedOption = -1;

    private DialogueDefinition dialogue;
    private String entryId;

    private final boolean hudVisible;
    private boolean typingAnimation = true;
    private long typingStartTime;
    private long lastTransitionTickTime = -1;
    private long autoplayTime = -1;
    private Statue statue = Statue.DIALOGUE;
    private final List<HistoryItem> history = new ArrayList<>();
    private final GILButton.ButtonManager buttonManager = GILButton.getManager();

    public DialogueScreen(DialogueDefinition dialogue, String entryId) {
        super(Text.literal("RegionStory"));

        buttonManager.addButton("autoplay");
        buttonManager.addButton("history");
        buttonManager.addButton("hidden");

        this.hudVisible = MinecraftClient.getInstance().options.hudHidden;
        MinecraftClient.getInstance().options.hudHidden = true;

        CameraTransitionController.beginEnter(client);
        applyEntry(dialogue, entryId);
    }

    @Override
    public boolean shouldPause() {return false;}

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ((int) history_background_alpha) << 24 | 0x162231);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        switch (statue) {
            case DIALOGUE -> {
                if (input.getKeycode() == K_ESC) {
                    ClientPlayNetworking.send(new RegionStoryMod.CloseDialoguePayload(dialogue.id));
                } else if (input.getKeycode() == K_F || input.getKeycode() == K_SPACE) {
                    if (typingAnimation) {
                        typingAnimation = false;
                    } else if (!dialogue.entry(entryId).options().isEmpty()) {
                        if (keyboardSelectedOption < 0 || input.getKeycode() != K_F) {return true;}
                        history.add(new HistoryItem(dialogue.entry(entryId).options().get(keyboardSelectedOption).text()));
                        ClientPlayNetworking.send(new RegionStoryMod.SelectOptionPayload(dialogue.id, entryId, keyboardSelectedOption));
                        playPopSound();
                    } else {
                        ClientPlayNetworking.send(new RegionStoryMod.AdvanceDialoguePayload(dialogue.id, entryId));
                        playPopSound();
                    }
                } else if (input.getKeycode() == K_W) {
                    keyboardSelectionChange(1);
                } else if (input.getKeycode() == K_S) {
                    keyboardSelectionChange(-1);
                }
            }
            case HIDE_UI -> statue = Statue.DIALOGUE;
            case HISTORY -> {
                if (input.getKeycode() == K_ESC) {
                    statue = Statue.DIALOGUE;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        switch (statue) {
            case DIALOGUE -> {
                if (buttonManager.isButtonHovered("autoplay")) {
                    RegionStoryClient.config.autoplay = !RegionStoryClient.config.autoplay;
                    AutoConfig.getConfigHolder(RegionStoryConfig.class).save();
                } else if (buttonManager.isButtonHovered("hidden")) {
                    statue = Statue.HIDE_UI;
                } else if (buttonManager.isButtonHovered("history")) {
                    statue = Statue.HISTORY;
//                    history.forEach(historyItem -> {
//                        if (historyItem.isOption) {
//                            System.out.println("Option : " + historyItem.dialogue);
//                        } else {
//                            System.out.println(historyItem.character + " : " + historyItem.dialogue);
//                        }
//                    });
                } else if (typingAnimation) {
                    typingAnimation = false;
                    return true;
                } else if (!dialogue.entry(entryId).options().isEmpty()) {
                    if (mouseHoveredOption < 0) {return true;}
                    history.add(new HistoryItem(dialogue.entry(entryId).options().get(mouseHoveredOption).text()));
                    ClientPlayNetworking.send(new RegionStoryMod.SelectOptionPayload(dialogue.id, entryId, mouseHoveredOption));
                    playPopSound();
                } else {
                    autoplayTime = -1;
                    ClientPlayNetworking.send(new RegionStoryMod.AdvanceDialoguePayload(dialogue.id, entryId));
                    playPopSound();
                }
            }
            case HIDE_UI ->  statue = Statue.DIALOGUE;
            case HISTORY -> {}
        }
        return true;
    }

    @Override
    public void removed() {
        MinecraftClient.getInstance().options.hudHidden = hudVisible;
        CameraTransitionController.beginExit(client);
    }

    @Override
    public void tick() {
        switch (statue) {
            case DIALOGUE -> {
                if (RegionStoryClient.config.autoplay && dialogue.entry(entryId).options().isEmpty()) {
                    if (!typingAnimation && autoplayTime < 0) {
                        autoplayTime = System.currentTimeMillis() + (long) (RegionStoryClient.config.autoplayDelay * 1000);
                    }
                    if (autoplayTime >= 0 && autoplayTime <= System.currentTimeMillis()) {
                        playPopSound();
                        ClientPlayNetworking.send(new RegionStoryMod.AdvanceDialoguePayload(dialogue.id, entryId));
                        autoplayTime = -1;
                    }
                }
            }
            case HISTORY -> {
            }
        }

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        transitionTick();
        switch (statue) {
            case DIALOGUE -> {
                DialogueDefinition.Entry entry = dialogue.entry(entryId);
                List<String> dialogueLines = splitText(entry.text());
                float renderHeight = height - scale(Math.max(DIALOGUE_BASE_HEIGHT + dialogueLines.size() * BODY_LINE_HEIGHT, DIALOGUE_MIN_HEIGHT * height));

                renderBackground(context, renderHeight);
                renderHeight = renderSpeakerName(context, renderHeight, entry);
                renderDialogueText(context, renderHeight, dialogueLines);
                renderContinueIcon(context, entry);
                renderOptions(context, entry, dialogueLines, mouseX, mouseY);
                renderButtons(context, mouseX, mouseY);
            }
            case HIDE_UI -> {}
            case HISTORY -> {}
        }
    }

    private void transitionTick() {
        // 由于原版的tick方法每秒执行20次 (每50ms计算一次) ，会导致过度动画存在明显卡顿
        // 因此让过度动画在渲染时计算，并添加限制每16ms计算一次
        if (System.currentTimeMillis() - lastTransitionTickTime < 16) {
            return;
        }
        lastTransitionTickTime = System.currentTimeMillis();
        switch (statue) {
            case DIALOGUE -> {
                if (!typingAnimation) {
                    option_offset = MathHelper.lerp(OPTION_DELTA, option_offset, 0);
                }
                history_background_alpha = MathHelper.lerp(HISTORY_BACKGROUND_ALPHA_DELTA, history_background_alpha, 0);
            }
            case HISTORY -> {
                history_background_alpha = MathHelper.lerp(HISTORY_BACKGROUND_ALPHA_DELTA, history_background_alpha, HISTORY_BACKGROUND_ALPHA);
            }
        }
    }

    private void renderBackground(DrawContext context, float renderHeight) {
        context.drawTexturedQuad(Identifier.of(RegionStoryMod.MOD_ID, "textures/gui/dialogue_background.png"), 0, (int) renderHeight, width, height, 0, 1, 0, 1);
    }

    private float renderSpeakerName(DrawContext context, float renderHeight, DialogueDefinition.Entry entry) {
        renderHeight += scale(20);
        GILText.textRender(context, entry.speaker(), width / 2f, renderHeight).color(0xffffd34f).center().scale(scale(1.3f)).render();
        if (entry.speakerTitle() != null && !entry.speakerTitle().isBlank()) {
            renderHeight += scale(16);
            GILText.textRender(context, entry.speakerTitle(), width / 2f, renderHeight).color(0xffe9b94f).center().scale(scale(1f)).render();
            // TODO: 名称称号两侧的装饰线
        } else {
            // TODO: 名称与正文间的分割线
        }
        return renderHeight;
    }

    private void renderDialogueText(DrawContext context, float renderHeight, List<String> dialogueLines) {
        // 由当前时间减去打字机动画开始时间得出当前应该显示的字数
        int typingCount = (int) (System.currentTimeMillis() - typingStartTime) / TYPING_SPEED_MILLISECOND;
        renderHeight += scale(16);

        for (String line : dialogueLines) {
            if (typingCount >= line.length() || !typingAnimation) {  // 如果typingCount>=文本长度，说明本行文本已经完全显示
                GILText.textRender(context, line, width / 2f, renderHeight).color(0xfff7f7f2).scale(scale(1.3f)).center().render();
            } else if (typingCount > 0) {  // 如果文本长度>typingCount>0，说明打字机动画进行到本行，渲染部分文本
                GILText.textRender(context, line.substring(0, typingCount), width / 2f, renderHeight).color(0xfff7f7f2).scale(scale(1.3f)).center(line).render();
            }  // 否则说明本行文本还不应该渲染
            typingCount -= line.length();  // 获取剩余字数
            renderHeight += scale(BODY_LINE_HEIGHT);
        }

        if (typingCount >= 0) {  // 如果在完全渲染完后typingCount>=0，即渲染字数大于等于总字数，说明打字机动画结束
            typingAnimation = false;
        }
    }

    private void renderContinueIcon(DrawContext context, DialogueDefinition.Entry entry) {
        if (typingAnimation || !entry.options().isEmpty()) return;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(width / 2f, height - HOVER_DIAMOND_Y);
        context.getMatrices().scale(scale(0.95f));
        context.state.addSimpleElement(new ContinueIconElementRenderState(new Matrix3x2f(context.getMatrices()), 0xffb02d, context.scissorStack.peekLast()));
        context.getMatrices().popMatrix();
    }

    private void renderOptions(DrawContext context, DialogueDefinition.Entry entry, List<String> dialogueLines, int mouseX, int mouseY) {
        if (typingAnimation) return;
        float y = height - scale(Math.max(DIALOGUE_BASE_HEIGHT + dialogueLines.size() * BODY_LINE_HEIGHT, DIALOGUE_MIN_HEIGHT * height) + 20);
        int index = 0;
        mouseHoveredOption = -1;
        for (DialogueDefinition.Option option : entry.options()) {
            float x = (1 - scale(1 - DialogueRegionHint.OPTION_ANCHOR_X)) * width;
            boolean mouseHover = x <= mouseX && mouseX <= x + scale(0.95 - DialogueRegionHint.OPTION_ANCHOR_X) * width && y <= mouseY && mouseY <= y + scale(DialogueRegionHint.OPTION_HEIGHT);
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(option_offset, 0);
            DialogueRegionHint.renderOption(context, (int) x, (int) y, option.text(), option.icon(), mouseHover, index == keyboardSelectedOption);
            context.getMatrices().popMatrix();
            y -= scale(DialogueRegionHint.OPTION_HEIGHT + DialogueRegionHint.OPTION_GAP);
            if (mouseHover) {mouseHoveredOption = index;}
            index++;
        }
    }

    private void renderButtons(DrawContext context, int mouseX, int mouseY) {
        if (RegionStoryClient.config.autoplay) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(16f, 16f);
            context.getMatrices().scale(7.9f);
            context.getMatrices().rotate((float) Math.toRadians(-System.currentTimeMillis() / 3d % 360));
            context.drawTexturedQuad(dialogue.entry(entryId).options().isEmpty() ? Identifier.of(RegionStoryMod.MOD_ID, "textures/gui/autoplay.png") : Identifier.of(RegionStoryMod.MOD_ID, "textures/gui/autoplay_disabled.png"), -1, -1, 1, 1, 0, 1, 0, 1);
            context.getMatrices().popMatrix();
        }
        buttonManager.renderButton("autoplay", context, 16f, 16f, 8f, mouseX, mouseY);
        GILText.textRender(context, RegionStoryClient.config.autoplay ? "播放中" : "自动", 26f , 16f - client.textRenderer.fontHeight / 2f).color(dialogue.entry(entryId).options().isEmpty() ? 0xffffffff : 0x40ffffff).render();
        buttonManager.renderButton("history", context, 68f, 16f, 8f, mouseX, mouseY);
        buttonManager.renderButton("hidden", context, 94f, 16f, 8f, mouseX, mouseY);
    }

    private float scale(double number) {return (float) (RegionStoryClient.config.scale * number);}

    private List<String> splitText(String text){
        int limit = width - (int) scale(DIALOGUE_SIDE_PADDING * 2);
        List<String> line = new ArrayList<>();
        line.add("");
        for (char character : text.toCharArray()) {
            if (character == '\n') {
                line.add("");
            } else {
                line.set(line.size() - 1, line.getLast() + character);
                if (scale(GILText.width(line.getLast()) * DialogueRegionHint.OPTION_TEXT_SCALE) > limit) {
                    line.add("");
                }
            }
        }
        return line;
    }

    public void applyEntry(DialogueDefinition dialogue, String entryId) {
        typingAnimation = true;
        typingStartTime = System.currentTimeMillis();
        if (dialogue != null && dialogue.entry(entryId) != null) {
            this.dialogue = dialogue;
            this.entryId = entryId;
            this.typingStartTime = System.currentTimeMillis();
            this.keyboardSelectedOption = dialogue.entry(entryId).options().size() - 1;
            buttonManager.setButtonDisabled("autoplay", !dialogue.entry(entryId).options().isEmpty());
            history.add(new HistoryItem(dialogue.entry(entryId).speaker(), dialogue.entry(entryId).text()));

            option_offset = OPTION_TRANSITION;
        }
    }

    public void keyboardSelectionChange(double vertical) {
        if (vertical > 0) {
            keyboardSelectedOption ++;
            if (keyboardSelectedOption >= dialogue.entry(entryId).options().size()) {
                keyboardSelectedOption = 0;
            }
        } else if (vertical < 0) {
            keyboardSelectedOption --;
            if (keyboardSelectedOption < 0) {
                keyboardSelectedOption = dialogue.entry(entryId).options().size() - 1;
            }
        }
    }

    public void playPopSound() {
        if (client.player != null) {
            client.player.getEntityWorld().playSoundClient(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 2f);
        }
    }

    public static class HistoryItem {
        public String character = "";
        public String dialogue;
        public boolean isOption = false;

        public HistoryItem(String character, String dialogue) {
            this.character = character;
            this.dialogue = dialogue;
        }

        public HistoryItem(String dialogue) {
            this.dialogue = dialogue;
            this.isOption = true;
        }
    }

    private enum Statue {
        DIALOGUE,
        HISTORY,
        HIDE_UI
    }
}
