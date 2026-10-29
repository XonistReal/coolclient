// Animated menu backgrounds. Exactly one BG_* macro is defined by the Java side; each one
// implements background(uv, p, t). They take the theme's accents and base colour, so every
// background matches every theme.

uniform vec4 uRect;     // x, y, w, h in GUI units
uniform vec4 uTime;     // x seconds, y opacity, z corner radius (GUI units)
uniform vec4 uAccentA;
uniform vec4 uAccentB;
uniform vec4 uBase;     // theme panel colour

VARYING_IN vec2 vPos;

float px1() { return max(fwidth(vPos.y) / max(uRect.w, 1.0), 1e-4); }

vec3 vignette(vec3 c, vec2 uv) {
    vec2 q = uv - 0.5;
    return c * (1.0 - dot(q, q) * 0.9);
}

#ifdef BG_AURORA
vec3 background(vec2 uv, vec2 p, float t) {
    vec3 top = uBase.rgb * 0.22;
    vec3 bottom = uBase.rgb * 1.25 + vec3(0.0, 0.01, 0.05);
    vec3 col = mix(bottom, top, uv.y);
    col += vec3(stars2(p + 3.0, 60.0, 0.16, t)) * smoothstep(0.25, 0.6, uv.y) * 0.9;

    for (int k = 0; k < 3; k++) {
        float fk = float(k);
        float x = uv.x;
        float amp = 0.05 + fk * 0.015;
        float wave = sin(x * 5.2 + t * (0.22 + fk * 0.07) + fk * 1.7) * amp
                + (noise(vec2(x * 4.0 + fk * 9.0, t * 0.12)) - 0.5) * amp * 1.6;
        float c = 0.72 - fk * 0.07 + wave;
        float height = 0.16 + 0.07 * sin(x * 3.1 + t * 0.3 + fk);
        float dy = uv.y - c;
        float prof = dy > 0.0 ? exp(-dy / (height * 0.42)) : exp(-dy * dy / (height * height * 0.01));
        float rays = 0.55 + 0.45 * noise(vec2(x * 70.0 + fk * 13.0, t * 0.45 + dy * 3.0));
        float inten = 0.45 + 0.55 * noise(vec2(x * 7.0 + fk * 3.0, t * 0.35));
        vec3 green = vec3(0.36, 1.0, 0.6);
        vec3 ca = k == 0 ? uAccentA.rgb : (k == 1 ? uAccentB.rgb : green);
        vec3 cb = k == 0 ? green : (k == 1 ? uAccentA.rgb : uAccentB.rgb);
        col += mix(ca, cb, x) * prof * rays * inten * 0.38;
    }

    float a = px1() * 1.5;
    float r1 = 0.31 + (fbm(vec2(uv.x * 4.0 + 3.0, 1.0)) - 0.35) * 0.30;
    float r2 = 0.20 + (fbm(vec2(uv.x * 5.5 + 11.0, 7.0)) - 0.35) * 0.24;
    col = mix(col, mix(vec3(0.04, 0.063, 0.133), uBase.rgb, 0.4), smoothstep(r1 + a, r1 - a, uv.y));
    col = mix(col, mix(vec3(0.024, 0.04, 0.086), uBase.rgb * 0.6, 0.4), smoothstep(r2 + a, r2 - a, uv.y));
    col *= mix(0.55, 1.0, smoothstep(0.0, 0.4, uv.y));
    return col;
}
#endif

#ifdef BG_NEBULA
vec3 background(vec2 uv, vec2 p, float t) {
    vec3 col = uBase.rgb * 0.3;
    vec2 q = p * 2.2;
    vec2 w = vec2(fbm(q + vec2(0.0, t * 0.02)), fbm(q + vec2(5.2, 1.3) - t * 0.015));
    float n = fbm(q + 2.5 * w + t * 0.01);
    float n2 = fbm(q * 1.7 - 1.8 * w + 4.0);
    vec3 neb = mix(uAccentB.rgb, uAccentA.rgb, smoothstep(0.3, 0.75, n2));
    col += neb * pow(n, 2.6) * 2.4;
    col += mix(uAccentA.rgb, vec3(1.0), 0.6) * pow(n, 6.0) * 2.2;
    col *= 0.55 + 0.45 * smoothstep(0.22, 0.6, fbm(q * 3.0 + 9.0));
    col += vec3(stars2(p, 70.0, 0.14, t) + 0.6 * stars2(p + 7.0, 150.0, 0.12, t * 1.3));
    return vignette(col, uv);
}
#endif

#ifdef BG_SYNTHWAVE
vec3 background(vec2 uv, vec2 p, float t) {
    float horizon = 0.38;
    float a = px1() * 1.5;
    vec3 col;
    if (uv.y > horizon) {
        float h = (uv.y - horizon) / (1.0 - horizon);
        col = mix(mix(uAccentB.rgb, vec3(1.0, 0.45, 0.35), 0.45) * 0.9, uBase.rgb * 0.5, pow(h, 0.55));
        col += vec3(stars2(p, 55.0, 0.15, t)) * smoothstep(0.35, 0.8, h);
        vec2 sc = vec2(0.0, horizon - 0.5 + 0.2);
        float r = 0.2;
        vec2 sp = p - sc;
        float d = length(sp);
        float yy = sp.y / r;
        float stripe = yy > 0.05 ? 1.0 : step(mix(0.08, 0.6, clamp(-yy, 0.0, 1.0)), fract(yy * 7.0 - t * 0.35));
        float disc = smoothstep(r + a, r - a, d) * stripe;
        vec3 sunCol = mix(vec3(1.0, 0.9, 0.35), mix(uAccentB.rgb, vec3(1.0, 0.2, 0.6), 0.5), clamp(0.5 - yy * 0.5, 0.0, 1.0));
        col += mix(uAccentB.rgb, vec3(1.0, 0.4, 0.6), 0.5) * exp(-d * 4.5) * 0.4;
        col = mix(col, sunCol, disc);
        float m = horizon + 0.035 + (fbm(vec2(uv.x * 6.0, 2.0)) - 0.4) * 0.12;
        col = mix(col, uBase.rgb * 0.35, smoothstep(m + a, m - a, uv.y));
    } else {
        float depth = 1.0 / max(horizon - uv.y, 0.002);
        vec2 g = vec2(p.x * depth * 0.6, depth * 0.6 + t * 1.2);
        vec2 gw = fwidth(g);
        vec2 gl = abs(fract(g) - 0.5) / max(gw, 1e-4);
        float line = 1.0 - clamp(min(gl.x, gl.y) - 0.5, 0.0, 1.0);
        float fade = smoothstep(0.0, 0.12, horizon - uv.y);
        col = uBase.rgb * 0.25 + uAccentA.rgb * line * fade * 0.9;
        col += mix(uAccentB.rgb, vec3(1.0, 0.4, 0.6), 0.5) * exp(-(horizon - uv.y) * 14.0) * 0.35;
    }
    return col;
}
#endif

#ifdef BG_LIQUID
vec3 background(vec2 uv, vec2 p, float t) {
    vec2 q = p * 1.5;
    float s = t * 0.09;
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        q += 0.32 * vec2(sin(q.y * 2.1 + s * 3.0 + fi), cos(q.x * 1.7 - s * 2.0 + fi * 1.3));
    }
    float n = 0.5 + 0.5 * sin(q.x * 2.0 + q.y * 1.3);
    float m = 0.5 + 0.5 * cos(length(q) * 2.4 - s * 4.0);
    vec3 dark = uBase.rgb * 0.7;
    vec3 c = mix(dark, uAccentA.rgb * 0.75, smoothstep(0.25, 0.95, n));
    c = mix(c, uAccentB.rgb * 0.8, smoothstep(0.35, 1.0, m) * 0.75);
    c = mix(c, dark, 0.3);
    c += vec3(1.0) * pow(max(n * m, 0.0), 8.0) * 0.18;
    return vignette(c, uv);
}
#endif

#ifdef BG_WAVES
vec3 background(vec2 uv, vec2 p, float t) {
    vec3 col = mix(uBase.rgb * 1.2, uBase.rgb * 0.35, uv.y);
    float a = px1() * 1.5;
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float w = 0.62 - fi * 0.08
                + 0.045 * sin(uv.x * (3.0 + fi * 0.7) + t * (0.3 + 0.08 * fi) + fi)
                + 0.025 * sin(uv.x * (7.0 + fi) - t * 0.5 + fi * 2.0);
        vec3 lc = mix(uAccentA.rgb, uAccentB.rgb, fi / 5.0);
        float below = smoothstep(w + a, w - a, uv.y);
        col = mix(col, mix(uBase.rgb, lc, 0.18 + fi * 0.05), below * 0.55);
        col += lc * exp(-abs(uv.y - w) * 160.0) * 0.55;
    }
    col += vec3(stars2(p, 60.0, 0.1, t)) * smoothstep(0.65, 0.9, uv.y) * 0.6;
    return col;
}
#endif

#ifdef BG_WARP
vec3 background(vec2 uv, vec2 p, float t) {
    vec3 col = uBase.rgb * 0.25;
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float depth = fract(t * 0.07 + fi * 0.25);
        float scale = mix(18.0, 0.6, depth);
        vec2 sp = p * scale + vec2(fi * 17.0, fi * 31.0);
        vec2 cell = floor(sp);
        vec2 h = hash22(cell);
        float d = length(sp - cell - 0.15 - h * 0.7);
        float fade = smoothstep(0.0, 0.5, depth) * smoothstep(1.0, 0.85, depth);
        float present = step(0.55, hash12(cell + fi));
        vec3 sc = mix(uAccentA.rgb, uAccentB.rgb, h.x);
        sc = mix(sc, vec3(1.0), 0.5);
        col += sc * present * fade * smoothstep(0.06 + 0.02 * depth, 0.0, d) * 1.4;
    }
    col += mix(uAccentA.rgb, uAccentB.rgb, 0.5) * exp(-length(p) * 3.5) * 0.25;
    return vignette(col, uv);
}
#endif

#ifdef BG_LAVA
vec3 background(vec2 uv, vec2 p, float t) {
    float f = 0.0;
    for (int i = 0; i < 7; i++) {
        float fi = float(i);
        vec2 c = vec2(sin(t * 0.21 * (1.0 + fi * 0.13) + fi * 1.7) * 0.75,
                      cos(t * 0.17 * (1.0 + fi * 0.11) + fi * 2.3) * 0.42);
        float r = 0.11 + 0.035 * sin(fi * 2.1 + t * 0.3);
        vec2 d = p - c;
        f += r * r / max(dot(d, d), 1e-4);
    }
    float a = fwidth(f) * 1.2;
    float m = smoothstep(1.0 - a, 1.0 + a, f);
    vec3 blob = mix(uAccentB.rgb, uAccentA.rgb, clamp(uv.y + 0.15 * sin(t * 0.2), 0.0, 1.0));
    vec3 col = uBase.rgb * 0.45 + blob * clamp(f * 0.12, 0.0, 0.45);
    col = mix(col, blob * 0.9, m);
    col += vec3(1.0) * smoothstep(1.6, 3.5, f) * 0.12;
    return vignette(col, uv);
}
#endif

#ifdef BG_MINIMAL
vec3 background(vec2 uv, vec2 p, float t) {
    vec3 col = uBase.rgb * 0.85;
    vec2 c1 = vec2(sin(t * 0.11) * 0.5 - 0.3, cos(t * 0.09) * 0.25 + 0.15);
    vec2 c2 = vec2(cos(t * 0.07) * 0.5 + 0.35, sin(t * 0.13) * 0.25 - 0.15);
    col += uAccentA.rgb * 0.22 * exp(-dot(p - c1, p - c1) * 3.0);
    col += uAccentB.rgb * 0.22 * exp(-dot(p - c2, p - c2) * 3.0);
    vec2 g = p * 28.0;
    float dotMask = smoothstep(0.09, 0.04, length(fract(g) - 0.5));
    col += vec3(dotMask) * 0.025;
    return vignette(col, uv);
}
#endif

void main() {
    vec2 uv = (vPos - uRect.xy) / uRect.zw;
    uv.y = 1.0 - uv.y;
    float aspect = uRect.z / max(uRect.w, 1.0);
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);
    vec3 c = background(uv, p, uTime.x);
    c += (hash12(gl_FragCoord.xy + fract(uTime.x) * 91.0) - 0.5) * (2.0 / 255.0);
    float alpha = uTime.y;
    if (uTime.z > 0.0) {
        float edge = sdRoundBox(vPos - uRect.xy - uRect.zw * 0.5, uRect.zw * 0.5, vec4(uTime.z));
        alpha *= clamp(0.5 - edge / max(fwidth(edge), 1e-4), 0.0, 1.0);
    }
    FRAG_COLOR = vec4(c, alpha);
}
