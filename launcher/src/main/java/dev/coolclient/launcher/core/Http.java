package dev.coolclient.launcher.core;

import dev.coolclient.core.json.Json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/** HTTP helpers: JSON requests and verified, resumable-by-retry downloads. */
public final class Http {
    public static final String USER_AGENT = "CoolClientLauncher/" + LauncherInfo.VERSION;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private Http() {}

    public interface Task {
        void run() throws Exception;
    }

    public static HttpResponse<String> send(HttpRequest.Builder req) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpResponse<String> res = CLIENT.send(req.header("User-Agent", USER_AGENT).timeout(Duration.ofSeconds(30)).build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (res.statusCode() >= 500 && attempt < 2) {
                    last = new IOException("HTTP " + res.statusCode());
                } else {
                    return res;
                }
            } catch (IOException e) {
                last = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
            sleep(500L * (attempt + 1));
        }
        throw new IOException("Request failed: " + last.getMessage(), last);
    }

    public static Map<String, Object> getJson(String url) throws IOException {
        HttpResponse<String> res = send(HttpRequest.newBuilder(URI.create(url)).GET());
        if (res.statusCode() != 200) throw new IOException("HTTP " + res.statusCode() + " for " + url);
        return Json.parseObject(res.body());
    }

    public static List<Object> getJsonArray(String url) throws IOException {
        HttpResponse<String> res = send(HttpRequest.newBuilder(URI.create(url)).GET());
        if (res.statusCode() != 200) throw new IOException("HTTP " + res.statusCode() + " for " + url);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) Json.parse(res.body());
        return list;
    }

    /** POSTs and returns {status, parsed JSON body (empty map if not JSON)}. */
    public static Object[] post(String url, String contentType, String body) throws IOException {
        HttpResponse<String> res = send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", contentType).header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
        Map<String, Object> json;
        try {
            json = Json.parseObject(res.body());
        } catch (RuntimeException e) {
            json = new java.util.LinkedHashMap<String, Object>();
        }
        return new Object[]{res.statusCode(), json};
    }

    // ------------------------------------------------------------------ downloads

    public static String sha1(File f) throws IOException {
        try (InputStream in = Files.newInputStream(f.toPath())) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] chunk = new byte[1 << 16];
            for (int n; (n = in.read(chunk)) > 0; ) md.update(chunk, 0, n);
            return hex(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** True if the file exists and matches the expected size/hash (when given). */
    public static boolean isValid(File f, String sha1, long size) {
        if (!f.isFile()) return false;
        if (size > 0 && f.length() != size) return false;
        if (sha1 == null || sha1.isEmpty()) return f.length() > 0;
        try {
            return sha1(f).equalsIgnoreCase(sha1);
        } catch (IOException e) {
            return false;
        }
    }

    /** Downloads to dest via a temp file, verifying the SHA-1 when known. Skips valid files. */
    public static void download(String url, File dest, String sha1, long size) throws IOException {
        if (isValid(dest, sha1, size)) return;
        File dir = dest.getParentFile();
        if (dir != null) dir.mkdirs();
        File tmp = new File(dest.getPath() + ".part");
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpResponse<InputStream> res = CLIENT.send(HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", USER_AGENT).timeout(Duration.ofMinutes(5)).GET().build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() != 200) {
                    res.body().close();
                    throw new IOException("HTTP " + res.statusCode() + " downloading " + url);
                }
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(tmp.toPath())) {
                    byte[] chunk = new byte[1 << 16];
                    for (int n; (n = in.read(chunk)) > 0; ) {
                        md.update(chunk, 0, n);
                        out.write(chunk, 0, n);
                    }
                }
                if (sha1 != null && !sha1.isEmpty() && !hex(md.digest()).equalsIgnoreCase(sha1)) {
                    throw new IOException("Checksum mismatch for " + url);
                }
                Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return;
            } catch (IOException e) {
                last = e;
                sleep(600L * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
        tmp.delete();
        throw last;
    }

    /** Runs tasks on {@code threads} workers, reporting (done, total). Fails on the first error. */
    public static void pool(List<Task> tasks, int threads, Progress.Fraction progress) throws IOException {
        if (tasks.isEmpty()) return;
        ExecutorService exec = Executors.newFixedThreadPool(Math.min(threads, tasks.size()));
        AtomicInteger done = new AtomicInteger();
        try {
            List<Future<?>> futures = new java.util.ArrayList<Future<?>>();
            for (Task t : tasks) {
                futures.add(exec.submit(() -> {
                    t.run();
                    if (progress != null) progress.update(done.incrementAndGet() / (float) tasks.size());
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException) throw (IOException) cause;
                    throw new IOException(cause.getMessage(), cause);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } finally {
            exec.shutdownNow();
        }
    }

    public static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
        return sb.toString();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
