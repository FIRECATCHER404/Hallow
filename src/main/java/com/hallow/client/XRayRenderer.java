package com.hallow.client;

import java.util.List;

import com.hallow.client.cheat.modules.XRayModule;
import com.hallow.client.config.HallowConfigManager;
import com.hallow.client.config.XRayMode;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class XRayRenderer {
    private static final VoxelShape BOX_SHAPE = Shapes.box(0.04, 0.04, 0.04, 0.96, 0.96, 0.96);

    private final XRayModule module;

    public XRayRenderer(XRayModule module) {
        this.module = module;
    }

    public void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::render);
    }

    private void render(WorldRenderContext context) {
        if (!module.isEnabled() || HallowConfigManager.get().xray.mode != XRayMode.ESP) {
            return;
        }

        List<XRayModule.HighlightTarget> targets = module.targets();
        if (targets.isEmpty()) {
            return;
        }

        PoseStack poseStack = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        VertexConsumer consumer = context.consumers().getBuffer(RenderTypes.lines());

        for (XRayModule.HighlightTarget target : targets) {
            ShapeRenderer.renderShape(
                poseStack,
                consumer,
                BOX_SHAPE,
                target.pos().getX() - camera.x,
                target.pos().getY() - camera.y,
                target.pos().getZ() - camera.z,
                rgb(target.red(), target.green(), target.blue()),
                1.0F
            );
        }
    }

    private static int rgb(float red, float green, float blue) {
        int r = clamp((int) (red * 255.0F));
        int g = clamp((int) (green * 255.0F));
        int b = clamp((int) (blue * 255.0F));
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
