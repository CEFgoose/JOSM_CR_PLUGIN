# JOSM Conditional Restrictions Plugin - Development Guide

## Overview

This plugin extends JOSM's GraphView functionality to support conditional restrictions for routing analysis. It enables power users to visualize and validate routing graphs that consider time-based and condition-based access restrictions.

## Quick Start

### Building the Plugin
```bash
cd josm-conditional-restrictions-plugin
ant clean build
```

### Running Tests
```bash
ant test
```

### Installing in JOSM
```bash
ant install
```

Then restart JOSM and enable the plugin in Preferences → Plugins.

## Architecture

### Core Components

- **ConditionalRestrictionsPlugin.java** - Main plugin class, integrates with JOSM
- **ConditionalRestrictionParser.java** - Parses OSM conditional restriction syntax
- **ConditionalRoutingGraph.java** - Routing graph with conditional restriction support
- **ConditionalParametersPanel.java** - UI for selecting analysis parameters
- **ConditionalValidator.java** - Validates conditional restriction syntax
- **TimeUtils.java** - Time parsing and evaluation utilities

### Data Models

- **ConditionalRestriction.java** - Represents a parsed conditional restriction
- **TimeCondition.java** - Represents time-based conditions (days, hours)

### User Interface

- **ConditionalRestrictionsDialog.java** - Results display dialog
- **AnalyzeConditionalRestrictionsAction.java** - Menu action (Tools → Analyze Conditional Restrictions)
- **ConditionalRestrictionsPreferences.java** - Plugin configuration panel

## Features

### Supported Conditional Tags
- `access:conditional` - General access restrictions
- `oneway:conditional` - One-way restrictions
- `hgv:conditional` - Heavy goods vehicle restrictions
- `maxspeed:conditional` - Speed limit restrictions
- `parking:conditional` - Parking restrictions
- `bicycle:conditional` - Bicycle access restrictions
- `motor_vehicle:conditional` - Motor vehicle restrictions
- `foot:conditional` - Pedestrian access restrictions

### Condition Syntax Support
- **Time ranges**: `07:00-19:00`, `22:00-06:00` (overnight)
- **Day ranges**: `Mo-Fr`, `Sa-Su`, individual days
- **Complex conditions**: `Mo-Fr 07:00-19:00; Sa 08:00-14:00`
- **Vehicle conditions**: `weight>7.5`, `height<3.5`
- **Multiple conditions**: `(Mo-Fr 07:00-19:00) AND (weight<3.5)`

### Vehicle Profiles
- **PEDESTRIAN** - Walking routes
- **BICYCLE** - Cycling routes with bike-specific restrictions
- **CAR** - Standard car routing
- **MOTORCYCLE** - Motorcycle-specific routing
- **BUS** - Public transport routing
- **HGV** - Heavy goods vehicle routing
- **DELIVERY** - Delivery vehicle routing

## Usage

### Basic Analysis
1. Load OSM data in JOSM
2. Go to Tools → Analyze Conditional Restrictions
3. Select vehicle type and current time
4. View results in the analysis dialog

### Advanced Features
1. **Custom Time Analysis** - Set specific date/time to see how restrictions change
2. **Vehicle Customization** - Override default vehicle dimensions (weight, height)
3. **Condition Testing** - Test custom conditional expressions
4. **Syntax Validation** - Automatic validation with error suggestions

## Development

### Adding New Condition Types
1. Extend `ConditionalRestrictionParser.parseCondition()`
2. Add new condition class implementing appropriate interface
3. Update `ConditionalRoutingGraph` to handle new condition type
4. Add tests for new functionality

### Extending Vehicle Profiles
1. Add new profile to `VehicleProfile` enum in `ConditionalRoutingGraph`
2. Define default dimensions and allowed ways
3. Update UI dropdown in `ConditionalParametersPanel`
4. Add test cases for new profile

### UI Customization
1. Modify `ConditionalParametersPanel` for new input fields
2. Update `ConditionalRestrictionsDialog` for new result displays
3. Add internationalization keys in `plugin.properties`
4. Update preferences in `ConditionalRestrictionsPreferences`

## Testing

### Unit Tests
- `ConditionalRestrictionParserTest` - Parser functionality
- `ConditionalRoutingGraphTest` - Routing graph behavior
- `TimeUtilsTest` - Time parsing and evaluation

### Test Data
- `data/sample-conditional-restrictions.osm` - Comprehensive test dataset
- Covers all major conditional restriction types
- Real-world scenarios for validation

### Running Tests
```bash
# Run all tests
ant test

# Run specific test
ant test-single -Dtest.class=ConditionalRestrictionParserTest

# Clean build and test
ant test-all
```

## Configuration

### Plugin Preferences
Access via JOSM → Preferences → Plugins → Conditional Restrictions:

- **Default Vehicle Type** - Vehicle profile for analysis
- **Auto-validate on Load** - Automatic syntax validation
- **Highlight Active Restrictions** - Visual indicators on map
- **Analysis Timeout** - Maximum analysis time
- **Cache Settings** - Performance optimization
- **Display Options** - Result formatting preferences

### Custom Vehicle Profiles
Define custom vehicle characteristics:
- Weight and height limits
- Speed preferences
- Allowed way types
- Restriction exemptions

## Performance

### Optimization Features
- **Condition Caching** - Parsed conditions cached for reuse
- **Lazy Evaluation** - Conditions evaluated only when needed
- **Graph Pruning** - Irrelevant edges excluded from analysis
- **Timeout Protection** - Analysis limited by configurable timeout

### Large Dataset Handling
- Progressive analysis for large areas
- Memory-efficient data structures
- Background processing where possible
- User feedback during long operations

## Troubleshooting

### Common Issues

**Plugin Not Loading**
- Check JOSM log for errors
- Verify JOSM version compatibility
- Ensure all dependencies available

**Parsing Errors**
- Check conditional restriction syntax
- Use validator to identify issues
- Refer to OSM conditional restrictions wiki

**Performance Issues**
- Reduce analysis area
- Increase timeout settings
- Enable caching options
- Check available memory

### Debug Mode
Enable debug logging:
```bash
java -Djosm.debug=true -jar josm.jar
```

## Contributing

### Code Style
- Follow JOSM coding standards
- Use meaningful variable names
- Include comprehensive JavaDoc
- Add unit tests for new features

### Pull Requests
1. Fork the repository
2. Create feature branch
3. Implement changes with tests
4. Update documentation
5. Submit pull request

## License

This plugin is released under the same license as JOSM (GPL v2 or later).

## Support

For issues and feature requests:
1. Check existing GitHub issues
2. Review documentation and examples
3. Submit detailed issue reports
4. Include sample data and steps to reproduce

## Version History

- **v1.0.0** - Initial release with basic conditional restriction support
- **v1.1.0** - Added vehicle profiles and advanced time conditions
- **v1.2.0** - Enhanced UI and validation features
- **v2.0.0** - Full GraphView integration and routing analysis