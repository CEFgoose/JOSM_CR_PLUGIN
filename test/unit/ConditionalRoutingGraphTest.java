package org.openstreetmap.josm.plugins.conditionalrestrictions.test;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.conditionalrestrictions.routing.ConditionalRoutingGraph;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser.ParseException;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.*;

/**
 * Comprehensive unit tests for ConditionalRoutingGraph functionality.
 * Tests routing graph construction, vehicle profile handling, and edge cases
 * for conditional restrictions in various scenarios.
 * 
 * @author CEF
 */
public class ConditionalRoutingGraphTest {
    
    private ConditionalRoutingGraph routingGraph;
    private ConditionalRestrictionParser parser;
    private DataSet dataSet;
    
    @Before
    public void setUp() {
        routingGraph = new ConditionalRoutingGraph();
        parser = new ConditionalRestrictionParser();
        dataSet = new DataSet();
    }
    
    @After
    public void tearDown() {
        if (dataSet != null) {
            dataSet.clear();
        }
    }
    
    /**
     * Creates a simple test network with nodes and ways
     */
    private void createTestNetwork() {
        // Create nodes for a simple network
        Node node1 = new Node(new LatLon(52.0, 13.0));
        Node node2 = new Node(new LatLon(52.001, 13.0));
        Node node3 = new Node(new LatLon(52.0, 13.001));
        Node node4 = new Node(new LatLon(52.001, 13.001));
        
        dataSet.addPrimitive(node1);
        dataSet.addPrimitive(node2);
        dataSet.addPrimitive(node3);
        dataSet.addPrimitive(node4);
        
        // Create ways connecting the nodes
        Way way1 = new Way();
        way1.addNode(node1);
        way1.addNode(node2);
        way1.put("highway", "primary");
        dataSet.addPrimitive(way1);
        
        Way way2 = new Way();
        way2.addNode(node2);
        way2.addNode(node4);
        way2.put("highway", "secondary");
        dataSet.addPrimitive(way2);
        
        Way way3 = new Way();
        way3.addNode(node1);
        way3.addNode(node3);
        way3.put("highway", "residential");
        dataSet.addPrimitive(way3);
        
        Way way4 = new Way();
        way4.addNode(node3);
        way4.addNode(node4);
        way4.put("highway", "tertiary");
        dataSet.addPrimitive(way4);
    }
    
    @Test
    public void testBasicGraphConstruction() {
        createTestNetwork();
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        // Verify graph was built
        assertNotNull(routingGraph);
        assertTrue("Graph should have nodes", routingGraph.getNodeCount() > 0);
        assertTrue("Graph should have edges", routingGraph.getEdgeCount() > 0);
        
        // Should have 4 nodes and 4 ways (edges)
        assertEquals("Should have 4 nodes", 4, routingGraph.getNodeCount());
        assertEquals("Should have 4 edges", 4, routingGraph.getEdgeCount());
    }
    
    @Test
    public void testGraphWithTimeBasedRestrictions() throws ParseException {
        createTestNetwork();
        
        // Add time-based access restriction to one way
        Way restrictedWay = dataSet.getWays().iterator().next();
        restrictedWay.put("access:conditional", "no @ (Mo-Fr 07:00-19:00)");
        
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        ConditionalRestriction restriction = parser.parse("access:conditional", "no @ (Mo-Fr 07:00-19:00)");
        restriction.setWay(restrictedWay);
        restrictions.put(restrictedWay, Arrays.asList(restriction));
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        // Test routing during restricted hours (Monday 10:00)
        LocalDateTime mondayMorning = LocalDateTime.of(2023, 10, 16, 10, 0); // Monday
        assertFalse("Way should be restricted on Monday morning", 
                   routingGraph.isWayAccessible(restrictedWay, "car", mondayMorning));
        
        // Test routing outside restricted hours (Monday 20:00)
        LocalDateTime mondayEvening = LocalDateTime.of(2023, 10, 16, 20, 0); // Monday
        assertTrue("Way should be accessible on Monday evening", 
                  routingGraph.isWayAccessible(restrictedWay, "car", mondayEvening));
        
        // Test routing on weekend (Saturday 10:00)
        LocalDateTime saturdayMorning = LocalDateTime.of(2023, 10, 14, 10, 0); // Saturday
        assertTrue("Way should be accessible on Saturday", 
                  routingGraph.isWayAccessible(restrictedWay, "car", saturdayMorning));
    }
    
    @Test
    public void testVehicleSpecificRestrictions() throws ParseException {
        createTestNetwork();
        
        Way restrictedWay = dataSet.getWays().iterator().next();
        restrictedWay.put("hgv:conditional", "no @ (Mo-Su 00:00-24:00)");
        
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        ConditionalRestriction restriction = parser.parse("hgv:conditional", "no @ (Mo-Su 00:00-24:00)");
        restriction.setWay(restrictedWay);
        restrictions.put(restrictedWay, Arrays.asList(restriction));
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        LocalDateTime testTime = LocalDateTime.of(2023, 10, 16, 10, 0);
        
        // HGV should be restricted
        assertFalse("HGV should be restricted", 
                   routingGraph.isWayAccessible(restrictedWay, "hgv", testTime));
        
        // Car should still be accessible
        assertTrue("Car should be accessible", 
                  routingGraph.isWayAccessible(restrictedWay, "car", testTime));
        
        // Bicycle should be accessible
        assertTrue("Bicycle should be accessible", 
                  routingGraph.isWayAccessible(restrictedWay, "bicycle", testTime));
    }
    
    @Test
    public void testMaxspeedRestrictions() throws ParseException {
        createTestNetwork();
        
        Way speedRestrictedWay = dataSet.getWays().iterator().next();
        speedRestrictedWay.put("maxspeed", "50");
        speedRestrictedWay.put("maxspeed:conditional", "30 @ (Mo-Fr 07:00-19:00)");
        
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        ConditionalRestriction restriction = parser.parse("maxspeed:conditional", "30 @ (Mo-Fr 07:00-19:00)");
        restriction.setWay(speedRestrictedWay);
        restrictions.put(speedRestrictedWay, Arrays.asList(restriction));
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        // Test speed during restricted hours
        LocalDateTime mondayMorning = LocalDateTime.of(2023, 10, 16, 10, 0);
        assertEquals("Speed should be 30 during restricted hours", 
                    30, routingGraph.getEffectiveMaxSpeed(speedRestrictedWay, mondayMorning));
        
        // Test speed outside restricted hours
        LocalDateTime mondayEvening = LocalDateTime.of(2023, 10, 16, 20, 0);
        assertEquals("Speed should be 50 outside restricted hours", 
                    50, routingGraph.getEffectiveMaxSpeed(speedRestrictedWay, mondayEvening));
    }
    
    @Test
    public void testOnewayConditionalRestrictions() throws ParseException {
        createTestNetwork();
        
        Way onewayWay = dataSet.getWays().iterator().next();
        onewayWay.put("oneway", "no");
        onewayWay.put("oneway:conditional", "yes @ (Mo-Fr 07:00-19:00)");
        
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        ConditionalRestriction restriction = parser.parse("oneway:conditional", "yes @ (Mo-Fr 07:00-19:00)");
        restriction.setWay(onewayWay);
        restrictions.put(onewayWay, Arrays.asList(restriction));
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        // Test oneway during restricted hours
        LocalDateTime mondayMorning = LocalDateTime.of(2023, 10, 16, 10, 0);
        assertTrue("Way should be oneway during restricted hours", 
                  routingGraph.isWayOneway(onewayWay, mondayMorning));
        
        // Test bidirectional outside restricted hours
        LocalDateTime mondayEvening = LocalDateTime.of(2023, 10, 16, 20, 0);
        assertFalse("Way should be bidirectional outside restricted hours", 
                   routingGraph.isWayOneway(onewayWay, mondayEvening));
    }
    
    @Test
    public void testMultipleConditionalRestrictions() throws ParseException {
        createTestNetwork();
        
        Way multiRestrictedWay = dataSet.getWays().iterator().next();
        multiRestrictedWay.put("access:conditional", "no @ (Mo-Fr 07:00-19:00; Sa 08:00-16:00)");
        multiRestrictedWay.put("maxspeed:conditional", "30 @ (22:00-06:00)");
        
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        List<ConditionalRestriction> wayRestrictions = new ArrayList<>();
        
        ConditionalRestriction accessRestriction = parser.parse("access:conditional", 
            "no @ (Mo-Fr 07:00-19:00; Sa 08:00-16:00)");
        accessRestriction.setWay(multiRestrictedWay);
        wayRestrictions.add(accessRestriction);
        
        ConditionalRestriction speedRestriction = parser.parse("maxspeed:conditional", "30 @ (22:00-06:00)");
        speedRestriction.setWay(multiRestrictedWay);
        wayRestrictions.add(speedRestriction);
        
        restrictions.put(multiRestrictedWay, wayRestrictions);
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        // Test Monday morning - access restricted, normal speed
        LocalDateTime mondayMorning = LocalDateTime.of(2023, 10, 16, 10, 0);
        assertFalse("Access should be restricted Monday morning", 
                   routingGraph.isWayAccessible(multiRestrictedWay, "car", mondayMorning));
        
        // Test Monday night - access allowed, speed restricted
        LocalDateTime mondayNight = LocalDateTime.of(2023, 10, 16, 23, 0);
        assertTrue("Access should be allowed Monday night", 
                  routingGraph.isWayAccessible(multiRestrictedWay, "car", mondayNight));
        assertEquals("Speed should be 30 at night", 
                    30, routingGraph.getEffectiveMaxSpeed(multiRestrictedWay, mondayNight));
        
        // Test Saturday afternoon - access restricted
        LocalDateTime saturdayAfternoon = LocalDateTime.of(2023, 10, 14, 14, 0);
        assertFalse("Access should be restricted Saturday afternoon", 
                   routingGraph.isWayAccessible(multiRestrictedWay, "car", saturdayAfternoon));
    }
    
    @Test
    public void testWeightRestrictions() throws ParseException {
        createTestNetwork();
        
        Way weightRestrictedWay = dataSet.getWays().iterator().next();
        weightRestrictedWay.put("hgv:conditional", "no @ (weight>7.5)");
        
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        ConditionalRestriction restriction = parser.parse("hgv:conditional", "no @ (weight>7.5)");
        restriction.setWay(weightRestrictedWay);
        restrictions.put(weightRestrictedWay, Arrays.asList(restriction));
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        LocalDateTime testTime = LocalDateTime.of(2023, 10, 16, 10, 0);
        
        // Test with heavy vehicle (>7.5t)
        assertFalse("Heavy HGV should be restricted", 
                   routingGraph.isWayAccessibleForVehicle(weightRestrictedWay, "hgv", 8.0, testTime));
        
        // Test with light vehicle (<=7.5t)
        assertTrue("Light HGV should be accessible", 
                  routingGraph.isWayAccessibleForVehicle(weightRestrictedWay, "hgv", 3.5, testTime));
        
        // Test with car (weight irrelevant)
        assertTrue("Car should always be accessible", 
                  routingGraph.isWayAccessibleForVehicle(weightRestrictedWay, "car", 1.5, testTime));
    }
    
    @Test
    public void testComplexTimeRanges() throws ParseException {
        createTestNetwork();
        
        // Test overnight restriction (crosses midnight)
        Way overnightRestrictedWay = dataSet.getWays().iterator().next();
        overnightRestrictedWay.put("access:conditional", "no @ (22:00-06:00)");
        
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        ConditionalRestriction restriction = parser.parse("access:conditional", "no @ (22:00-06:00)");
        restriction.setWay(overnightRestrictedWay);
        restrictions.put(overnightRestrictedWay, Arrays.asList(restriction));
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        // Test during night hours
        LocalDateTime lateNight = LocalDateTime.of(2023, 10, 16, 23, 0);
        assertFalse("Should be restricted at 23:00", 
                   routingGraph.isWayAccessible(overnightRestrictedWay, "car", lateNight));
        
        LocalDateTime earlyMorning = LocalDateTime.of(2023, 10, 16, 5, 0);
        assertFalse("Should be restricted at 05:00", 
                   routingGraph.isWayAccessible(overnightRestrictedWay, "car", earlyMorning));
        
        // Test during day hours
        LocalDateTime afternoon = LocalDateTime.of(2023, 10, 16, 14, 0);
        assertTrue("Should be accessible at 14:00", 
                  routingGraph.isWayAccessible(overnightRestrictedWay, "car", afternoon));
    }
    
    @Test
    public void testGraphRouting() {
        createTestNetwork();
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        // Get nodes for routing test
        List<Node> nodes = new ArrayList<>(dataSet.getNodes());
        Node startNode = nodes.get(0);
        Node endNode = nodes.get(3);
        
        LocalDateTime testTime = LocalDateTime.of(2023, 10, 16, 10, 0);
        
        // Test basic routing
        List<Way> route = routingGraph.findRoute(startNode, endNode, "car", testTime);
        assertNotNull("Route should be found", route);
        assertFalse("Route should not be empty", route.isEmpty());
    }
    
    @Test
    public void testVehicleProfiles() {
        Set<String> supportedVehicles = routingGraph.getSupportedVehicleTypes();
        
        assertTrue("Should support car", supportedVehicles.contains("car"));
        assertTrue("Should support bicycle", supportedVehicles.contains("bicycle"));
        assertTrue("Should support foot", supportedVehicles.contains("foot"));
        assertTrue("Should support hgv", supportedVehicles.contains("hgv"));
        assertTrue("Should support motorcycle", supportedVehicles.contains("motorcycle"));
    }
    
    @Test
    public void testGraphStatistics() {
        createTestNetwork();
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        
        routingGraph.buildGraph(dataSet, restrictions);
        
        ConditionalRoutingGraph.GraphStatistics stats = routingGraph.getStatistics();
        
        assertNotNull("Statistics should not be null", stats);
        assertEquals("Should have correct node count", 4, stats.getNodeCount());
        assertEquals("Should have correct edge count", 4, stats.getEdgeCount());
        assertTrue("Build time should be positive", stats.getBuildTimeMs() >= 0);
    }
    
    @Test
    public void testEmptyDataSet() {
        DataSet emptyDataSet = new DataSet();
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        
        routingGraph.buildGraph(emptyDataSet, restrictions);
        
        assertEquals("Empty graph should have 0 nodes", 0, routingGraph.getNodeCount());
        assertEquals("Empty graph should have 0 edges", 0, routingGraph.getEdgeCount());
    }
    
    @Test
    public void testNullSafety() {
        // Test null parameters
        routingGraph.buildGraph(null, new HashMap<>());
        assertEquals("Null dataset should result in empty graph", 0, routingGraph.getNodeCount());
        
        createTestNetwork();
        routingGraph.buildGraph(dataSet, null);
        assertTrue("Null restrictions should still build basic graph", routingGraph.getNodeCount() > 0);
    }
    
    @Test
    public void testGraphClear() {
        createTestNetwork();
        Map<Way, List<ConditionalRestriction>> restrictions = new HashMap<>();
        
        routingGraph.buildGraph(dataSet, restrictions);
        assertTrue("Graph should have content", routingGraph.getNodeCount() > 0);
        
        routingGraph.clear();
        assertEquals("Cleared graph should be empty", 0, routingGraph.getNodeCount());
        assertEquals("Cleared graph should have no edges", 0, routingGraph.getEdgeCount());
    }
}