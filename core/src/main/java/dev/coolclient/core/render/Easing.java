package dev.coolclient.core.render;

public enum Easing {
    LINEAR {
        @Override public float apply(float t) { return t; }
    },
    OUT_QUAD {
        @Override public float apply(float t) { return 1f - (1f - t) * (1f - t); }
    },
    OUT_CUBIC {
        @Override public float apply(float t) { float u = 1f - t; return 1f - u * u * u; }
    },
    IN_OUT_CUBIC {
        @Override public float apply(float t) {
            return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
        }
    },
    OUT_EXPO {
        @Override public float apply(float t) { return t >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * t); }
    },
    OUT_BACK {
        @Override public float apply(float t) {
            float c1 = 1.70158f, c3 = c1 + 1f;
            float u = t - 1f;
            return 1f + c3 * u * u * u + c1 * u * u;
        }
    };

    public abstract float apply(float t);
}
