// GUI vertex shader: positions are GUI coordinates, MVP maps them (with the renderer's
// translate/scale stack) to clip space.
ATTRIBUTE vec2 aPos;
VARYING_OUT vec2 vPos;

void main() {
    vPos = aPos;
    gl_Position = MVP * vec4(aPos, 0.0, 1.0);
}
