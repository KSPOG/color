package com.example.colorbot;


import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private final JList<String> savedScriptsList = new JList<>();
    private final Map<String, String> savedScripts = new LinkedHashMap<>();
    private final JButton captureButton = new JButton();
    private final ColorLibrary library = new ColorLibrary();
    private final ColorMonitor monitor = new ColorMonitor(library);
    private final ColorScriptEngine scriptEngine = new ColorScriptEngine(library);
    private final ExecutorService scriptExecutor = Executors.newSingleThreadExecutor();

    private KeyStroke captureKeyStroke;

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

        savedScriptsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        savedScriptsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String name = savedScriptsList.getSelectedValue();
                if (name != null) {
                    appendLog("Selected script: " + name);
                }
            }
        });
        seedSavedScripts();

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


        captureButton.setText("Capture coords & color (" + captureHotkeyField.getText() + ")");
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

        panel.setBorder(BorderFactory.createTitledBorder("Scripts"));

        JPanel savedPanel = new JPanel(new BorderLayout(4, 4));
        savedPanel.setBorder(BorderFactory.createTitledBorder("Saved scripts"));
        savedPanel.add(new JScrollPane(savedScriptsList), BorderLayout.CENTER);

        JPanel savedButtons = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JButton loadButton = new JButton("Load selected");
        loadButton.addActionListener(e -> loadSelectedScript());
        gbc.gridy = 0;
        savedButtons.add(loadButton, gbc);

        JButton saveButton = new JButton("Save current...");
        saveButton.addActionListener(e -> saveCurrentScript());
        gbc.gridy = 1;
        savedButtons.add(saveButton, gbc);

        JButton deleteButton = new JButton("Delete selected");
        deleteButton.addActionListener(e -> deleteSelectedScript());
        gbc.gridy = 2;
        savedButtons.add(deleteButton, gbc);

        savedPanel.add(savedButtons, BorderLayout.SOUTH);

        JPanel editorPanel = new JPanel(new BorderLayout(4, 4));
        editorPanel.setBorder(BorderFactory.createTitledBorder("Editor"));
        editorPanel.add(new JLabel("Supported: WAIT, PRESS, HOLD, RELEASE, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_COLOR, LOOPS"), BorderLayout.NORTH);
        editorPanel.add(new JScrollPane(scriptArea), BorderLayout.CENTER);
        JButton runButton = new JButton("Run script");
        runButton.addActionListener(e -> runScript());
        editorPanel.add(runButton, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, savedPanel, editorPanel);
        splitPane.setResizeWeight(0.3);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void seedSavedScripts() {
        savedScripts.clear();
        savedScripts.put("Blue Eye example", defaultScript());
        savedScripts.put("Looped press", "# Looping example\nLOOP 3\n  PRESS " + visibleKeyField.getText() + "\n  WAIT 250\nEND_LOOP\nLOG Loop finished");
        refreshSavedScriptsList();
        savedScriptsList.setSelectedIndex(0);
    }

    private void refreshSavedScriptsList() {
        javax.swing.DefaultListModel<String> model = new javax.swing.DefaultListModel<>();
        savedScripts.keySet().forEach(model::addElement);
        savedScriptsList.setModel(model);
    }

    private void loadSelectedScript() {
        String name = savedScriptsList.getSelectedValue();
        if (name == null) {
            JOptionPane.showMessageDialog(this, "Select a script to load.");
            return;
        }
        scriptArea.setText(savedScripts.get(name));
        appendLog("Loaded script: " + name);
    }

    private void saveCurrentScript() {
        String name = JOptionPane.showInputDialog(this, "Save script as:", "My Script");
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        savedScripts.put(name.trim(), scriptArea.getText());
        refreshSavedScriptsList();
        savedScriptsList.setSelectedValue(name.trim(), true);
        appendLog("Saved script: " + name.trim());
    }

    private void deleteSelectedScript() {
        String name = savedScriptsList.getSelectedValue();
        if (name == null) {
            JOptionPane.showMessageDialog(this, "Select a script to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete script '" + name + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            savedScripts.remove(name);
            refreshSavedScriptsList();
            appendLog("Deleted script: " + name);
        }
    }

    private String defaultScript() {
        return "# Blue Eye Macro style example\n" +
                "If Color.At coordinate is not (RGB '255', '0', '0', '" + (MouseInfo.getPointerInfo().getLocation().x) + "', '" + (MouseInfo.getPointerInfo().getLocation().y) + "') begin\n" +
                "  Macro.Pause('250')\n" +
                "  Keyboard.Hold keys('{" + visibleKeyField.getText() + "}')\n" +
                "  Macro.Pause('50')\n" +
                "  Keyboard.Release keys('{" + visibleKeyField.getText() + "}')\n" +
                "end\n" +
                "Macro.Loop('3') begin\n" +
                "  WAIT 100\n" +
                "  PRESS " + visibleKeyField.getText() + "\n" +
                "end";
    }

    private void registerCaptureHotkey() {
        captureHotkeyField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCaptureHotkeyBinding();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCaptureHotkeyBinding();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCaptureHotkeyBinding();
            }
        });
        updateCaptureHotkeyBinding();
    }

    private void updateCaptureHotkeyBinding() {
        ActionMap actionMap = getRootPane().getActionMap();
        javax.swing.InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (captureKeyStroke != null) {
            inputMap.remove(captureKeyStroke);
        }
        try {
            int keyCode = KeyName.toKeyCode(captureHotkeyField.getText());
            captureKeyStroke = KeyStroke.getKeyStroke(keyCode, 0, false);
            inputMap.put(captureKeyStroke, "capture-target");
            actionMap.put("capture-target", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    captureTarget();
                }
            });
            captureButton.setText("Capture coords & color (" + captureHotkeyField.getText().trim() + ")");
        } catch (IllegalArgumentException ex) {
            captureButton.setText("Capture coords & color");
            captureKeyStroke = null;
        }
      
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
