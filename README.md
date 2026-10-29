# Reels Blocker

A small, free Android app that blocks **only Instagram Reels** during times you choose. Feed, Stories, DMs, search and profiles keep working.

## How it works

An Accessibility Service watches Instagram's screen. When the Reels viewer is showing (the Reels tab, or a reel opened from the feed, Explore or a DM) and you're inside a block time, it presses Back for you, or switches to the Home tab if Back wouldn't leave Reels. Nothing is collected or sent anywhere, and the app has no internet permission.

## Install

1. Download `app-release.apk` (from a release or the **Build APK** Actions run) and open it on your phone. Allow installs from that source if asked.
2. Open **Reels Blocker** → **Open accessibility settings** → **Reels Blocker** → turn it on.
   - Android 13+ may show *"Restricted setting"* for sideloaded apps. Go to **Settings → Apps → Reels Blocker → ⋮ → Allow restricted settings**, then try again.
3. Add block times (start, end, days). Times that end before they start run past midnight, e.g. 22:00–07:00. Or turn on **Block Reels all the time**.

Some phones (Xiaomi, Samsung, OnePlus, etc.) kill background services. If blocking stops working, set the app's battery usage to **Unrestricted**.

## Build

```sh
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
./gradlew testReleaseUnitTest
```

The release build uses the committed `keystore/debug.keystore`. That keystore isn't secret, but it means every build is signed the same way, so new versions install over old ones.

## If Instagram changes

Detection uses Instagram's view IDs (`clips_viewer_view_pager`, `clips_tab`, `feed_tab`) in `ReelsBlockerService.kt`, plus a fallback that looks for a selected tab labelled "Reels". If an Instagram update stops the blocking, check the current IDs with `adb shell uiautomator dump` while Reels is open, then update the lists.
