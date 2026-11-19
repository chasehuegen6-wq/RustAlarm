# Rust+ Alarm Blaster

Android app that watches for Rust+ notifications (smart alarm, raid alerts) and
turns your phone into a siren: max alarm volume, looping ringtone, vibration,
screen wake-up, and a big STOP button. Works over silent mode and Do Not Disturb.

## Build the APK (pick one)

### A) GitHub Actions (no Android Studio)
1. Create a free GitHub repo and push this folder to it (branch `main`).
2. Open the repo's **Actions** tab, then **Build APK**, then **Run workflow** (it also runs on every push).
3. When it finishes, open the run and download the **RustAlarm-debug-apk** artifact.
   Unzip it to get `app-debug.apk` and copy it to your phone.

### B) Android Studio
1. Install Android Studio (free), then **Open** this folder and let Gradle sync.
2. **Build > Build Bundle(s) / APK(s) > Build APK(s)**, or plug in your phone and press Run.
3. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install and set up
1. Open the APK on your phone and allow "install unknown apps" for your file manager or browser.
2. Open **Rust Alarm** and do the five setup buttons in order:
   - Notification access: turn ON for Rust Alarm (this is the essential one).
   - Allow notifications, Do Not Disturb override, full-screen alerts, and battery optimization.
3. Choose your ringtone file (or use the default alarm sound).
4. Press **Test alarm now** to confirm it is loud, then **Stop alarm**.
5. In the Rust+ app make sure notifications are enabled for your smart alarm.

## Optional filter
Rust+ also sends other notifications. In the app, set **Filter** to words from your
alarm's message (comma-separated, not case-sensitive), for example `alarm, raid`.
Empty means every Rust+ notification triggers the siren.

## Notes
- The Rust+ package name defaults to `com.facepunch.rust.companion`. If nothing triggers,
  check the real package name (Settings > Apps > Rust+ > App details) and edit it in the app.
- Some phones (Xiaomi, Samsung, OnePlus) aggressively kill background apps. Set battery to
  Unrestricted and, if needed, enable autostart / lock the app in Recents.
- It plays on the alarm stream, so the ringer switch and media volume don't matter.
