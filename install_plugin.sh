#!/bin/bash

# JOSM Conditional Restrictions Plugin Auto-Installer
# Automatically detects OS and installs plugin to correct JOSM directory

set -e  # Exit on any error

PLUGIN_NAME="ConditionalRestrictions.jar"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGIN_JAR="${SCRIPT_DIR}/${PLUGIN_NAME}"

echo "🔧 JOSM Conditional Restrictions Plugin Installer"
echo "================================================"

# Check if plugin JAR exists
if [ ! -f "$PLUGIN_JAR" ]; then
    echo "❌ Error: Plugin JAR not found at $PLUGIN_JAR"
    echo "Please build the plugin first with: ant clean dist"
    exit 1
fi

# Detect operating system and set JOSM plugins directory
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    JOSM_PLUGINS_DIR="$HOME/Library/JOSM/plugins"
    OS_NAME="macOS"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    JOSM_PLUGINS_DIR="$HOME/.josm/plugins"
    OS_NAME="Linux"
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
    # Windows
    JOSM_PLUGINS_DIR="$APPDATA/JOSM/plugins"
    OS_NAME="Windows"
else
    echo "❌ Error: Unsupported operating system: $OSTYPE"
    exit 1
fi

echo "🖥️  Detected OS: $OS_NAME"
echo "📁 JOSM plugins directory: $JOSM_PLUGINS_DIR"

# Create plugins directory if it doesn't exist
if [ ! -d "$JOSM_PLUGINS_DIR" ]; then
    echo "📁 Creating JOSM plugins directory..."
    mkdir -p "$JOSM_PLUGINS_DIR"
fi

# Check if JOSM is running
if pgrep -f "josm" > /dev/null; then
    echo "⚠️  WARNING: JOSM appears to be running!"
    echo "   Please close JOSM before installing the plugin."
    echo "   Continue anyway? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Installation cancelled."
        exit 1
    fi
fi

# Install GraphView dependency check
echo "🔍 Checking for GraphView plugin dependency..."
GRAPHVIEW_JAR="$JOSM_PLUGINS_DIR/graphview.jar"
if [ ! -f "$GRAPHVIEW_JAR" ]; then
    echo "⚠️  GraphView plugin not found!"
    echo "   The ConditionalRestrictions plugin requires GraphView to function."
    echo "   Please install GraphView through JOSM's Plugin Manager:"
    echo "   1. Start JOSM"
    echo "   2. Go to Preferences → Plugins"
    echo "   3. Search for 'graphview'"
    echo "   4. Check the box and click OK"
    echo "   5. Restart JOSM"
    echo ""
    echo "   Continue installing ConditionalRestrictions anyway? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Installation cancelled. Please install GraphView first."
        exit 1
    fi
fi

# Copy plugin JAR
echo "📦 Installing ConditionalRestrictions plugin..."
cp "$PLUGIN_JAR" "$JOSM_PLUGINS_DIR/"

# Verify installation
if [ -f "$JOSM_PLUGINS_DIR/$PLUGIN_NAME" ]; then
    echo "✅ Plugin installed successfully!"
    echo ""
    echo "📋 Next Steps:"
    echo "1. Start (or restart) JOSM"
    echo "2. Go to Preferences → Plugins"
    echo "3. Find 'ConditionalRestrictions' and check the box"
    echo "4. Click OK and restart JOSM when prompted"
    echo "5. Look for 'Test Conditional Restrictions' in the Tools menu"
    echo ""
    echo "🧪 Test the plugin:"
    echo "- Click Tools → Test Conditional Restrictions"
    echo "- You should see a dialog confirming the plugin is working"
    echo ""
    echo "Plugin location: $JOSM_PLUGINS_DIR/$PLUGIN_NAME"
else
    echo "❌ Error: Installation failed!"
    echo "Plugin was not copied to $JOSM_PLUGINS_DIR"
    exit 1
fi