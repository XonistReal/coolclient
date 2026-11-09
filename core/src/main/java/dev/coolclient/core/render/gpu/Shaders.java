package dev.coolclient.core.render.gpu;

import dev.coolclient.core.api.Gpu;
import dev.coolclient.core.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Compiles and caches the GLSL programs in {@code assets/coolclient/shaders}. Sources are
 * written once in a neutral dialect; a generated header maps it to GLSL 120 (1.8.9's legacy
 * context) or GLSL 150 (core profiles on 1.21.4 and the launcher). A program that fails to
 * compile is remembered as failed, and callers fall back to CPU geometry.
 */
public final class Shaders {
    /** A linked program and its lazily looked-up uniform locations. */
    public static final class Program {
        public final int id;
        private final Gpu gpu;
        private final Map<String, Integer> uniforms = new HashMap<String, Integer>();

        Program(Gpu gpu, int id) {
            this.gpu = gpu;
            this.id = id;
        }

        public int u(String name) {
            Integer loc = uniforms.get(name);
            if (loc == null) {
                loc = gpu.uniformLocation(id, name);
                uniforms.put(name, loc);
            }
            return loc;
        }
    }

    private static final Program FAILED = new Program(null, 0);
    private static final Map<Gpu, Map<String, Program>> CACHE = new IdentityHashMap<Gpu, Map<String, Program>>();
    private static final Map<String, String> SOURCES = new HashMap<String, String>();

    private Shaders() {}

    /** The SDF shape/glass program. */
    public static Program shape(Gpu gpu) {
        return get(gpu, "shape", "ui.vsh", "shape.fsh", "");
    }

    /** Full-screen post effects (colour grading, frame blending). */
    public static Program post(Gpu gpu) {
        return get(gpu, "post", "ui.vsh", "post.fsh", "");
    }

    public static Program background(Gpu gpu, Backgrounds.Style style) {
        return get(gpu, "bg:" + style.define, "ui.vsh", "background.fsh", "#define " + style.define + "\n");
    }

    public static Program sky(Gpu gpu, SkyStyle style, boolean preview) {
        return preview
                ? get(gpu, "skyp:" + style.define, "ui.vsh", "sky.fsh", "#define " + style.define + "\n#define PREVIEW\n")
                : get(gpu, "sky:" + style.define, "sky.vsh", "sky.fsh", "#define " + style.define + "\n");
    }

    private static Program get(Gpu gpu, String key, String vsh, String fsh, String defines) {
        Map<String, Program> programs = CACHE.get(gpu);
        if (programs == null) {
            programs = new HashMap<String, Program>();
            CACHE.put(gpu, programs);
        }
        Program p = programs.get(key);
        if (p == null) {
            p = compile(gpu, key, vsh, fsh, defines);
            programs.put(key, p);
        }
        return p == FAILED ? null : p;
    }

    private static Program compile(Gpu gpu, String key, String vsh, String fsh, String defines) {
        try {
            String common = source("common.glsl");
            String vs = header(gpu, false) + defines + source(vsh);
            String fs = header(gpu, true) + defines + common + source(fsh);
            int id = gpu.createProgram(key, vs, fs);
            if (id == 0) return FAILED;
            return new Program(gpu, id);
        } catch (Throwable t) {
            Log.error("Could not build shader " + key, t);
            return FAILED;
        }
    }

    private static String header(Gpu gpu, boolean fragment) {
        StringBuilder sb = new StringBuilder();
        if (gpu.glslVersion() >= 150) {
            sb.append("#version 150\n");
            sb.append("#define ATTRIBUTE in\n#define VARYING_OUT out\n#define VARYING_IN in\n#define TEX texture\n");
            sb.append("uniform mat4 uMVP;\n#define MVP uMVP\n");
            if (fragment) sb.append("out vec4 fragColor;\n#define FRAG_COLOR fragColor\n");
        } else {
            sb.append("#version 120\n");
            sb.append("#define ATTRIBUTE attribute\n#define VARYING_OUT varying\n#define VARYING_IN varying\n#define TEX texture2D\n");
            sb.append("#define MVP gl_ModelViewProjectionMatrix\n#define FRAG_COLOR gl_FragColor\n");
        }
        return sb.toString();
    }

    private static synchronized String source(String file) throws IOException {
        String s = SOURCES.get(file);
        if (s != null) return s;
        InputStream in = Shaders.class.getResourceAsStream("/assets/coolclient/shaders/" + file);
        if (in == null) throw new IOException("Missing shader " + file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            s = new String(out.toByteArray(), StandardCharsets.UTF_8) + "\n";
        } finally {
            in.close();
        }
        SOURCES.put(file, s);
        return s;
    }
}
