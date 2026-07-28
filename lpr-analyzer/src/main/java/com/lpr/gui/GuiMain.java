package com.lpr.gui;

import java.awt.EventQueue;
import java.util.Locale;

/**
 * Entry point for the AWT GUI version of the LPR analyzer.
 * The console version (com.lpr.Main) still exists separately —
 * run whichever one you need.
 */
public class GuiMain {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        EventQueue.invokeLater(() -> {
            LPRFrame frame = new LPRFrame();
            frame.setVisible(true);
        });
    }
}
