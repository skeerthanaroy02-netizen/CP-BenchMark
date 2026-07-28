package com.lpr.scoring;

import com.lpr.model.LPRScoreResult;
import com.lpr.model.UserProfile;

/**
 * Implements the LPR scoring rubric:
 *
 *   Global Rank                20%
 *   Problems Solved             20%
 *   Easy/Medium/Hard mix        10%
 *   Contest Rating & Attendance 20%
 *   Streak / Consistency        10%
 *   Acceptance Rate             10%
 *   Badges Earned                5%
 *   Total Submissions            5%
 *   ------------------------------
 *   Total                      100  ->  scaled to /10
 *
 * Each sub-scorer below documents the thresholds it uses. These thresholds
 * are reasonable starting points for a 2nd-year project and are easy to
 * tune later (e.g. after showing real profile data to your guide).
 */
public class LPRScoringEngine {

    public LPRScoreResult score(UserProfile p) {
        LPRScoreResult result = new LPRScoreResult(p.getUsername());

        result.setRankScore(scoreRank(p.getGlobalRanking()));
        result.setSolvedScore(scoreSolved(p.getProblemsSolvedTotal()));
        result.setDifficultyScore(scoreDifficultyMix(p));
        result.setContestScore(scoreContest(p.getContestRating(), p.getContestsAttended()));
        result.setStreakScore(scoreStreak(p.getCurrentStreak()));
        result.setAcceptanceScore(scoreAcceptance(p.getAcceptanceRate()));
        result.setBadgeScore(scoreBadges(p.getBadgeCount()));
        result.setSubmissionScore(scoreSubmissions(p.getTotalSubmissions()));

        double total = result.getRankScore() + result.getSolvedScore() + result.getDifficultyScore()
                + result.getContestScore() + result.getStreakScore() + result.getAcceptanceScore()
                + result.getBadgeScore() + result.getSubmissionScore();

        result.setTotalOutOf100(total);
        result.setLprOutOf10(Math.round((total / 10.0) * 100.0) / 100.0); // 2 decimal places

        generateFeedback(p, result);
        return result;
    }

    // ---- Sub-scorers (each returns marks out of its weight) ----

    /** Weight: 20. Lower global rank number = better. */
    private double scoreRank(int rank) {
        if (rank <= 0) return 0; // unranked / no data
        if (rank <= 5000) return 20;
        if (rank <= 20000) return 17;
        if (rank <= 50000) return 14;
        if (rank <= 100000) return 10;
        if (rank <= 300000) return 6;
        return 3;
    }

    /** Weight: 20. Total problems solved across all difficulties. */
    private double scoreSolved(int solved) {
        if (solved >= 1000) return 20;
        if (solved >= 500) return 17;
        if (solved >= 300) return 14;
        if (solved >= 150) return 10;
        if (solved >= 50) return 6;
        return Math.min(3, solved / 10.0);
    }

    /**
     * Weight: 10. Rewards a healthy mix rather than only Easy problems.
     * Full marks require meaningful Medium+Hard representation.
     */
    private double scoreDifficultyMix(UserProfile p) {
        int total = p.getProblemsSolvedTotal();
        if (total == 0) return 0;

        double mediumHardRatio = (p.getProblemsMedium() + p.getProblemsHard()) / (double) total;
        double hardRatio = p.getProblemsHard() / (double) total;

        double score = mediumHardRatio * 7 + hardRatio * 3;
        return Math.min(10, Math.round(score * 10.0) / 10.0);
    }

    /** Weight: 20. Combines rating level with contest attendance/experience. */
    private double scoreContest(int rating, int attended) {
        if (attended == 0) return 0;

        double ratingScore;
        if (rating >= 2100) ratingScore = 15;
        else if (rating >= 1800) ratingScore = 12;
        else if (rating >= 1600) ratingScore = 9;
        else if (rating >= 1400) ratingScore = 6;
        else ratingScore = 3;

        double attendanceScore = Math.min(5, attended / 4.0); // 20 contests = full 5

        return Math.min(20, ratingScore + attendanceScore);
    }

    /** Weight: 10. Rewards sustained daily practice. */
    private double scoreStreak(int streak) {
        if (streak >= 100) return 10;
        if (streak >= 60) return 8;
        if (streak >= 30) return 6;
        if (streak >= 14) return 4;
        if (streak >= 7) return 2;
        return 0;
    }

    /** Weight: 10. Acceptance rate as a percentage (0-100). */
    private double scoreAcceptance(double acceptanceRate) {
        if (acceptanceRate >= 75) return 10;
        if (acceptanceRate >= 65) return 8;
        if (acceptanceRate >= 55) return 6;
        if (acceptanceRate >= 45) return 4;
        return 2;
    }

    /** Weight: 5. */
    private double scoreBadges(int badgeCount) {
        return Math.min(5, badgeCount * 0.7);
    }

    /** Weight: 5. Rewards overall activity/volume. */
    private double scoreSubmissions(int totalSubmissions) {
        if (totalSubmissions >= 2000) return 5;
        if (totalSubmissions >= 1000) return 4;
        if (totalSubmissions >= 500) return 3;
        if (totalSubmissions >= 200) return 2;
        if (totalSubmissions >= 50) return 1;
        return 0;
    }

    // ---- Feedback generation ----

    private void generateFeedback(UserProfile p, LPRScoreResult r) {
        // Strengths
        if (r.getRankScore() >= 14) r.addStrength("Strong global ranking");
        if (r.getSolvedScore() >= 14) r.addStrength("High volume of problems solved");
        if (r.getContestScore() >= 14) r.addStrength("Strong contest performance");
        if (r.getStreakScore() >= 6) r.addStrength("Excellent consistency / streak");
        if (r.getAcceptanceScore() >= 8) r.addStrength("Strong acceptance rate — clean, well-tested submissions");
        if (r.getDifficultyScore() >= 7) r.addStrength("Good balance of Medium/Hard problems");
        if (r.getBadgeScore() >= 3.5) r.addStrength("Good badge collection");

        // Weaknesses + suggestions
        if (r.getRankScore() < 10) {
            r.addWeakness("Global rank is on the lower side");
            r.addSuggestion("Solve at least 2 problems daily to steadily climb rank.");
        }
        if (r.getSolvedScore() < 10) {
            r.addWeakness("Total problems solved is below a strong placement benchmark");
            r.addSuggestion("Target 500+ solved problems over time.");
        }
        if (r.getDifficultyScore() < 5) {
            r.addWeakness("Problem-solving is skewed toward Easy problems");
            r.addSuggestion("Increase Medium and Hard problem practice.");
        }
        if (r.getContestScore() < 10) {
            r.addWeakness("Limited contest participation or rating");
            r.addSuggestion("Participate in more Weekly/Biweekly contests.");
        }
        if (r.getStreakScore() < 6) {
            r.addWeakness("Consistency could improve");
            r.addSuggestion("Maintain a streak of at least 30 days.");
        }
        if (r.getAcceptanceScore() < 6) {
            r.addWeakness("Acceptance rate is lower than ideal");
            r.addSuggestion("Improve acceptance rate by debugging before submission.");
        }
        if (r.getBadgeScore() < 2) {
            r.addWeakness("Few badges earned");
            r.addSuggestion("Earn more badges by completing study plans.");
        }

        if (r.getStrengths().isEmpty()) {
            r.addStrength("Consistent baseline activity — a good foundation to build on");
        }
    }
}
