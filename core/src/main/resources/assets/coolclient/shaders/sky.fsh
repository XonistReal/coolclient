// Procedural skyboxes. Exactly one SKY_* macro is defined by the Java side. In the world the
// view direction comes from the inverse view-projection matrix; with PREVIEW defined the
// shader renders the same sky into a GUI rectangle through a virtual camera instead.

uniform mat4 uInvViewProj;
uniform vec4 uSun;      // xyz sun direction (world space), w daylight 0..1
uniform vec4 uSky;      // x seconds, y rotation (radians), z brightness, w sun and moon on
uniform vec4 uRect;     // preview rectangle (GUI units)
uniform vec4 uCam;      // preview: yaw, pitch, tan(fov / 2)
uniform sampler2D uImage;

VARYING_IN vec2 vPos;

vec3 rotY(vec3 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(c * v.x + s * v.z, v.y, -s * v.x + c * v.z);
}

// Cube-face coordinates (xy) and face index (z), so stars get an even 2D grid on the sphere.
vec3 cubeUv(vec3 d) {
    vec3 a = abs(d);
    if (a.x >= a.y && a.x >= a.z) return vec3(d.zy / a.x, d.x > 0.0 ? 0.0 : 1.0);
    if (a.y >= a.z) return vec3(d.xz / a.y, d.y > 0.0 ? 2.0 : 3.0);
    return vec3(d.xy / a.z, d.z > 0.0 ? 4.0 : 5.0);
}

float starLayer(vec3 d, float scale, float density, float t) {
    vec3 c = cubeUv(d);
    vec2 p = c.xy * scale;
    vec2 cell = floor(p);
    vec2 h = hash22(cell + c.z * 97.0);
    float present = step(1.0 - density, hash12(cell * 1.7 + c.z * 13.0));
    float dist = length(p - cell - 0.15 - 0.7 * h);
    float size = 0.06 + 0.1 * h.x * h.y;
    float tw = 0.55 + 0.45 * sin(t * (1.0 + 3.0 * h.y) + h.x * 30.0);
    return present * smoothstep(size, 0.0, dist) * tw;
}

float stars(vec3 d, float t) {
    return starLayer(d, 60.0, 0.10, t) * 1.2 + starLayer(d, 150.0, 0.12, t * 1.3) * 0.7;
}

// Tangent-plane coordinates around a direction, for discs (sun, moons).
vec2 around(vec3 d, vec3 axis) {
    vec3 right = normalize(cross(axis, abs(axis.y) > 0.99 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, axis);
    return vec2(dot(d, right), dot(d, up));
}

vec3 sunMoon(vec3 col, vec3 d) {
    vec3 s = normalize(uSun.xyz);
    float cs = dot(d, s);
    float sun = smoothstep(0.99935, 0.99965, cs);
    col += vec3(1.0, 0.92, 0.78) * (sun * 4.0 + pow(max(cs, 0.0), 400.0) * 0.8 + pow(max(cs, 0.0), 16.0) * 0.07);
    float cm = dot(d, -s);
    float moon = smoothstep(0.99945, 0.99970, cm);
    vec2 mp = around(d, -s) / 0.026;
    float craters = fbm(mp * 2.5 + 3.0);
    col = mix(col, vec3(0.86, 0.89, 0.96) * (0.7 + 0.35 * craters), moon);
    col += vec3(0.55, 0.65, 1.0) * pow(max(cm, 0.0), 600.0) * 0.25;
    return col;
}

#ifdef SKY_AURORA
#define OWN_SUN 0
vec3 skyColor(vec3 d, vec3 r, float t) {
    float y = d.y;
    vec3 horizon = vec3(0.05, 0.10, 0.22);
    vec3 col = y > 0.0 ? mix(horizon, vec3(0.005, 0.012, 0.04), pow(y, 0.45))
                       : mix(horizon, vec3(0.01, 0.015, 0.03), clamp(-y * 3.0, 0.0, 1.0));
    col += stars(r, t) * smoothstep(0.0, 0.25, y);
    if (y > 0.0) {
        vec3 acc = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            float fi = float(i);
            float h = 1.0 + fi * 0.06;
            vec2 pt = r.xz * (h / (y + 0.05));
            float n = fbm(pt * 0.32 + vec2(t * 0.012, fi * 0.015));
            float band = exp(-pow((n - 0.55) * 9.0, 2.0));
            float rays = 0.55 + 0.45 * noise(vec2(pt.x * 5.0 + pt.y * 2.0, t * 0.3));
            vec3 c = mix(vec3(0.2, 1.0, 0.55), vec3(0.55, 0.3, 1.0), fi / 7.0);
            acc += c * band * rays * (1.0 - fi / 9.0);
        }
        col += acc * 0.12 * smoothstep(0.0, 0.25, y);
    }
    return col;
}
#endif

#ifdef SKY_SPACE
#define OWN_SUN 0
vec3 skyColor(vec3 d, vec3 r, float t) {
    vec3 col = vec3(0.004, 0.005, 0.012);
    vec3 n = normalize(vec3(0.3, 0.8, 0.5));
    float band = dot(r, n);
    float m = exp(-band * band * 14.0);
    float clouds = fbm3(r * 2.2 + 7.0);
    float dust = fbm3(r * 4.0);
    col += vec3(0.35, 0.32, 0.48) * m * clouds * 0.7;
    col += vec3(0.95, 0.78, 0.62) * m * pow(clouds, 3.0) * 0.7;
    col *= 1.0 - m * smoothstep(0.45, 0.7, dust) * 0.8;
    col += vec3(0.28, 0.05, 0.32) * pow(fbm3(r * 1.5 + 3.0), 3.0) * 0.7;
    col += vec3(0.02, 0.16, 0.28) * pow(fbm3(r * 1.8 - 5.0), 3.0) * 0.7;
    col += stars(r, t) * (0.8 + m * 1.2) + starLayer(r, 320.0, 0.3, t) * m * 0.7;
    return col;
}
#endif

#ifdef SKY_NEBULA
#define OWN_SUN 0
vec3 skyColor(vec3 d, vec3 r, float t) {
    vec3 q = r * 1.6;
    vec3 w = vec3(fbm3(q + vec3(0.0, t * 0.01, 0.0)), fbm3(q + 5.2), fbm3(q - 3.1));
    float n = fbm3(q + w * 1.8);
    float n2 = fbm3(q * 2.0 - w + 9.0);
    vec3 col = vec3(0.01, 0.006, 0.02);
    vec3 neb = mix(vec3(0.55, 0.15, 0.9), vec3(0.1, 0.75, 0.85), smoothstep(0.35, 0.7, n2));
    neb = mix(neb, vec3(1.0, 0.35, 0.55), smoothstep(0.6, 0.85, w.x));
    col += neb * pow(n, 2.2) * 1.4 + vec3(1.0, 0.9, 1.0) * pow(n, 7.0) * 2.0;
    col += stars(r, t);
    return col;
}
#endif

#ifdef SKY_SUNSET
#define OWN_SUN 1
vec3 skyColor(vec3 d, vec3 r, float t) {
    vec3 sunDir = normalize(vec3(1.0, 0.06, 0.25));
    float y = d.y;
    float cs = max(dot(d, sunDir), 0.0);
    vec3 zenith = vec3(0.12, 0.16, 0.42);
    vec3 mid = vec3(0.75, 0.38, 0.55);
    vec3 col = y > 0.0 ? mix(mid, zenith, smoothstep(0.0, 0.6, y))
                       : mix(mid, vec3(0.25, 0.12, 0.2), clamp(-y * 4.0, 0.0, 1.0));
    col = mix(col, vec3(1.0, 0.55, 0.25), exp(-abs(y) * 9.0) * (0.55 + 0.45 * cs));
    col += vec3(1.0, 0.6, 0.3) * pow(cs, 8.0) * 0.6 + vec3(1.0, 0.85, 0.6) * pow(cs, 120.0);
    col += vec3(1.0, 0.95, 0.8) * smoothstep(0.9990, 0.9994, cs) * 2.0;
    if (y > 0.0) {
        vec2 pt = r.xz / (y + 0.08);
        float c = fbm(pt * 0.6 + vec2(t * 0.01, 0.0));
        float cl = smoothstep(0.5, 0.75, c) * smoothstep(0.0, 0.15, y);
        vec3 cloudCol = mix(vec3(0.45, 0.22, 0.35), vec3(1.0, 0.6, 0.45), pow(cs, 2.0));
        col = mix(col, cloudCol, cl * 0.85);
    }
    return col;
}
#endif

#ifdef SKY_SYNTHWAVE
#define OWN_SUN 1
vec3 skyColor(vec3 d, vec3 r, float t) {
    float y = d.y;
    vec3 col;
    if (y >= 0.0) {
        col = mix(vec3(1.0, 0.35, 0.45), vec3(0.08, 0.02, 0.18), pow(y, 0.5));
        col += stars(r, t) * smoothstep(0.15, 0.5, y) * 0.8;
        vec3 sd = normalize(vec3(0.0, 0.16, -1.0));
        if (dot(d, sd) > 0.0) {
            vec2 sp = around(d, sd);
            float R = 0.22;
            float dist = length(sp);
            float yy = sp.y / R;
            float stripe = yy > 0.05 ? 1.0 : step(mix(0.08, 0.6, clamp(-yy, 0.0, 1.0)), fract(yy * 7.0 - t * 0.35));
            float disc = smoothstep(R, R - 0.003, dist) * stripe;
            vec3 sunCol = mix(vec3(1.0, 0.9, 0.3), vec3(1.0, 0.2, 0.6), clamp(0.5 - yy * 0.5, 0.0, 1.0));
            col += vec3(1.0, 0.3, 0.6) * exp(-dist * 4.0) * 0.35;
            col = mix(col, sunCol, disc);
        }
    } else {
        vec2 g = r.xz / max(-y, 0.002) * 0.5;
        g.y += t * 1.0;
        vec2 gl = abs(fract(g) - 0.5) / max(fwidth(g), vec2(1e-4));
        float line = 1.0 - clamp(min(gl.x, gl.y) - 0.5, 0.0, 1.0);
        col = vec3(0.04, 0.0, 0.08) + vec3(0.9, 0.2, 1.0) * line * smoothstep(0.0, 0.15, -y);
        col += vec3(1.0, 0.3, 0.6) * exp(y * 25.0) * 0.4;
    }
    return col;
}
#endif

#ifdef SKY_COTTON
#define OWN_SUN 0
vec3 skyColor(vec3 d, vec3 r, float t) {
    float y = d.y;
    vec3 col = mix(vec3(1.0, 0.78, 0.86), vec3(0.55, 0.72, 1.0), smoothstep(-0.05, 0.6, y));
    if (y > 0.0) {
        vec2 pt = r.xz / (y + 0.12);
        vec2 drift = vec2(t * 0.012, t * 0.004);
        float c = fbm(pt * 0.45 + drift);
        float shade = fbm(pt * 0.45 + drift + 0.15);
        float cl = smoothstep(0.42, 0.7, c);
        vec3 cc = mix(vec3(1.0, 0.82, 0.93), vec3(1.0), clamp((c - shade) * 4.0 + 0.5, 0.0, 1.0));
        col = mix(col, cc, cl * smoothstep(0.0, 0.2, y));
    }
    return col;
}
#endif

#ifdef SKY_BLOODMOON
#define OWN_SUN 1
vec3 skyColor(vec3 d, vec3 r, float t) {
    float y = d.y;
    vec3 col = mix(vec3(0.22, 0.03, 0.03), vec3(0.03, 0.0, 0.01), smoothstep(0.0, 0.7, y));
    col += stars(r, t) * vec3(1.0, 0.5, 0.5) * 0.6 * smoothstep(0.1, 0.4, y);
    vec3 md = normalize(vec3(-0.6, 0.45, -0.66));
    float cm = dot(d, md);
    vec2 mp = around(d, md) / 0.122;
    float craters = fbm(mp * 3.0 + 5.0);
    vec3 mc = mix(vec3(0.55, 0.06, 0.04), vec3(0.95, 0.25, 0.12), craters) * (0.8 + 0.2 * (1.0 - length(mp)));
    col += vec3(0.9, 0.1, 0.05) * pow(max(cm, 0.0), 60.0) * 0.6 + vec3(0.6, 0.05, 0.02) * pow(max(cm, 0.0), 8.0) * 0.2;
    col = mix(col, mc, smoothstep(0.9925, 0.9929, cm));
    if (y > 0.0) {
        vec2 pt = r.xz / (y + 0.1);
        float c = fbm(pt * 0.5 + vec2(t * 0.02, 0.0));
        col = mix(col, vec3(0.06, 0.01, 0.01), smoothstep(0.45, 0.75, c) * smoothstep(0.0, 0.25, y) * 0.85);
    }
    return col;
}
#endif

#ifdef SKY_DYNAMIC
#define OWN_SUN 0
vec3 skyColor(vec3 d, vec3 r, float t) {
    vec3 s = normalize(uSun.xyz);
    float day = uSun.w;
    float y = max(d.y, 0.0);
    float cs = dot(d, s);
    vec3 col = mix(vec3(0.7, 0.82, 1.0), vec3(0.22, 0.45, 0.95), pow(y, 0.6));
    float low = 1.0 - smoothstep(0.0, 0.35, abs(s.y));
    col = mix(col, vec3(1.0, 0.5, 0.25), low * exp(-y * 6.0) * (0.35 + 0.65 * pow(max(cs, 0.0), 2.0)));
    col += vec3(1.0, 0.8, 0.6) * pow(max(cs, 0.0), 32.0) * 0.4 * (0.4 + low);
    vec3 night = mix(vec3(0.03, 0.05, 0.12), vec3(0.005, 0.01, 0.03), pow(y, 0.5));
    night += stars(r, t) * smoothstep(0.0, 0.2, d.y);
    col = mix(night, col, day);
    if (d.y < 0.0) col *= mix(1.0, 0.4, clamp(-d.y * 3.0, 0.0, 1.0));
    if (d.y > 0.0) {
        vec2 pt = r.xz / (d.y + 0.1);
        float c = fbm(pt * 0.4 + vec2(t * 0.008, 0.0));
        float cl = smoothstep(0.55, 0.8, c) * smoothstep(0.0, 0.2, d.y);
        vec3 cc = mix(vec3(0.08, 0.09, 0.13), mix(vec3(1.0), vec3(1.0, 0.7, 0.5), low), day);
        col = mix(col, cc, cl * 0.8);
    }
    return col;
}
#endif

#ifdef SKY_IMAGE
#define OWN_SUN 0
vec3 skyColor(vec3 d, vec3 r, float t) {
    vec2 uv = vec2(atan(r.z, r.x) / TAU + 0.5, acos(clamp(r.y, -1.0, 1.0)) / PI);
    return TEX(uImage, uv).rgb;
}
#endif

#ifndef OWN_SUN
#define OWN_SUN 0
#endif

void main() {
#ifdef PREVIEW
    vec2 uv = (vPos - uRect.xy) / uRect.zw;
    vec2 ndc = vec2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
    float aspect = uRect.z / max(uRect.w, 1.0);
    vec3 v = normalize(vec3(ndc.x * aspect * uCam.z, ndc.y * uCam.z, -1.0));
    float cp = cos(uCam.y);
    float sp = sin(uCam.y);
    v = vec3(v.x, cp * v.y - sp * v.z, sp * v.y + cp * v.z);
    vec3 d = rotY(v, uCam.x);
#else
    vec4 a = uInvViewProj * vec4(vPos, -1.0, 1.0);
    vec4 b = uInvViewProj * vec4(vPos, 1.0, 1.0);
    vec3 d = normalize(b.xyz / b.w - a.xyz / a.w);
#endif
    vec3 r = rotY(d, uSky.y);
    vec3 col = skyColor(d, r, uSky.x);
#if OWN_SUN == 0
    if (uSky.w > 0.5) col = sunMoon(col, d);
#endif
    col *= uSky.z;
    col += (hash12(gl_FragCoord.xy) - 0.5) * (1.5 / 255.0);
    float alpha = 1.0;
#ifdef PREVIEW
    float edge = sdRoundBox(vPos - uRect.xy - uRect.zw * 0.5, uRect.zw * 0.5, vec4(uCam.w));
    alpha = clamp(0.5 - edge / max(fwidth(edge), 1e-4), 0.0, 1.0);
#endif
    FRAG_COLOR = vec4(col, alpha);
}
