# GraphView Plugin Integration

This document explains how the ConditionalRestrictions plugin integrates with the GraphView plugin to provide conditional routing analysis.

## Overview

The GraphView integration allows conditional restrictions to be considered during routing analysis in JOSM. This enables users to see how time-based and vehicle-specific restrictions affect route planning and accessibility.

## Integration Architecture

### Graceful Degradation
The integration is designed to work gracefully whether GraphView is present or not:

- **With GraphView**: Provides enhanced routing analysis with conditional restrictions
- **Without GraphView**: Works independently using the built-in ConditionalRoutingGraph

### Key Components

1. **GraphViewIntegration**: Main integration class that handles detection and communication with GraphView
2. **ConditionalRoutingGraph**: Core routing engine that considers conditional restrictions
3. **Reflection-based Integration**: Uses reflection to avoid hard dependencies on GraphView

### Integration Points

The plugin provides several extension hooks to GraphView:

#### 1. Routing Graph Provider
- Method: `getConditionalRoutingGraph(Object dataSet)`
- Provides GraphView with a routing graph that considers conditional restrictions
- Returns: ConditionalRoutingGraph instance

#### 2. Cost Calculator
- Method: `calculateConditionalCost(Object way, Object fromNode, Object toNode, String vehicleProfile, String dateTime)`
- Calculates routing costs considering active conditional restrictions
- Returns: Cost value (Double.POSITIVE_INFINITY if blocked)

#### 3. Affected Ways Provider
- Method: `getAffectedWays(Object dataSet, String vehicleProfile, String dateTime)`
- Identifies ways with active conditional restrictions for the given parameters
- Returns: List of way IDs

## Vehicle Profiles

The integration supports multiple vehicle profiles:

- **CAR** (motor_vehicle)
- **BICYCLE** 
- **PEDESTRIAN** (foot)
- **HGV** (truck)
- **BUS**
- **MOTORCYCLE**
- **DELIVERY** (goods)

## Supported Conditional Tags

The integration analyzes these conditional restriction tags:

- `access:conditional`
- `oneway:conditional`
- `hgv:conditional`
- `maxspeed:conditional`
- `parking:conditional`
- `bicycle:conditional`
- `motor_vehicle:conditional`
- `foot:conditional`

## Usage Examples

### Time-based Restrictions
```
access:conditional = no @ (Mo-Fr 07:00-09:00)
```
Blocks access for all vehicles during weekday morning rush hours.

### Vehicle-specific Restrictions
```
hgv:conditional = no @ (22:00-06:00)
```
Blocks heavy goods vehicles during night hours.

### Combined Conditions
```
motor_vehicle:conditional = no @ (Mo-Fr 15:00-18:00 AND weight>3.5)
```
Blocks heavy motor vehicles during weekday evening rush hours.

## Technical Implementation

### Plugin Detection
The integration uses reflection to detect GraphView:

```java
graphViewClass = Class.forName("org.openstreetmap.josm.plugins.graphview.core.GraphViewPlugin");
```

### Method Registration
Extension hooks are registered using reflection:

```java
Method registerProvider = findMethod("registerRoutingGraphProvider", Object.class);
registerProvider.invoke(graphViewPlugin, this);
```

### Safe Parameter Handling
All external method calls use Object parameters and reflection for type safety:

```java
if (dataSet.getClass().getSimpleName().equals("DataSet")) {
    // Safe to process
}
```

## Error Handling

The integration includes comprehensive error handling:

- **ClassNotFoundException**: GraphView not installed - continues independently
- **Method Invocation Errors**: Falls back to basic routing
- **Parameter Type Mismatches**: Uses default values and logs warnings

## Logging

Integration status is logged to help with debugging:

- Plugin detection results
- Extension hook registration status
- Error conditions and fallback behavior

## Future Enhancements

Potential areas for future development:

1. **Dynamic Parameter Updates**: Real-time parameter changes from GraphView UI
2. **Visual Indication**: Highlight restricted ways in GraphView display
3. **Route Alternatives**: Suggest alternative routes when restrictions block paths
4. **Performance Optimization**: Cache restriction evaluations for better performance

## Testing

The integration can be tested by:

1. Installing both ConditionalRestrictions and GraphView plugins
2. Loading OSM data with conditional restriction tags
3. Using GraphView's routing analysis features
4. Checking that conditional restrictions are considered in route planning

Without GraphView installed, the plugin should work normally using its own routing analysis features.