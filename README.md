# Color Bot

A small Java Swing utility that watches a single on-screen coordinate for a target color. It can capture the current mouse position and color on a configurable hotkey, then press different keys depending on whether that color is visible. If the expected color disappears, the bot can raise an error to alert you immediately.

## Features
- Capture the current mouse coordinates and pixel color using a configurable capture hotkey (default: F8).
- Press one key when the color is visible and another when it is not (defaults: F9 for visible, F10 for missing).
- Optional failure mode that throws an error whenever the color is not visible at the captured point.
- Manual verification button to force an error if the color is missing.

## Running
1. Build the project:
   ```bash
   mvn package
   ```
2. Launch the app:
   ```bash
   java -jar target/color-bot-1.0.0.jar
   ```

## Using the bot
1. Enter your preferred hotkeys in the configuration fields.
2. Press the capture hotkey (or click **Capture coords & color**) to record the pixel under your cursor.
3. Click **Start monitoring** to begin checking the color every 250 ms. The bot presses the configured keys based on visibility and raises an error if the color is missing while the fail-safe is enabled.
4. Use **Verify color now** to manually trigger the error if the color cannot be seen.
