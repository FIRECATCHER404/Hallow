package com.hallow.client;

import java.util.List;

import com.hallow.client.cheat.modules.PlayerEspModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class PlayerEspRenderer {
    private static final int BOX_RED = 255;
    private static final int BOX_GREEN = 138;
    private static final int BOX_BLUE = 64;

    private final PlayerEspModule module;

    public PlayerEspRenderer(PlayerEspModule module) {
        this.module = module;
    }

    public void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::render);
    }

    private void render(WorldRenderContext context) {
        if (!module.isEnabled() || context.consumers() == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        List<Player> targets = module.trackedPlayers(client);
        if (targets.isEmpty()) {
            return;
        }

        PoseStack poseStack = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        VertexConsumer consumer = context.consumers().getBuffer(RenderTypes.lines());

        for (Player target : targets) {
            AABB box = target.getBoundingBox().inflate(0.05).move(-camera.x, -camera.y, -camera.z);
            ShapeRenderer.renderShape(
                poseStack,
                consumer,
                Shapes.create(box),
                0.0,
                0.0,
                0.0,
                (BOX_RED << 16) | (BOX_GREEN << 8) | BOX_BLUE,
                1.0F
            );

            Vec3 targetPos = target.getBoundingBox().getCenter().subtract(camera);
            drawLineSegment(
                poseStack,
                consumer,
                new Vec3(0.0, 0.0, 0.0),
                targetPos,
                BOX_RED,
                BOX_GREEN,
                BOX_BLUE,
                120,
                2.4F
            );
            drawLineSegment(
                poseStack,
                consumer,
                new Vec3(0.0, 0.0, 0.0),
                targetPos,
                255,
                255,
                255,
                200,
                1.15F
            );
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
}
