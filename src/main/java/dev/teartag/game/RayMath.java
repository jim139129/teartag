package dev.teartag.game;

import net.minecraft.world.phys.Vec3;

public final class RayMath {
    private static final double EPSILON = 1.0E-7;

    private RayMath() {
    }

    public static Vec3 rotateAroundAxis(Vec3 vector, Vec3 axis, double radians) {
        Vec3 unit = axis.normalize();
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return vector.scale(cosine)
            .add(unit.cross(vector).scale(sine))
            .add(unit.scale(unit.dot(vector) * (1.0 - cosine)));
    }

    /** Returns distance along a normalized ray, or NaN when the finite rectangle is missed. */
    public static double intersectRectangle(Vec3 origin, Vec3 direction, Vec3 center, Vec3 normal, Vec3 right,
                                            double halfWidth, double halfHeight, double maxDistance) {
        return intersectRectangle(origin, direction, center, normal, right, Vec3.Y_AXIS, halfWidth, halfHeight, maxDistance);
    }

    public static double intersectRectangle(Vec3 origin, Vec3 direction, Vec3 center, Vec3 normal, Vec3 right, Vec3 up,
                                            double halfWidth, double halfHeight, double maxDistance) {
        double denominator = direction.dot(normal);
        if (Math.abs(denominator) < EPSILON) return Double.NaN;
        double distance = center.subtract(origin).dot(normal) / denominator;
        if (distance < 0.0 || distance > maxDistance) return Double.NaN;
        Vec3 local = origin.add(direction.scale(distance)).subtract(center);
        double horizontal = local.dot(right);
        double vertical = local.dot(up);
        return Math.abs(horizontal) <= halfWidth + EPSILON && Math.abs(vertical) <= halfHeight + EPSILON ? distance : Double.NaN;
    }
}
