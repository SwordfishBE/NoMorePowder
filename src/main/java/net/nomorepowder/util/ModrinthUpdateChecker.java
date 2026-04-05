package net.nomorepowder.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.nomorepowder.NoMorePowder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModrinthUpdateChecker {

    private static final String PROJECT_ID = "kPiAZGSa";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private static final AtomicBoolean CHECK_STARTED = new AtomicBoolean(false);

    private ModrinthUpdateChecker() {
    }

    public static void checkOnceAsync() {
        if (!CHECK_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(ModrinthUpdateChecker::checkForUpdate, "nomorepowder-modrinth-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private static void checkForUpdate() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "NoMorePowder/" + currentVersion())
                .GET()
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                NoMorePowder.LOGGER.debug("[{}] Update check returned HTTP {}.", NoMorePowder.modName(), response.statusCode());
                return;
            }

            Optional<String> latestVersion = extractLatestVersion(response.body());
            if (latestVersion.isEmpty()) {
                NoMorePowder.LOGGER.debug("[{}] Update check returned no usable versions.", NoMorePowder.modName());
                return;
            }

            String currentVersion = currentVersion();
            String newestVersion = latestVersion.get();
            if (isNewerVersion(newestVersion, currentVersion)) {
                NoMorePowder.LOGGER.info("[{}] New version available: {} (current: {})",
                        NoMorePowder.modName(),
                        newestVersion, currentVersion);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            NoMorePowder.LOGGER.debug("[{}] Update check failed.", NoMorePowder.modName(), e);
        }
    }

    private static Optional<String> extractLatestVersion(String responseBody) {
        JsonElement root = JsonParser.parseString(responseBody);
        if (!root.isJsonArray()) {
            return Optional.empty();
        }

        JsonArray versions = root.getAsJsonArray();
        String minecraftVersion = currentMinecraftVersion();
        VersionCandidate newestCompatibleRelease = null;
        VersionCandidate newestRelease = null;

        for (JsonElement versionElement : versions) {
            if (!versionElement.isJsonObject()) {
                continue;
            }

            JsonObject versionObject = versionElement.getAsJsonObject();
            String versionNumber = getString(versionObject, "version_number");
            if (versionNumber == null || versionNumber.isBlank()) {
                continue;
            }

            Instant publishedAt = getPublishedAt(versionObject);
            if (publishedAt == null) {
                continue;
            }

            String versionType = getString(versionObject, "version_type");
            if (!"release".equalsIgnoreCase(versionType)) {
                continue;
            }

            VersionCandidate candidate = new VersionCandidate(versionNumber, publishedAt);
            if (newestRelease == null || candidate.publishedAt().isAfter(newestRelease.publishedAt())) {
                newestRelease = candidate;
            }

            boolean fabricMatch = jsonArrayContains(versionObject, "loaders", "fabric");
            boolean minecraftMatch = minecraftVersion != null
                    && jsonArrayContains(versionObject, "game_versions", minecraftVersion);
            if (fabricMatch && minecraftMatch
                    && (newestCompatibleRelease == null
                    || candidate.publishedAt().isAfter(newestCompatibleRelease.publishedAt()))) {
                newestCompatibleRelease = candidate;
            }
        }

        if (newestCompatibleRelease != null) {
            return Optional.of(newestCompatibleRelease.versionNumber());
        }

        return newestRelease == null ? Optional.empty() : Optional.of(newestRelease.versionNumber());
    }

    private static String getString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }

        return value.getAsString();
    }

    private static boolean jsonArrayContains(JsonObject object, String key, String expectedValue) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray() || expectedValue == null || expectedValue.isBlank()) {
            return false;
        }

        for (JsonElement element : value.getAsJsonArray()) {
            if (!element.isJsonNull() && expectedValue.equalsIgnoreCase(element.getAsString())) {
                return true;
            }
        }

        return false;
    }

    private static Instant getPublishedAt(JsonObject object) {
        String publishedAt = getString(object, "date_published");
        if (publishedAt == null || publishedAt.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(publishedAt);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String currentVersion() {
        return NoMorePowder.modVersion();
    }

    private static String currentMinecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    private static boolean isNewerVersion(String candidate, String current) {
        try {
            Version candidateVersion = Version.parse(candidate);
            Version currentVersion = Version.parse(current);
            return candidateVersion.compareTo(currentVersion) > 0;
        } catch (VersionParsingException e) {
            NoMorePowder.LOGGER.debug("[{}] Failed to compare versions. Candidate: {}, current: {}",
                    NoMorePowder.modName(),
                    candidate, current, e);
            return false;
        }
    }

    private record VersionCandidate(String versionNumber, Instant publishedAt) {
    }
}
