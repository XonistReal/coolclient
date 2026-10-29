package dev.coolclient.core;

import dev.coolclient.core.util.Mat4;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Mat4Test {
    @Test
    public void inverseTimesMatrixIsIdentity() {
        // A perspective projection times a rotated, translated view, column-major.
        float f = 1f / (float) Math.tan(Math.toRadians(35)), n = 0.05f, far = 512f;
        float[] proj = {f / 1.6f, 0, 0, 0, 0, f, 0, 0, 0, 0, (far + n) / (n - far), -1, 0, 0, 2 * far * n / (n - far), 0};
        float c = (float) Math.cos(0.7), s = (float) Math.sin(0.7);
        float[] view = {c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0, 3, -2, 5, 1};
        float[] vp = new float[16], inv = new float[16], id = new float[16];
        Mat4.mul(proj, view, vp);
        assertTrue(Mat4.invert(vp, inv));
        Mat4.mul(vp, inv, id);
        for (int i = 0; i < 16; i++) assertEquals("element " + i, i % 5 == 0 ? 1f : 0f, id[i], 1e-3f);
    }
}
