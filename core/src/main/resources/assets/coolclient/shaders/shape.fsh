// The UI uber-shader: every rounded rectangle, outline, shadow, glow, circle, ring, arc and
// frosted-glass panel is one quad shaded by a signed distance field. Anti-aliasing comes from
// fwidth(), so edges stay one device pixel wide at any GUI scale or HUD zoom.

uniform vec4 uRect;      // centre xy, half size zw (GUI units)
uniform vec4 uRadii;     // top-left, top-right, bottom-right, bottom-left
uniform vec4 uColor0;
uniform vec4 uColor1;
uniform vec4 uParams;    // x mode, y stroke width / blur size, z gradient kind, w shine phase (<0 = off)
uniform vec4 uArc;       // x radius, y start angle, z sweep (radians, 0 = up, clockwise)
uniform vec4 uViewport;  // glass: framebuffer size xy, blur level z, grain w
uniform sampler2D uBackdrop;

VARYING_IN vec2 vPos;

// Modes
#define M_FILL 0.0
#define M_STROKE 1.0
#define M_SHADOW 2.0
#define M_GLOW 3.0
#define M_ARC 4.0
#define M_GLASS 5.0

bool isMode(float m) { return abs(uParams.x - m) < 0.5; }

vec4 gradientColor(vec2 p) {
    float g = uParams.z;
    float t = 0.0;
    if (g > 3.5) t = (p.x / uRect.z + p.y / uRect.w) * 0.25 + 0.5;
    else if (g > 2.5) t = length(p / uRect.zw);
    else if (g > 1.5) t = p.y / uRect.w * 0.5 + 0.5;
    else if (g > 0.5) t = p.x / uRect.z * 0.5 + 0.5;
    return mix(uColor0, uColor1, clamp(t, 0.0, 1.0));
}

// Gaussian-ish blur of the captured backdrop: a mip level picked by uViewport.z plus two
// rings of taps on that level to hide the blockiness of the mip chain.
vec3 backdrop(vec2 uv) {
    float lod = uViewport.z;
    vec2 px = exp2(lod) / uViewport.xy;
    vec3 c = TEX(uBackdrop, uv, lod).rgb * 0.16;
    float w = 0.16;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.785398;
        vec2 o = vec2(cos(a), sin(a));
        c += TEX(uBackdrop, uv + o * px * 1.2, lod).rgb * 0.075;
        c += TEX(uBackdrop, uv + vec2(o.x - o.y, o.x + o.y) * 0.7071 * px * 2.6, lod).rgb * 0.03;
        w += 0.105;
    }
    return c / w;
}

void main() {
    vec2 p = vPos - uRect.xy;

    if (isMode(M_ARC)) {
        float ang = atan(p.x, -p.y);
        float rel = mod(ang - uArc.y, TAU);
        float d;
        if (rel <= uArc.z) {
            d = abs(length(p) - uArc.x) - uParams.y * 0.5;
        } else {
            vec2 e0 = uArc.x * vec2(sin(uArc.y), -cos(uArc.y));
            float a1 = uArc.y + uArc.z;
            vec2 e1 = uArc.x * vec2(sin(a1), -cos(a1));
            d = min(length(p - e0), length(p - e1)) - uParams.y * 0.5;
        }
        float aa = max(fwidth(d), 1e-4);
        vec4 c = uColor0;
        c.a *= clamp(0.5 - d / aa, 0.0, 1.0);
        FRAG_COLOR = c;
        return;
    }

    float d = sdRoundBox(p, uRect.zw, uRadii);
    float aa = max(fwidth(d), 1e-4);
    float cov;
    if (isMode(M_STROKE)) {
        float w = uParams.y;
        float ds = abs(d + w * 0.5) - w * 0.5;
        cov = clamp(0.5 - ds / aa, 0.0, 1.0);
    } else if (isMode(M_SHADOW)) {
        float b = max(uParams.y, 0.001);
        cov = 0.5 - 0.5 * erfApprox((d - b * 0.15) / (b * 0.4));
    } else if (isMode(M_GLOW)) {
        float b = max(uParams.y, 0.001);
        float k = 1.0 - clamp(d / b, 0.0, 1.0);
        cov = k * k * clamp(d / aa + 0.5, 0.0, 1.0);
    } else {
        cov = clamp(0.5 - d / aa, 0.0, 1.0);
    }

    vec4 col;
    if (isMode(M_GLASS)) {
        vec2 uv = gl_FragCoord.xy / uViewport.xy;
        vec3 g = backdrop(uv);
        g = mix(g, uColor0.rgb, uColor0.a);
        // A soft top-down sheen and a faint inner rim sell the glass.
        float top = 1.0 - (p.y / uRect.w * 0.5 + 0.5);
        g += vec3(1.0) * top * top * 0.045;
        g += vec3(1.0) * (1.0 - smoothstep(0.0, 1.6, -d)) * 0.05;
        g += (hash12(gl_FragCoord.xy) - 0.5) * uViewport.w;
        col = vec4(g, uColor1.a);
    } else {
        col = gradientColor(p);
    }

    if (uParams.w >= 0.0) {
        float s = (p.x + p.y * 0.6) / (uRect.z * 2.0 + 1.0);
        float pos = uParams.w * 3.0 - 1.5;
        float band = 1.0 - smoothstep(0.0, 0.16, abs(s - pos));
        col.rgb = mix(col.rgb, vec3(1.0), band * 0.32);
    }

    col.a *= cov;
    FRAG_COLOR = col;
}
