package dev.coolclient.launcher.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** The Cool Client mod jars bundled inside the launcher. */
public final class ModJars {
    private ModJars() {}

    /** Copies the bundled jar for a version out of the launcher (when changed) and returns it. */
    public static File ensure(String version) throws IOException {
        String res = "/bundled-mods/coolclient-" + version + ".jar";
        File dest = new File(LauncherPaths.MODS, "coolclient-" + version + ".jar");
        try (InputStream in = ModJars.class.getResourceAsStream(res)) {
            if (in == null) throw new IOException("This launcher build has no Cool Client jar for " + version + ".");
            byte[] data = in.readAllBytes();
            if (dest.isFile() && dest.length() == data.length && java.util.Arrays.equals(Files.readAllBytes(dest.toPath()), data)) {
                return dest;
            }
            dest.getParentFile().mkdirs();
            File tmp = new File(dest.getPath() + ".tmp");
            Files.write(tmp.toPath(), data);
            Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest;
        }
    }
}
