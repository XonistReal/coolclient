// Full-screen post effects over the finished world frame.
// uMode 0: colour grading of uFrame (saturation, contrast, brightness, hue).
// uMode 1: frame blending, uFrame (the previous frame) drawn with alpha uGrade.x.
VARYING_IN vec2 vPos;
uniform sampler2D uFrame;
uniform vec4 uViewport;   // framebuffer size xy
uniform vec4 uGrade;      // x saturation, y contrast, z brightness, w hue shift (radians)
uniform int uMode;

vec3 hueShift(vec3 c, float a) {
    // Rotation around the grey axis (Rodrigues).
    const vec3 k = vec3(0.57735, 0.57735, 0.57735);
    float ca = cos(a);
    return c * ca + cross(k, c) * sin(a) + k * dot(k, c) * (1.0 - ca);
}

void main() {
    vec2 uv = gl_FragCoord.xy / uViewport.xy;
    vec4 src = TEX(uFrame, uv);
    if (uMode == 1) {
        FRAG_COLOR = vec4(src.rgb, uGrade.x);
        return;
    }
    vec3 c = src.rgb;
    if (uGrade.w != 0.0) c = hueShift(c, uGrade.w);
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(l), c, uGrade.x);
    c = (c - 0.5) * uGrade.y + 0.5;
    c *= uGrade.z;
    FRAG_COLOR = vec4(clamp(c, 0.0, 1.0), 1.0);
}
