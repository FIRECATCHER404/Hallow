package com.hallow.client;

import java.util.ArrayList;
import java.util.List;

import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ProjectilePredictor {
    private ProjectilePredictor() {
    }

    public static Prediction predict(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }

        LaunchProfile profile = resolveLaunchProfile(client.player);
        if (profile == null) {
            return null;
        }

        int maxSteps = HallowConfigManager.get().projectilePredict.maxSteps;
        List<Vec3> points = new ArrayList<>(maxSteps + 1);
        Vec3 position = profile.origin();
        Vec3 velocity = profile.velocity();
        AABB projectileBounds = new AABB(position, position).inflate(profile.collisionRadius());
        Vec3 landing = position;
        boolean hit = false;

        points.add(position);

        for (int step = 0; step < maxSteps; step++) {
            Vec3 nextPosition = position.add(velocity);
            BlockHitResult blockHit = client.level.clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
            Vec3 collisionEnd = blockHit.getType() == HitResult.Type.MISS ? nextPosition : blockHit.getLocation();
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                client.player,
                position,
                collisionEnd,
                projectileBounds.expandTowards(velocity).inflate(1.0),
                entity -> canHitEntity(client.player, entity),
                profile.collisionRadius()
            );

            if (entityHit != null) {
                landing = entityHit.getLocation();
                points.add(landing);
                hit = true;
                break;
            }

            if (blockHit.getType() != HitResult.Type.MISS) {
                landing = blockHit.getLocation();
                points.add(landing);
                hit = true;
                break;
            }

            position = nextPosition;
            projectileBounds = projectileBounds.move(velocity);
            points.add(position);
            landing = position;
            velocity = velocity.scale(profile.drag()).add(0.0, -profile.gravity(), 0.0);
            if (velocity.lengthSqr() < 1.0E-4) {
                break;
            }
        }

        if (points.size() < 2) {
            return null;
        }

        return new Prediction(points, landing, hit, profile.label());
    }

    public static String describeHeldProjectile(Minecraft client) {
        if (client.player == null) {
            return "idle";
        }

        LaunchProfile profile = resolveLaunchProfile(client.player);
        return profile != null ? profile.label() : "idle";
    }

    private static LaunchProfile resolveLaunchProfile(LocalPlayer player) {
        ItemStack stack = resolveRelevantStack(player);
        if (stack.isEmpty()) {
            return null;
        }

        Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.2));
        Vec3 inheritedVelocity = player.getDeltaMovement();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        if (stack.getItem() instanceof BowItem) {
            if (!player.isUsingItem() || player.getUseItem() != stack) {
                return null;
            }

            int useTicks = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
            float power = BowItem.getPowerForTime(useTicks);
            if (power < 0.1F) {
                return null;
            }

            return new LaunchProfile(origin, Vec3.directionFromRotation(pitch, yaw).scale(power * 3.0F).add(inheritedVelocity), 0.05, 0.99, 0.3, "bow");
        }

        if (stack.getItem() instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(stack)) {
                return null;
            }

            return new LaunchProfile(origin, Vec3.directionFromRotation(pitch, yaw).scale(3.15).add(inheritedVelocity), 0.05, 0.99, 0.3, "crossbow");
        }

        if (stack.getItem() instanceof TridentItem) {
            if (!player.isUsingItem() || player.getUseItem() != stack) {
                return null;
            }

            int useTicks = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
            if (useTicks < 10) {
                return null;
            }

            return new LaunchProfile(origin, Vec3.directionFromRotation(pitch, yaw).scale(2.5).add(inheritedVelocity), 0.05, 0.99, 0.3, "trident");
        }

        if (stack.getItem() instanceof ThrowablePotionItem || stack.getItem() instanceof SplashPotionItem) {
            return new LaunchProfile(origin, Vec3.directionFromRotation(pitch - 20.0F, yaw).scale(0.5).add(inheritedVelocity), 0.05, 0.99, 0.25, "potion");
        }

        if (stack.getItem() instanceof ExperienceBottleItem) {
            return new LaunchProfile(origin, Vec3.directionFromRotation(pitch - 20.0F, yaw).scale(0.7).add(inheritedVelocity), 0.07, 0.8, 0.25, "xp bottle");
        }

        if (stack.getItem() instanceof EnderpearlItem) {
            return new LaunchProfile(origin, Vec3.directionFromRotation(pitch, yaw).scale(1.5).add(inheritedVelocity), 0.03, 0.99, 0.25, "ender pearl");
        }

        if (stack.getItem() instanceof SnowballItem) {
            return new LaunchProfile(origin, Vec3.directionFromRotation(pitch, yaw).scale(1.5).add(inheritedVelocity), 0.03, 0.99, 0.25, "snowball");
        }

        if (stack.getItem() instanceof EggItem) {
            return new LaunchProfile(origin, Vec3.directionFromRotation(pitch, yaw).scale(1.5).add(inheritedVelocity), 0.03, 0.99, 0.25, "egg");
        }

        return null;
    }

    private static ItemStack resolveRelevantStack(LocalPlayer player) {
        if (player.isUsingItem() && !player.getUseItem().isEmpty()) {
            return player.getUseItem();
        }

        ItemStack mainHand = player.getMainHandItem();
        if (canPredict(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (canPredict(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static boolean canPredict(ItemStack stack) {
        return !stack.isEmpty() && (
            stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof ThrowablePotionItem
                || stack.getItem() instanceof ExperienceBottleItem
                || stack.getItem() instanceof EnderpearlItem
                || stack.getItem() instanceof SnowballItem
                || stack.getItem() instanceof EggItem
        );
    }

    public record Prediction(List<Vec3> points, Vec3 landingPoint, boolean hit, String label) {
    }

    private static boolean canHitEntity(LocalPlayer player, Entity entity) {
        return entity != null
            && entity.isAlive()
            && entity.isPickable()
            && entity != player
            && !entity.isSpectator();
    }

    private record LaunchProfile(Vec3 origin, Vec3 velocity, double gravity, double drag, double collisionRadius, String label) {
    }
}
