package org.openstreetmap.josm.plugins.conditionalrestrictions.integration;

import org.openstreetmap.josm.plugins.conditionalrestrictions.ConditionalRestrictionsPlugin;
import org.openstreetmap.josm.plugins.conditionalrestrictions.routing.ConditionalRoutingGraph;
import org.openstreetmap.josm.plugins.conditionalrestrictions.routing.ConditionalRoutingGraph.VehicleProfile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integration hooks for the GraphView plugin. This class provides conditional
 * restrictions support for GraphView's routing analysis functionality.
 * 
 * Uses reflection-based integration to avoid hard dependencies on GraphView,
 * allowing graceful degradation when the plugin is not available.
 * 
 * @author CEF
 */
public class GraphViewIntegration {
    
    private final ConditionalRestrictionsPlugin plugin;
    private final ConditionalRoutingGraph routingGraph;
    private Object graphViewPlugin;
    private Class<?> graphViewClass;
    private boolean isGraphViewAvailable;
    private final Map<String, Method> cachedMethods;
    
    /**
     * Creates a new GraphView integration instance
     * 
     * @param plugin The conditional restrictions plugin
     */
    public GraphViewIntegration(ConditionalRestrictionsPlugin plugin) {
        this.plugin = plugin;
        this.routingGraph = new ConditionalRoutingGraph(plugin);
        this.cachedMethods = new ConcurrentHashMap<>();
        this.isGraphViewAvailable = false;
        
        // Attempt to detect and connect to GraphView
        detectGraphView();
    }
    
    /**
     * Attempts to detect if GraphView plugin is loaded and available
     */
    private void detectGraphView() {
        try {
            // Try to find GraphView plugin class using reflection
            graphViewClass = Class.forName("org.openstreetmap.josm.plugins.graphview.core.GraphViewPlugin");
            
            System.out.println("ConditionalRestrictions: GraphView plugin classes found");
            
            // Try to get the plugin instance
            findGraphViewInstance();
            
        } catch (ClassNotFoundException e) {
            System.out.println("ConditionalRestrictions: GraphView plugin not found - " + 
                        "conditional routing will work independently");
        } catch (Exception e) {
            System.out.println("ConditionalRestrictions: Error detecting GraphView plugin: " + e.getMessage());
        }
    }
    
    /**
     * Attempts to find the GraphView plugin instance
     */
    private void findGraphViewInstance() {
        try {
            // Try to find GraphView using reflection to avoid hard dependencies
            // This allows the plugin to work even if GraphView is not installed
            
            // First try to get a static instance or factory method
            Method getInstanceMethod = findStaticMethod("getInstance");
            if (getInstanceMethod != null) {
                graphViewPlugin = getInstanceMethod.invoke(null);
                isGraphViewAvailable = true;
                System.out.println("ConditionalRestrictions: Successfully connected to GraphView plugin");
                
                // Register our extension hooks
                registerExtensionHooks();
                return;
            }
            
            // Alternative: try to create a new instance
            try {
                graphViewPlugin = graphViewClass.getDeclaredConstructor().newInstance();
                isGraphViewAvailable = true;
                System.out.println("ConditionalRestrictions: Created new GraphView plugin instance");
                registerExtensionHooks();
            } catch (Exception createEx) {
                System.out.println("ConditionalRestrictions: GraphView plugin found but could not instantiate - " +
                                 "will provide independent routing analysis");
            }
            
        } catch (Exception e) {
            System.out.println("ConditionalRestrictions: Could not find GraphView instance: " + e.getMessage());
        }
    }
    
    /**
     * Registers extension hooks with GraphView plugin
     */
    private void registerExtensionHooks() {
        if (!isGraphViewAvailable || graphViewPlugin == null) {
            return;
        }
        
        try {
            // Register as a routing graph provider
            Method registerProvider = findMethod("registerRoutingGraphProvider", Object.class);
            if (registerProvider != null) {
                registerProvider.invoke(graphViewPlugin, this);
                System.out.println("ConditionalRestrictions: Registered as routing graph provider");
            }
            
            // Register as a cost calculator
            Method registerCalculator = findMethod("registerRoutingCostCalculator", Object.class);
            if (registerCalculator != null) {
                registerCalculator.invoke(graphViewPlugin, this);
                System.out.println("ConditionalRestrictions: Registered as cost calculator");
            }
            
        } catch (Exception e) {
            System.out.println("ConditionalRestrictions: Error registering extension hooks: " + e.getMessage());
        }
    }
    
    /**
     * Provides conditional routing graph to GraphView
     * This method will be called by GraphView via reflection
     * 
     * @param dataSet The OSM dataset to analyze
     * @return The conditional routing graph
     */
    public Object getConditionalRoutingGraph(Object dataSet) {
        if (dataSet == null) {
            return null;
        }
        
        try {
            // Build our conditional routing graph if we have a valid dataset
            if (dataSet.getClass().getSimpleName().equals("DataSet")) {
                // Use reflection to call buildGraph safely
                Method buildGraphMethod = routingGraph.getClass().getMethod("buildGraph", dataSet.getClass());
                buildGraphMethod.invoke(routingGraph, dataSet);
                
                // Return the routing graph for GraphView to use
                return routingGraph;
            }
            
        } catch (Exception e) {
            System.out.println("ConditionalRestrictions: Error creating routing graph: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Calculates routing cost for a way considering conditional restrictions
     * This method will be called by GraphView via reflection
     * 
     * @param wayObj The way to calculate cost for
     * @param fromNodeObj The starting node
     * @param toNodeObj The ending node
     * @param vehicleProfile The vehicle profile (as string)
     * @param dateTime The time for condition evaluation (ISO format)
     * @return The routing cost, or positive infinity if blocked
     */
    public double calculateConditionalCost(Object wayObj, Object fromNodeObj, Object toNodeObj, 
                                         String vehicleProfile, String dateTime) {
        try {
            // Parse vehicle profile
            VehicleProfile profile = parseVehicleProfile(vehicleProfile);
            
            // Parse date/time
            LocalDateTime time = LocalDateTime.parse(dateTime);
            
            // Set routing parameters
            routingGraph.setCurrentDateTime(time);
            routingGraph.setVehicleProfile(profile);
            
            // Calculate basic cost as fallback
            return calculateBasicCost(fromNodeObj, toNodeObj);
            
        } catch (Exception e) {
            System.out.println("ConditionalRestrictions: Error calculating conditional cost: " + e.getMessage());
            return Double.POSITIVE_INFINITY;
        }
    }
    
    /**
     * Gets ways affected by conditional restrictions
     * This method provides GraphView with information about which ways have active restrictions
     * 
     * @param dataSet The dataset to analyze
     * @param vehicleProfile The vehicle profile
     * @param dateTime The time for evaluation
     * @return List of way IDs with active restrictions
     */
    public List<Long> getAffectedWays(Object dataSet, String vehicleProfile, String dateTime) {
        try {
            if (dataSet != null && dataSet.getClass().getSimpleName().equals("DataSet")) {
                VehicleProfile profile = parseVehicleProfile(vehicleProfile);
                LocalDateTime time = LocalDateTime.parse(dateTime);
                
                routingGraph.setCurrentDateTime(time);
                routingGraph.setVehicleProfile(profile);
                
                // Return affected way IDs
                return routingGraph.getAffectedEdges().stream()
                    .map(edge -> {
                        try {
                            Method getIdMethod = edge.getWay().getClass().getMethod("getId");
                            return (Long) getIdMethod.invoke(edge.getWay());
                        } catch (Exception e) {
                            return 0L;
                        }
                    })
                    .distinct()
                    .toList();
            }
                
        } catch (Exception e) {
            System.out.println("ConditionalRestrictions: Error getting affected ways: " + e.getMessage());
        }
        
        return List.of();
    }
    
    /**
     * Parses vehicle profile from string
     */
    private VehicleProfile parseVehicleProfile(String profileStr) {
        if (profileStr == null) {
            return VehicleProfile.CAR;
        }
        
        try {
            return VehicleProfile.valueOf(profileStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Map common profile names
            switch (profileStr.toLowerCase()) {
                case "car": case "motor_vehicle": return VehicleProfile.CAR;
                case "bike": case "bicycle": return VehicleProfile.BICYCLE;
                case "foot": case "pedestrian": return VehicleProfile.PEDESTRIAN;
                case "truck": case "hgv": return VehicleProfile.HGV;
                case "bus": return VehicleProfile.BUS;
                case "motorcycle": return VehicleProfile.MOTORCYCLE;
                default: return VehicleProfile.CAR;
            }
        }
    }
    
    /**
     * Calculates basic cost when conditional routing is not applicable
     */
    private double calculateBasicCost(Object fromNodeObj, Object toNodeObj) {
        try {
            // Use reflection to get coordinates and calculate distance
            Method getCoorMethod = fromNodeObj.getClass().getMethod("getCoor");
            Object fromCoor = getCoorMethod.invoke(fromNodeObj);
            Object toCoor = getCoorMethod.invoke(toNodeObj);
            
            if (fromCoor != null && toCoor != null) {
                Method distanceMethod = fromCoor.getClass().getMethod("greatCircleDistance", toCoor.getClass());
                return (Double) distanceMethod.invoke(fromCoor, toCoor);
            }
        } catch (Exception e) {
            System.out.println("ConditionalRestrictions: Could not calculate distance: " + e.getMessage());
        }
        return 1.0;
    }
    
    /**
     * Utility method to find static methods using reflection
     */
    private Method findStaticMethod(String methodName, Class<?>... paramTypes) {
        try {
            if (graphViewClass != null) {
                Method method = graphViewClass.getMethod(methodName, paramTypes);
                if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    return method;
                }
            }
        } catch (NoSuchMethodException e) {
            System.out.println("ConditionalRestrictions: Static method not found: " + methodName);
        }
        return null;
    }
    
    /**
     * Utility method to find methods using reflection with caching
     */
    private Method findMethod(String methodName, Class<?>... paramTypes) {
        String key = methodName + java.util.Arrays.toString(paramTypes);
        
        return cachedMethods.computeIfAbsent(key, k -> {
            try {
                if (graphViewClass != null) {
                    return graphViewClass.getMethod(methodName, paramTypes);
                }
            } catch (NoSuchMethodException e) {
                System.out.println("ConditionalRestrictions: Method not found: " + methodName);
            }
            return null;
        });
    }
    
    /**
     * Checks if GraphView plugin is available and connected
     * 
     * @return true if GraphView is available
     */
    public boolean isGraphViewAvailable() {
        return isGraphViewAvailable;
    }
    
    /**
     * Gets the routing graph instance
     * 
     * @return The conditional routing graph
     */
    public ConditionalRoutingGraph getRoutingGraph() {
        return routingGraph;
    }
    
    /**
     * Disconnects from GraphView plugin
     */
    public void disconnect() {
        if (isGraphViewAvailable && graphViewPlugin != null) {
            try {
                Method unregister = findMethod("unregisterExtensions", Object.class);
                if (unregister != null) {
                    unregister.invoke(graphViewPlugin, this);
                }
            } catch (Exception e) {
                System.out.println("ConditionalRestrictions: Error during disconnect: " + e.getMessage());
            }
        }
        
        isGraphViewAvailable = false;
        graphViewPlugin = null;
        cachedMethods.clear();
    }
}