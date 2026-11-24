package com.p1nero.cobblemon.rumblecore.prologue;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.slf4j.Logger;

/** Seeds the fixed hall before levels open. Compare preset revisions, not mutable world data. */
final class HallMap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private record Region(URL source, String hash) {}

    private HallMap() {}

    private static URL presetRegion(Path preset, String region) throws IOException {
        // The two central equipment regions are versioned with Core. A stale installation's config
        // must not mask their updates; the rest of the terrain still comes from modpack_override.
        if (region.equals("region/r.-1.-1.mca") || region.equals("region/r.-1.0.mca")) {
            var resource = HallMap.class.getResource("/defaults/cobblemon_rumble_core/maps/gyms/" + region);
            if (resource == null) throw new IOException("Missing bundled hall region: " + region);
            return resource;
        }
        return preset.resolve(region).toUri().toURL();
    }

    static void install(Path preset, Path world) throws IOException {
        Path stamp = world.resolve(".rumble-map-sha256.properties");
        var installed = new Properties();
        if (Files.isRegularFile(stamp)) {
            try (var input = Files.newInputStream(stamp)) { installed.load(input); }
        }
        var regions = new LinkedHashMap<String, Region>();
        for (int x = -2; x <= 1; x++) {
            for (int z = -2; z <= 1; z++) {
                String region = "region/r." + x + "." + z + ".mca";
                URL source = presetRegion(preset, region);
                try (var input = source.openStream()) {
                    regions.put(region, new Region(source, sha256(input)));
                }
            }
        }

        Path backup = null;
        int copied = 0;
        var next = new Properties();
        for (var entry : regions.entrySet()) {
            String region = entry.getKey();
            Region template = entry.getValue();
            String hash = template.hash();
            next.setProperty(region, hash);
            Path dest = world.resolve(region);
            boolean exists = Files.isRegularFile(dest);
            if (exists && hash.equals(installed.getProperty(region))) continue;
            if (exists && hash.equals(sha256(dest))) continue;
            if (exists) {
                // Always leave the pre-update terrain available for recovery; never touch entities/ or world SavedData.
                if (backup == null) {
                    Path backups = world.resolve(".rumble-map-backups");
                    Files.createDirectories(backups);
                    backup = Files.createTempDirectory(backups, "before-");
                }
                Path saved = backup.resolve(region);
                Files.createDirectories(saved.getParent());
                Files.copy(dest, saved);
            }
            Files.createDirectories(dest.getParent());
            Path tmp = Files.createTempFile(dest.getParent(), ".rumble-region-", ".tmp");
            try {
                try (var input = template.source().openStream()) {
                    Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                if (!hash.equals(sha256(tmp))) throw new IOException("Hall preset changed during install: " + region);
                replace(tmp, dest);
            } finally {
                Files.deleteIfExists(tmp);
            }
            copied++;
        }
        if (!next.equals(installed)) {
            Path tmp = Files.createTempFile(world, ".rumble-hash-", ".tmp");
            try {
                try (var output = Files.newOutputStream(tmp)) { next.store(output, "Installed hall preset SHA-256"); }
                replace(tmp, stamp);
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
        if (copied > 0) {
            LOGGER.info("Installed {} hall terrain region(s), backup: {}", copied, backup);
        }
    }

    private static void replace(Path source, Path dest) throws IOException {
        Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String sha256(Path file) throws IOException {
        try (var input = Files.newInputStream(file)) { return sha256(input); }
    }

    private static String sha256(InputStream input) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] buffer = new byte[65536];
        for (int size; (size = input.read(buffer)) != -1;) digest.update(buffer, 0, size);
        return HexFormat.of().formatHex(digest.digest());
    }
}
