package com.lpr.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Plain data holder for everything the LPR engine needs about one user.
 * Populated either by LeetCodeClient (GraphQL fetch) or by manual console entry.
 */
public class UserProfile {

    private String username;

    private int globalRanking;          // profile.ranking
    private int problemsSolvedTotal;
    private int problemsEasy;
    private int problemsMedium;
    private int problemsHard;

    private int totalSubmissions;
    private int acceptedSubmissions;    // used to derive acceptance rate

    private int contestRating;          // 0 if user has never entered a contest
    private int contestsAttended;

    private int currentStreak;          // consecutive active days, from calendar
    private int totalActiveDays;

    private int badgeCount;
    private List<String> badgeNames;

    public UserProfile() {}

    public UserProfile(String username) {
        this.username = username;
    }

    // ---- Getters & setters ----

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getGlobalRanking() { return globalRanking; }
    public void setGlobalRanking(int globalRanking) { this.globalRanking = globalRanking; }

    public int getProblemsSolvedTotal() { return problemsSolvedTotal; }
    public void setProblemsSolvedTotal(int problemsSolvedTotal) { this.problemsSolvedTotal = problemsSolvedTotal; }

    public int getProblemsEasy() { return problemsEasy; }
    public void setProblemsEasy(int problemsEasy) { this.problemsEasy = problemsEasy; }

    public int getProblemsMedium() { return problemsMedium; }
    public void setProblemsMedium(int problemsMedium) { this.problemsMedium = problemsMedium; }

    public int getProblemsHard() { return problemsHard; }
    public void setProblemsHard(int problemsHard) { this.problemsHard = problemsHard; }

    public int getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(int totalSubmissions) { this.totalSubmissions = totalSubmissions; }

    public int getAcceptedSubmissions() { return acceptedSubmissions; }
    public void setAcceptedSubmissions(int acceptedSubmissions) { this.acceptedSubmissions = acceptedSubmissions; }

    public int getContestRating() { return contestRating; }
    public void setContestRating(int contestRating) { this.contestRating = contestRating; }

    public int getContestsAttended() { return contestsAttended; }
    public void setContestsAttended(int contestsAttended) { this.contestsAttended = contestsAttended; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getTotalActiveDays() { return totalActiveDays; }
    public void setTotalActiveDays(int totalActiveDays) { this.totalActiveDays = totalActiveDays; }

    public int getBadgeCount() { return badgeCount; }
    public void setBadgeCount(int badgeCount) { this.badgeCount = badgeCount; }

    public List<String> getBadgeNames() { return badgeNames; }
    public void setBadgeNames(List<String> badgeNames) { this.badgeNames = badgeNames; }

    public double getAcceptanceRate() {
        if (totalSubmissions == 0) return 0.0;
        return (acceptedSubmissions * 100.0) / totalSubmissions;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "username='" + username + '\'' +
                ", globalRanking=" + globalRanking +
                ", problemsSolvedTotal=" + problemsSolvedTotal +
                ", easy/med/hard=" + problemsEasy + "/" + problemsMedium + "/" + problemsHard +
                ", acceptanceRate=" + String.format("%.1f", getAcceptanceRate()) + "%" +
                ", contestRating=" + contestRating +
                ", contestsAttended=" + contestsAttended +
                ", currentStreak=" + currentStreak +
                ", badgeCount=" + badgeCount +
                '}';
    }
}
