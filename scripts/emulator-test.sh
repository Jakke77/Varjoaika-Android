#!/usr/bin/env bash
set -euo pipefail
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
sdkmanager "system-images;android-${ANDROID_API};default;x86_64"
echo no | avdmanager create avd --force --name varjo-test --package "system-images;android-${ANDROID_API};default;x86_64"
sudo chown "$USER" /dev/kvm
emulator -avd varjo-test -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot > emulator.log 2>&1 &
EMU_PID=$!
trap 'kill "$EMU_PID" || true' EXIT
timeout 120 adb wait-for-device || { cat emulator.log; exit 1; }
for attempt in $(seq 1 180); do
  if [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; then break; fi
  sleep 2
done
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 || { cat emulator.log; exit 1; }
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
gradle --no-daemon connectedDebugAndroidTest
adb shell am start -n fi.varjoaika.android/fi.varjoaika.widget.CalendarActivity
mkdir -p previews
adb pull /sdcard/Android/data/fi.varjoaika.android/files/previews previews/widgets
sleep 2
adb exec-out screencap -p > "previews/calendar-api-${ANDROID_API}.png"
adb logcat -d > device.log
