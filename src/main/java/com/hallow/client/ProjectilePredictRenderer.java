package com.hallow.client;

import com.hallow.client.cheat.modules.ProjectilePredictModule;
import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

public final class ProjectilePredictRenderer {
    private final ProjectilePredictModule module;

    public ProjectilePredictRenderer(ProjectilePredictModule module) {
        this.module = module;
    }

    public void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::render);
    }

    private void render(WorldRenderContext context) {
        if (!module.isEnabled() || context.consumers() == null) {
            return;
        }

        ProjectilePredictor.Prediction prediction = ProjectilePredictor.predict(net.minecraft.client.Minecraft.getInstance());
        if (prediction == null) {
            return;
        }

        HallowConfig.ProjectilePredictSettings config = HallowConfigManager.get().projectilePredict;
        PoseStack poseStack = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        VertexConsumer consumer = context.consumers().getBuffer(RenderTypes.lines());

        int outerRed = toRgb((float) config.arcRed);
        int outerGreen = toRgb((float) config.arcGreen);
        int outerBlue = toRgb((float) config.arcBlue);
        float outerWidth = (float) config.lineWidth;
        float coreWidth = Math.max(1.0F, outerWidth * 0.46F);
        double landingRadius = 0.24 + (config.lineWidth * 0.05);

        for (int index = 0; index < prediction.points().size() - 1; index++) {
            Vec3 start = prediction.points().get(index).subtract(camera);
            Vec3 end = prediction.points().get(index + 1).subtract(camera);
            drawLineSegment(poseStack, consumer, start, end, outerRed, outerGreen, outerBlue, 96, outerWidth);
            drawLineSegment(poseStack, consumer, start, end, 255, 255, 255, 172, coreWidth);
        }

        if (config.showLandingMarker) {
            Vec3 landing = prediction.landingPoint().subtract(camera);
            drawLandingMarker(poseStack, consumer, landing, outerRed, outerGreen, outerBlue, landingRadius, outerWidth, coreWidth);
        }
    }

    private static void drawLineSegment(
        PoseStack poseStack,
        VertexConsumer consumer,
        Vec3 start,
        Vec3 end,
        int red,
        int green,
        int blue,
        int alpha,
        float lineWidth
    ) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 1.0E-4) {
            return;
        }

        Pose pose = poseStack.last();
        float normalX = (float) (delta.x / length);
        float normalY = (float) (delta.y / length);
        float normalZ = (float) (delta.z / length);
        consumer.addVertex(pose, (float) start.x, (float) start.y, (float) start.z)
            .setColor(red, green, blue, alpha)
            .setNormal(pose, normalX, normalY, normalZ)
            .setLineWidth(lineWidth);
        consumer.addVertex(pose, (float) end.x, (float) end.y, (float) end.z)
            .setColor(red, green, blue, alpha)
            .setNormal(pose, normalX, normalY, normalZ)
            .setLineWidth(lineWidth);
    }

    private static void drawLandingMarker(
        PoseStack poseStack,
        VertexConsumer consumer,
        Vec3 center,
        int red,
        int green,
        int blue,
        double radius,
        float outerWidth,
        float coreWidth
    ) {
        drawCrossAxis(poseStack, consumer, center, new Vec3(radius, 0.0, 0.0), red, green, blue, outerWidth, coreWidth);
        drawCrossAxis(poseStack, consumer, center, new Vec3(0.0, 0.0, radius), red, green, blue, outerWidth, coreWidth);
        drawCrossAxis(poseStack, consumer, center, new Vec3(0.0, radius * 0.45, 0.0), red, green, blue, outerWidth, coreWidth);
    }

    private static void drawCrossAxis(
        PoseStack poseStack,
        VertexConsumer consumer,
        Vec3 center,
        Vec3 offset,
        int red,
        int green,
        int blue,
        float outerWidth,
        float coreWidth
    ) {
        Vec3 start = center.subtract(offset);
        Vec3 end = center.add(offset);
        drawLineSegment(poseStack, consumer, start, end, red, green, blue, 112, outerWidth + 0.75F);
        drawLineSegment(poseStack, consumer, start, end, 255, 255, 255, 196, coreWidth + 0.1F);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int toRgb(float value) {
        return (int) (clamp01(value) * 255.0F);
    }
}
