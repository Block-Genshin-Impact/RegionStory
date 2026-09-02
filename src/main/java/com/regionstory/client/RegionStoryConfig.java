package com.regionstory.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lightweight client JSON configuration. */
public final class RegionStoryConfig {
    private static final float DEFAULT_RIGHT_REAR_YAW_OFFSET = 18.0F;
    private static final float MAX_RIGHT_REAR_YAW_OFFSET = 75.0F;

    public static int enterDuration = 12;
    public static int exitDuration = 12;
    public static double thirdPersonDistance = 4.5D;
    public static double heightOffset = 0.6D;
    /** Positive values place the camera on the player's right rear side. */
    public static float yawOffset = DEFAULT_RIGHT_REAR_YAW_OFFSET;
    public static float pitchOffset = 8.0F;

    public static void load() {
        // 配置损坏时保留默认值；数值范围也在读取阶段统一限制，避免镜头参数导致异常。
        Path path = FabricLoader.getInstance().getConfigDir().resolve("regionstory.json");
        try {
            boolean migratedLegacyCameraAngle = false;
            if (Files.exists(path)) {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                enterDuration = clampInt(json, "enterDuration", enterDuration, 1, 200);
                exitDuration = clampInt(json, "exitDuration", exitDuration, 1, 200);
                thirdPersonDistance = clampDouble(json, "thirdPersonDistance",
                        thirdPersonDistance, 0.5D, 16.0D);
                heightOffset = clampDouble(json, "heightOffset", heightOffset, -4.0D, 4.0D);
                // 旧版将 180 度用于前视镜头。新版仅接受后视角两侧的小角度偏移，
                // 因此把超出范围的历史值迁移到稳定的右后视默认值。
                double configuredYawOffset = json.has("yawOffset")
                        ? json.get("yawOffset").getAsDouble() : yawOffset;
                if (Math.abs(configuredYawOffset) > MAX_RIGHT_REAR_YAW_OFFSET) {
                    yawOffset = DEFAULT_RIGHT_REAR_YAW_OFFSET;
                    migratedLegacyCameraAngle = true;
                } else {
                    yawOffset = (float) configuredYawOffset;
                }
                pitchOffset = (float) clampDouble(json, "pitchOffset", pitchOffset, -60.0D, 60.0D);
                if (migratedLegacyCameraAngle) {
                    Files.writeString(path,
                            new GsonBuilder().setPrettyPrinting().create().toJson(snapshot()),
                            StandardCharsets.UTF_8);
                }
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path,
                        new GsonBuilder().setPrettyPrinting().create().toJson(snapshot()),
                        StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // Invalid config uses the defaults so the client can still start.
        }
    }

    private static JsonObject snapshot() {
        JsonObject result = new JsonObject();
        result.addProperty("enterDuration", enterDuration);
        result.addProperty("exitDuration", exitDuration);
        result.addProperty("thirdPersonDistance", thirdPersonDistance);
        result.addProperty("heightOffset", heightOffset);
        result.addProperty("yawOffset", yawOffset);
        result.addProperty("pitchOffset", pitchOffset);
        return result;
    }

    private static int clampInt(JsonObject json, String key, int fallback, int min, int max) {
        int value = json.has(key) ? json.get(key).getAsInt() : fallback;
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(JsonObject json, String key, double fallback,
                                      double min, double max) {
        double value = json.has(key) ? json.get(key).getAsDouble() : fallback;
        return Math.max(min, Math.min(max, value));
    }

    private RegionStoryConfig() {
    }
}
