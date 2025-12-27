# Color Bot

A small Java Swing utility inspired by Blue Eye Macro. It watches an on-screen coordinate for a target color, presses configurable keys when that color appears or disappears, and exposes a built-in scripting playground powered by a lightweight automation library.

## Features
- Capture the current mouse coordinates and pixel color using the capture hotkey (F8).
- Take a Blue Eye-style screenshot capture by pressing **F12** to open the screenshot picker immediately, then click a pixel in the image to store its coordinates and color.

- Press one key when the color is visible and another when it is not (defaults: F9 for visible, F10 for missing).
- Optional failure mode that stops monitoring and raises an error whenever the color is not visible at the captured point.
- Manual verification button to force an error if the color is missing.
- Script console that understands simple macro commands (WAIT, PRESS, HOLD, RELEASE, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_COLOR, LOOP/END_LOOP) so you can chain reactions the way Blue Eye Macro does. `IF_COLOR` now accepts plain decimal RGB values (e.g., `IF_COLOR 239 373 255 0 0 THEN PRESS F9 ELSE PRESS F10`). Use `LOOP FOREVER ... END_LOOP` (or `Macro.Loop('FOREVER') begin ... end`) for a permanent loop.
- One-click **Script guide** button beside the editor that summarizes the syntax and available functions.
- Saved script sidebar so you can store, reload, and delete your favorite macros while editing a new one in the code space; scripts persist across restarts and are stored as `.ini` files inside the `scripts/` folder.
- Blue Eye-style blocks such as `If Color.At coordinate is (RGB 'R','G','B','X','Y') begin ... end` and `Macro.Loop('3') begin ... end` for familiar script structure.
- Inline autofill: when your cursor is inside the `(RGB ...)` portion of the `If Color.At coordinate is not (...)` line, the editor drops in the latest captured RGB and coordinates for you.
- Resizable split between the script editor and a clearable log so you can keep coding while monitoring output.
- Scripts stop automatically when an error occurs and log the offending line.
- Update checker runs at startup and via a **Check for updates** button, comparing the current build against a remote version file and offering to download the latest `.jar` when newer than your local copy.

- Press one key when the color is visible and another when it is not (defaults: F9 for visible, F10 for missing).
- Optional failure mode that stops monitoring and raises an error whenever the color is not visible at the captured point.
- Manual verification button to force an error if the color is missing.
- Script console that understands simple macro commands (WAIT, PRESS, HOLD, RELEASE, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_COLOR, LOOP/END_LOOP) so you can chain reactions the way Blue Eye Macro does. `IF_COLOR` now accepts plain decimal RGB values (e.g., `IF_COLOR 239 373 255 0 0 THEN PRESS F9 ELSE PRESS F10`). Use `LOOP FOREVER ... END_LOOP` (or `Macro.Loop('FOREVER') begin ... end`) for a permanent loop.
- One-click **Script guide** button beside the editor that summarizes the syntax and available functions.
- Saved script sidebar so you can store, reload, and delete your favorite macros while editing a new one in the code space; scripts persist across restarts and are stored as `.ini` files inside the `scripts/` folder.
- Blue Eye-style blocks such as `If Color.At coordinate is (RGB 'R','G','B','X','Y') begin ... end` and `Macro.Loop('3') begin ... end` for familiar script structure.
- Inline autofill: when your cursor is inside the `(RGB ...)` portion of the `If Color.At coordinate is not (...)` line, the editor drops in the latest captured RGB and coordinates for you.
- Resizable split between the script editor and a clearable log so you can keep coding while monitoring output.
- Scripts stop automatically when an error occurs and log the offending line.
- Update checker runs at startup and via a **Check for updates** button, comparing the current build against a remote version file and offering to download the latest `.jar` when newer than your local copy.
- Capture the current mouse coordinates and pixel color using a configurable capture hotkey (default: F8).
- Take a Blue Eye-style screenshot capture: press **F12** (or click the **Spy glass (F12)** button) to open the screenshot picker immediately, then click a pixel in the image to store its coordinates and color.
- Press one key when the color is visible and another when it is not (defaults: F9 for visible, F10 for missing).
- Optional failure mode that stops monitoring and raises an error whenever the color is not visible at the captured point.
- Manual verification button to force an error if the color is missing.
- Script console that understands simple macro commands (WAIT, PRESS, HOLD, RELEASE, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_COLOR, LOOP/END_LOOP) so you can chain reactions the way Blue Eye Macro does. `IF_COLOR` now accepts plain decimal RGB values (e.g., `IF_COLOR 239 373 255 0 0 THEN PRESS F9 ELSE PRESS F10`).
- Saved script sidebar so you can store, reload, and delete your favorite macros while editing a new one in the code space.
- Blue Eye-style blocks such as `If Color.At coordinate is (RGB 'R','G','B','X','Y') begin ... end` and `Macro.Loop('3') begin ... end` for familiar script structure.
- Inline autofill: when your cursor is inside the `(RGB ...)` portion of the `If Color.At coordinate is not (...)` line, the editor drops in the latest captured RGB and coordinates for you.

## Building
```bash
mvn package
```

- Script console that understands simple macro commands (WAIT, PRESS, HOLD, RELEASE, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_COLOR, LOOP/END_LOOP) so you can chain reactions the way Blue Eye Macro does.
- Saved script sidebar so you can store, reload, and delete your favorite macros while editing a new one in the code space.
- Blue Eye-style blocks such as `If Color.At coordinate is (RGB 'R','G','B','X','Y') begin ... end` and `Macro.Loop('3') begin ... end` for familiar script structure.
- Script console that understands simple macro commands (WAIT, PRESS, TYPE, MOVE, CLICK, CAPTURE_TARGET, IF_TARGET_VISIBLE, IF_COLOR, LOG) so you can chain reactions the way Blue Eye Macro does.




## Building
```bash
mvn package
```

## Running
```bash
java -jar target/color-bot-1.0.0.jar
```


### Update checks
* The app checks for updates at startup and via the **Check for updates** button near the monitoring controls.
* By default it reads from `https://example.com/colorbot/latest-version.txt`. Override with:
  * JVM property: `-Dcolorbot.updateUrl=https://yourhost/latest.txt`
  * Env var: `COLORBOT_UPDATE_URL=https://yourhost/latest.txt`
* When a newer version is found, the app offers to download the latest build from `https://github.com/KSPOG/color/raw/refs/heads/main/live%20build/Prime%20Bot.jar` into the current working directory as `Prime Bot.jar`. Override the download source with `-Dcolorbot.downloadUrl=...` or `COLORBOT_DOWNLOAD_URL=...`.
* The current build reports version `1.0.0` (stored in `src/main/resources/version.txt`); when the remote value is higher, a dialog is shown and the downloader can run automatically.

### Custom window icon
Place your desired icon at `src/main/resources/icon.png` (or drop `resources/icon.png` next to the jar/working directory) and the app will use it for the title bar on startup. A plain `icon.png` next to the executable also works as a fallback.

## Using the bot
1. Press the capture hotkey (F8) to record the pixel under your cursor. For a Blue Eye Macro-style screenshot grab, press **F12** to immediately open the screenshot, then click the pixel to store its coordinates and color. Hold **Ctrl + scroll** to zoom in/out when picking pixels.
2. Click **Start monitoring** to begin checking the color every 250 ms. The bot presses the configured keys based on visibility and raises an error if the color is missing while the fail-safe is enabled.
3. Use **Verify color now** to manually check the color.
4. Use the **Saved scripts** box to load an example or one of your own, then edit or author a new macro in the **Editor** code space. Saved entries are written as `scripts/<name>.ini` on disk and reloaded automatically the next time you start the app. Click **Script guide** to see the available commands at a glance, and **Run script** to execute. For example:
   ```
   If Color.At coordinate is not (RGB '255', '233', '166', '391', '67') begin
     Macro.Pause('1800')
     Keyboard.Hold keys('{TAB}')
     Macro.Pause('1800')
     Keyboard.Release keys('{TAB}')
   end
   If Color.At coordinate is not (RGB '255', '0', '0', '993', '626') && If Color.At coordinate is not (RGB '255', '0', '0', '1024', '540') begin
     LOG Both checks passed
   end
   SET FireBoltCD = 5000
   SET LastFireBolt = TIMER
   IF_COOLDOWN LastFireBolt FireBoltCD THEN PRESS F6
   # Cooldowns also persist to cooldowns/cooldowns.properties for external controllers
   Macro.Loop('3') begin
     WAIT 150
     PRESS F9
   end
   LOOP FOREVER
     PRESS F10
   END_LOOP
   SET MagicMissileCD = 4000
   SET LastMagicMissile = TIMER
   IF_COOLDOWN LastMagicMissile MagicMissileCD THEN PRESS F6
   # Example: disable specific skills you don't have unlocked yet
   DISABLE Skill2
   DISABLE Skill3
   DISABLE Skill4
   # Sample cooldown loop matching your script
   Macro.Loop('FOREVER') begin
     If Color.At coordinate is (RGB '152', '2', '0', '960', '35') begin
       IF_COOLDOWN LastSkill1 Skill1CD THEN PRESS 3
       IF_COOLDOWN LastSkill1 Skill1CD THEN SET LastSkill1 = TIMER
       Macro.Pause('1000')
     end
   end
   ```
   `begin`/`end` blocks can be nested. Keys accept the `{NAME}` format used by Blue Eye Macro.


### Where to place `Macro.Loop`
To repeat a block—such as the Fiesta Online "Fighter" example—wrap the actions you want to cycle inside a `Macro.Loop` block after any one-time setup. Example:

```
################
#  Fighter Example  #
################

Macro.Pause('1800')

Macro.Loop('FOREVER') begin
  If Color.At coordinate is not (RGB '3', '59', '143', '222', '82') begin
    LOG Healing SP
    Macro.Pause('1800')
    Keyboard.Hold keys('{E}')
    Macro.Pause('1800')
    Keyboard.Release keys('{E}')
  end

  If Color.At coordinate is not (RGB '255', '0', '0', '1093', '686') begin
    LOG Healing HP
    Macro.Pause('1800')
    Keyboard.Hold keys('{A}')
    Macro.Pause('1800')
    Keyboard.Release keys('{A}')
  end
end
```
Place `Macro.Loop('FOREVER') begin` right after your initial pause or setup, and close it with `end` once all repeating checks and actions are inside.


### Where to place `Macro.Loop`
To repeat a block—such as the Fiesta Online "Fighter" example—wrap the actions you want to cycle inside a `Macro.Loop` block after any one-time setup. Example:

```
################
#  Fighter Example  #
################

Macro.Pause('1800')

Macro.Loop('FOREVER') begin
  If Color.At coordinate is not (RGB '3', '59', '143', '222', '82') begin
    LOG Healing SP
    Macro.Pause('1800')
    Keyboard.Hold keys('{E}')
    Macro.Pause('1800')
    Keyboard.Release keys('{E}')
  end

  If Color.At coordinate is not (RGB '255', '0', '0', '1093', '686') begin
    LOG Healing HP
    Macro.Pause('1800')
    Keyboard.Hold keys('{A}')
    Macro.Pause('1800')
    Keyboard.Release keys('{A}')
  end
end
```
Place `Macro.Loop('FOREVER') begin` right after your initial pause or setup, and close it with `end` once all repeating checks and actions are inside.


## Using the bot
1. Enter your preferred hotkeys in the configuration fields.
2. Press the capture hotkey (or click **Capture coords & color**) to record the pixel under your cursor. For a Blue Eye Macro-style screenshot grab, press **F12** (or click **Spy glass (F12)**) to immediately open the screenshot, then click the pixel to store its coordinates and color.
3. Click **Start monitoring** to begin checking the color at the chosen interval. The bot presses the configured keys based on visibility and raises an error if the color is missing while the fail-safe is enabled.
4. Use **Verify color now** to manually check the color.
2. Press the capture hotkey (or click **Capture coords & color**) to record the pixel under your cursor.
3. Click **Start monitoring** to begin checking the color at the chosen interval. The bot presses the configured keys based on visibility and raises an error if the color is missing while the fail-safe is enabled.
4. Use **Verify color now** to manually check the color.
5. Use the **Saved scripts** box to load an example or one of your own, then edit or author a new macro in the **Editor** code space. Click **Run script** to execute it. For example:
   ```
   If Color.At coordinate is not (RGB '255', '233', '166', '391', '67') begin
     Macro.Pause('50')
     Keyboard.Hold keys('{TAB}')
     Macro.Pause('50')
     Keyboard.Release keys('{TAB}')
   end
   Macro.Loop('3') begin
     WAIT 150
     PRESS F9
   end
   ```
   `begin`/`end` blocks can be nested. Keys accept the `{NAME}` format used by Blue Eye Macro.
5. Write a quick macro in the **Script playground** to orchestrate checks and presses. For example:
   ```
   # Capture the pixel and act on it
   CAPTURE_TARGET
   WAIT 500
   IF_TARGET_VISIBLE THEN PRESS F9 ELSE PRESS F10
   LOG Done!
   ```


