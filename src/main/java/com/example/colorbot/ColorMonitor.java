package com.example.colorbot;


import java.awt.Robot;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;

import java.util.concurrent.TimeUnit;

import java.util.function.Consumer;

/**
 * Background task that continuously checks for the target color and triggers key presses.
 */
public class ColorMonitor implements AutoCloseable {
    private final ColorLibrary library;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> currentTask;


    public ColorMonitor(Robot robot) {
        this(new ColorLibrary(robot));
    }


    public ColorMonitor(ColorLibrary library) {
        this.library = Objects.requireNonNull(library, "library");
        this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "color-monitor");
                t.setDaemon(true);
                return t;
            }
        });
    }

    public synchronized void start(ColorSample sample, String visibleKey, String missingKey,
                                   boolean failSafe, int intervalMs, Consumer<String> statusConsumer) {
        stop();
        Runnable runnable = () -> {
            boolean visible = library.isColorAt(sample);
            if (visible) {
                library.pressKey(visibleKey);
                statusConsumer.accept("Visible: pressed " + visibleKey);
            } else {
                library.pressKey(missingKey);
                String message = "Missing: pressed " + missingKey;
                if (failSafe) {
                    message += " (fail-safe)";
                }
                statusConsumer.accept(message);
                if (failSafe) {
                    throw new IllegalStateException("Target color not found at " + sample.location());
                }
            }
        };
        currentTask = executor.scheduleAtFixedRate(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                statusConsumer.accept("Monitoring stopped: " + e.getMessage());
                stop();
            }

        }, 0, intervalMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        }, 0, intervalMs, TimeUnit.MILLISECONDS);

    }

    public synchronized void stop() {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    @Override
    public void close() {
        stop();
        executor.shutdownNow();
    }
}
