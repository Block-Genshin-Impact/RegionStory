package com.regionstory.client;

import com.regionstory.RegionStoryMod;
import com.regionstory.client.ui.RegionStoryPipelineRenderer;
import com.regionstory.client.ui.RegionStoryUiMetrics;
import com.regionstory.data.DialogueDefinition;
import com.regionstory.data.DialogueManager;
import com.tp4.genshinlib.client.GILText;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client entry point for the region hint, key binding, and dialogue payloads. */
public final class RegionStoryClient implements ClientModInitializer {
    public static final KeyBinding TALK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.regionstory.talk", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, KeyBinding.Category.create(RegionStoryMod.REGION_HINT)));
    public static List<String > currentRegion = new ArrayList<>();
    public static Map<String, String> currentPrompt = new HashMap<>();
    public static Map<String, String> currentIcon = new HashMap<>();

    public static int selectedRegionIndex = -1;
    private static final DialogueManager CLIENT_DIALOGUES = new DialogueManager();

    @Override
    public void onInitializeClient() {
        RegionStoryConfig.load();

        ClientPlayNetworking.registerGlobalReceiver(RegionStoryMod.RegionHintPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (!currentRegion.contains(payload.regionId())) {
                        currentRegion.add(payload.regionId());
                        currentPrompt.put(payload.regionId(), payload.prompt());
                        currentIcon.put(payload.regionId(), payload.icon());
                    } else {
                        currentPrompt.replace(payload.regionId(), payload.prompt());
                        currentIcon.replace(payload.regionId(), payload.icon());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(RegionStoryMod.RemoveRegionHintPayload.ID, ((payload, context) ->
                context.client().execute(() -> {
                    if (currentRegion.contains(payload.regionId())) {
                        currentRegion.remove(payload.regionId());
                        currentPrompt.remove(payload.regionId());
                        currentIcon.remove(payload.regionId());
                    }
                })));

        ClientPlayNetworking.registerGlobalReceiver(RegionStoryMod.OpenDialoguePayload.ID, (payload, context) ->
                context.client().execute(() -> receiveDialogue(payload)));

        ClientPlayNetworking.registerGlobalReceiver(RegionStoryMod.CloseDialoguePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().currentScreen instanceof DialogueScreen screen) {
                        screen.closeFromServer();
                    }
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentRegion = new ArrayList<>();
            currentPrompt = new HashMap<>();
            currentIcon = new HashMap<>();
            CLIENT_DIALOGUES.clear();
            CameraTransitionController.reset(client);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CameraTransitionController.tick(client);
            if (TALK_KEY.wasPressed() && client.currentScreen == null && !currentRegion.isEmpty()) {
                sendStartDialogue(client, currentRegion.get(selectedRegionIndex));
            }
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (!MinecraftClient.getInstance().options.hudHidden) {
                if (selectedRegionIndex < 0 || selectedRegionIndex >= currentRegion.size()) {
                    selectedRegionIndex = currentRegion.size() - 1;
                }
                currentRegion.forEach((region) -> RegionStoryClient.renderHint(context, region));
            }
        });
    }

    private static void sendStartDialogue(MinecraftClient client, String region) {
        if (client.player == null || client.currentScreen != null || currentRegion.isEmpty()
                || !RegionStoryPipelineRenderer.available()) return;
        ClientPlayNetworking.send(new RegionStoryMod.StartDialoguePayload(region));
    }

    private static void receiveDialogue(RegionStoryMod.OpenDialoguePayload payload) {
        DialogueDefinition dialogue = CLIENT_DIALOGUES.loadSerialized(payload.json());
        if (dialogue == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof DialogueScreen screen) {
            screen.applyEntry(dialogue, payload.entryId());
        } else {
            client.setScreen(new DialogueScreen(dialogue, payload.entryId()));
        }
    }

    private static void renderHint(DrawContext context, String region) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null || currentRegion.isEmpty()
                || !RegionStoryPipelineRenderer.available()) return;

        int[] bounds = hintBounds(client, region);
        int x = bounds[0];
        int y = bounds[1] - currentRegion.indexOf(region) * 25;
        int boxWidth = bounds[2];
        int boxHeight = bounds[3];
        int panelX = x + RegionStoryUiMetrics.HINT_KEY_WIDTH + RegionStoryUiMetrics.HINT_KEY_GAP;
        int panelWidth = boxWidth - RegionStoryUiMetrics.HINT_KEY_WIDTH
                - RegionStoryUiMetrics.HINT_KEY_GAP;
        boolean selected = currentRegion.indexOf(region) == selectedRegionIndex;

        RegionStoryUi.drawOpenFadePanel(context, panelX, y, panelWidth, boxHeight, selected);

        int iconSize = RegionStoryUiMetrics.HINT_ICON_SIZE;
        int iconX = panelX + 9;
        int iconY = y + (boxHeight - iconSize) / 2;
        if (currentIcon.get(region).isBlank()) {
            RegionStoryUi.drawReferenceChatIcon(context, iconX, iconY, iconSize);
        } else {
            RegionStoryUi.drawIcon(context, client, currentIcon.get(region), iconX, iconY, iconSize,
                    RegionStoryUi.blend(0xFFF6F8FB, 0xFFFFF3AE, 0));
        }

        String label = displayHintLabel(client, Math.max(32, panelWidth - 48), region);
        int labelHeight = Math.max(1, Math.round(client.textRenderer.fontHeight
                * RegionStoryUiMetrics.HINT_TEXT_SCALE));
        float labelY = y + Math.max(1, (boxHeight - labelHeight) / 2f);
        GILText.textRender(context, label, panelX + 30, labelY).color(RegionStoryUi.blend(0xFFF6F2E7, 0xFFFFF7C5, 0)).scale(RegionStoryUiMetrics.HINT_TEXT_SCALE).render();

        if (selected) {
            int keyHeight = Math.min(RegionStoryUiMetrics.HINT_KEY_HEIGHT, boxHeight);
            int keyY = y + (boxHeight - keyHeight) / 2;
            // The white F key remains an independent control outside the prompt panel.
            int keyTextWidth = GILText.width("F");
            int keyTextHeight = Math.max(1, Math.round(client.textRenderer.fontHeight
                    * RegionStoryUiMetrics.HINT_KEY_TEXT_SCALE));
            float keyTextX = x + (RegionStoryUiMetrics.HINT_KEY_WIDTH - keyTextWidth * RegionStoryUiMetrics.HINT_KEY_TEXT_SCALE) / 2.0F - 10;
            float keyTextY = keyY + (keyHeight - keyTextHeight) / 2.0F + 1;
            context.drawTexturedQuad(Identifier.of("regionstory", "textures/gui/hint_key.png"), (int) keyTextX - 4, (int) ((keyTextHeight + 2) * 0.1f + keyTextY - 2), (int) keyTextX + keyTextHeight, (int) ((keyTextHeight + 2) * 0.9f + keyTextY),0, 1, 0, 1);
            GILText.textRender(context, "F", keyTextX, keyTextY).scale(RegionStoryUiMetrics.HINT_KEY_TEXT_SCALE).color(0xff000000).outline(false).render();
            GILText.textRender(context, "◢", (float) (panelX - 10 + Math.sin(System.currentTimeMillis() / 200d) -1), labelY + 6).scale(0.8f).rotate(-45).outline(false).render();
        }
    }

    private static int[] hintBounds(MinecraftClient client, String region) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int textWidth = Math.round(GILText.width(hintLabel(region))
                * RegionStoryUiMetrics.HINT_TEXT_SCALE);
        int maxPanelWidth = Math.max(96, width - RegionStoryUiMetrics.PANEL_MARGIN * 2
                - RegionStoryUiMetrics.HINT_KEY_WIDTH - RegionStoryUiMetrics.HINT_KEY_GAP);
        int panelWidth = Math.min(maxPanelWidth, Math.max(140, textWidth + 48));
        int boxWidth = panelWidth + RegionStoryUiMetrics.HINT_KEY_WIDTH
                + RegionStoryUiMetrics.HINT_KEY_GAP;
        int boxHeight = RegionStoryUiMetrics.HINT_HEIGHT;
        int panelX = MathHelper.clamp((int) (width * RegionStoryUiMetrics.HINT_ANCHOR_X),
                RegionStoryUiMetrics.PANEL_MARGIN + RegionStoryUiMetrics.HINT_KEY_WIDTH
                        + RegionStoryUiMetrics.HINT_KEY_GAP,
                Math.max(RegionStoryUiMetrics.PANEL_MARGIN,
                        width - panelWidth - RegionStoryUiMetrics.PANEL_MARGIN));
        int x = panelX - RegionStoryUiMetrics.HINT_KEY_WIDTH - RegionStoryUiMetrics.HINT_KEY_GAP;
        int y = MathHelper.clamp((int) (height * RegionStoryUiMetrics.HINT_ANCHOR_Y),
                RegionStoryUiMetrics.PANEL_MARGIN,
                Math.max(RegionStoryUiMetrics.PANEL_MARGIN,
                        height - boxHeight - RegionStoryUiMetrics.PANEL_MARGIN));
        return new int[]{x, y, boxWidth, boxHeight};
    }

    private static String hintLabel(String region) {
        String label = currentPrompt == null ? "" : currentPrompt.get(region).trim();
        label = label.replaceFirst("(?i)^\\s*(按\\s*)?f\\s*", "").trim();
        return label.isEmpty() ? "对话" : label;
    }

    private static String displayHintLabel(MinecraftClient client, int maxWidth, String region) {
        String label = hintLabel(region);
        int scaledWidth = Math.round(GILText.width(label)
                * RegionStoryUiMetrics.HINT_TEXT_SCALE);
        if (scaledWidth <= maxWidth) return label;

        String suffix = "...";
        String candidate = label;
        while (candidate.length() > 1
                && Math.round(GILText.width(candidate + suffix)
                * RegionStoryUiMetrics.HINT_TEXT_SCALE) > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate + suffix;
    }
}
