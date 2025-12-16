# Color Bot

A small Java Swing utility inspired by Blue Eye Macro. It watches an on-screen coordinate for a target color, presses configurable keys when that color appears or disappears, and exposes a built-in scripting playground powered by a lightweight automation library.

## Features
- Capture the current mouse coordinates and pixel color using a configurable capture hotkey (default: F8).
- Press one key when the color is visible and another when it is not (defaults: F9 for visible, F10 for missing).
- Optional failure mode that stops monitoring and raises an error whenever the color is not visible at the captured point.
- Manual verification button to force an error if the color is missing.
- Script console that understands simple macro commands (WAIT, PRESS, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_TARGET_VISIBLE, IF_COLOR, LOG) so you can chain reactions the way Blue Eye Macro does.

## Building
```bash
mvn package
```

## Running
```bash
java -jar target/color-bot-1.0.0.jar
```

## Using the bot
1. Enter your preferred hotkeys in the configuration fields.
2. Press the capture hotkey (or click **Capture coords & color**) to record the pixel under your cursor.
3. Click **Start monitoring** to begin checking the color at the chosen interval. The bot presses the configured keys based on visibility and raises an error if the color is missing while the fail-safe is enabled.
4. Use **Verify color now** to manually check the color.
5. Write a quick macro in the **Script playground** to orchestrate checks and presses. For example:
   ```
   # Capture the pixel and act on it
   CAPTURE_TARGET
   WAIT 500
   IF_TARGET_VISIBLE THEN PRESS F9 ELSE PRESS F10
   LOG Done!
   ```
