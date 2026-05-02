package com.hallow.client;

import java.util.List;

import com.hallow.client.cheat.modules.BreadcrumbTrailModule;
import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

public final class BreadcrumbTrailRenderer {
    private final BreadcrumbTrailModule module;

    public BreadcrumbTrailRenderer(BreadcrumbTrailModule module) {
        this.module = module;
    }

    public void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::render);
    }

    private void render(WorldRenderContext context) {
        if (!module.isEnabled() || context.consumers() == null) {
            return;
        }

        List<Vec3> points = module.trailPoints();
        if (points.size() < 2) {
            return;
        }

        HallowConfig.BreadcrumbTrailSettings config = HallowConfigManager.get().breadcrumbTrail;
        PoseStack poseStack = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        VertexConsumer consumer = context.consumers().getBuffer(RenderTypes.lines());

        int red = toRgb(config.red);
        int green = toRgb(config.green);
        int blue = toRgb(config.blue);
        float outerWidth = (float) config.lineWidth;
        float coreWidth = Math.max(1.0F, outerWidth * 0.42F);
        int segments = points.size() - 1;

        for (int index = 0; index < segments; index++) {
            Vec3 start = points.get(index).subtract(camera);
            Vec3 end = points.get(index + 1).subtract(camera);
            float age = segments <= 1 ? 1.0F : (index + 1.0F) / segments;
            int alpha = config.fade ? (int) (36 + (age * 164)) : 184;

            drawLineSegment(poseStack, consumer, start, end, red, green, blue, alpha, outerWidth);
            if (config.whiteCore) {
                drawLineSegment(poseStack, consumer, start, end, 255, 255, 255, Math.min(210, alpha + 34), coreWidth);
            }
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

    private static int toRgb(double value) {
        return (int) (clamp01(value) * 255.0);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
