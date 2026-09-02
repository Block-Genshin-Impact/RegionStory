package com.regionstory.client;

import com.regionstory.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Camera interpolation used while a dialogue screen owns the interaction. */
public final class CameraTransitionController {
    private enum Mode { IDLE, ENTERING, DIALOGUE, EXITING }

    private static Mode mode = Mode.IDLE;
    private static long startTime;
    private static Perspective previousPerspective = Perspective.FIRST_PERSON;

    public static void beginEnter(MinecraftClient client) {
        if (mode == Mode.IDLE) {
            previousPerspective = client.options.getPerspective();
        }
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        startTime = System.currentTimeMillis();
        mode = Mode.ENTERING;
    }

    public static void beginExit(MinecraftClient client) {
        if (mode == Mode.IDLE) return;
        startTime = System.currentTimeMillis();
        mode = Mode.EXITING;
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    public static void tick(MinecraftClient client) {
        if (mode == Mode.IDLE) return;
        if (mode == Mode.ENTERING && getTicks() >= RegionStoryClient.config.camera.enterDuration) {
            mode = Mode.DIALOGUE;
        }
        if (mode == Mode.EXITING && getTicks() >= RegionStoryClient.config.camera.exitDuration) {
            client.options.setPerspective(previousPerspective);
            mode = Mode.IDLE;
        }
    }

    public static boolean active() {
        return mode != Mode.IDLE;
    }

    public static void reset(MinecraftClient client) {
        client.options.setPerspective(previousPerspective);
        mode = Mode.IDLE;
        startTime = System.currentTimeMillis();
    }

    public static void apply(Camera camera, Entity focusedEntity) {
        if (!active() || focusedEntity == null) return;

        // 使用 smoothstep 让进入和退出的速度在首尾自然收敛，避免镜头突然加速。
        double progress = mode == Mode.EXITING
                ? 1.0D - MathHelper.clamp(getTicks() / (double) RegionStoryClient.config.camera.exitDuration, 0.0D, 1.0D)
                : MathHelper.clamp(getTicks() / (double) RegionStoryClient.config.camera.enterDuration, 0.0D, 1.0D);
        double eased = progress * progress * (3.0D - 2.0D * progress);
        double distance = RegionStoryClient.config.camera.thirdPersonDistance * eased;
        // 从玩家的水平朝向构建局部坐标：正 yawOffset 永远是“玩家右后方”，
        // 不再依赖世界坐标角度，从而避免镜头跑到人物正前方。
        Vec3d forward = focusedEntity.getRotationVec(1.0F);
        Vec3d horizontalForward = new Vec3d(forward.x, 0.0D, forward.z).normalize();
        if (horizontalForward.lengthSquared() < 1.0E-6D) {
            double yaw = Math.toRadians(focusedEntity.getYaw());
            horizontalForward = new Vec3d(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        Vec3d backward = horizontalForward.negate();
        Vec3d right = new Vec3d(-horizontalForward.z, 0.0D, horizontalForward.x);
        double offset = Math.toRadians(RegionStoryClient.config.camera.yawOffset * eased);
        Vec3d cameraOffset = backward.multiply(Math.cos(offset) * distance)
                .add(right.multiply(Math.sin(offset) * distance)).add(backward.normalize().multiply(0.5));
        double x = focusedEntity.getX() + cameraOffset.x;
        double z = focusedEntity.getZ() + cameraOffset.z;
        double y = focusedEntity.getY() + focusedEntity.getStandingEyeHeight()
                + RegionStoryClient.config.camera.heightOffset * eased;

        CameraAccessor accessor = (CameraAccessor) camera;
        accessor.regionstory$setPos(x, y, z);
        accessor.regionstory$setRotation(
                focusedEntity.getYaw() - RegionStoryClient.config.camera.yawOffset * (float) eased,
                RegionStoryClient.config.camera.pitchOffset * (float) eased);
    }

    private CameraTransitionController() {
    }

    private static float getTicks() {
        return (System.currentTimeMillis() - startTime) / 50f;
    }
}
