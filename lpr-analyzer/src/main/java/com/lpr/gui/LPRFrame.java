package com.lpr.gui;

import com.lpr.fetch.LeetCodeClient;
import com.lpr.fetch.LeetCodeFetchException;
import com.lpr.model.LPRScoreResult;
import com.lpr.model.UserProfile;
import com.lpr.scoring.LPRScoringEngine;
import com.lpr.storage.FileStorageManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main AWT window for the LPR analyzer.
 *
 * Same flow as the console version (Main.java):
 *   username -> fetch (or manual-entry fallback) -> score -> report -> save.
 *
 * The network fetch runs on a background thread so the UI doesn't freeze;
 * results are pushed back onto the AWT event thread via EventQueue.invokeLater.
 */
public class LPRFrame extends Frame {

    private final TextField usernameField = new TextField(20);
    private final Button analyzeButton = new Button("Analyze");
    private final TextArea resultArea = new TextArea(28, 70);
    private final Label statusLabel = new Label("Enter a LeetCode username and click Analyze.");

    private final LeetCodeClient client = new LeetCodeClient();
    private final LPRScoringEngine engine = new LPRScoringEngine();
    private final FileStorageManager storage = new FileStorageManager();

    public LPRFrame() {
        super("CP BenchMark");
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(10, 10));

        Panel topPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new Label("LeetCode username:"));
        topPanel.add(usernameField);
        topPanel.add(analyzeButton);

        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        Panel bottomPanel = new Panel(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(resultArea, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        analyzeButton.addActionListener(this::onAnalyze);
        usernameField.addActionListener(this::onAnalyze); // pressing Enter also triggers analysis

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        setSize(760, 640);
        setLocationRelativeTo(null); // center on screen
    }

    private void onAnalyze(ActionEvent e) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("Please enter a username first.");
            return;
        }

        analyzeButton.setEnabled(false);
        usernameField.setEnabled(false);
        resultArea.setText("");
        statusLabel.setText("Fetching public profile data for '" + username + "'...");

        // Run network + scoring off the AWT event thread so the window stays responsive.
        Thread worker = new Thread(() -> runAnalysis(username));
        worker.setDaemon(true);
        worker.start();
    }

    private void runAnalysis(String username) {
        UserProfile profile;
        boolean usedFallback = false;

        try {
            profile = client.fetchProfile(username);
        } catch (LeetCodeFetchException ex) {
            usedFallback = true;
            EventQueue.invokeLater(() ->
                    statusLabel.setText("Auto-fetch failed (" + ex.getMessage() + "). Opening manual entry..."));

            // ManualEntryDialog is modal, so this call blocks this background
            // thread until the user submits or cancels it — which is fine,
            // since it keeps the AWT event thread free to repaint the dialog.
            ManualEntryDialog dialog = new ManualEntryDialog(this, username);
            profile = dialog.showAndGet();

            if (profile == null) {
                EventQueue.invokeLater(() -> {
                    statusLabel.setText("Cancelled. Enter a username and click Analyze to try again.");
                    analyzeButton.setEnabled(true);
                    usernameField.setEnabled(true);
                });
                return;
            }
        }

        LPRScoreResult result = engine.score(profile);
        double previousScore = storage.getPreviousScore(username);
        storage.save(profile, result);

        final UserProfile finalProfile = profile;
        final boolean finalUsedFallback = usedFallback;
        EventQueue.invokeLater(() -> {
            resultArea.setText(buildReportText(finalProfile, result, previousScore, finalUsedFallback));
            statusLabel.setText("Done. Snapshot saved to ./data/" + username.replaceAll("[^a-zA-Z0-9_-]", "_") + ".json");
            analyzeButton.setEnabled(true);
            usernameField.setEnabled(true);
        });
    }

    private String buildReportText(UserProfile p, LPRScoreResult r, double previousScore, boolean usedFallback) {
        StringBuilder sb = new StringBuilder();

        if (usedFallback) {
            sb.append("(Data entered manually - auto-fetch was unavailable)\n\n");
        }

        sb.append(p).append("\n\n");
        sb.append("----------- LPR SCORE REPORT -----------\n");
        sb.append(String.format("Global Rank score       : %.1f / 20%n", r.getRankScore()));
        sb.append(String.format("Problems Solved score   : %.1f / 20%n", r.getSolvedScore()));
        sb.append(String.format("Difficulty Mix score    : %.1f / 10%n", r.getDifficultyScore()));
        sb.append(String.format("Contest score           : %.1f / 20%n", r.getContestScore()));
        sb.append(String.format("Streak/Consistency score: %.1f / 10%n", r.getStreakScore()));
        sb.append(String.format("Acceptance Rate score   : %.1f / 10%n", r.getAcceptanceScore()));
        sb.append(String.format("Badges score            : %.1f / 5%n", r.getBadgeScore()));
        sb.append(String.format("Submissions score       : %.1f / 5%n", r.getSubmissionScore()));
        sb.append("-----------------------------------------\n");
        sb.append(String.format("TOTAL                   : %.1f / 100%n", r.getTotalOutOf100()));
        sb.append(String.format("LPR SCORE               : %.2f / 10%n", r.getLprOutOf10()));
        sb.append("-----------------------------------------\n\n");

        if (previousScore >= 0) {
            double delta = Math.round((r.getLprOutOf10() - previousScore) * 100.0) / 100.0;
            String trend = delta > 0 ? "improved by" : (delta < 0 ? "dropped by" : "unchanged since");
            sb.append(String.format("Compared to last check: %s %.2f (previous: %.2f/10)%n%n",
                    trend, Math.abs(delta), previousScore));
        }

        sb.append("Strengths:\n");
        r.getStrengths().forEach(s -> sb.append("  + ").append(s).append("\n"));

        if (!r.getWeaknesses().isEmpty()) {
            sb.append("\nWeaknesses:\n");
            r.getWeaknesses().forEach(w -> sb.append("  - ").append(w).append("\n"));
        }

        if (!r.getSuggestions().isEmpty()) {
            sb.append("\nSuggestions:\n");
            r.getSuggestions().forEach(s -> sb.append("  * ").append(s).append("\n"));
        }

        return sb.toString();
    }
}
