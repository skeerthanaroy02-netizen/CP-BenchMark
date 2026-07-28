package com.lpr.gui;

import com.lpr.model.UserProfile;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Modal AWT dialog used as the manual-entry fallback when LeetCodeClient
 * can't fetch a profile automatically (private profile, network issue,
 * or the unofficial API has changed shape).
 *
 * Usage: new ManualEntryDialog(parentFrame, username).showAndGet()
 * returns the filled UserProfile, or null if the user cancelled.
 */
public class ManualEntryDialog extends Dialog {

    private final TextField rankField = new TextField("0");
    private final TextField easyField = new TextField("0");
    private final TextField mediumField = new TextField("0");
    private final TextField hardField = new TextField("0");
    private final TextField submissionsField = new TextField("0");
    private final TextField acceptedField = new TextField("0");
    private final TextField contestRatingField = new TextField("0");
    private final TextField contestsAttendedField = new TextField("0");
    private final TextField streakField = new TextField("0");
    private final TextField activeDaysField = new TextField("0");
    private final TextField badgesField = new TextField("0");

    private final String username;
    private UserProfile result; // null unless user clicks OK

    public ManualEntryDialog(Frame parent, String username) {
        super(parent, "Manual Entry - " + username, true); // modal
        this.username = username;
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(10, 10));

        Panel form = new Panel(new GridLayout(0, 2, 8, 6));
        form.add(new Label("Global rank:"));
        form.add(rankField);
        form.add(new Label("Easy problems solved:"));
        form.add(easyField);
        form.add(new Label("Medium problems solved:"));
        form.add(mediumField);
        form.add(new Label("Hard problems solved:"));
        form.add(hardField);
        form.add(new Label("Total submissions:"));
        form.add(submissionsField);
        form.add(new Label("Accepted submissions (or 0 to estimate):"));
        form.add(acceptedField);
        form.add(new Label("Contest rating (0 if none):"));
        form.add(contestRatingField);
        form.add(new Label("Contests attended:"));
        form.add(contestsAttendedField);
        form.add(new Label("Current streak (days):"));
        form.add(streakField);
        form.add(new Label("Total active days:"));
        form.add(activeDaysField);
        form.add(new Label("Badges earned:"));
        form.add(badgesField);

        Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT));
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        buttons.add(cancelButton);
        buttons.add(okButton);

        Label heading = new Label("Could not auto-fetch '" + username + "'. Enter stats manually:");
        add(heading, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        okButton.addActionListener(this::onOk);
        cancelButton.addActionListener((ActionEvent e) -> {
            result = null;
            setVisible(false);
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                result = null;
                setVisible(false);
            }
        });

        setSize(460, 420);
        setResizable(false);
        // Center on parent
        Window parent = getOwner();
        if (parent != null) {
            setLocationRelativeTo(parent);
        }
    }

    private void onOk(ActionEvent e) {
        UserProfile p = new UserProfile(username);
        p.setGlobalRanking(parseIntSafe(rankField.getText()));

        int easy = parseIntSafe(easyField.getText());
        int medium = parseIntSafe(mediumField.getText());
        int hard = parseIntSafe(hardField.getText());
        p.setProblemsEasy(easy);
        p.setProblemsMedium(medium);
        p.setProblemsHard(hard);
        p.setProblemsSolvedTotal(easy + medium + hard);

        int submissions = parseIntSafe(submissionsField.getText());
        int accepted = parseIntSafe(acceptedField.getText());
        p.setTotalSubmissions(submissions);
        p.setAcceptedSubmissions(accepted > 0 ? accepted : submissions);

        p.setContestRating(parseIntSafe(contestRatingField.getText()));
        p.setContestsAttended(parseIntSafe(contestsAttendedField.getText()));
        p.setCurrentStreak(parseIntSafe(streakField.getText()));
        p.setTotalActiveDays(parseIntSafe(activeDaysField.getText()));
        p.setBadgeCount(parseIntSafe(badgesField.getText()));
        p.setBadgeNames(List.of());

        this.result = p;
        setVisible(false);
    }

    private int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Shows the dialog (blocking, since it's modal) and returns the filled
     * profile, or null if the user cancelled/closed the window.
     */
    public UserProfile showAndGet() {
        setVisible(true); // blocks here until OK/Cancel/close
        return result;
    }
}
