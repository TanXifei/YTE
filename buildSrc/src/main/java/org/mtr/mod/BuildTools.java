package org.mtr.mod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonafanho.apitools.ModId;
import com.jonafanho.apitools.ModLoader;
import com.jonafanho.apitools.ModProvider;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gradle.api.Project;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

public class BuildTools {

    private static final Logger LOGGER = LogManager.getLogger("Build");
    public final String minecraftVersion;
    public final String loader;
    public final int javaLanguageVersion;
    private final Path path;
    private final String version;
    private final int majorVersion;
    private final String yarnVersionFallback;
    private final String fabricLoaderVersionFallback;
    private final String fabricApiVersionFallback;
    private final String modMenuVersionFallback;

    public BuildTools(String minecraftVersion, String loader, Project project) throws IOException {
        this.minecraftVersion = minecraftVersion;
        this.loader = loader;
        path = project.getProjectDir().toPath();
        version = project.getVersion().toString();
        majorVersion = Integer.parseInt(minecraftVersion.split("\\.")[1]);
        javaLanguageVersion = majorVersion <= 16 ? 8 : majorVersion == 17 ? 16 : 17;
        yarnVersionFallback = (String) project.findProperty("yarnVersion");
        fabricLoaderVersionFallback = (String) project.findProperty("fabricLoaderVersion");
        fabricApiVersionFallback = (String) project.findProperty("fabricApiVersion");
        modMenuVersionFallback = (String) project.findProperty("modMenuVersion");
    }

    private static Path getCacheDir() {
        final Path cacheDir = Path.of(System.getProperty("user.home"), ".gradle", "buildtools-cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (Exception e) {
            LOGGER.error("Failed to create cache directory", e);
        }
        return cacheDir;
    }

    private static String hashUrl(String url) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(url.hashCode());
        }
    }

    private static JsonElement getJson(String url) {
        final Path cacheFile = getCacheDir().resolve(hashUrl(url) + ".json");

        // Try network first
        for (int i = 0; i < 5; i++) {
            try {
                final String response = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
                final JsonElement element = JsonParser.parseString(response);
                // Cache successful response
                try {
                    Files.writeString(cacheFile, response);
                } catch (Exception e) {
                    LOGGER.error("Failed to cache response", e);
                }
                return element;
            } catch (Exception e) {
                LOGGER.error("Network attempt " + (i + 1) + " failed for: " + url, e);
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        // Network failed, try reading from cache
        if (Files.exists(cacheFile)) {
            try {
                LOGGER.info("Using cached response for: " + url);
                return JsonParser.parseString(Files.readString(cacheFile));
            } catch (Exception e) {
                LOGGER.error("Failed to read cache", e);
            }
        }

        return new JsonObject();
    }

    public String getFabricVersion() {
        try {
            return getJson("https://meta.fabricmc.net/v2/versions/loader/" + minecraftVersion).getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("loader").get("version").getAsString();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch fabric loader version, using fallback: " + fabricLoaderVersionFallback, e);
            return fabricLoaderVersionFallback;
        }
    }

    public String getYarnVersion() {
        try {
            return getJson("https://meta.fabricmc.net/v2/versions/yarn/" + minecraftVersion).getAsJsonArray().get(0).getAsJsonObject().get("version").getAsString();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch yarn version, using fallback: " + yarnVersionFallback, e);
            return yarnVersionFallback;
        }
    }

    public String getFabricApiVersion() {
        try {
            final String modIdString = "fabric-api";
            return new ModId(modIdString, ModProvider.MODRINTH).getModFiles(minecraftVersion, ModLoader.FABRIC, "").get(0).fileName.split(".jar")[0].replace(modIdString + "-", "");
        } catch (Exception e) {
            LOGGER.error("Failed to fetch fabric API version, using fallback: " + fabricApiVersionFallback, e);
            return fabricApiVersionFallback;
        }
    }

    public String getForgeVersion() {
        return getJson("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json").getAsJsonObject().getAsJsonObject("promos").get(minecraftVersion + "-latest").getAsString();
    }

    public String getModMenuVersion() {
        if (minecraftVersion.equals("1.20.4")) {
            return "9.0.0";
        }
        try {
            final String modIdString = "modmenu";
            return new ModId(modIdString, ModProvider.MODRINTH).getModFiles(minecraftVersion, ModLoader.FABRIC, "").get(0).fileName.split("\\.jar")[0].replace(modIdString + "-", "");
        } catch (Exception e) {
            LOGGER.error("Failed to fetch modmenu version, using fallback: " + modMenuVersionFallback, e);
            return modMenuVersionFallback;
        }
    }

    public String getMCVersionNumber() {
        String[] parts = minecraftVersion.split("\\.");
        String major = parts[0];
        String minor = parts.length > 1 ? String.format("%02d", Integer.parseInt(parts[1])) : "00";
        String patch = parts.length > 2 ? String.format("%02d", Integer.parseInt(parts[2])) : "00";
        return major + minor + patch;
    }


    public void copyBuildFile() throws IOException {
        final Path directory = path.getParent().resolve("build/release");
        Files.createDirectories(directory);
        Files.copy(path.resolve(String.format("build/libs/%s-%s.jar", loader, version)), directory.resolve(String.format("Yunzhu-Transit-Extension-%s-%s+%s.jar", loader, version, minecraftVersion)), StandardCopyOption.REPLACE_EXISTING);
    }
}
