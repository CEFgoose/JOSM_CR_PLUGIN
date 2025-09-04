# JOSM Conditional Restrictions Plugin v1.0.0

## 🎉 Initial Release

We're excited to announce the first stable release of the JOSM Conditional Restrictions Plugin! This plugin enhances JOSM's capabilities for working with time-based and condition-based access restrictions in OpenStreetMap data.

### 🚀 Features

- **Comprehensive Tag Support**: Handles all major conditional restriction tags including:
  - `access:conditional`
  - `oneway:conditional`  
  - `vehicle:conditional` / `motor_vehicle:conditional`
  - `hgv:conditional`
  - `maxspeed:conditional`
  - `parking:conditional`
  - `restriction:conditional`

- **Time-based Restrictions**: Full support for complex time conditions:
  - Day ranges (Mo-Fr, Sa-Su)
  - Time ranges (07:00-19:00) 
  - Combined conditions (Mo-Fr 07:00-19:00; Sa 09:00-14:00)
  - Holiday support

- **Visual Feedback**: Color-coded visualization of restrictions directly in the JOSM map view

- **Validation**: Real-time syntax validation with error highlighting to ensure correct tagging

- **GraphView Integration**: Optional integration with the GraphView plugin for advanced routing analysis

### 📋 Requirements

- JOSM version 18000 or later
- GraphView plugin (optional, for routing features)
- Java 8 or later

### 📦 Installation

1. Download `ConditionalRestrictions.jar` from this release
2. Place the JAR file in your JOSM plugins directory:
   - **Windows**: `%APPDATA%\JOSM\plugins\`
   - **macOS**: `~/Library/JOSM/plugins/`
   - **Linux**: `~/.josm/plugins/`
3. Restart JOSM
4. The plugin will load automatically

### 🔧 Usage

Once installed, the plugin automatically processes conditional restrictions in your OSM data:
- Conditional tags are highlighted with special colors in the map view
- Validation warnings appear for incorrectly formatted conditional tags
- Integration with GraphView (if installed) shows restrictions in routing graphs

### 🐛 Known Issues

- None reported in testing

### 🙏 Credits

- Developed for the KAART mapping team
- Based on OpenStreetMap conditional restriction tagging standards
- Built on the JOSM plugin framework

### 📝 Feedback

Please report any issues or feature requests on our [GitHub Issues](https://github.com/CEFgoose/JOSM_CR_PLUGIN/issues) page.

---

**Full Changelog**: This is the initial release