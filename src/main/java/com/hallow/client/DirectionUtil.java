package com.hallow.client;

import java.util.Locale;

import net.minecraft.world.phys.Vec3;

public final class DirectionUtil {
    private static final String[] HEADINGS = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};

    private DirectionUtil() {
    }

    public static String formatDistance(double blocks) {
        return String.format(Locale.ROOT, "%.1fm", blocks);
    }

    public static String heading(Vec3 from, Vec3 to) {
        double deltaX = to.x - from.x;
        double deltaZ = to.z - from.z;

        if (Math.abs(deltaX) < 0.001 && Math.abs(deltaZ) < 0.001) {
            return "HERE";
        }

        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX));
        int index = Math.floorMod((int) Math.round(angle / 45.0), HEADINGS.length);
        return HEADINGS[index];
    }

    public static String verticalOffset(double deltaY) {
        if (Math.abs(deltaY) < 0.5) {
            return "level";
        }

        return (deltaY > 0 ? "+" : "") + Math.round(deltaY) + "y";
    }
}

