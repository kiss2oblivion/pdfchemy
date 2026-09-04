#!/usr/bin/env bash
# ==============================================================================
# PDFchemy Tools — Universal Linux / Unix Launcher & Dependency Resolver
# ==============================================================================

set -e

APP_NAME="PDFchemy Tools"
JAR_NAME="PDFchemy-universal-1.0.2.jar"
MIN_JAVA_VERSION=17

# Resolve script directory to find the JAR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$SCRIPT_DIR/$JAR_NAME"

if [ ! -f "$JAR_PATH" ]; then
    # Auto-detect any universal jar in the folder
    FOUND_JAR=$(ls "$SCRIPT_DIR"/PDFchemy-universal-*.jar 2>/dev/null | head -n 1)
    if [ -n "$FOUND_JAR" ]; then
        JAR_PATH="$FOUND_JAR"
    else
        JAR_PATH="./$JAR_NAME"
    fi
fi

check_java_installed() {
    if command -v java >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

get_java_version() {
    local version_str
    version_str=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    local major_version
    major_version=$(echo "$version_str" | awk -F '.' '{print ($1 == "1" ? $2 : $1)}')
    echo "$major_version"
}

install_java_prompt() {
    echo ""
    echo "=================================================================="
    echo " ⚠️  $APP_NAME: Dependency Check"
    echo "=================================================================="
    echo " PDFchemy requires Java $MIN_JAVA_VERSION or higher to run."
    
    if check_java_installed; then
        local current_ver
        current_ver=$(get_java_version)
        echo " Current installed Java version: $current_ver (Outdated)"
    else
        echo " No Java Runtime Environment was found on your system."
    fi
    echo ""

    # Detect package manager
    local pkg_cmd=""
    local pkg_name=""
    if command -v apt-get >/dev/null 2>&1; then
        pkg_cmd="sudo apt-get update && sudo apt-get install -y openjdk-21-jre"
        pkg_name="APT (Debian / Ubuntu / Mint / Pop!_OS)"
    elif command -v dnf >/dev/null 2>&1; then
        pkg_cmd="sudo dnf install -y java-21-openjdk"
        pkg_name="DNF (Fedora / RHEL / Rocky / AlmaLinux)"
    elif command -v pacman >/dev/null 2>&1; then
        pkg_cmd="sudo pacman -S --noconfirm jre21-openjdk"
        pkg_name="Pacman (Arch / Manjaro / EndeavourOS)"
    elif command -v zypper >/dev/null 2>&1; then
        pkg_cmd="sudo zypper install -y java-21-openjdk"
        pkg_name="Zypper (openSUSE)"
    elif command -v apk >/dev/null 2>&1; then
        pkg_cmd="sudo apk add openjdk21-jre"
        pkg_name="APK (Alpine Linux)"
    elif command -v brew >/dev/null 2>&1; then
        pkg_cmd="brew install openjdk@21"
        pkg_name="Homebrew (macOS / Linux)"
    fi

    if [ -n "$pkg_cmd" ]; then
        echo " Detected package manager: $pkg_name"
        read -r -p " Would you like to install OpenJDK 21 automatically? [y/N]: " response
        case "$response" in
            [yY][eE][sS]|[yY])
                echo ""
                echo " Installing OpenJDK 21..."
                eval "$pkg_cmd"
                echo " Installation completed successfully!"
                echo ""
                ;;
            *)
                echo ""
                echo " Installation cancelled. Please install Java $MIN_JAVA_VERSION+ manually via:"
                echo "   $pkg_cmd"
                exit 1
                ;;
        esac
    else
        echo " Could not detect your system's package manager."
        echo " Please install OpenJDK 17 or 21 using your distribution's software center."
        exit 1
    fi
}

# --- Step 1: Check if Java exists and meets minimum version ---
if ! check_java_installed; then
    install_java_prompt
fi

CURRENT_VER=$(get_java_version)
if [ "$CURRENT_VER" -lt "$MIN_JAVA_VERSION" ]; then
    install_java_prompt
fi

# --- Step 2: Launch the application ---
if [ -f "$JAR_PATH" ]; then
    exec java -jar "$JAR_PATH" "$@"
else
    echo " Error: Cannot find $JAR_NAME in $SCRIPT_DIR."
    echo " Please place $JAR_NAME in the same directory as this script."
    exit 1
fi
