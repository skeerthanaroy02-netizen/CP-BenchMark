package com.lpr.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lpr.model.LPRScoreResult;
import com.lpr.model.UserProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Persists profile history to local JSON files under ./data/<username>.json.
 *
 * This stands in for the MySQL/JDBC layer described in the full project
 * proposal. It's deliberately isolated behind this one class so that
 * swapping to JDBC later means writing a JdbcStorageManager with the same
 * method signatures (save/loadHistory) — nothing else in the app needs to change.
 */
public class FileStorageManager {

    private static final Path DATA_DIR = Paths.get("data");
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper mapper = new ObjectMapper();

    public FileStorageManager() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Could not create data directory: " + e.getMessage(), e);
        }
    }

    /**
     * Appends a new snapshot (profile + score) to that user's history file.
     * Each file is a JSON array of snapshots, oldest first — this is what
     * powers "progress tracking" later without needing a DB.
     */
    public void save(UserProfile profile, LPRScoreResult result) {
        Path file = fileFor(profile.getUsername());
        ArrayNode history = readHistoryArray(file);

        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("timestamp", LocalDateTime.now().format(TS_FORMAT));
        snapshot.put("username", profile.getUsername());
        snapshot.put("globalRanking", profile.getGlobalRanking());
        snapshot.put("problemsSolvedTotal", profile.getProblemsSolvedTotal());
        snapshot.put("problemsEasy", profile.getProblemsEasy());
        snapshot.put("problemsMedium", profile.getProblemsMedium());
        snapshot.put("problemsHard", profile.getProblemsHard());
        snapshot.put("totalSubmissions", profile.getTotalSubmissions());
        snapshot.put("acceptanceRate", Math.round(profile.getAcceptanceRate() * 10.0) / 10.0);
        snapshot.put("contestRating", profile.getContestRating());
        snapshot.put("contestsAttended", profile.getContestsAttended());
        snapshot.put("currentStreak", profile.getCurrentStreak());
        snapshot.put("badgeCount", profile.getBadgeCount());
        snapshot.put("lprScore", result.getLprOutOf10());

        history.add(snapshot);

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), history);
        } catch (IOException e) {
            System.err.println("Warning: could not save history for '" + profile.getUsername() + "': " + e.getMessage());
        }
    }

    /** Returns true if this user has at least one previously saved snapshot. */
    public boolean hasHistory(String username) {
        return Files.exists(fileFor(username));
    }

    /** Returns the previous LPR score for this user, or -1 if none exists. */
    public double getPreviousScore(String username) {
        ArrayNode history = readHistoryArray(fileFor(username));
        if (history.isEmpty()) return -1;
        return history.get(history.size() - 1).path("lprScore").asDouble(-1);
    }

    /** Loads full history for the "progress tracking" feature (raw JSON, newest last). */
    public List<String> loadHistoryRaw(String username) {
        ArrayNode history = readHistoryArray(fileFor(username));
        return history.findValuesAsText("timestamp");
    }

    // ---- Internal helpers ----

    private Path fileFor(String username) {
        String safeName = username.replaceAll("[^a-zA-Z0-9_-]", "_");
        return DATA_DIR.resolve(safeName + ".json");
    }

    private ArrayNode readHistoryArray(Path file) {
        File f = file.toFile();
        if (!f.exists()) {
            return mapper.createArrayNode();
        }
        try {
            var node = mapper.readTree(f);
            if (node.isArray()) {
                return (ArrayNode) node;
            }
        } catch (IOException e) {
            System.err.println("Warning: could not read existing history at " + file + ": " + e.getMessage());
        }
        return mapper.createArrayNode();
    }
}
