package org.openstreetmap.josm.plugins.conditionalrestrictions.routing;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction.RestrictionType;
import org.openstreetmap.josm.plugins.conditionalrestrictions.ConditionalRestrictionsPlugin;
import org.openstreetmap.josm.tools.Logging;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extended routing graph that considers conditional restrictions when building
 * routing networks. Integrates with GraphView plugin functionality to provide
 * conditional routing analysis.
 * 
 * This class extends the basic routing graph concept to handle time-dependent
 * and vehicle-specific restrictions that affect accessibility and routing costs.
 * 
 * @author CEF
 */
public class ConditionalRoutingGraph {
    
    /**
     * Vehicle profile definitions for conditional routing
     */
    public enum VehicleProfile {
        PEDESTRIAN("foot", 0.0, 0.0, 5.0),
        BICYCLE("bicycle", 0.0, 0.0, 15.0),
        CAR("motor_vehicle", 2.0, 1.8, 50.0),
        MOTORCYCLE("motorcycle", 0.3, 1.5, 50.0),
        BUS("bus", 12.0, 3.2, 50.0),
        HGV("hgv", 40.0, 4.0, 80.0),
        DELIVERY("goods", 7.5, 2.5, 50.0);
        
        private final String osmTag;
        private final double defaultWeight; // tonnes
        private final double defaultHeight; // meters  
        private final double defaultSpeed;  // km/h
        
        VehicleProfile(String osmTag, double weight, double height, double speed) {
            this.osmTag = osmTag;
            this.defaultWeight = weight;
            this.defaultHeight = height;
            this.defaultSpeed = speed;
        }
        
        public String getOsmTag() { return osmTag; }
        public double getDefaultWeight() { return defaultWeight; }
        public double getDefaultHeight() { return defaultHeight; }
        public double getDefaultSpeed() { return defaultSpeed; }
    }
    
    /**
     * Represents an edge in the routing graph with conditional properties
     */
    public static class ConditionalEdge {
        private final Way way;
        private final Node fromNode;
        private final Node toNode;
        private final List<ConditionalRestriction> restrictions;
        private final double baseWeight;
        private boolean bidirectional;
        
        public ConditionalEdge(Way way, Node fromNode, Node toNode, double baseWeight) {
            this.way = way;
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.baseWeight = baseWeight;
            this.restrictions = new ArrayList<>();
            this.bidirectional = true;
        }
        
        public void addRestriction(ConditionalRestriction restriction) {
            restrictions.add(restriction);
        }
        
        /**
         * Calculates the routing cost for this edge at a specific time with a specific vehicle
         * 
         * @param dateTime The time to evaluate conditions
         * @param profile The vehicle profile
         * @param customWeight Custom vehicle weight (null to use profile default)
         * @param customHeight Custom vehicle height (null to use profile default)
         * @return Routing cost, or Double.POSITIVE_INFINITY if blocked
         */
        public double getCost(LocalDateTime dateTime, VehicleProfile profile, 
                            Double customWeight, Double customHeight) {
            
            double vehicleWeight = customWeight != null ? customWeight : profile.getDefaultWeight();
            double vehicleHeight = customHeight != null ? customHeight : profile.getDefaultHeight();
            
            double cost = baseWeight;
            boolean accessDenied = false;
            double speedLimit = profile.getDefaultSpeed();
            
            // Check each conditional restriction
            for (ConditionalRestriction restriction : restrictions) {
                if (!restriction.isActiveAt(dateTime)) {
                    continue; // Restriction not active at this time
                }
                
                if (!restriction.appliesToVehicle(vehicleWeight, vehicleHeight)) {
                    continue; // Restriction doesn't apply to this vehicle
                }
                
                RestrictionType type = restriction.getType();
                String value = restriction.getRestrictionValue().toLowerCase();
                
                switch (type) {
                    case ACCESS:
                        if ("no".equals(value) || "private".equals(value)) {
                            // Check if it applies to our vehicle type
                            if (appliesToVehicleType(restriction, profile)) {
                                accessDenied = true;
                            }
                        }
                        break;
                        
                    case ONEWAY:
                        if ("yes".equals(value) || "-1".equals(value)) {
                            bidirectional = false;
                            // Additional logic needed to determine direction
                        }
                        break;
                        
                    case SPEED:
                        try {
                            double restrictedSpeed = Double.parseDouble(value);
                            speedLimit = Math.min(speedLimit, restrictedSpeed);
                        } catch (NumberFormatException e) {
                            Logging.warn("Invalid speed value in conditional restriction: " + value);
                        }
                        break;
                        
                    case HGV:
                        if ("no".equals(value) && profile == VehicleProfile.HGV) {
                            accessDenied = true;
                        }
                        break;
                        
                    case BICYCLE:
                        if ("no".equals(value) && profile == VehicleProfile.BICYCLE) {
                            accessDenied = true;
                        }
                        break;
                        
                    case FOOT:
                        if ("no".equals(value) && profile == VehicleProfile.PEDESTRIAN) {
                            accessDenied = true;
                        }
                        break;
                        
                    default:
                        // Handle other restriction types generically
                        if ("no".equals(value)) {
                            cost *= 1.5; // Penalty for restrictions we don't fully understand
                        }
                        break;
                }
            }
            
            if (accessDenied) {
                return Double.POSITIVE_INFINITY;
            }
            
            // Adjust cost based on speed limit
            if (speedLimit < profile.getDefaultSpeed()) {
                cost *= (profile.getDefaultSpeed() / speedLimit);
            }
            
            return cost;
        }
        
        /**
         * Checks if a restriction applies to a specific vehicle type
         */
        private boolean appliesToVehicleType(ConditionalRestriction restriction, VehicleProfile profile) {
            String baseTag = restriction.getBaseTag().toLowerCase();
            String vehicleTag = profile.getOsmTag();
            
            // Direct match
            if (baseTag.equals(vehicleTag)) {
                return true;
            }
            
            // Hierarchical matching
            switch (baseTag) {
                case "access":
                case "vehicle":
                    return true; // Applies to all vehicles
                case "motor_vehicle":
                    return profile != VehicleProfile.PEDESTRIAN && profile != VehicleProfile.BICYCLE;
                default:
                    return false;
            }
        }
        
        // Getters
        public Way getWay() { return way; }
        public Node getFromNode() { return fromNode; }
        public Node getToNode() { return toNode; }
        public double getBaseWeight() { return baseWeight; }
        public List<ConditionalRestriction> getRestrictions() { return new ArrayList<>(restrictions); }
        public boolean isBidirectional() { return bidirectional; }
    }
    
    private final Map<Node, List<ConditionalEdge>> adjacencyList;
    private final Map<Way, ConditionalEdge> wayToEdgeMap;
    private final ConditionalRestrictionsPlugin plugin;
    private LocalDateTime currentDateTime;
    private VehicleProfile currentProfile;
    private Double customWeight;
    private Double customHeight;
    
    /**
     * Creates a new conditional routing graph
     * 
     * @param plugin The conditional restrictions plugin instance
     */
    public ConditionalRoutingGraph(ConditionalRestrictionsPlugin plugin) {
        this.plugin = plugin;
        this.adjacencyList = new ConcurrentHashMap<>();
        this.wayToEdgeMap = new ConcurrentHashMap<>();
        this.currentDateTime = LocalDateTime.now();
        this.currentProfile = VehicleProfile.CAR;
    }
    
    /**
     * Builds the routing graph from a dataset
     * 
     * @param dataSet The OSM dataset to analyze
     */
    public void buildGraph(DataSet dataSet) {
        if (dataSet == null) {
            return;
        }
        
        Logging.info("Building conditional routing graph from " + dataSet.getWays().size() + " ways");
        
        adjacencyList.clear();
        wayToEdgeMap.clear();
        
        // Get restrictions from plugin
        Map<Way, List<ConditionalRestriction>> restrictions = plugin.getRestrictions();
        
        // Process each way
        for (Way way : dataSet.getWays()) {
            if (!isRoutableWay(way)) {
                continue;
            }
            
            List<Node> nodes = way.getNodes();
            if (nodes.size() < 2) {
                continue;
            }
            
            // Create edges between consecutive nodes
            for (int i = 0; i < nodes.size() - 1; i++) {
                Node fromNode = nodes.get(i);
                Node toNode = nodes.get(i + 1);
                
                double weight = calculateBaseWeight(fromNode, toNode, way);
                ConditionalEdge edge = new ConditionalEdge(way, fromNode, toNode, weight);
                
                // Add conditional restrictions if present
                List<ConditionalRestriction> wayRestrictions = restrictions.get(way);
                if (wayRestrictions != null) {
                    for (ConditionalRestriction restriction : wayRestrictions) {
                        edge.addRestriction(restriction);
                    }
                }
                
                // Add to adjacency list
                adjacencyList.computeIfAbsent(fromNode, k -> new ArrayList<>()).add(edge);
                
                // Add reverse edge if bidirectional
                if (!isOneWay(way)) {
                    ConditionalEdge reverseEdge = new ConditionalEdge(way, toNode, fromNode, weight);
                    if (wayRestrictions != null) {
                        for (ConditionalRestriction restriction : wayRestrictions) {
                            reverseEdge.addRestriction(restriction);
                        }
                    }
                    adjacencyList.computeIfAbsent(toNode, k -> new ArrayList<>()).add(reverseEdge);
                }
                
                wayToEdgeMap.put(way, edge);
            }
        }
        
        Logging.info("Built routing graph with " + adjacencyList.size() + " nodes and " + 
                    wayToEdgeMap.size() + " edges");
    }
    
    /**
     * Finds the shortest path between two nodes considering conditional restrictions
     * 
     * @param start Start node
     * @param end End node
     * @return List of nodes representing the path, or empty list if no path found
     */
    public List<Node> findShortestPath(Node start, Node end) {
        if (start == null || end == null || start.equals(end)) {
            return new ArrayList<>();
        }
        
        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Node> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>();
        Set<Node> visited = new HashSet<>();
        
        // Initialize distances
        distances.put(start, 0.0);
        queue.offer(new NodeDistance(start, 0.0));
        
        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            Node currentNode = current.node;
            
            if (visited.contains(currentNode)) {
                continue;
            }
            visited.add(currentNode);
            
            if (currentNode.equals(end)) {
                break; // Found shortest path to destination
            }
            
            List<ConditionalEdge> edges = adjacencyList.get(currentNode);
            if (edges == null) {
                continue;
            }
            
            for (ConditionalEdge edge : edges) {
                Node neighbor = edge.getToNode();
                if (visited.contains(neighbor)) {
                    continue;
                }
                
                double edgeCost = edge.getCost(currentDateTime, currentProfile, customWeight, customHeight);
                if (edgeCost == Double.POSITIVE_INFINITY) {
                    continue; // Edge is blocked
                }
                
                double newDistance = distances.get(currentNode) + edgeCost;
                double currentDistance = distances.getOrDefault(neighbor, Double.POSITIVE_INFINITY);
                
                if (newDistance < currentDistance) {
                    distances.put(neighbor, newDistance);
                    previous.put(neighbor, currentNode);
                    queue.offer(new NodeDistance(neighbor, newDistance));
                }
            }
        }
        
        // Reconstruct path
        List<Node> path = new ArrayList<>();
        Node current = end;
        
        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }
        
        // Return empty list if no path found
        if (path.isEmpty() || !path.get(0).equals(start)) {
            return new ArrayList<>();
        }
        
        return path;
    }
    
    /**
     * Gets all edges that are affected by conditional restrictions at the current time
     * 
     * @return List of edges with active restrictions
     */
    public List<ConditionalEdge> getAffectedEdges() {
        List<ConditionalEdge> affected = new ArrayList<>();
        
        for (List<ConditionalEdge> edges : adjacencyList.values()) {
            for (ConditionalEdge edge : edges) {
                boolean hasActiveRestriction = false;
                
                for (ConditionalRestriction restriction : edge.getRestrictions()) {
                    if (restriction.isActiveAt(currentDateTime) && 
                        restriction.appliesToVehicle(customWeight, customHeight)) {
                        hasActiveRestriction = true;
                        break;
                    }
                }
                
                if (hasActiveRestriction) {
                    affected.add(edge);
                }
            }
        }
        
        return affected;
    }
    
    /**
     * Sets the current time for conditional evaluation
     */
    public void setCurrentDateTime(LocalDateTime dateTime) {
        this.currentDateTime = dateTime != null ? dateTime : LocalDateTime.now();
    }
    
    /**
     * Sets the current vehicle profile
     */
    public void setVehicleProfile(VehicleProfile profile) {
        this.currentProfile = profile != null ? profile : VehicleProfile.CAR;
    }
    
    /**
     * Sets custom vehicle dimensions
     */
    public void setCustomVehicleDimensions(Double weight, Double height) {
        this.customWeight = weight;
        this.customHeight = height;
    }
    
    /**
     * Determines if a way is suitable for routing
     */
    private boolean isRoutableWay(Way way) {
        if (way.isIncomplete() || way.getNodes().size() < 2) {
            return false;
        }
        
        String highway = way.get("highway");
        if (highway == null) {
            return false;
        }
        
        // Exclude certain highway types
        Set<String> excludedTypes = Set.of("proposed", "construction", "abandoned", "razed");
        return !excludedTypes.contains(highway);
    }
    
    /**
     * Checks if a way is one-way
     */
    private boolean isOneWay(Way way) {
        String oneway = way.get("oneway");
        return "yes".equals(oneway) || "true".equals(oneway) || "1".equals(oneway) || "-1".equals(oneway);
    }
    
    /**
     * Calculates base weight for an edge (e.g., based on distance)
     */
    private double calculateBaseWeight(Node fromNode, Node toNode, Way way) {
        if (fromNode.getCoor() == null || toNode.getCoor() == null) {
            return 1.0; // Default weight if coordinates unavailable
        }
        
        // Calculate distance in meters
        double distance = fromNode.getCoor().greatCircleDistance(toNode.getCoor());
        
        // Apply highway-based speed factor
        String highway = way.get("highway");
        double speedFactor = getSpeedFactor(highway);
        
        return distance / speedFactor;
    }
    
    /**
     * Gets speed factor based on highway type
     */
    private double getSpeedFactor(String highway) {
        if (highway == null) return 1.0;
        
        switch (highway) {
            case "motorway": return 4.0;
            case "trunk": return 3.5;
            case "primary": return 3.0;
            case "secondary": return 2.5;
            case "tertiary": return 2.0;
            case "residential": return 1.5;
            case "service": return 1.2;
            case "footway":
            case "path": return 0.8;
            default: return 1.0;
        }
    }
    
    // Getters
    public LocalDateTime getCurrentDateTime() { return currentDateTime; }
    public VehicleProfile getCurrentProfile() { return currentProfile; }
    public Double getCustomWeight() { return customWeight; }
    public Double getCustomHeight() { return customHeight; }
    public Map<Node, List<ConditionalEdge>> getAdjacencyList() { return new HashMap<>(adjacencyList); }
    
    /**
     * Helper class for Dijkstra's algorithm
     */
    private static class NodeDistance implements Comparable<NodeDistance> {
        final Node node;
        final double distance;
        
        NodeDistance(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}