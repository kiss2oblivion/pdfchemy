#!/usr/bin/env bash
set -e

DISPLAY_NUM=99
RESOLUTION="1280x800x24"
OUTPUT_DIR="/mnt/e/backup_ext_hdd/android-projects-2026-07-30/Shrinkpdf/distribution/linux"
SCREENSHOT_DASHBOARD="$OUTPUT_DIR/pdfchemy-linux-clean-dashboard.png"

killall -9 pdfchemy ffmpeg Xvfb openbox 2>/dev/null || true
sleep 1

Xvfb :$DISPLAY_NUM -screen 0 $RESOLUTION -ac +extension GLX +render -noreset &
XVFB_PID=$!
sleep 2

export DISPLAY=:$DISPLAY_NUM
openbox &
OPENBOX_PID=$!
sleep 1

pdfchemy &
APP_PID=$!
sleep 5

# Dismiss setup dialog with Escape
xdotool key Escape
sleep 1.5

# Take screenshot of clean dashboard
scrot "$SCREENSHOT_DASHBOARD"

kill -9 $APP_PID 2>/dev/null || true
kill -9 $OPENBOX_PID 2>/dev/null || true
kill -9 $XVFB_PID 2>/dev/null || true

echo "Saved screenshot to $SCREENSHOT_DASHBOARD"
