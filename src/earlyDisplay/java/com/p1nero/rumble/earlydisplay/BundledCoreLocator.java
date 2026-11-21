package com.p1nero.rumble.earlydisplay;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/** Bridges the early SERVICE layer to normal mod discovery without repacking Core or its dependencies. */
public final class BundledCoreLocator implements IModFileCandidateLocator {
    private static final String PAYLOAD = "/rumble-core/core.jar";

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        try {
            var core = cachedCore(FMLPaths.GAMEDIR.get().resolve(".rumble-cache/core"));
            pipeline.addPath(core, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
        } catch (IOException e) {
            pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.technical_error",
                    "Could not unpack the bundled Cobblemon Rumble Core: " + e).withCause(e));
        }
    }

    private static Path cachedCore(Path cache) throws IOException {
        byte[] payload;
        try (var input = BundledCoreLocator.class.getResourceAsStream(PAYLOAD)) {
            if (input == null) throw new FileNotFoundException(PAYLOAD);
            payload = input.readAllBytes();
        }

        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] digest = sha256.digest(payload);
        Path directory = cache.resolve(HexFormat.of().formatHex(digest));
        Path core = directory.resolve("cobblemon-rumble-core.jar");
        if (Files.isRegularFile(core) && Files.size(core) == payload.length
                && MessageDigest.isEqual(digest, sha256.digest(Files.readAllBytes(core)))) {
            return core;
        }

        Files.createDirectories(directory);
        Path staging = Files.createTempFile(directory, "core-", ".tmp");
        try {
            Files.write(staging, payload);
            try {
                Files.move(staging, core, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(staging, core, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(staging);
        }
        return core;
    }

    @Override
    public String toString() {
        return "Cobblemon Rumble bundled Core";
    }
}
