package com.regionstory.client;

import com.regionstory.RegionStoryMod;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = RegionStoryMod.MOD_ID)
public class RegionStoryConfig implements ConfigData {
    public boolean autoplay = false;
    public float autoplayDelay = 1f;
    public float scale = 0.8f;

    @ConfigEntry.Gui.CollapsibleObject
    public Camera camera = new Camera();
    public static class Camera {
        public int enterDuration = 12;
        public int exitDuration = 12;
        public double thirdPersonDistance = 4.5d;
        public double heightOffset = 0.6d;
        public float yawOffset = 18f;
        public float pitchOffset = 8f;
    }
}
