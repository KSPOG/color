package com.example.colorbot;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ColorBotApp extends JFrame {
    private final JTextField coordinateField = new JTextField();
    private final JTextField colorField = new JTextField();
    private final JTextField captureHotkeyField = new JTextField("F8");
    private final JTextField visibleKeyField = new JTextField("F9");
    private final JTextField missingKeyField = new JTextField("F10");
    private final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(250, 50, 5_000, 50));
    private final JCheckBox failSafeCheckbox = new JCheckBox("Fail-safe: stop when missing", true);
    private final JTextArea logArea = new JTextArea();
    private final JTextArea scriptArea = new JTextArea();
    private final ColorLibrary library = new ColorLibrary();
    private final ColorMonitor monitor = new ColorMonitor(library);
    private final ColorScriptEngine scriptEngine = new ColorScriptEngine(library);
    private final ExecutorService scriptExecutor = Executors.newSingleThreadExecutor();

    public ColorBotApp() {
        super("Color Bot");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildUi();
        pack();
        setLocationRelativeTo(null);
        registerCaptureHotkey();
    }

    private void buildUi() {
        coordinateField.setEditable(false);
        colorField.setEditable(false);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        scriptArea.setText(defaultScript());
        scriptArea.setLineWrap(true);
        scriptArea.setPreferredSize(new Dimension(480, 200));

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        main.add(buildConfigPanel(), BorderLayout.NORTH);
        main.add(buildScriptPanel(), BorderLayout.CENTER);
        main.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        add(main);
    }

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("Coordinates"), gbc);
        gbc.gridx = 1;
        panel.add(coordinateField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("Color"), gbc);
        gbc.gridx = 3;
        panel.add(colorField, gbc);

        JButton captureButton = new JButton("Capture coords & color (" + captureHotkeyField.getText() + ")");
        captureButton.addActionListener(e -> captureTarget());
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        panel.add(captureButton, gbc);
        gbc.gridwidth = 1;

        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("Capture hotkey"), gbc);
        gbc.gridx = 1;
        panel.add(captureHotkeyField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("Visible key"), gbc);
        gbc.gridx = 3;
        panel.add(visibleKeyField, gbc);

        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("Missing key"), gbc);
        gbc.gridx = 1;
        panel.add(missingKeyField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("Interval (ms)"), gbc);
        gbc.gridx = 3;
        panel.add(intervalSpinner, gbc);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        panel.add(failSafeCheckbox, gbc);
        gbc.gridwidth = 1;

        JButton startButton = new JButton("Start monitoring");
        JButton verifyButton = new JButton("Verify color now");
        startButton.addActionListener(e -> toggleMonitor(startButton));
        verifyButton.addActionListener(e -> verifyColor());

        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(startButton, gbc);
        gbc.gridx = 1;
        panel.add(verifyButton, gbc);

        return panel;
    }

    private JPanel buildScriptPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Script playground"));
        panel.add(new JScrollPane(scriptArea), BorderLayout.CENTER);
        panel.add(new JLabel("Supported: WAIT, PRESS, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_TARGET_VISIBLE, IF_COLOR, LOG"), BorderLayout.NORTH);
        JButton runButton = new JButton("Run script");
        runButton.addActionListener(e -> runScript());
        panel.add(runButton, BorderLayout.SOUTH);
        return panel;
    }

    private String defaultScript() {
        return "# Example macro inspired by Blue Eye Macro\n" +
                "CAPTURE_TARGET\n" +
                "WAIT 500\n" +
                "IF_TARGET_VISIBLE THEN PRESS " + visibleKeyField.getText() + " ELSE PRESS " + missingKeyField.getText() + "\n" +
                "LOG Done";
    }

    private void registerCaptureHotkey() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (event.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            try {
                int expectedCode = KeyName.toKeyCode(captureHotkeyField.getText());
                if (event.getKeyCode() == expectedCode) {
                    captureTarget();
                    return true;
                }
            } catch (Exception ignored) {
                // ignore invalid keys
            }
            return false;
        });
    }

    private void captureTarget() {
        ColorSample sample = library.captureCurrentPixel();
        library.setTargetSample(sample);
        coordinateField.setText(sample.location().x + ", " + sample.location().y);
        colorField.setText(sample.toHex());
        appendLog("Captured target at " + sample.location() + " = " + sample.toHex());
    }

    private void toggleMonitor(JButton startButton) {
        if (startButton.getText().startsWith("Stop")) {
            monitor.stop();
            startButton.setText("Start monitoring");
            appendLog("Monitoring stopped");
            return;
        }
        ColorSample sample = library.getTargetSample()
                .orElseGet(() -> {
                    ColorSample captured = library.captureCurrentPixel();
                    library.setTargetSample(captured);
                    return captured;
                });
        int interval = (Integer) intervalSpinner.getValue();
        boolean failSafe = failSafeCheckbox.isSelected();
        monitor.start(sample, visibleKeyField.getText(), missingKeyField.getText(), failSafe, interval, this::appendLog);
        startButton.setText("Stop monitoring");
        appendLog("Monitoring started at " + sample.location() + " for color " + sample.toHex());
    }

    private void verifyColor() {
        boolean visible = library.isTargetVisible();
        String message = visible ? "Color is visible" : "Color is missing";
        appendLog(message);
        if (!visible && failSafeCheckbox.isSelected()) {
            JOptionPane.showMessageDialog(this, "Configured color is missing!", "Fail-safe", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runScript() {
        String scriptText = scriptArea.getText();
        scriptExecutor.submit(() -> scriptEngine.run(scriptText, this::appendLog));
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ColorBotApp app = new ColorBotApp();
            app.setVisible(true);
        });
    }
}
