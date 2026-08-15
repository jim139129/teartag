package dev.teartag.game;

import dev.teartag.config.NameTagConfig;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

public final class HitDetector {
    public enum Result { MISS, NORMAL, WALL_ATTEMPT }

    private HitDetector() {
    }

    public static Result detect(ServerPlayer attacker, ServerPlayer target, NameTagConfig config) {
        if (attacker == target || attacker.level() != target.level()) return Result.MISS;
        Vec3 origin = attacker.getEyePosition();
        Vec3 direction = attacker.getViewVector(1.0F).normalize();
        double maxDistance = config.attackDistance();
        if (origin.distanceToSqr(target.position()) > (maxDistance + 2.0) * (maxDistance + 2.0)) return Result.MISS;

        Frame frame = frame(target, config);
        double plateDistance = RayMath.intersectRectangle(origin, direction, frame.center, frame.normal, frame.right, frame.up,
            config.tagWidth() / 2.0, config.tagHeight() / 2.0, maxDistance);
        if (Double.isNaN(plateDistance)) return Result.MISS;

        Vec3 plateHit = origin.add(direction.scale(plateDistance));
        HitResult blockHit = target.level().clip(new ClipContext(origin, plateHit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
        double blockDistance = blockHit.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY : origin.distanceTo(blockHit.getLocation());
        boolean blockedBeforePlate = blockDistance + 1.0E-4 < plateDistance;
        var bodyHit = target.getBoundingBox().clip(origin, plateHit);
        boolean rayCrossesBody = bodyHit.isPresent();
        double bodyEntryDistance = bodyHit.map(origin::distanceTo).orElse(Double.POSITIVE_INFINITY);
        boolean wallBehind = hasWallBehind(target, frame, config);

        double approach = direction.dot(frame.normal);
        if (!blockedBeforePlate && approach < 0.0 && !rayCrossesBody) return Result.NORMAL;
        boolean clearUntilBody = !blockedBeforePlate || blockDistance + 1.0E-4 >= bodyEntryDistance;
        if (wallBehind && rayCrossesBody && approach > 0.0 && clearUntilBody) return Result.WALL_ATTEMPT;
        return Result.MISS;
    }

    public static Frame frame(ServerPlayer target, NameTagConfig config) {
        double radians = Math.toRadians(target.yBodyRot);
        Vec3 normal = new Vec3(Math.sin(radians), 0.0, -Math.cos(radians));
        Vec3 right = new Vec3(normal.z, 0.0, -normal.x);
        Vec3 up = Vec3.Y_AXIS;
        if (target.getPose() == Pose.SWIMMING || target.getPose() == Pose.FALL_FLYING || target.getPose() == Pose.SPIN_ATTACK) {
            float pitch = target.getPose() == Pose.SWIMMING && !target.isInWater() ? -90.0F : -90.0F - target.getXRot();
            normal = RayMath.rotateAroundAxis(normal, right, Math.toRadians(pitch));
            up = RayMath.rotateAroundAxis(up, right, Math.toRadians(pitch));
        }
        double poseHeight = target.getDimensions(target.getPose()).height();
        double y = Math.min(config.tagVerticalOffset(), Math.max(config.tagHeight() / 2.0, poseHeight - config.tagHeight() / 2.0));
        Vec3 center = target.position().add(normal.scale(config.tagBackOffset())).add(up.scale(y));
        return new Frame(center, normal.normalize(), right.normalize(), up.normalize());
    }

    private static boolean hasWallBehind(ServerPlayer target, Frame frame, NameTagConfig config) {
        double halfWidth = config.tagWidth() * 0.35;
        double halfHeight = config.tagHeight() * 0.35;
        Vec3[] samples = {
            frame.center,
            frame.center.add(frame.right.scale(halfWidth)).add(frame.up.scale(halfHeight)),
            frame.center.add(frame.right.scale(halfWidth)).add(frame.up.scale(-halfHeight)),
            frame.center.add(frame.right.scale(-halfWidth)).add(frame.up.scale(halfHeight)),
            frame.center.add(frame.right.scale(-halfWidth)).add(frame.up.scale(-halfHeight))
        };
        int blocked = 0;
        for (Vec3 sample : samples) {
            Vec3 behind = sample.add(frame.normal.scale(config.wallDetectionDistance()));
            HitResult hit = target.level().clip(new ClipContext(sample, behind, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
            if (hit.getType() != HitResult.Type.MISS) blocked++;
        }
        return blocked >= 3;
    }

    public record Frame(Vec3 center, Vec3 normal, Vec3 right, Vec3 up) {
    }
}
