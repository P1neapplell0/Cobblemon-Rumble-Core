package com.p1nero.cobblemon.rumblecore.prologue;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Properties;

/** Seeds the fixed hall before levels open. Compare preset revisions, not mutable world data. */
final class HallMap {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private HallMap() {}

    static void install(Path preset, Path world) throws IOException {
        Path stamp = world.resolve(".rumble-map-sha256.properties");
        var installed = new Properties();
        if (Files.isRegularFile(stamp)) {
            try (var input = Files.newInputStream(stamp)) { installed.load(input); }
        }
        var hashes = new LinkedHashMap<String, String>();
        for (int x = -2; x <= 1; x++) {
            for (int z = -2; z <= 1; z++) {
                String region = "region/r." + x + "." + z + ".mca";
                hashes.put(region, sha256(preset.resolve(region)));
            }
        }

        Path backup = null;
        int copied = 0;
        var next = new Properties();
        for (var entry : hashes.entrySet()) {
            String region = entry.getKey();
            String hash = entry.getValue();
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
                Files.copy(preset.resolve(region), tmp, StandardCopyOption.REPLACE_EXISTING);
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
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536];
            for (int size; (size = input.read(buffer)) != -1;) digest.update(buffer, 0, size);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
