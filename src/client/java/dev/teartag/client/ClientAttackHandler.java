package dev.teartag.client;

import dev.teartag.game.RayMath;
import dev.teartag.network.AttemptTearPayload;
import java.util.Comparator;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

public final class ClientAttackHandler {
    private static boolean wasDown;

    private ClientAttackHandler() {
    }

    public static void tick(Minecraft client) {
        boolean down = client.options.keyAttack.isDown();
        if (down && !wasDown && client.player != null && client.level != null && !client.canInterruptScreen()) {
            findTarget(client);
        }
        wasDown = down;
    }

    private static void findTarget(Minecraft client) {
        Vec3 origin = client.player.getEyePosition();
        Vec3 direction = client.player.getViewVector(1.0F).normalize();
        client.level.players().stream()
            .filter(player -> player != client.player)
            .map(player -> candidate(player, origin, direction))
            .filter(candidate -> candidate != null)
            .min(Comparator.comparingDouble(Candidate::distance))
            .ifPresent(candidate -> ClientPlayNetworking.send(new AttemptTearPayload(candidate.player.getUUID())));
    }

    private static Candidate candidate(AbstractClientPlayer player, Vec3 origin, Vec3 direction) {
        ClientNametagState state = ClientNametagStore.byUuid(player.getUUID());
        if (state == null || !state.enabled() || state.eliminated()) return null;
        double radians = Math.toRadians(player.yBodyRot);
        Vec3 normal = new Vec3(Math.sin(radians), 0.0, -Math.cos(radians));
        Vec3 right = new Vec3(normal.z, 0.0, -normal.x);
        Vec3 up = Vec3.Y_AXIS;
        if (player.getPose() == net.minecraft.world.entity.Pose.SWIMMING
            || player.getPose() == net.minecraft.world.entity.Pose.FALL_FLYING
            || player.getPose() == net.minecraft.world.entity.Pose.SPIN_ATTACK) {
            float pitch = player.getPose() == net.minecraft.world.entity.Pose.SWIMMING && !player.isInWater() ? -90.0F : -90.0F - player.getXRot();
            normal = RayMath.rotateAroundAxis(normal, right, Math.toRadians(pitch));
            up = RayMath.rotateAroundAxis(up, right, Math.toRadians(pitch));
        }
        double poseHeight = player.getDimensions(player.getPose()).height();
        double y = Math.min(state.verticalOffset(), Math.max(state.height() / 2.0, poseHeight - state.height() / 2.0));
        Vec3 center = player.position().add(normal.scale(state.backOffset())).add(up.scale(y));
        double distance = RayMath.intersectRectangle(origin, direction, center, normal.normalize(), right.normalize(), up.normalize(),
            state.width() / 2, state.height() / 2, state.attackDistance());
        return Double.isNaN(distance) ? null : new Candidate(player, distance);
    }

    private record Candidate(AbstractClientPlayer player, double distance) {
    }
}
