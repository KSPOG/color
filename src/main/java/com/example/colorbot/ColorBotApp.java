package com.example.colorbot;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class ColorBotApp extends JFrame {
    private static final String DEFAULT_CAPTURE_HOTKEY = "F8";
    private static final String DEFAULT_VISIBLE_KEY = "F9";
    private static final String DEFAULT_MISSING_KEY = "F10";
    private static final int DEFAULT_INTERVAL_MS = 250;
    private static final String VERSION_RESOURCE = "/version.txt";
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String CURRENT_VERSION = loadCurrentVersion();
    private static final String DEFAULT_UPDATE_URL = "https://example.com/colorbot/latest-version.txt";
    private static final String DEFAULT_DOWNLOAD_URL = "https://github.com/KSPOG/color/raw/refs/heads/main/live%20build/Prime%20Bot.jar";
    private final JCheckBox failSafeCheckbox = new JCheckBox("Fail-safe: stop when missing", true);
    private final JTextArea logArea = new JTextArea();
    private final JTextArea scriptArea = new JTextArea();
    private final JList<String> savedScriptsList = new JList<>();
    private final Map<String, String> savedScripts = new LinkedHashMap<>();
    private final JButton runButton = new JButton("Run script");
    private final ColorLibrary library = new ColorLibrary();
    private final ExternalCooldownController externalCooldowns = new ExternalCooldownController();
    private final ColorMonitor monitor = new ColorMonitor(library);
    private final ColorScriptEngine scriptEngine = new ColorScriptEngine(library, externalCooldowns);
    private final ExecutorService scriptExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private KeyStroke captureKeyStroke;
    private final KeyStroke screenshotKeyStroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0, false);
    private boolean updatingScriptAutoFill = false;
    private static final int MAX_LOG_LINES = 500;
    private final Path scriptsDirectory = Paths.get("scripts");
    private final Path legacySavedScriptsFile = Paths.get(System.getProperty("user.home"), ".colorbot-scripts.properties");
    private java.util.concurrent.Future<?> runningScriptFuture;

    private static String loadCurrentVersion() {
        try (InputStream in = ColorBotApp.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (in != null) {
                String value = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to read version file: " + ex.getMessage());
        }
        return DEFAULT_VERSION;
    }

    public ColorBotApp() {
        super("Color Bot");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setWindowIcon();
        buildUi();
        pack();
        setLocationRelativeTo(null);
        registerCaptureHotkey();
        registerScreenshotHotkey();
        checkForUpdatesAsync();
    }

    private void setWindowIcon() {
        String[] candidates = new String[] {
            "/icon.png", // standard maven resources path
            "/resources/icon.png", // nested resources directory
            "icon.png", // file in working directory
            "resources/icon.png" // file in working-directory resources folder
        };

        for (String candidate : candidates) {
            Image icon = loadIconImage(candidate);
            if (icon != null) {
                setIconImage(icon);
                return;
            }
        }

        System.err.println("Window icon not found in classpath or ./resources");
    }

    private Image loadIconImage(String resourcePath) {
        java.net.URL iconUrl = getClass().getResource(resourcePath);
        if (iconUrl == null) {
            String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            iconUrl = getClass().getClassLoader().getResource(normalized);
        }
        if (iconUrl != null) {
            return new ImageIcon(iconUrl).getImage();
        }

        Path filePath = Paths.get(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
        if (Files.exists(filePath)) {
            return new ImageIcon(filePath.toString()).getImage();
        }

        return null;
    }

    private void buildUi() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        scriptArea.setText(defaultScript());
        scriptArea.setLineWrap(false);
        scriptArea.setRows(24);
        scriptArea.setColumns(100);
        scriptArea.setAutoscrolls(true);
        ((DefaultCaret) scriptArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        attachScriptAutofill();
        savedScriptsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        savedScriptsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String name = savedScriptsList.getSelectedValue();
                if (name != null) {
                    appendLog("Selected script: " + name);
                }
            }
        });
        loadSavedScripts();

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        main.add(buildConfigPanel(), BorderLayout.NORTH);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildScriptPanel(), buildLogPanel());
        centerSplit.setResizeWeight(0.7);
        centerSplit.setOneTouchExpandable(true);
        main.add(centerSplit, BorderLayout.CENTER);

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
        gbc.gridwidth = 4;
        panel.add(failSafeCheckbox, gbc);
        gbc.gridwidth = 1;

        JButton startButton = new JButton("Start monitoring");
        JButton verifyButton = new JButton("Verify color now");
        JButton updateButton = new JButton("Check for updates");
        startButton.addActionListener(e -> toggleMonitor(startButton));
        verifyButton.addActionListener(e -> verifyColor());
        updateButton.addActionListener(e -> checkForUpdatesAsync());

        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(startButton, gbc);
        gbc.gridx = 1;
        panel.add(verifyButton, gbc);
        gbc.gridx = 2;
        panel.add(updateButton, gbc);

        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Log"));
        logArea.setRows(8);

        JButton clearButton = new JButton("Clear log");
        clearButton.addActionListener(e -> clearLog());

        JPanel controls = new JPanel(new BorderLayout());
        controls.add(clearButton, BorderLayout.EAST);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
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

        JPanel editorHeader = new JPanel(new BorderLayout());
        editorHeader.add(new JLabel("Supported: WAIT, PRESS, HOLD, RELEASE, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_COLOR, LOOPS"), BorderLayout.CENTER);
        JButton guideButton = new JButton("Script guide");
        guideButton.addActionListener(e -> showScriptGuide());
        editorHeader.add(guideButton, BorderLayout.EAST);
        editorPanel.add(editorHeader, BorderLayout.NORTH);

        JScrollPane scriptScroll = new JScrollPane(scriptArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        editorPanel.add(scriptScroll, BorderLayout.CENTER);
        runButton.addActionListener(e -> toggleScriptRun());
        editorPanel.add(runButton, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, savedPanel, editorPanel);
        splitPane.setResizeWeight(0.3);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void showScriptGuide() {
        String message = String.join("\n",
                "Color Bot scripting quick guide:",
                "",
                "Basics:",
                "  WAIT <ms>                   - Pause for milliseconds (same as Macro.Pause('<ms>'))",
                "  PRESS <KEY>                 - Tap a key (e.g., PRESS F9)",
                "  HOLD <KEY> / RELEASE <KEY>  - Hold or release a key (Keyboard.Hold/Release keys('{X}'))",
                "  TYPE <text>                 - Type text literally",
                "  MOVE x y / CLICK            - Move mouse to x y, CLICK uses current position",
                "  CAPTURE_TARGET              - Grab current mouse pixel for coords & color",
                "",
                "Color checks:",
                "  IF_COLOR r g b x y          - Run next lines only when pixel matches RGB",
                "  If Color.At coordinate is not (RGB 'r', 'g', 'b', 'x', 'y') begin ... end",
                "  If Color.At ... && If Color.At ... begin ... end (AND multiple color checks)",
                "  • Missing colors log a message and wait 1000 ms before retrying",
                "",
                "Loops:",
                "  LOOP <count|FOREVER> ... END_LOOP",
                "  Macro.Loop('<count|FOREVER>') begin ... end",
                "  • FOREVER repeats endlessly until you press Stop script",
                "",
                "Cooldowns:",
                "  SET <name> = <ms|TIMER>     - Store a cooldown or timestamp (Timer = now)",
                "  • Values sync to 'cooldowns/cooldowns.properties' so external tools can share state",
                "  IF_COOLDOWN <last> <cd> THEN <action> [ELSE <action>]",
                "  Example: SET FireBoltCD = 5000 / SET LastFireBolt = TIMER",
                "",
                "Hotkeys:",
                "  F8 captures live pixel; F12 opens the screenshot picker (Spy glass)",
                "  • Hold CTRL + scroll to zoom the screenshot picker",
                "",
                "Tips:",
                "  • Saved scripts live in the 'scripts' folder as .ini files",
                "  • Caret inside the (RGB ...) line autofills with the last captured pixel");

        JTextArea guideArea = new JTextArea(message);
        guideArea.setEditable(false);
        guideArea.setLineWrap(true);
        guideArea.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(guideArea), "Script guide", JOptionPane.INFORMATION_MESSAGE);
    }

    private void checkForUpdatesAsync() {
        appendLog("Checking for updates...");
        CompletableFuture.runAsync(() -> {
            String url = resolveUpdateUrl();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
                String latest = reader.readLine();
                if (latest == null || latest.trim().isEmpty()) {
                    appendLog("Update check failed: empty response from " + url);
                    return;
                }

                final String latestVersion = latest.trim();
                int comparison = compareVersions(latestVersion, CURRENT_VERSION);
                if (comparison <= 0) {
                    appendLog("You are up to date (" + CURRENT_VERSION + ")");
                } else {
                    appendLog("Update available: " + latestVersion + " (current " + CURRENT_VERSION + ")");
                    final String downloadUrl = resolveDownloadUrl();
                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showConfirmDialog(this,
                                "New version available: " + latestVersion + "\nCurrent version: " + CURRENT_VERSION +
                                        "\nDownload now?\nSource: " + downloadUrl,
                                "Update available", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            downloadUpdateAsync(latestVersion, downloadUrl);
                        }
                    });
                }
            } catch (Exception ex) {
                appendLog("Update check failed: " + ex.getMessage());
            }
        }, backgroundExecutor);
    }

    private String resolveUpdateUrl() {
        String propertyUrl = System.getProperty("colorbot.updateUrl");
        if (propertyUrl != null && !propertyUrl.isBlank()) {
            return propertyUrl;
        }
        String envUrl = System.getenv("COLORBOT_UPDATE_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl;
        }
        return DEFAULT_UPDATE_URL;
    }

    private String resolveDownloadUrl() {
        String propertyUrl = System.getProperty("colorbot.downloadUrl");
        if (propertyUrl != null && !propertyUrl.isBlank()) {
            return propertyUrl;
        }
        String envUrl = System.getenv("COLORBOT_DOWNLOAD_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl;
        }
        return DEFAULT_DOWNLOAD_URL;
    }

    private void downloadUpdateAsync(String version, String downloadUrl) {
        appendLog("Downloading update " + version + " from " + downloadUrl + "...");
        CompletableFuture.runAsync(() -> {
            Path target = Paths.get("Prime Bot.jar");
            try (InputStream in = new URL(downloadUrl).openStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                String message = "Downloaded update to " + target.toAbsolutePath();
                appendLog(message);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message,
                        "Update downloaded", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception ex) {
                appendLog("Update download failed: " + ex.getMessage());
            }
        }, backgroundExecutor);
    }

    private int compareVersions(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = parseVersionPart(i < latestParts.length ? latestParts[i] : "0");
            int currentPart = parseVersionPart(i < currentParts.length ? currentParts[i] : "0");
            if (latestPart != currentPart) {
                return Integer.compare(latestPart, currentPart);
            }
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void seedSavedScripts() {
        savedScripts.clear();
        savedScripts.put("Blue Eye example", defaultScript());
        savedScripts.put("Looped press", "# Looping example\nLOOP 3\n  PRESS " + DEFAULT_VISIBLE_KEY + "\n  WAIT 250\nEND_LOOP\nLOG Loop finished");
        persistSavedScripts();
        refreshSavedScriptsList();
        savedScriptsList.setSelectedIndex(0);
    }

    private void loadSavedScripts() {
        savedScripts.clear();
        ensureScriptsDirectory();
        try (Stream<Path> stream = Files.list(scriptsDirectory)) {
            stream.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".ini"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .forEach(this::readScriptFile);
        } catch (IOException ex) {
            appendLog("Failed to scan scripts folder: " + ex.getMessage());
        }

        if (savedScripts.isEmpty() && Files.exists(legacySavedScriptsFile)) {
            migrateLegacyScripts();
        }

        if (savedScripts.isEmpty()) {
            seedSavedScripts();
        } else {
            refreshSavedScriptsList();
            savedScriptsList.setSelectedIndex(0);
        }
    }

    private void readScriptFile(Path path) {
        String filename = path.getFileName().toString();
        String displayName = filename.endsWith(".ini") ? filename.substring(0, filename.length() - 4) : filename;
        try {
            savedScripts.put(displayName, Files.readString(path));
        } catch (IOException ex) {
            appendLog("Failed to load script from " + filename + ": " + ex.getMessage());
        }
    }

    private void migrateLegacyScripts() {
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(legacySavedScriptsFile)) {
            props.load(reader);
            for (String name : props.stringPropertyNames()) {
                savedScripts.put(name, props.getProperty(name));
            }
            appendLog("Migrated legacy saved scripts to scripts folder.");
            persistSavedScripts();
        } catch (IOException ex) {
            appendLog("Failed to migrate legacy scripts: " + ex.getMessage());
        }
    }

    private void persistSavedScripts() {
        ensureScriptsDirectory();
        Set<Path> expectedFiles = new HashSet<>();
        savedScripts.forEach((name, content) -> {
            Path file = scriptPathForName(name);
            expectedFiles.add(file);
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                writer.write(content);
            } catch (IOException ex) {
                appendLog("Failed to save script '" + name + "': " + ex.getMessage());
            }
        });

        try (Stream<Path> stream = Files.list(scriptsDirectory)) {
            stream.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".ini"))
                    .filter(path -> !expectedFiles.contains(path))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            appendLog("Failed to delete old script file '" + path.getFileName() + "': " + ex.getMessage());
                        }
                    });
        } catch (IOException ex) {
            appendLog("Failed to clean scripts folder: " + ex.getMessage());
        }
    }

    private Path scriptPathForName(String name) {
        String sanitized = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (sanitized.isEmpty()) {
            sanitized = "script";
        }
        return scriptsDirectory.resolve(sanitized + ".ini");
    }

    private void ensureScriptsDirectory() {
        try {
            Files.createDirectories(scriptsDirectory);
        } catch (IOException ex) {
            appendLog("Failed to create scripts folder: " + ex.getMessage());
        }
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
        persistSavedScripts();
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
            persistSavedScripts();
        }
    }

    private String defaultScript() {
        return "# Blue Eye Macro style example\n" +
                "If Color.At coordinate is not (RGB '255', '0', '0', '" + (MouseInfo.getPointerInfo().getLocation().x) + "', '" + (MouseInfo.getPointerInfo().getLocation().y) + "') begin\n" +
                "  Macro.Pause('1800')\n" +
                "  Keyboard.Hold keys('{" + DEFAULT_VISIBLE_KEY + "}')\n" +
                "  Macro.Pause('1800')\n" +
                "  Keyboard.Release keys('{" + DEFAULT_VISIBLE_KEY + "}')\n" +
                "end\n" +
                "Macro.Loop('3') begin\n" +
                "  WAIT 100\n" +
                "  PRESS " + DEFAULT_VISIBLE_KEY + "\n" +
                "end";
    }

    private void attachScriptAutofill() {
        scriptArea.addCaretListener(e -> maybeAutofillColorLine());
    }

    private void maybeAutofillColorLine() {
        if (updatingScriptAutoFill) {
            return;
        }
        Document doc = scriptArea.getDocument();
        int caret = scriptArea.getCaretPosition();
        try {
            int lineStart = Utilities.getRowStart(scriptArea, caret);
            int lineEnd = Utilities.getRowEnd(scriptArea, caret);
            if (lineStart == -1 || lineEnd == -1) {
                return;
            }
            String line = doc.getText(lineStart, lineEnd - lineStart);
            String marker = "If Color.At coordinate is not (RGB";
            int markerIndex = line.indexOf(marker);
            if (markerIndex == -1) {
                return;
            }

            int openParen = line.indexOf('(', markerIndex);
            int closeParen = line.indexOf(')', openParen + 1);
            if (openParen == -1 || closeParen == -1) {
                return;
            }

            int caretInLine = caret - lineStart;
            if (caretInLine <= openParen || caretInLine >= closeParen) {
                return;
            }

            ColorSample sample = library.getTargetSample().orElseGet(() -> {
                ColorSample fresh = library.captureCurrentPixel();
                library.setTargetSample(fresh);
                appendLog("Captured target at " + fresh.location() + " = " + fresh.toHex());
                return fresh;
            });

            int r = sample.color().getRed();
            int g = sample.color().getGreen();
            int b = sample.color().getBlue();
            int x = sample.location().x;
            int y = sample.location().y;
            String suffix = line.substring(closeParen + 1);
            String replacement = String.format("If Color.At coordinate is not (RGB '%d', '%d', '%d', '%d', '%d')%s", r, g, b, x, y, suffix);

            if (replacement.equals(line)) {
                return;
            }

            updatingScriptAutoFill = true;
            doc.remove(lineStart, lineEnd - lineStart);
            doc.insertString(lineStart, replacement, null);
            int newCaret = Math.min(lineStart + replacement.length(), doc.getLength());
            scriptArea.setCaretPosition(newCaret);
        } catch (BadLocationException ignored) {
            // ignore
        } finally {
            updatingScriptAutoFill = false;
        }
    }

    private void registerCaptureHotkey() {
        ActionMap actionMap = getRootPane().getActionMap();
        javax.swing.InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        int keyCode = KeyName.toKeyCode(DEFAULT_CAPTURE_HOTKEY);
        captureKeyStroke = KeyStroke.getKeyStroke(keyCode, 0, false);
        inputMap.put(captureKeyStroke, "capture-target");
        actionMap.put("capture-target", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                captureTarget();
            }
        });
    }

    private void registerScreenshotHotkey() {
        ActionMap actionMap = getRootPane().getActionMap();
        javax.swing.InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(screenshotKeyStroke, "screenshot-capture");
        actionMap.put("screenshot-capture", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appendLog("F12 pressed: capturing screenshot for pixel pick...");
                captureFromScreenshot();
            }
        });
    }

    private void captureTarget() {
        ColorSample sample = library.captureCurrentPixel();
        library.setTargetSample(sample);
        appendLog("Captured target at " + sample.location() + " = " + sample.toHex());
    }

    private void captureFromScreenshot() {
        appendLog("Capturing screenshot for pixel pick...");
        BufferedImage screenshot = library.captureScreenshot();
        BufferedImage copy = new BufferedImage(screenshot.getWidth(), screenshot.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(screenshot, 0, 0, null);
        g.dispose();
        final double[] zoom = {1.0};

        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Pick pixel from screenshot", true);
        javax.swing.JLabel label = new javax.swing.JLabel(new javax.swing.ImageIcon(copy));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = (int) Math.round(e.getX() / zoom[0]);
                int y = (int) Math.round(e.getY() / zoom[0]);
                if (x < 0 || y < 0 || x >= copy.getWidth() || y >= copy.getHeight()) {
                    return;
                }
                Color color = new Color(copy.getRGB(x, y), true);
                ColorSample sample = new ColorSample(new Point(x, y), color);
                library.setTargetSample(sample);
                appendLog("Captured screenshot target at (" + x + ", " + y + ") = " + sample.toHex());
                dialog.dispose();
            }
        });
        label.addMouseWheelListener(event -> {
            if (!event.isControlDown()) {
                return;
            }
            double nextZoom = zoom[0] + (event.getPreciseWheelRotation() < 0 ? 0.1 : -0.1);
            zoom[0] = Math.max(0.25, Math.min(8.0, nextZoom));
            BufferedImage scaled = scaleImage(copy, zoom[0]);
            label.setIcon(new javax.swing.ImageIcon(scaled));
            label.revalidate();
        });

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(label);
        dialog.add(scrollPane);
        dialog.setSize(new Dimension(Math.min(1000, copy.getWidth() + 50), Math.min(800, copy.getHeight() + 50)));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private BufferedImage scaleImage(BufferedImage source, double zoom) {
        int width = Math.max(1, (int) Math.round(source.getWidth() * zoom));
        int height = Math.max(1, (int) Math.round(source.getHeight() * zoom));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
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
        int interval = DEFAULT_INTERVAL_MS;
        boolean failSafe = failSafeCheckbox.isSelected();
        monitor.start(sample, DEFAULT_VISIBLE_KEY, DEFAULT_MISSING_KEY, failSafe, interval, this::appendLog);
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

    private synchronized boolean isScriptRunning() {
        return runningScriptFuture != null && !runningScriptFuture.isDone();
    }

    private void toggleScriptRun() {
        synchronized (this) {
            if (isScriptRunning()) {
                runningScriptFuture.cancel(true);
                runningScriptFuture = null;
                runButton.setText("Run script");
                appendLog("Stopping script...");
                return;
            }
            String scriptText = scriptArea.getText();
            runButton.setText("Stop script");
            appendLog("Running script...");
            runningScriptFuture = scriptExecutor.submit(() -> {
                try {
                    scriptEngine.run(scriptText, this::appendLog);
                    appendLog("Script finished");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    appendLog("Script stopped");
                } catch (ColorScriptEngine.ScriptExecutionException ex) {
                    appendLog("Script stopped due to error: " + ex.getMessage());
                } finally {
                    synchronized (ColorBotApp.this) {
                        runningScriptFuture = null;
                    }
                    SwingUtilities.invokeLater(() -> runButton.setText("Run script"));
                }
            });
        }
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
            trimLog();
        });
    }

    private void trimLog() {
        try {
            while (logArea.getLineCount() > MAX_LOG_LINES) {
                int end = logArea.getLineEndOffset(0);
                logArea.getDocument().remove(0, end);
            }
        } catch (BadLocationException ignored) {
            // ignore
        }
    }

    private void clearLog() {
        logArea.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ColorBotApp app = new ColorBotApp();
            app.setVisible(true);
        });
    }
}
