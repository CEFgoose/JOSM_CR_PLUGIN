# JOSM Conditional Restrictions Plugin - Deployment and Installation Guide

## Overview

This guide provides comprehensive instructions for building, installing, and deploying the JOSM Conditional Restrictions Plugin. The plugin extends JOSM's GraphView functionality to support conditional restrictions for routing analysis, enabling visualization and validation of time-based and condition-based access restrictions in OpenStreetMap data.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Building from Source](#building-from-source)
3. [Installation Methods](#installation-methods)
4. [Dependencies and Requirements](#dependencies-and-requirements)
5. [Platform-Specific Instructions](#platform-specific-instructions)
6. [Plugin Usage](#plugin-usage)
7. [Testing and Verification](#testing-and-verification)
8. [Troubleshooting](#troubleshooting)
9. [Development Setup](#development-setup)
10. [Known Limitations](#known-limitations)

## Prerequisites

### System Requirements

- **Java**: Java 8 (JDK 1.8) or later
- **JOSM**: Version 18000 or later
- **Operating System**: Windows, macOS, or Linux
- **Memory**: Minimum 2GB RAM recommended for JOSM with plugins
- **Disk Space**: 50MB for plugin and dependencies

### Required Software

1. **Java Development Kit (JDK)**
   - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
   - Verify installation: `java -version` and `javac -version`

2. **Apache Ant** (for building from source)
   - Download from [Apache Ant](https://ant.apache.org/bindownload.cgi)
   - Verify installation: `ant -version`

3. **JOSM**
   - Download from [JOSM website](https://josm.openstreetmap.de/)
   - Ensure version 18000 or later

## Building from Source

### Quick Build

```bash
# Clone the repository
git clone https://github.com/yourusername/josm-conditional-restrictions-plugin.git
cd josm-conditional-restrictions-plugin

# Clean build
ant clean dist

# Install to local JOSM
ant install
```

### Detailed Build Process

#### Method 1: Using Apache Ant (Recommended)

1. **Navigate to project directory**
   ```bash
   cd josm-conditional-restrictions-plugin
   ```

2. **Download test dependencies** (if planning to run tests)
   ```bash
   ant download-test-libs
   ```

3. **Build the plugin**
   ```bash
   # Clean previous builds
   ant clean
   
   # Compile sources
   ant compile
   
   # Create plugin JAR
   ant dist
   
   # Validate plugin structure
   ant validate
   ```

4. **Run tests** (optional but recommended)
   ```bash
   # Run all tests
   ant test
   
   # Run specific test
   ant test-single -Dtest.class=ConditionalRestrictionParserTest
   
   # Clean build and test
   ant test-all
   ```

#### Method 2: Manual Compilation

If Ant is not available, you can build manually:

1. **Set up classpath**
   ```bash
   # Create build directory
   mkdir -p build/classes
   
   # Set classpath (adjust paths as needed)
   export CLASSPATH=".:lib/*:$JOSM_HOME/josm.jar:dist/graphview.jar"
   ```

2. **Compile sources**
   ```bash
   javac -d build/classes -cp "$CLASSPATH" -encoding UTF-8 \
         -source 1.8 -target 1.8 \
         src/org/openstreetmap/josm/plugins/conditionalrestrictions/**/*.java
   ```

3. **Copy resources**
   ```bash
   cp -r images build/classes/
   cp -r data build/classes/
   ```

4. **Create JAR file**
   ```bash
   cd build/classes
   jar cf ../../ConditionalRestrictions.jar .
   cd ../..
   ```

### Build Targets Reference

| Target | Description |
|--------|-------------|
| `ant compile` | Compile plugin sources |
| `ant dist` | Build plugin JAR |
| `ant install` | Install plugin to local JOSM |
| `ant clean` | Clean build artifacts |
| `ant test` | Run all unit tests |
| `ant test-single -Dtest.class=ClassName` | Run specific test |
| `ant validate` | Validate plugin structure |
| `ant dist-src` | Create source distribution |
| `ant rebuild` | Clean rebuild |
| `ant help` | Show available targets |

## Installation Methods

### Method 1: Automatic Installation (Ant)

```bash
# Build and install in one step
ant install
```

This automatically:
- Builds the plugin JAR
- Detects your operating system
- Copies to the correct JOSM plugins directory
- Works on Windows, macOS, and Linux

### Method 2: Manual Installation

1. **Build the plugin** (if not already built)
   ```bash
   ant dist
   ```

2. **Locate JOSM plugins directory**:
   - **Windows**: `%APPDATA%\JOSM\plugins\`
   - **macOS**: `~/Library/JOSM/plugins/`
   - **Linux**: `~/.josm/plugins/`

3. **Copy plugin JAR**
   ```bash
   # Example for macOS
   cp ConditionalRestrictions.jar ~/Library/JOSM/plugins/
   
   # Example for Linux
   cp ConditionalRestrictions.jar ~/.josm/plugins/
   
   # Example for Windows (in Command Prompt)
   copy ConditionalRestrictions.jar "%APPDATA%\JOSM\plugins\"
   ```

### Method 3: Distribution Package

If you have a pre-built distribution:

1. **Download the plugin JAR** from releases
2. **Copy to JOSM plugins directory** (see locations above)
3. **Restart JOSM**

## Dependencies and Requirements

### Required Dependencies

1. **GraphView Plugin**
   - **Installation**: JOSM → Preferences → Plugins → Search "graphview" → Install
   - **Purpose**: Provides routing graph visualization capabilities
   - **Status**: Required dependency (plugin.requires=graphview)
   - **Version**: Compatible with current JOSM release

2. **JOSM Core**
   - **Minimum Version**: 18000
   - **Recommended**: Latest stable release
   - **Features Used**: Plugin API, OSM data structures, UI framework

### Optional Dependencies

1. **JUnit** (for development/testing)
   - Version: 4.13.2
   - Automatically downloaded by `ant download-test-libs`

2. **Hamcrest** (for testing)
   - Version: 1.3
   - Used for test assertions

### Dependency Installation Order

1. Install JOSM (version 18000+)
2. Install GraphView plugin via JOSM Plugin Manager
3. Install ConditionalRestrictions plugin
4. Restart JOSM
5. Enable both plugins in Preferences → Plugins

## Platform-Specific Instructions

### Windows

#### Prerequisites
```cmd
# Verify Java
java -version
javac -version

# Install Ant (using Chocolatey)
choco install ant

# Or download manually from Apache Ant website
```

#### Building
```cmd
# Open Command Prompt or PowerShell
cd josm-conditional-restrictions-plugin
ant clean dist install
```

#### Manual Installation Path
```cmd
# Copy to JOSM plugins directory
copy ConditionalRestrictions.jar "%APPDATA%\JOSM\plugins\"
```

#### Common Windows Issues
- **Java not found**: Add Java bin directory to PATH environment variable
- **Ant not found**: Download Ant binary, extract, and add bin directory to PATH
- **Permission errors**: Run Command Prompt as Administrator

### macOS

#### Prerequisites
```bash
# Verify Java
java -version
javac -version

# Install Ant (using Homebrew)
brew install ant

# Or download manually
```

#### Building
```bash
cd josm-conditional-restrictions-plugin
ant clean dist install
```

#### Manual Installation Path
```bash
# Copy to JOSM plugins directory
cp ConditionalRestrictions.jar ~/Library/JOSM/plugins/
```

#### Common macOS Issues
- **Java not found**: Install JDK from Oracle or use `brew install openjdk`
- **Permission denied**: Check file permissions with `ls -la`
- **JOSM not finding plugin**: Verify JOSM plugins directory exists

### Linux

#### Prerequisites
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-8-jdk ant

# CentOS/RHEL/Fedora
sudo dnf install java-1.8.0-openjdk-devel ant

# Arch Linux
sudo pacman -S jdk8-openjdk ant
```

#### Building
```bash
cd josm-conditional-restrictions-plugin
ant clean dist install
```

#### Manual Installation Path
```bash
# Copy to JOSM plugins directory
cp ConditionalRestrictions.jar ~/.josm/plugins/
```

#### Common Linux Issues
- **Java version conflicts**: Use `update-alternatives` to set default Java version
- **Missing build tools**: Install `build-essential` package
- **JOSM directory permissions**: Check `~/.josm` directory permissions

## Plugin Usage

### Initial Setup

1. **Start JOSM** and verify both plugins are loaded
2. **Load OSM data** with conditional restrictions
3. **Access plugin features** via Tools menu

### Basic Usage

#### 1. Analyzing Conditional Restrictions

1. **Menu Access**: Tools → Analyze Conditional Restrictions
2. **Parameter Selection**:
   - Vehicle type (car, bicycle, pedestrian, HGV, etc.)
   - Current date/time for analysis
   - Custom vehicle dimensions (optional)
3. **View Results**: Analysis dialog shows affected ways and restrictions

#### 2. GraphView Integration

1. **Enable GraphView**: View → GraphView
2. **Routing Analysis**: GraphView automatically considers conditional restrictions
3. **Visual Indicators**: Restricted ways highlighted in routing graph

#### 3. Validation Features

1. **Automatic Validation**: Conditional restriction syntax checked on load
2. **Error Reporting**: Invalid syntax highlighted with suggestions
3. **Manual Validation**: Tools → Validate Data includes conditional checks

### Advanced Features

#### Vehicle Profiles
- **PEDESTRIAN**: Walking routes with foot access restrictions
- **BICYCLE**: Cycling routes with bicycle-specific rules
- **CAR**: Standard motor vehicle routing
- **MOTORCYCLE**: Motorcycle-specific routing considerations
- **BUS**: Public transport routing with bus lane access
- **HGV**: Heavy goods vehicle routing with weight/height restrictions
- **DELIVERY**: Delivery vehicle routing with loading zone access

#### Time Analysis
- **Current Time**: Real-time restriction evaluation
- **Custom Time**: Set specific date/time for scenario analysis
- **Time Ranges**: Analyze restrictions over time periods

#### Conditional Tag Support
- `access:conditional` - General access restrictions
- `oneway:conditional` - Directional restrictions
- `hgv:conditional` - Heavy vehicle restrictions
- `maxspeed:conditional` - Speed limit restrictions
- `parking:conditional` - Parking restrictions
- `bicycle:conditional` - Bicycle access restrictions
- `motor_vehicle:conditional` - Motor vehicle restrictions
- `foot:conditional` - Pedestrian access restrictions

## Testing and Verification

### Basic Functionality Test

1. **Plugin Loading**
   ```
   1. Start JOSM
   2. Check Preferences → Plugins → ConditionalRestrictions is enabled
   3. Verify no error messages in JOSM log
   ```

2. **GraphView Dependency**
   ```
   1. Ensure GraphView plugin is installed and enabled
   2. Check Tools menu for "Analyze Conditional Restrictions"
   3. Verify View menu contains GraphView option
   ```

3. **Sample Data Test**
   ```
   1. Load data/sample-conditional-restrictions.osm
   2. Run Tools → Analyze Conditional Restrictions
   3. Verify analysis completes without errors
   4. Check results dialog displays restrictions
   ```

### Unit Tests

Run comprehensive test suite:

```bash
# Run all tests
ant test

# View test report
open test-reports/html/index.html
```

### Integration Testing

1. **Load real OSM data** with conditional restrictions
2. **Test different vehicle profiles** and time settings
3. **Verify GraphView integration** works correctly
4. **Check validation features** identify syntax errors

## Troubleshooting

### Common Issues and Solutions

#### Plugin Not Loading

**Symptoms**: Plugin not visible in Tools menu, missing from Plugin Manager

**Solutions**:
1. **Check JOSM version**: Ensure JOSM version 18000+
   ```bash
   # Check JOSM version in Help → About
   ```

2. **Verify file location**: Confirm JAR in correct plugins directory
   ```bash
   # Windows
   dir "%APPDATA%\JOSM\plugins\ConditionalRestrictions.jar"
   
   # macOS/Linux
   ls ~/.josm/plugins/ConditionalRestrictions.jar
   ```

3. **Check JOSM log**: Look for loading errors
   ```
   View → Toggle Dev Console
   Look for ConditionalRestrictions errors
   ```

4. **Reinstall plugin**:
   ```bash
   ant clean install
   ```

#### GraphView Dependency Missing

**Symptoms**: Plugin loads but features limited, no GraphView integration

**Solutions**:
1. **Install GraphView**: Preferences → Plugins → Search "graphview"
2. **Restart JOSM** after installing GraphView
3. **Verify both plugins enabled** in Preferences → Plugins

#### Build Failures

**Symptoms**: Compilation errors, missing dependencies

**Solutions**:
1. **Check Java version**:
   ```bash
   java -version
   javac -version
   # Should be 1.8+
   ```

2. **Update JOSM JAR**: Ensure josm-custom.jar is current version
   ```bash
   # Download latest JOSM development JAR
   ```

3. **Clean rebuild**:
   ```bash
   ant clean
   ant download-test-libs
   ant dist
   ```

4. **Check classpath**: Verify all dependencies available
   ```bash
   ant validate
   ```

#### Runtime Errors

**Symptoms**: Plugin loads but crashes during use

**Solutions**:
1. **Check memory**: Increase JOSM memory allocation
   ```bash
   java -Xmx2G -jar josm.jar
   ```

2. **Enable debug mode**:
   ```bash
   java -Djosm.debug=true -jar josm.jar
   ```

3. **Check OSM data**: Verify data doesn't contain malformed conditional tags
4. **Update dependencies**: Ensure GraphView plugin is current version

#### Parsing Errors

**Symptoms**: Conditional restrictions not recognized, validation errors

**Solutions**:
1. **Check syntax**: Verify conditional restriction follows OSM format
   ```
   Correct: access:conditional = no @ (Mo-Fr 07:00-19:00)
   Incorrect: access:conditional = no @ Mo-Fr 07:00-19:00
   ```

2. **Use validation**: Tools → Validate Data to identify issues
3. **Check supported tags**: Ensure tag type is supported by plugin
4. **Review OSM wiki**: Confirm syntax matches OSM conditional restrictions specification

### Debug Mode

Enable detailed logging:

```bash
# Start JOSM with debug output
java -Djosm.debug=true -jar josm.jar

# Check debug output in console
# Look for ConditionalRestrictions debug messages
```

### Log Analysis

Check JOSM log files:
- **Windows**: `%APPDATA%\JOSM\josm.log`
- **macOS**: `~/Library/Logs/JOSM/josm.log`
- **Linux**: `~/.josm/josm.log`

### Performance Issues

If plugin is slow:
1. **Reduce dataset size**: Analyze smaller areas
2. **Increase timeout**: Preferences → Conditional Restrictions → Analysis Timeout
3. **Enable caching**: Preferences → Conditional Restrictions → Cache Settings
4. **Check memory usage**: Monitor JOSM memory consumption

## Development Setup

### Setting Up Development Environment

1. **Clone repository**:
   ```bash
   git clone https://github.com/yourusername/josm-conditional-restrictions-plugin.git
   cd josm-conditional-restrictions-plugin
   ```

2. **IDE Setup** (IntelliJ IDEA):
   - Import project as Ant project
   - Configure JDK 8+
   - Add JOSM JAR to classpath
   - Add GraphView JAR to classpath

3. **Eclipse Setup**:
   - Import as existing project
   - Configure build path with JOSM and GraphView JARs
   - Set Java compiler compliance to 1.8

### Development Workflow

1. **Make changes** to source code
2. **Run tests**:
   ```bash
   ant test
   ```
3. **Build plugin**:
   ```bash
   ant clean dist
   ```
4. **Install for testing**:
   ```bash
   ant install
   ```
5. **Test in JOSM** with sample data

### Code Style Guidelines

- Follow JOSM coding standards
- Use meaningful variable names
- Include comprehensive JavaDoc comments
- Add unit tests for new features
- Maintain backward compatibility

### Contributing Process

1. **Fork repository** on GitHub
2. **Create feature branch**: `git checkout -b feature-name`
3. **Implement changes** with tests and documentation
4. **Run full test suite**: `ant test-all`
5. **Submit pull request** with detailed description

### Debugging Tips

1. **Use debug breakpoints** in IDE
2. **Add logging statements**:
   ```java
   Main.debug("ConditionalRestrictions: " + message);
   ```
3. **Test with sample data** in `data/sample-conditional-restrictions.osm`
4. **Use JOSM development mode** for enhanced debugging

## Known Limitations

### Current Limitations

1. **Complex Condition Expressions**
   - **Issue**: Very complex boolean expressions may not parse correctly
   - **Workaround**: Break into simpler conditional statements
   - **Example**: Instead of `(Mo-Fr 07:00-09:00 AND weight>7.5) OR (Sa-Su)` use separate tags

2. **Vehicle Dimension Detection**
   - **Issue**: Plugin cannot automatically detect actual vehicle dimensions
   - **Workaround**: Manually configure vehicle profile dimensions
   - **Impact**: May not catch all applicable restrictions

3. **Real-time Data Updates**
   - **Issue**: Conditional analysis uses static time, not continuously updated
   - **Workaround**: Manually refresh analysis for current time
   - **Future**: Real-time updates planned for future versions

4. **Large Dataset Performance**
   - **Issue**: Analysis may be slow on very large datasets (>10,000 ways)
   - **Workaround**: Analyze smaller geographic areas
   - **Mitigation**: Timeout settings prevent hanging

5. **Memory Usage**
   - **Issue**: Plugin may consume significant memory on large datasets
   - **Workaround**: Increase JOSM memory allocation
   - **Recommendation**: 2GB+ RAM for large area analysis

### Compatibility Issues

1. **JOSM Version Compatibility**
   - **Requirement**: JOSM version 18000+
   - **Issue**: Older JOSM versions not supported
   - **Solution**: Update JOSM to latest stable version

2. **GraphView Dependency**
   - **Requirement**: GraphView plugin must be installed
   - **Issue**: Some features disabled without GraphView
   - **Solution**: Install GraphView from JOSM Plugin Manager

3. **Java Version Compatibility**
   - **Requirement**: Java 8+
   - **Issue**: Older Java versions not supported
   - **Solution**: Update Java to version 8 or later

### Planned Improvements

1. **Enhanced Condition Parser**
   - Support for more complex boolean expressions
   - Better error messages for syntax issues
   - Validation suggestions

2. **Real-time Updates**
   - Continuous time updates for current restrictions
   - Live restriction status indicators
   - Automatic refresh options

3. **Performance Optimization**
   - Improved memory efficiency
   - Faster parsing algorithms
   - Background processing for large datasets

4. **Extended Vehicle Profiles**
   - More detailed vehicle characteristics
   - Custom profile creation interface
   - Profile import/export functionality

5. **Enhanced GraphView Integration**
   - Visual restriction indicators on map
   - Route alternative suggestions
   - Restriction impact analysis

### Workarounds and Best Practices

1. **For Large Datasets**:
   - Work with smaller geographic areas
   - Use data filtering to reduce dataset size
   - Increase JOSM memory allocation

2. **For Complex Restrictions**:
   - Break complex conditions into simpler parts
   - Use multiple conditional tags instead of complex expressions
   - Test syntax with validation features

3. **For Performance Issues**:
   - Enable caching in plugin preferences
   - Increase analysis timeout settings
   - Close other memory-intensive applications

4. **For Development**:
   - Use sample data for initial testing
   - Test with various vehicle profiles
   - Validate changes with unit tests

## Support and Resources

### Documentation
- [OpenStreetMap Conditional Restrictions Wiki](https://wiki.openstreetmap.org/wiki/Conditional_restrictions)
- [JOSM Plugin Development Guide](https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins)
- [GraphView Plugin Documentation](https://wiki.openstreetmap.org/wiki/JOSM/Plugins/GraphView)

### Community Support
- **JOSM Mailing List**: [josm-dev@openstreetmap.org](mailto:josm-dev@openstreetmap.org)
- **OpenStreetMap Forums**: [community.openstreetmap.org](https://community.openstreetmap.org/)
- **GitHub Issues**: [Project Issues Page](https://github.com/yourusername/josm-conditional-restrictions-plugin/issues)

### Reporting Issues
When reporting issues, please include:
1. JOSM version and platform
2. Plugin version
3. Sample OSM data (if applicable)
4. Steps to reproduce
5. Error messages or log excerpts
6. Expected vs actual behavior

This comprehensive guide should help you successfully deploy and use the JOSM Conditional Restrictions Plugin across different platforms and development scenarios.