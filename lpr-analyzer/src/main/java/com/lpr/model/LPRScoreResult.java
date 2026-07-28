package com.lpr.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the breakdown of a computed LPR score: per-category marks,
 * the final /10 rating, and generated feedback.
 */
public class LPRScoreResult {

    private String username;

    private double rankScore;          // out of 20
    private double solvedScore;        // out of 20
    private double difficultyScore;    // out of 10
    private double contestScore;       // out of 20
    private double streakScore;        // out of 10
    private double acceptanceScore;    // out of 10
    private double badgeScore;         // out of 5
    private double submissionScore;    // out of 5

    private double totalOutOf100;
    private double lprOutOf10;

    private final List<String> strengths = new ArrayList<>();
    private final List<String> weaknesses = new ArrayList<>();
    private final List<String> suggestions = new ArrayList<>();

    public LPRScoreResult(String username) {
        this.username = username;
    }

    public void addStrength(String s) { strengths.add(s); }
    public void addWeakness(String s) { weaknesses.add(s); }
    public void addSuggestion(String s) { suggestions.add(s); }

    // ---- Getters & setters ----

    public String getUsername() { return username; }

    public double getRankScore() { return rankScore; }
    public void setRankScore(double rankScore) { this.rankScore = rankScore; }

    public double getSolvedScore() { return solvedScore; }
    public void setSolvedScore(double solvedScore) { this.solvedScore = solvedScore; }

    public double getDifficultyScore() { return difficultyScore; }
    public void setDifficultyScore(double difficultyScore) { this.difficultyScore = difficultyScore; }

    public double getContestScore() { return contestScore; }
    public void setContestScore(double contestScore) { this.contestScore = contestScore; }

    public double getStreakScore() { return streakScore; }
    public void setStreakScore(double streakScore) { this.streakScore = streakScore; }

    public double getAcceptanceScore() { return acceptanceScore; }
    public void setAcceptanceScore(double acceptanceScore) { this.acceptanceScore = acceptanceScore; }

    public double getBadgeScore() { return badgeScore; }
    public void setBadgeScore(double badgeScore) { this.badgeScore = badgeScore; }

    public double getSubmissionScore() { return submissionScore; }
    public void setSubmissionScore(double submissionScore) { this.submissionScore = submissionScore; }

    public double getTotalOutOf100() { return totalOutOf100; }
    public void setTotalOutOf100(double totalOutOf100) { this.totalOutOf100 = totalOutOf100; }

    public double getLprOutOf10() { return lprOutOf10; }
    public void setLprOutOf10(double lprOutOf10) { this.lprOutOf10 = lprOutOf10; }

    public List<String> getStrengths() { return strengths; }
    public List<String> getWeaknesses() { return weaknesses; }
    public List<String> getSuggestions() { return suggestions; }
}
