# JOSM Conditional Restrictions Plugin

A JOSM plugin for parsing, visualizing, and validating conditional restrictions in OpenStreetMap data. This plugin helps mappers work with time-based and condition-based access rules on roads and paths.

## Overview

The Conditional Restrictions plugin enhances JOSM's capabilities for working with conditional tags - restrictions that apply only under certain conditions like specific times, days, or vehicle properties. It provides visualization, validation, and editing support for these complex tags commonly used in urban mapping.

## Features

- **Conditional Tag Parsing**: Parses and validates conditional restriction syntax
- **Time-based Conditions**: Supports day/time restrictions like "Mo-Fr 07:00-19:00"
- **Multiple Tag Support**: Handles common conditional tags:
  - `access:conditional`
  - `oneway:conditional`
  - `hgv:conditional`
  - `maxspeed:conditional`
  - `parking:conditional`
- **GraphView Integration**: Seamlessly extends GraphView's routing capabilities
- **Syntax Validation**: Validates conditional statement syntax and provides error feedback
- **Visual Indicators**: Shows conditional restrictions in the graph visualization

## Supported Syntax

The plugin supports OpenStreetMap's conditional restriction syntax:

```
tag:conditional = value @ (condition)
```

Examples:
- `access:conditional = no @ (Mo-Fr 07:00-19:00)`
- `oneway:conditional = yes @ (Mo-Fr 06:00-10:00)`
- `maxspeed:conditional = 30 @ (22:00-06:00)`
- `hgv:conditional = no @ (weight>7.5)`
- `parking:conditional = no @ (Mo-Fr 08:00-18:00; Sa 08:00-14:00)`

## Installation

1. Install JOSM (version 18000 or later)
2. Install the GraphView plugin through JOSM's plugin manager
3. Download the ConditionalRestrictions plugin JAR file
4. Place it in your JOSM plugins directory:
   - Windows: `%APPDATA%\JOSM\plugins\`
   - macOS: `~/Library/JOSM/plugins/`
   - Linux: `~/.josm/plugins/`
5. Restart JOSM

## Building from Source

### Prerequisites
- Java 8 or later
- Apache Ant
- JOSM development environment

### Build Steps
```bash
# Clone the repository
git clone https://github.com/yourusername/josm-conditional-restrictions-plugin.git
cd josm-conditional-restrictions-plugin

# Build the plugin
ant clean dist

# Install to local JOSM
ant install
```

## Usage

1. Open JOSM and load your OSM data
2. Enable the GraphView plugin (View → GraphView)
3. The plugin automatically processes conditional restrictions
4. Conditional restrictions will be displayed in the graph with special indicators
5. Use the validation features to check syntax correctness

## Development

### Project Structure
```
josm-conditional-restrictions-plugin/
├── src/
│   └── org/openstreetmap/josm/plugins/conditionalrestrictions/
│       ├── ConditionalRestrictionsPlugin.java    # Main plugin class
│       ├── parser/
│       │   └── ConditionalRestrictionParser.java  # Syntax parser
│       ├── data/
│       │   ├── TimeCondition.java                 # Time condition model
│       │   └── ConditionalRestriction.java        # Restriction model
│       └── ui/
│           └── ConditionalPanel.java              # UI components
├── test/
│   └── unit/                                      # Unit tests
├── data/                                          # Plugin data files
├── images/                                        # Plugin icons
├── lib/                                           # External libraries
└── build.xml                                      # Ant build file
```

### API

The plugin provides the following main classes:

- `ConditionalRestrictionsPlugin`: Main plugin entry point
- `ConditionalRestrictionParser`: Parses conditional tag syntax
- `TimeCondition`: Represents time-based conditions
- `ConditionalRestriction`: Data model for restrictions

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Submit a pull request

## License

This plugin is licensed under the GPL v3 or later, consistent with JOSM's license.

## Credits

- Based on the JOSM GraphView plugin
- Uses OpenStreetMap conditional restriction tagging scheme
- Developed for enhanced routing analysis in JOSM

## Support

For issues, feature requests, or questions:
- Create an issue on GitHub
- Contact the JOSM mailing list
- Check the OpenStreetMap wiki for conditional restriction documentation