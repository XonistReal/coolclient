// Full-screen triangle for the world sky; positions are already in clip space.
ATTRIBUTE vec2 aPos;
VARYING_OUT vec2 vPos;

void main() {
    vPos = aPos;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
