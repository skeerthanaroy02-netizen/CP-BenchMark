package com.lpr;

import com.lpr.fetch.LeetCodeClient;
import com.lpr.fetch.LeetCodeFetchException;
import com.lpr.model.LPRScoreResult;
import com.lpr.model.UserProfile;
import com.lpr.scoring.LPRScoringEngine;
import com.lpr.storage.FileStorageManager;

import java.util.Locale;
import java.util.Scanner;

/**
 * Console entry point for the LPR (LeetCode Profile Rating) analyzer.
 *
 * Flow:
 *   1. Ask for a LeetCode username.
 *   2. Try to fetch public stats via the unofficial GraphQL API.
 *   3. If that fails (network issue, private profile, API change), fall
 *      back to manual entry so a demo never gets blocked by a flaky endpoint.
 *   4. Run the profile through the scoring engine.
 *   5. Print the LPR score + feedback, and save a snapshot to file history.
 */
public class Main {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US); // consistent number formatting regardless of machine locale
        Scanner scanner = new Scanner(System.in);

        System.out.println("===================");
        System.out.println("  CP BenchMark ");
        System.out.println("===================\n");

        System.out.print("Enter LeetCode username: ");
        String username = scanner.nextLine().trim();

        UserProfile profile = null;
        LeetCodeClient client = new LeetCodeClient();

        System.out.println("\nFetching public profile data for '" + username + "'...");
        try {
            profile = client.fetchProfile(username);
            System.out.println("Fetch successful.\n");
        } catch (LeetCodeFetchException e) {
            System.out.println("Could not fetch profile automatically: " + e.getMessage());
            System.out.println("Falling back to manual entry.\n");
            profile = promptManualEntry(scanner, username);
        }

        System.out.println(profile);
        System.out.println();

        LPRScoringEngine engine = new LPRScoringEngine();
        LPRScoreResult result = engine.score(profile);

        printReport(result);

        FileStorageManager storage = new FileStorageManager();
        double previousScore = storage.getPreviousScore(username);
        storage.save(profile, result);

        if (previousScore >= 0) {
            double delta = Math.round((result.getLprOutOf10() - previousScore) * 100.0) / 100.0;
            String trend = delta > 0 ? "improved by" : (delta < 0 ? "dropped by" : "unchanged since");
            System.out.printf("Compared to your last check: %s %.2f (previous: %.2f/10)%n%n",
                    trend, Math.abs(delta), previousScore);
        }

        System.out.println("Snapshot saved to ./data/" + username.replaceAll("[^a-zA-Z0-9_-]", "_") + ".json");
        scanner.close();
    }

    /** Manual fallback data entry — mirrors what the app needs when the API is unavailable. */
    private static UserProfile promptManualEntry(Scanner scanner, String username) {
        UserProfile p = new UserProfile(username);

        p.setGlobalRanking(promptInt(scanner, "Global rank (e.g. 42000): "));
        p.setProblemsEasy(promptInt(scanner, "Easy problems solved: "));
        p.setProblemsMedium(promptInt(scanner, "Medium problems solved: "));
        p.setProblemsHard(promptInt(scanner, "Hard problems solved: "));
        p.setProblemsSolvedTotal(p.getProblemsEasy() + p.getProblemsMedium() + p.getProblemsHard());

        p.setTotalSubmissions(promptInt(scanner, "Total submissions: "));
        int accepted = promptInt(scanner, "Accepted submissions (or press Enter to estimate): ");
        p.setAcceptedSubmissions(accepted > 0 ? accepted : p.getTotalSubmissions());

        p.setContestRating(promptInt(scanner, "Contest rating (0 if none): "));
        p.setContestsAttended(promptInt(scanner, "Contests attended: "));

        p.setCurrentStreak(promptInt(scanner, "Current daily streak (days): "));
        p.setTotalActiveDays(promptInt(scanner, "Total active days: "));

        p.setBadgeCount(promptInt(scanner, "Number of badges earned: "));
        p.setBadgeNames(java.util.List.of()); // names aren't needed for scoring in manual mode

        return p;
    }

    private static int promptInt(Scanner scanner, String label) {
        System.out.print(label);
        String line = scanner.nextLine().trim();
        if (line.isEmpty()) return 0;
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("  (invalid number, using 0)");
            return 0;
        }
    }

    private static void printReport(LPRScoreResult r) {
        System.out.println("----------- LPR SCORE REPORT -----------");
        System.out.printf("Global Rank score       : %.1f / 20%n", r.getRankScore());
        System.out.printf("Problems Solved score   : %.1f / 20%n", r.getSolvedScore());
        System.out.printf("Difficulty Mix score    : %.1f / 10%n", r.getDifficultyScore());
        System.out.printf("Contest score           : %.1f / 20%n", r.getContestScore());
        System.out.printf("Streak/Consistency score: %.1f / 10%n", r.getStreakScore());
        System.out.printf("Acceptance Rate score   : %.1f / 10%n", r.getAcceptanceScore());
        System.out.printf("Badges score            : %.1f / 5%n", r.getBadgeScore());
        System.out.printf("Submissions score       : %.1f / 5%n", r.getSubmissionScore());
        System.out.println("-----------------------------------------");
        System.out.printf("TOTAL                   : %.1f / 100%n", r.getTotalOutOf100());
        System.out.printf("LPR SCORE               : %.2f / 10%n", r.getLprOutOf10());
        System.out.println("-----------------------------------------\n");

        System.out.println("Strengths:");
        r.getStrengths().forEach(s -> System.out.println("  + " + s));

        if (!r.getWeaknesses().isEmpty()) {
            System.out.println("\nWeaknesses:");
            r.getWeaknesses().forEach(w -> System.out.println("  - " + w));
        }

        if (!r.getSuggestions().isEmpty()) {
            System.out.println("\nSuggestions:");
            r.getSuggestions().forEach(s -> System.out.println("  * " + s));
        }
        System.out.println();
    }
}
