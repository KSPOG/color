package com.example.colorbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple externalized cooldown store backed by a properties file so other tools
 * (or multiple bot instances) can coordinate timestamps.
 */
public class ExternalCooldownController {
    private final Path directory;
    private final Path file;
    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    public ExternalCooldownController() {
        this(Path.of("cooldowns"), "cooldowns.properties");
    }

    public ExternalCooldownController(Path directory, String filename) {
        this.directory = directory;
        this.file = directory.resolve(filename);
        load();
    }

    public Optional<Long> get(String name) {
        return Optional.ofNullable(cache.get(normalize(name)));
    }

    public void put(String name, long value) {
        cache.put(normalize(name), value);
        persist();
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                try {
                    cache.put(normalize(key), Long.parseLong(props.getProperty(key)));
                } catch (NumberFormatException ignored) {
                    // skip malformed entry
                }
            }
        } catch (IOException ignored) {
            // best effort
        }
    }

    private void persist() {
        try {
            Files.createDirectories(directory);
            Properties props = new Properties();
            cache.forEach((k, v) -> props.setProperty(k, Long.toString(v)));
            try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                props.store(out, "Cooldown timestamps (ms since epoch)");
            }
        } catch (IOException ignored) {
            // best effort
        }
    }
}
