package com.example.colorbot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

    public ColorBotApp() {
        ColorBotConfig defaultConfig = new ColorBotConfig();
        bot = new ColorBot(defaultConfig);
        frame = new JFrame("Color Bot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(12, 12));
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        frame.setMinimumSize(new Dimension(520, 320));

        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        infoPanel.add(new JLabel("Target coordinates:"));
        coordsField.setEditable(false);
        infoPanel.add(coordsField);
        infoPanel.add(new JLabel("Target color (hex):"));
        colorField.setEditable(false);
        infoPanel.add(colorField);

        JPanel configPanel = new JPanel(new GridLayout(2, 3, 8, 8));
        captureKeyField.setText(defaultConfig.describeKey(defaultConfig.getCaptureKeyCode()));
        visibleKeyField.setText(defaultConfig.describeKey(defaultConfig.getVisibleKeyCode()));
        missingKeyField.setText(defaultConfig.describeKey(defaultConfig.getNotVisibleKeyCode()));
        configPanel.add(labeledField("Capture hotkey", captureKeyField));
        configPanel.add(labeledField("When visible key", visibleKeyField));
        configPanel.add(labeledField("When missing key", missingKeyField));
        configPanel.add(new JPanel());
        configPanel.add(failCheckbox);
        configPanel.add(new JPanel());

        JButton captureButton = new JButton("Capture coords & color");
        captureButton.addActionListener(this::captureUnderCursor);
        JButton monitorButton = new JButton("Start monitoring");
        monitorButton.addActionListener(evt -> toggleMonitoring(monitorButton));
        JButton verifyButton = new JButton("Verify color now");
        verifyButton.addActionListener(evt -> verifyColor());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 8));
        controls.add(captureButton);
        controls.add(monitorButton);
        controls.add(verifyButton);

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        statusPanel.add(statusLabel);

        frame.add(infoPanel, BorderLayout.NORTH);
        frame.add(configPanel, BorderLayout.CENTER);
        frame.add(controls, BorderLayout.SOUTH);
        frame.add(statusPanel, BorderLayout.PAGE_END);
        frame.pack();

        monitorTimer = new Timer(250, evt -> performMonitorTick());
        monitorTimer.setRepeats(true);

        bindCaptureHotkey(defaultConfig.getCaptureKeyCode());
        frame.setVisible(true);
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void captureUnderCursor(ActionEvent event) {
        updateConfigFromUi();
        ColorBot.CaptureResult result = bot.captureUnderCursor();
        coordsField.setText(result.point().x + ", " + result.point().y);
        colorField.setText(result.hex());
        statusLabel.setText("Captured coordinates and color code.");
    }

    private void toggleMonitoring(JButton monitorButton) {
        updateConfigFromUi();
        if (monitorTimer.isRunning()) {
            monitorTimer.stop();
            monitorButton.setText("Start monitoring");
            statusLabel.setText("Monitoring stopped");
        } else {
            monitorTimer.start();
            monitorButton.setText("Stop monitoring");
            statusLabel.setText("Monitoring color visibility...");
        }
    }

    private void performMonitorTick() {
        ColorBotConfig config = bot.getConfig();
        if (config.getTargetPoint() == null || config.getTargetColor() == null) {
            statusLabel.setText("Capture a coordinate and color before monitoring.");
            return;
        }
        try {
            ColorBot.VisibilityResult result = bot.performVisibilityActions();
            String state = result.visible() ? "visible" : "not visible";
            statusLabel.setText("Color is " + state + "; pressed key: "
                    + (result.visible() ? visibleKeyField.getText() : missingKeyField.getText()));
        } catch (ColorNotVisibleException ex) {
            statusLabel.setText(ex.getMessage());
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Color not visible", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verifyColor() {
        updateConfigFromUi();
        try {
            bot.ensureColorVisible();
            statusLabel.setText("Color is visible at the captured coordinates.");
        } catch (ColorNotVisibleException ex) {
            statusLabel.setText(ex.getMessage());
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Color not visible", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateConfigFromUi() {
        ColorBotConfig config = bot.getConfig();
        config.setCaptureKeyCode(parseKeyCode(captureKeyField.getText(), config.getCaptureKeyCode()));
        config.setVisibleKeyCode(parseKeyCode(visibleKeyField.getText(), config.getVisibleKeyCode()));
        config.setNotVisibleKeyCode(parseKeyCode(missingKeyField.getText(), config.getNotVisibleKeyCode()));
        config.setFailWhenMissing(failCheckbox.isSelected());
        bot.updateConfig(config);
        bindCaptureHotkey(config.getCaptureKeyCode());
    }

    private void bindCaptureHotkey(int keyCode) {
        KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, 0);
        String name = "capture";
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
        frame.getRootPane().getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                captureUnderCursor(e);
            }
        });
    }

    private int parseKeyCode(String text, int fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        KeyStroke stroke = KeyStroke.getKeyStroke(text.trim().toUpperCase());
        if (stroke != null) {
            return stroke.getKeyCode();
        }
        return fallback;
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new ColorBotApp());
    }
}
