package github.com.gengyoubo.CE.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Verify the spear axis after the real renderer's body flip and hand transforms. */
public final class SpearFlightPoseTest {
    public static void main(String[] args) {
        for (float pitch : new float[]{-70, -30, 0, 30, 70}) {
            for (float ticks : new float[]{0, 2.5F, 5, 9.5F, 10, 30}) {
                for (float raise : new float[]{0, 0.5F, 1}) {
                    // Independently reproduce PlayerRenderer.setupRotations, including its transition.
                    float body = Math.min(1, ticks * ticks / 100) * (-90 - pitch);
                    float aim = SpearFlightPose.aimPitch(pitch, pitch, ticks);
                    Vector3f axis = new Matrix4f().rotateX(radians(body)).scale(-1, -1, 1)
                            .rotateX(radians(aim - 50 * raise))
                            .rotateX(radians(-90)).rotateY(radians(180))
                            .rotateX(radians(-50 * raise)).transformDirection(new Vector3f(0, 1, 0));
                    Vector3f expected = new Vector3f(0, -(float) Math.sin(radians(pitch)),
                            -(float) Math.cos(radians(pitch)));
                    if (axis.distance(expected) > 0.0001F) {
                        throw new AssertionError("Spear axis differs from view at pitch=" + pitch + ", flight=" + ticks);
                    }
                }
            }
        }
        System.out.println("All spear flight pose checks passed (90 transforms)");
    }
    private static float radians(float degrees) { return degrees * (float) Math.PI / 180; }
}
