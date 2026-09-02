package com.regionstory.client;

import com.regionstory.RegionStoryMod;
import com.regionstory.data.DialogueDefinition;
import com.regionstory.data.DialogueManager;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
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
    public static RegionStoryConfig config;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(RegionStoryConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(RegionStoryConfig.class).getConfig();

        ClientPlayNetworking.registerGlobalReceiver(RegionStoryMod.RegionHintPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (selectedRegionIndex < 0 || selectedRegionIndex >= currentRegion.size()) {
                        selectedRegionIndex = currentRegion.size() - 1;
                    }
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
                    if (context.client().currentScreen instanceof DialogueScreen) {
                        context.client().setScreen(null);
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
            if (TALK_KEY.wasPressed() && client.currentScreen == null && !currentRegion.isEmpty() && selectedRegionIndex >= 0) {
                sendStartDialogue(client, currentRegion.get(selectedRegionIndex));
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, Identifier.of(RegionStoryMod.MOD_ID, "region_hint"), DialogueRegionHint::render);
    }

    private static void sendStartDialogue(MinecraftClient client, String region) {
        if (client.player == null || client.currentScreen != null || currentRegion.isEmpty()) return;
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
}
