package com.lpr.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpr.model.UserProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Talks to LeetCode's unofficial public GraphQL endpoint to pull profile stats
 * for a given username. No login/credentials are ever used or required —
 * only data that is publicly visible on a user's profile page is read.
 *
 * NOTE: This endpoint is not officially documented/supported by LeetCode and
 * its shape can change without notice. If a call fails or returns nulls,
 * catch LeetCodeFetchException and fall back to manual entry
 * (see com.lpr.Main -> promptManualEntry()).
 */
public class LeetCodeClient {

    private static final String GRAPHQL_URL = "https://leetcode.com/graphql";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public LeetCodeClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Fetches and merges profile stats, contest info, and calendar/streak data
     * for the given public username.
     */
    public UserProfile fetchProfile(String username) throws LeetCodeFetchException {
        UserProfile profile = new UserProfile(username);

        JsonNode profileData = runQuery(PROFILE_QUERY, username);
        applyProfileData(profile, profileData);

        JsonNode contestData = runQuery(CONTEST_QUERY, username);
        applyContestData(profile, contestData);

        JsonNode calendarData = runQuery(CALENDAR_QUERY, username);
        applyCalendarData(profile, calendarData);

        return profile;
    }

    // ---- Individual GraphQL queries ----

    private static final String PROFILE_QUERY =
        "query userProfile($username: String!) { " +
        "  matchedUser(username: $username) { " +
        "    username " +
        "    profile { ranking } " +
        "    submitStatsGlobal { " +
        "      acSubmissionNum { difficulty count submissions } " +
        "    } " +
        "    badges { displayName } " +
        "  } " +
        "}";

    private static final String CONTEST_QUERY =
        "query userContestInfo($username: String!) { " +
        "  userContestRanking(username: $username) { " +
        "    attendedContestsCount " +
        "    rating " +
        "  } " +
        "}";

    private static final String CALENDAR_QUERY =
        "query userCalendar($username: String!) { " +
        "  matchedUser(username: $username) { " +
        "    userCalendar { streak totalActiveDays } " +
        "  } " +
        "}";

    // ---- Core HTTP + parsing ----

    private JsonNode runQuery(String query, String username) throws LeetCodeFetchException {
        try {
            Map<String, Object> variables = Map.of("username", username);
            Map<String, Object> body = Map.of("query", query, "variables", variables);
            String jsonBody = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPHQL_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Referer", "https://leetcode.com/" + username + "/")
                    .header("User-Agent", "Mozilla/5.0 (LPR-Analyzer academic project)")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new LeetCodeFetchException(
                    "LeetCode API returned HTTP " + response.statusCode() + " for user '" + username + "'");
            }

            JsonNode root = mapper.readTree(response.body());

            if (root.has("errors")) {
                throw new LeetCodeFetchException(
                    "LeetCode API returned an error for user '" + username + "': " + root.get("errors").toString());
            }

            return root.path("data");

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LeetCodeFetchException("Network error while contacting LeetCode: " + e.getMessage(), e);
        }
    }

    private void applyProfileData(UserProfile profile, JsonNode data) throws LeetCodeFetchException {
        JsonNode matchedUser = data.path("matchedUser");
        if (matchedUser.isMissingNode() || matchedUser.isNull()) {
            throw new LeetCodeFetchException(
                "No public profile found for username '" + profile.getUsername() +
                "'. It may not exist, or may be private.");
        }

        profile.setGlobalRanking(matchedUser.path("profile").path("ranking").asInt(0));

        int easy = 0, medium = 0, hard = 0, allSolved = 0, allSubmissions = 0;
        for (JsonNode entry : matchedUser.path("submitStatsGlobal").path("acSubmissionNum")) {
            String difficulty = entry.path("difficulty").asText("");
            int count = entry.path("count").asInt(0);
            int submissions = entry.path("submissions").asInt(0);

            switch (difficulty) {
                case "Easy" -> easy = count;
                case "Medium" -> medium = count;
                case "Hard" -> hard = count;
                case "All" -> { allSolved = count; allSubmissions = submissions; }
                default -> { /* ignore unknown difficulty buckets */ }
            }
        }

        profile.setProblemsEasy(easy);
        profile.setProblemsMedium(medium);
        profile.setProblemsHard(hard);
        profile.setProblemsSolvedTotal(allSolved);
        profile.setTotalSubmissions(allSubmissions);
        // LeetCode's public API doesn't expose failed-vs-accepted submission counts
        // directly here, so we approximate accepted submissions as solved-problem
        // submissions count, which is the closest public proxy for acceptance rate.
        profile.setAcceptedSubmissions(allSubmissions);

        List<String> badgeNames = new ArrayList<>();
        int badgeCount = 0;
        Iterator<JsonNode> badges = matchedUser.path("badges").elements();
        while (badges.hasNext()) {
            badgeNames.add(badges.next().path("displayName").asText("Badge"));
            badgeCount++;
        }
        profile.setBadgeNames(badgeNames);
        profile.setBadgeCount(badgeCount);
    }

    private void applyContestData(UserProfile profile, JsonNode data) {
        JsonNode ranking = data.path("userContestRanking");
        if (ranking.isMissingNode() || ranking.isNull()) {
            // User has simply never attended a contest — not an error.
            profile.setContestRating(0);
            profile.setContestsAttended(0);
            return;
        }
        profile.setContestRating(ranking.path("rating").asInt(0));
        profile.setContestsAttended(ranking.path("attendedContestsCount").asInt(0));
    }

    private void applyCalendarData(UserProfile profile, JsonNode data) {
        JsonNode calendar = data.path("matchedUser").path("userCalendar");
        profile.setCurrentStreak(calendar.path("streak").asInt(0));
        profile.setTotalActiveDays(calendar.path("totalActiveDays").asInt(0));
    }
}
