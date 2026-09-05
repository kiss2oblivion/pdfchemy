#!/usr/bin/env bash
set -e

DISPLAY_NUM=99
RESOLUTION="1280x800x24"
OUTPUT_DIR="/mnt/e/backup_ext_hdd/android-projects-2026-07-30/Shrinkpdf/distribution/linux"
VIDEO_OUT="$OUTPUT_DIR/pdfchemy-linux-demo.mp4"
SCREENSHOT_MANIFESTO="$OUTPUT_DIR/pdfchemy-linux-manifesto.png"

mkdir -p "$OUTPUT_DIR"

killall -9 pdfchemy ffmpeg Xvfb openbox 2>/dev/null || true
sleep 1

Xvfb :$DISPLAY_NUM -screen 0 $RESOLUTION -ac +extension GLX +render -noreset &
XVFB_PID=$!
sleep 2

export DISPLAY=:$DISPLAY_NUM
openbox &
OPENBOX_PID=$!
sleep 1

# Launch with English
pdfchemy --lang=en &
APP_PID=$!
sleep 5

# Start recording
ffmpeg -y -video_size 1280x800 -framerate 30 -f x11grab -draw_mouse 1 -i :$DISPLAY_NUM.0 \
    -c:v libx264 -preset fast -crf 22 -pix_fmt yuv420p -movflags +faststart \
    -t 16 "$VIDEO_OUT" &
FFMPEG_PID=$!

# Dismiss language setup if present
sleep 1
xdotool key Escape
sleep 1.2

# Tour the dashboard cards
xdotool mousemove 280 500
sleep 1.2
xdotool mousemove 560 500
sleep 1.2
xdotool mousemove 830 500
sleep 1.2

# Scroll down smoothly on the dashboard to reveal footer
xdotool mousemove 600 500
xdotool click 5
xdotool click 5
xdotool click 5
sleep 1

# Click the Manifesto link in footer (now scrolled up into view)
xdotool mousemove 930 700
sleep 0.8
xdotool click 1
sleep 1

# Also click rail manifesto icon if dialog not open yet
xdotool mousemove 55 760
sleep 0.5
xdotool click 1
sleep 1.5

# Take screenshot of Manifesto dialog
scrot "$SCREENSHOT_MANIFESTO" || true

# Pause to showcase the Manifesto guarantees in video
sleep 3

# Close with Escape
xdotool key Escape
sleep 1.5

# Return to center
xdotool mousemove 640 400
sleep 1

wait $FFMPEG_PID || true

kill -9 $APP_PID 2>/dev/null || true
kill -9 $OPENBOX_PID 2>/dev/null || true
kill -9 $XVFB_PID 2>/dev/null || true

echo "=== Finished Recording! ==="
ls -lh "$VIDEO_OUT" "$SCREENSHOT_MANIFESTO"
