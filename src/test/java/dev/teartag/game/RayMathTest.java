package dev.teartag.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RayMathTest {
    private static final Vec3 CENTER = new Vec3(0, 1, 0);
    private static final Vec3 NORMAL = new Vec3(0, 0, -1);
    private static final Vec3 RIGHT = new Vec3(-1, 0, 0);

    @Test
    void hitsRectangleCenter() {
        double distance = RayMath.intersectRectangle(new Vec3(0, 1, -3), new Vec3(0, 0, 1), CENTER, NORMAL, RIGHT, 0.5, 0.5, 4);
        assertEquals(3.0, distance, 1.0E-7);
    }

    @Test
    void rejectsOutsideAndBeyondRange() {
        assertTrue(Double.isNaN(RayMath.intersectRectangle(new Vec3(0.6, 1, -3), new Vec3(0, 0, 1), CENTER, NORMAL, RIGHT, 0.5, 0.5, 4)));
        assertTrue(Double.isNaN(RayMath.intersectRectangle(new Vec3(0, 1, -3), new Vec3(0, 0, 1), CENTER, NORMAL, RIGHT, 0.5, 0.5, 2.9)));
    }

    @Test
    void rejectsParallelRay() {
        assertTrue(Double.isNaN(RayMath.intersectRectangle(new Vec3(0, 1, -3), new Vec3(1, 0, 0), CENTER, NORMAL, RIGHT, 0.5, 0.5, 4)));
    }

    @Test
    void preservesApproachSideForCallers() {
        assertTrue(new Vec3(0, 0, 1).dot(NORMAL) < 0.0, "a rear attacker approaches against the back-facing normal");
        assertTrue(new Vec3(0, 0, -1).dot(NORMAL) > 0.0, "a front attacker shoots with the back-facing normal after crossing the body");
    }

    @Test
    void rotatesPoseAxesAroundPlayerRightAxis() {
        Vec3 rotated = RayMath.rotateAroundAxis(new Vec3(0, 0, -1), new Vec3(-1, 0, 0), -Math.PI / 2);
        assertEquals(0.0, rotated.x, 1.0E-7);
        assertEquals(1.0, rotated.y, 1.0E-7);
        assertEquals(0.0, rotated.z, 1.0E-7);
    }
}
